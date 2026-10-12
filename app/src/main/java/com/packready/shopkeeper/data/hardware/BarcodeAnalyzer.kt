package com.packready.shopkeeper.data.hardware

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ConcurrentHashMap

/**
 * CameraX ImageAnalysis.Analyzer with a 1,400ms debouncing ring buffer.
 * Ensures continuous, hands-free desk scanning without duplicated reads.
 */
class BarcodeAnalyzer(
    private val debounceWindowMs: Long = 1400L,
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val lastScannedTimestamps = ConcurrentHashMap<String, Long>()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { rawBarcode ->
                            if (shouldProcessScan(rawBarcode)) {
                                onBarcodeDetected(rawBarcode)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignore transient frame analysis errors
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun shouldProcessScan(barcode: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastScannedTimestamps[barcode] ?: 0L
        return if (currentTime - lastTime > debounceWindowMs) {
            lastScannedTimestamps[barcode] = currentTime
            // Cleanup timestamps older than 10 seconds to prevent unbounded memory growth
            if (lastScannedTimestamps.size > 100) {
                val cutoff = currentTime - 10_000L
                lastScannedTimestamps.entries.removeIf { it.value < cutoff }
            }
            true
        } else {
            false
        }
    }

    fun resetDebounce(barcode: String? = null) {
        if (barcode != null) {
            lastScannedTimestamps.remove(barcode)
        } else {
            lastScannedTimestamps.clear()
        }
    }
}
