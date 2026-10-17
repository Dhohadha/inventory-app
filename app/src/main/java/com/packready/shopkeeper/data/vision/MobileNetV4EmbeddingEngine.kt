package com.packready.shopkeeper.data.vision

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * MobileNetV4 ArcFace 512-D INT8 Product Embedding Engine.
 * Extracts 512-dimensional metric feature vectors optimized for retail packaging.
 * Automatically accelerated via ONNX Runtime Android with multi-threaded CPU execution.
 */
class MobileNetV4EmbeddingEngine(
    private val context: Context,
    private val modelAssetName: String = "mobilenetv4_arcface_512_int8.onnx",
    private val numThreads: Int = 4
) : ProductEmbeddingEngine {

    companion object {
        private const val TAG = "MobileNetV4Engine"
        const val DEFAULT_INPUT_SIZE = 224
        const val EMBEDDING_DIM = 512

        // Standard ImageNet Normalization Constants
        val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    override val embeddingDimension: Int = EMBEDDING_DIM
    override val modelName: String = "MobileNetV4-ArcFace-512D-INT8"
    override val modelInputWidth: Int = DEFAULT_INPUT_SIZE
    override val modelInputHeight: Int = DEFAULT_INPUT_SIZE

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        try {
            val modelFile = getAssetFile(modelAssetName)
            Log.d(TAG, "Initializing ONNX model: $modelAssetName (${modelFile.length()} bytes)")
            ortEnv = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(numThreads)
                setInterOpNumThreads(numThreads)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            ortSession = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)
            Log.d(TAG, "ONNX Session created successfully! Inputs=${ortSession?.inputNames}, Outputs=${ortSession?.outputNames}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX session for $modelAssetName", e)
        }
    }

    private fun getAssetFile(assetName: String): File {
        val file = File(context.cacheDir, assetName)
        if (!file.exists() || file.length() == 0L) {
            try {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy asset $assetName: ${e.message}", e)
            }
        }
        return file
    }

    @Synchronized
    override fun extractEmbedding(bitmap: Bitmap): FloatArray {
        val resized = if (bitmap.width != modelInputWidth || bitmap.height != modelInputHeight) {
            Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, true)
        } else {
            bitmap
        }

        val totalPixels = modelInputWidth * modelInputHeight
        val intValues = IntArray(totalPixels)
        resized.getPixels(intValues, 0, modelInputWidth, 0, 0, modelInputWidth, modelInputHeight)
        if (resized != bitmap) resized.recycle()

        // Prepare planar float buffer [1, 3, H, W] with ImageNet normalization
        val floatBuffer = FloatBuffer.allocate(1 * 3 * totalPixels)
        val channelStride = totalPixels

        val rOffset = 0
        val gOffset = channelStride
        val bOffset = channelStride * 2

        val rawFloats = FloatArray(3 * totalPixels)

        for (i in 0 until totalPixels) {
            val pixel = intValues[i]
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            rawFloats[rOffset + i] = (r - MEAN[0]) / STD[0]
            rawFloats[gOffset + i] = (g - MEAN[1]) / STD[1]
            rawFloats[bOffset + i] = (b - MEAN[2]) / STD[2]
        }

        floatBuffer.put(rawFloats)
        floatBuffer.rewind()

        val env = ortEnv ?: run {
            Log.e(TAG, "ortEnv is null!")
            return FloatArray(embeddingDimension)
        }
        val session = ortSession ?: run {
            Log.e(TAG, "ortSession is null!")
            return FloatArray(embeddingDimension)
        }

        val inputShape = longArrayOf(1, 3, modelInputHeight.toLong(), modelInputWidth.toLong())
        val inputTensor = OnnxTensor.createTensor(env, floatBuffer, inputShape)

        return try {
            val results = session.run(mapOf(session.inputNames.iterator().next() to inputTensor))
            val outputTensor = (try { results.get("embedding").orElse(null) } catch (_: Exception) { null } ?: results[0]) as? OnnxTensor
            val outputBuffer = outputTensor?.floatBuffer
            val embedding = FloatArray(embeddingDimension)
            outputBuffer?.get(embedding)
            results.close()
            l2Normalize(embedding)
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}", e)
            FloatArray(embeddingDimension)
        } finally {
            inputTensor.close()
        }
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSq = 0f
        for (v in vector) sumSq += v * v
        val norm = sqrt(sumSq) + 1e-10f
        val invNorm = 1.0f / norm
        val normalized = FloatArray(vector.size)
        for (i in vector.indices) {
            normalized[i] = vector[i] * invNorm
        }
        return normalized
    }

    override fun computeCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        return VectorSearchEngine.computeCosineSimilarity(a, b)
    }

    override fun close() {
        try {
            ortSession?.close()
            ortEnv?.close()
        } catch (_: Exception) {}
    }
}
