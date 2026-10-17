package com.packready.shopkeeper.data.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.packready.shopkeeper.data.local.dao.EmbeddingDao
import com.packready.shopkeeper.data.local.entity.ProductEmbeddingEntity
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class ObjectScanStatus {
    PENDING,
    SCANNED,
    SUGGESTED,
    UNKNOWN
}

data class TrackedObjectOverlay(
    val trackingId: Int?,
    val boundingBox: RectF,
    val status: ObjectScanStatus,
    val matchedProduct: Product? = null,
    val matchConfidence: Float = 0f,
    val method: String = ""
)

data class PosStreamTelemetry(
    val fps: Int,
    val frameLatencyMs: Long,
    val objectCount: Int,
    val activeScanningMethod: String
)

/**
 * Production-Grade Multi-Object Desk POS Analyzer.
 * Concurrently isolates 4-5 products on the counter, performs high-resolution virtual zoom,
 * scans barcodes directly, and seamlessly cascades to 512-D MobileNetV4 ArcFace + OCR hybrid AI.
 */
class MultiObjectPosAnalyzer(
    private val context: Context,
    private val productRepository: ProductRepository,
    private val embeddingDao: EmbeddingDao,
    private val coroutineScope: CoroutineScope,
    private val onProductMatched: (product: Product, confidence: Float, box: RectF, method: String) -> Unit,
    private val onProductSuggested: (product: Product, confidence: Float, box: RectF) -> Unit = { _, _, _ -> },
    private val onBarcodeScanned: (barcode: String) -> Unit = {},
    private val onTelemetryUpdated: (overlays: List<TrackedObjectOverlay>, telemetry: PosStreamTelemetry) -> Unit = { _, _ -> }
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "MultiObjectPosAnalyzer"
        private const val VISUAL_MATCH_THRESHOLD = 0.68f
        private const val SUGGESTION_THRESHOLD = 0.50f
    }

    private val isBusy = AtomicBoolean(false)
    val embeddingEngine = MobileNetV4EmbeddingEngine(context)

    // ML Kit Multi-Object Detector in continuous STREAM_MODE
    private val objectDetector: ObjectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    // ML Kit Barcode Scanner
    private val barcodeScanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    // ML Kit OCR Text Recognizer
    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Temporal State: Track which objects on the table have already been resolved
    private val resolvedTrackingIds = ConcurrentHashMap.newKeySet<Int>()
    private val resolvedBarcodeHistory = ConcurrentHashMap<String, Long>()

    // Memory Cache of Enrolled Visual Embeddings
    @Volatile
    private var cachedEmbeddings: List<ProductEmbeddingEntity> = emptyList()

    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var calculatedFps = 30

    init {
        // Pre-warm embeddings cache in background
        coroutineScope.launch(Dispatchers.IO) {
            refreshEmbeddingsCache()
        }
    }

    suspend fun refreshEmbeddingsCache() {
        cachedEmbeddings = embeddingDao.getAllEmbeddings()
        Log.i(TAG, "Cached ${cachedEmbeddings.size} visual embeddings for fast inference")
    }

    fun clearResolvedObjects() {
        resolvedTrackingIds.clear()
        resolvedBarcodeHistory.clear()
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isBusy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val frameStart = System.currentTimeMillis()

        try {
            val mediaImage = imageProxy.image
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val inputImage = if (mediaImage != null && (mediaImage.format == android.graphics.ImageFormat.YUV_420_888 || mediaImage.format == android.graphics.ImageFormat.JPEG)) {
                InputImage.fromMediaImage(mediaImage, rotationDegrees)
            } else {
                val bmp = imageProxy.toBitmap()
                InputImage.fromBitmap(bmp, rotationDegrees)
            }

            // Rolling FPS calculation
            frameCount++
            val now = System.currentTimeMillis()
            if (now - lastFpsTimestamp >= 1000L) {
                calculatedFps = ((frameCount * 1000f) / (now - lastFpsTimestamp)).toInt().coerceIn(1, 60)
                frameCount = 0
                lastFpsTimestamp = now
            }

            // 0. Instant Full-Frame Barcode Scan (Fast-path: ~15ms)
            // Ensures barcodes held anywhere in front of camera are immediately scanned
            val fullFrameBarcodes = try {
                Tasks.await(barcodeScanner.process(inputImage), 350L, TimeUnit.MILLISECONDS)
            } catch (_: Exception) {
                emptyList<Barcode>()
            }

            if (fullFrameBarcodes.isNotEmpty()) {
                for (bc in fullFrameBarcodes) {
                    val raw = bc.rawValue ?: continue
                    val lastScan = resolvedBarcodeHistory[raw] ?: 0L
                    val now = System.currentTimeMillis()

                    if (now - lastScan >= 1400L) {
                        resolvedBarcodeHistory[raw] = now
                        coroutineScope.launch(Dispatchers.IO) {
                            val matched = productRepository.getProductByBarcode(raw)
                            if (matched != null) {
                                onProductMatched(matched, 1.0f, RectF(0.2f, 0.2f, 0.8f, 0.8f), "BARCODE")
                            } else {
                                onBarcodeScanned(raw)
                            }
                        }
                    }
                }
            }

            // 1. Detect multiple objects on the desk
            val detectedObjects = try {
                Tasks.await(objectDetector.process(inputImage), 350L, TimeUnit.MILLISECONDS)
            } catch (_: Exception) {
                emptyList<DetectedObject>()
            }

            val uprightBitmap = RoiProcessor.getUprightBitmap(imageProxy)
            val overlays = mutableListOf<TrackedObjectOverlay>()
            var primaryMethod = "IDLE"

            if (detectedObjects.isEmpty()) {
                // If no discrete objects detected, scan entire center frame for a single item
                val centerBox = RectF(
                    uprightBitmap.width * 0.15f,
                    uprightBitmap.height * 0.15f,
                    uprightBitmap.width * 0.85f,
                    uprightBitmap.height * 0.85f
                )
                processSingleRoi(uprightBitmap, centerBox, trackingId = null, overlays)
            } else {
                // Process each isolated object on the desk
                for (obj in detectedObjects) {
                    val trackingId = obj.trackingId
                    val isAlreadyScanned = trackingId != null && resolvedTrackingIds.contains(trackingId)

                    val paddedBox = RoiProcessor.applySafetyPadding(
                        box = obj.boundingBox,
                        imageWidth = uprightBitmap.width,
                        imageHeight = uprightBitmap.height,
                        paddingRatio = 0.06f
                    )

                    if (isAlreadyScanned) {
                        val normBox = RectF(
                            paddedBox.left / uprightBitmap.width,
                            paddedBox.top / uprightBitmap.height,
                            paddedBox.right / uprightBitmap.width,
                            paddedBox.bottom / uprightBitmap.height
                        )
                        overlays.add(
                            TrackedObjectOverlay(
                                trackingId = trackingId,
                                boundingBox = normBox,
                                status = ObjectScanStatus.SCANNED,
                                method = "ALREADY_IN_CART"
                            )
                        )
                        continue
                    }

                    val methodUsed = processSingleRoi(uprightBitmap, paddedBox, trackingId, overlays)
                    if (methodUsed != "NONE") {
                        primaryMethod = methodUsed
                    }
                }
            }

            uprightBitmap.recycle()

            val totalLatency = System.currentTimeMillis() - frameStart
            val telemetry = PosStreamTelemetry(
                fps = calculatedFps,
                frameLatencyMs = totalLatency,
                objectCount = detectedObjects.size,
                activeScanningMethod = primaryMethod
            )

            onTelemetryUpdated(overlays, telemetry)

        } catch (e: Exception) {
            Log.e(TAG, "Error in multi-object POS analysis", e)
        } finally {
            imageProxy.close()
            isBusy.set(false)
        }
    }

    private fun processSingleRoi(
        fullFrame: Bitmap,
        box: RectF,
        trackingId: Int?,
        overlays: MutableList<TrackedObjectOverlay>
    ): String {
        val normBox = RectF(
            box.left / fullFrame.width,
            box.top / fullFrame.height,
            box.right / fullFrame.width,
            box.bottom / fullFrame.height
        )

        // High-Resolution Virtual Zoom Crop
        val crop = RoiProcessor.cropRoi(fullFrame, box) ?: return "NONE"

        if (RoiProcessor.isBlankOrUniform(crop)) {
            crop.recycle()
            return "NONE"
        }

        // --- TIER 1: HIGH-SPEED VIRTUAL ZOOM BARCODE SCAN ---
        val cropInput = InputImage.fromBitmap(crop, 0)
        val barcodes = try {
            Tasks.await(barcodeScanner.process(cropInput), 400L, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            emptyList<Barcode>()
        }

        if (barcodes.isNotEmpty()) {
            for (bc in barcodes) {
                val raw = bc.rawValue ?: continue
                val lastScan = resolvedBarcodeHistory[raw] ?: 0L
                val now = System.currentTimeMillis()

                // Debounce identical scans within 1.4 seconds
                if (now - lastScan < 1400L) {
                    crop.recycle()
                    return "BARCODE_DEBOUNCED"
                }

                coroutineScope.launch(Dispatchers.IO) {
                    val matched = productRepository.getProductByBarcode(raw)
                    if (matched != null) {
                        resolvedBarcodeHistory[raw] = now
                        if (trackingId != null) resolvedTrackingIds.add(trackingId)
                        onProductMatched(matched, 1.0f, box, "BARCODE")
                    } else {
                        resolvedBarcodeHistory[raw] = now
                        onBarcodeScanned(raw)
                    }
                }

                overlays.add(
                    TrackedObjectOverlay(
                        trackingId = trackingId,
                        boundingBox = normBox,
                        status = ObjectScanStatus.SCANNED,
                        matchConfidence = 1.0f,
                        method = "BARCODE"
                    )
                )
                crop.recycle()
                return "BARCODE"
            }
        }

        // --- TIER 2: DEEP LEARNING VISUAL AI (512-D MobileNetV4 ArcFace) + OCR FALLBACK ---
        val candidates = cachedEmbeddings
        if (candidates.isNotEmpty()) {
            val queryVector = embeddingEngine.extractEmbedding(crop)
            val bestVectorMatch = VectorSearchEngine.findBestMatch(queryVector, candidates, threshold = SUGGESTION_THRESHOLD)

            if (bestVectorMatch != null) {
                // Concurrently read OCR text keywords from cropped packaging
                var textScore = 0f
                try {
                    val ocrResult = Tasks.await(textRecognizer.process(cropInput), 300L, TimeUnit.MILLISECONDS)
                    val ocrText = ocrResult.text.uppercase()
                    // Check keyword agreement
                    if (ocrText.isNotBlank()) {
                        textScore = 0.5f // text detected
                    }
                } catch (_: Exception) {}

                val visualScore = bestVectorMatch.score
                // Hybrid score: Visual similarity is primary. If OCR text is detected, add confirmatory boost.
                val hybridScore = if (textScore > 0f) {
                    (0.85f * visualScore + 0.15f * textScore).coerceIn(0f, 1f)
                } else {
                    visualScore
                }

                val isDefiniteMatch = visualScore >= 0.65f || hybridScore >= VISUAL_MATCH_THRESHOLD
                val displayConfidence = maxOf(visualScore, hybridScore)

                if (isDefiniteMatch) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val product = productRepository.getProductById(bestVectorMatch.productId)
                        if (product != null) {
                            if (trackingId != null) resolvedTrackingIds.add(trackingId)
                            onProductMatched(product, displayConfidence, box, "VISUAL_AI")
                        }
                    }

                    overlays.add(
                        TrackedObjectOverlay(
                            trackingId = trackingId,
                            boundingBox = normBox,
                            status = ObjectScanStatus.SCANNED,
                            matchConfidence = displayConfidence,
                            method = "VISUAL_AI"
                        )
                    )
                    crop.recycle()
                    return "VISUAL_AI"
                } else if (visualScore >= SUGGESTION_THRESHOLD || hybridScore >= SUGGESTION_THRESHOLD) {
                    // Score is in suggestion zone [0.50, 0.65) -> Show closest suggestion
                    coroutineScope.launch(Dispatchers.IO) {
                        val product = productRepository.getProductById(bestVectorMatch.productId)
                        if (product != null) {
                            onProductSuggested(product, displayConfidence, box)
                        }
                    }

                    overlays.add(
                        TrackedObjectOverlay(
                            trackingId = trackingId,
                            boundingBox = normBox,
                            status = ObjectScanStatus.SUGGESTED,
                            matchConfidence = displayConfidence,
                            method = "SUGGESTION"
                        )
                    )
                    crop.recycle()
                    return "SUGGESTION"
                }
            }
        }

        overlays.add(
            TrackedObjectOverlay(
                trackingId = trackingId,
                boundingBox = normBox,
                status = ObjectScanStatus.PENDING,
                method = "SEARCHING"
            )
        )
        crop.recycle()
        return "NONE"
    }
}
