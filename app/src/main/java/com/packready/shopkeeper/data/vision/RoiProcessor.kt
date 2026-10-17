package com.packready.shopkeeper.data.vision

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Utility for applying safety margin padding, extracting upright bitmaps,
 * cropping detected object ROIs, and populating direct Float32 buffers for neural inference.
 */
object RoiProcessor {

    const val DEFAULT_INPUT_SIZE = 224
    private const val SAFETY_MARGIN_PADDING_RATIO = 0.05f

    /**
     * Converts a CameraX [ImageProxy] to an upright [Bitmap].
     */
    fun getUprightBitmap(imageProxy: ImageProxy): Bitmap {
        val rawBitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )
            if (rotated != rawBitmap) {
                rawBitmap.recycle()
            }
            rotated
        } else {
            rawBitmap
        }
    }

    /**
     * Expands an ML Kit [Rect] bounding box by safety padding on each side.
     */
    fun applySafetyPadding(
        box: Rect,
        imageWidth: Int,
        imageHeight: Int,
        paddingRatio: Float = SAFETY_MARGIN_PADDING_RATIO
    ): RectF {
        val padX = box.width() * paddingRatio
        val padY = box.height() * paddingRatio

        val left = (box.left - padX).coerceIn(0f, imageWidth.toFloat())
        val top = (box.top - padY).coerceIn(0f, imageHeight.toFloat())
        val right = (box.right + padX).coerceIn(left + 1f, imageWidth.toFloat())
        val bottom = (box.bottom + padY).coerceIn(top + 1f, imageHeight.toFloat())

        return RectF(left, top, right, bottom)
    }

    fun applySafetyPadding(
        box: RectF,
        imageWidth: Int,
        imageHeight: Int,
        paddingRatio: Float = SAFETY_MARGIN_PADDING_RATIO
    ): RectF {
        val padX = box.width() * paddingRatio
        val padY = box.height() * paddingRatio

        val left = (box.left - padX).coerceIn(0f, imageWidth.toFloat())
        val top = (box.top - padY).coerceIn(0f, imageHeight.toFloat())
        val right = (box.right + padX).coerceIn(left + 1f, imageWidth.toFloat())
        val bottom = (box.bottom + padY).coerceIn(top + 1f, imageHeight.toFloat())

        return RectF(left, top, right, bottom)
    }

    /**
     * Crops the ROI from [source] based on [paddedBox] at native source resolution.
     */
    fun cropRoi(source: Bitmap, paddedBox: RectF): Bitmap? {
        val srcW = source.width
        val srcH = source.height

        val left = paddedBox.left.toInt().coerceIn(0, srcW - 1)
        val top = paddedBox.top.toInt().coerceIn(0, srcH - 1)
        val right = paddedBox.right.toInt().coerceIn(left + 1, srcW)
        val bottom = paddedBox.bottom.toInt().coerceIn(top + 1, srcH)

        val cropW = right - left
        val cropH = bottom - top

        if (cropW <= 0 || cropH <= 0) return null
        return Bitmap.createBitmap(source, left, top, cropW, cropH)
    }

    /**
     * Crops the ROI from [source] and scales directly to target dimensions.
     */
    fun cropAndScaleRoi(
        source: Bitmap,
        paddedBox: RectF,
        targetWidth: Int = DEFAULT_INPUT_SIZE,
        targetHeight: Int = DEFAULT_INPUT_SIZE
    ): Bitmap {
        val srcW = source.width
        val srcH = source.height

        val left = paddedBox.left.toInt().coerceIn(0, srcW - 1)
        val top = paddedBox.top.toInt().coerceIn(0, srcH - 1)
        val right = paddedBox.right.toInt().coerceIn(left + 1, srcW)
        val bottom = paddedBox.bottom.toInt().coerceIn(top + 1, srcH)

        val cropW = right - left
        val cropH = bottom - top

        if (cropW <= 0 || cropH <= 0) {
            return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        }

        val cropped = Bitmap.createBitmap(source, left, top, cropW, cropH)
        val scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
        if (scaled != cropped) {
            cropped.recycle()
        }
        return scaled
    }

    /**
     * Checks if a [Bitmap] represents a blank wall, finger over lens, pitch-black shadow, or uniform flat texture.
     */
    fun isBlankOrUniform(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return true

        val sampleW = 40.coerceAtMost(w)
        val sampleH = 40.coerceAtMost(h)
        val stepX = (w.toFloat() / sampleW).coerceAtLeast(1f)
        val stepY = (h.toFloat() / sampleH).coerceAtLeast(1f)

        var sum = 0.0
        var nonBlackCount = 0
        val total = sampleW * sampleH
        val pixels = FloatArray(total)

        var idx = 0
        for (y in 0 until sampleH) {
            val py = (y * stepY).toInt().coerceIn(0, h - 1)
            for (x in 0 until sampleW) {
                val px = (x * stepX).toInt().coerceIn(0, w - 1)
                val color = bitmap.getPixel(px, py)
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                pixels[idx++] = lum
                sum += lum
                if (lum > 18f) nonBlackCount++
            }
        }

        if (nonBlackCount.toFloat() / total < 0.06f) return true

        val mean = sum / total
        if (mean < 14.0 || mean > 248.0) return true

        var varSum = 0.0
        for (lum in pixels) {
            val d = lum - mean
            varSum += d * d
        }
        val stdDev = sqrt(varSum / total)
        return stdDev < 7.0
    }

    /**
     * Segments and isolates the product object from packaging/counter background using ML Kit Object Detection.
     * Applies safety margin padding and crops the ROI. If no discrete object is detected or crop is blank,
     * gracefully returns the source bitmap.
     */
    fun segmentProductObject(source: Bitmap): Bitmap {
        return try {
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .build()
            val detector = ObjectDetection.getClient(options)
            val inputImage = InputImage.fromBitmap(source, 0)
            val detectedObjects = Tasks.await(detector.process(inputImage), 1200L, TimeUnit.MILLISECONDS)

            if (detectedObjects.isNotEmpty()) {
                // Find most salient object (largest bounding box area)
                val primaryObject = detectedObjects.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                if (primaryObject != null && primaryObject.boundingBox.width() > 40 && primaryObject.boundingBox.height() > 40) {
                    val paddedBox = applySafetyPadding(
                        primaryObject.boundingBox,
                        source.width,
                        source.height,
                        paddingRatio = 0.05f
                    )
                    val crop = cropRoi(source, paddedBox)
                    if (crop != null && !isBlankOrUniform(crop)) {
                        return crop
                    }
                }
            }
            source
        } catch (_: Exception) {
            source
        }
    }
}
