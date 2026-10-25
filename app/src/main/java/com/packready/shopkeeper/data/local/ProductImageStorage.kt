package com.packready.shopkeeper.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ProductImageStorage {
    private const val DIRECTORY_NAME = "product_images"

    fun getImageDirectory(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveBitmap(context: Context, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val dir = getImageDirectory(context)
        val maxDimension = 800
        val finalBmp = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
        } else {
            bitmap
        }
        val file = File(dir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp")
        FileOutputStream(file).use { out ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                finalBmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, 78, out)
            } else {
                @Suppress("DEPRECATION")
                finalBmp.compress(Bitmap.CompressFormat.WEBP, 78, out)
            }
        }
        if (finalBmp != bitmap) {
            finalBmp.recycle()
        }
        file.absolutePath
    }

    suspend fun saveUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = getImageDirectory(context)
            val file = File(dir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp")
            val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return@withContext null

            val maxDimension = 800
            val finalBmp = if (bmp.width > maxDimension || bmp.height > maxDimension) {
                val ratio = minOf(maxDimension.toFloat() / bmp.width, maxDimension.toFloat() / bmp.height)
                Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt().coerceAtLeast(1), (bmp.height * ratio).toInt().coerceAtLeast(1), true)
            } else {
                bmp
            }

            FileOutputStream(file).use { output ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    finalBmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, 78, output)
                } else {
                    @Suppress("DEPRECATION")
                    finalBmp.compress(Bitmap.CompressFormat.WEBP, 78, output)
                }
            }
            if (finalBmp != bmp) finalBmp.recycle()
            bmp.recycle()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downloads an external product photo (from Open Food Facts, Gemini, or Cloudinary),
     * scales it down to high-efficiency mobile thumbnail dimensions (max 512x512),
     * and saves it using WebP / compressed JPEG (quality 75) so it occupies MINIMAL disk space (typically 20-35 KB).
     * Returns the local file absolute path.
     */
    suspend fun saveRemoteUrlLowSpace(context: Context, imageUrl: String, barcode: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = imageUrl.trim()
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                return@withContext null
            }
            val dir = getImageDirectory(context)
            // Use hashed filename to prevent redundant re-downloads
            val hash = cleanUrl.hashCode().toString().replace("-", "n")
            val prefix = if (!barcode.isNullOrBlank()) "prod_${barcode.trim()}" else "ext"
            val targetFile = File(dir, "${prefix}_${hash}.webp")
            if (targetFile.exists() && targetFile.length() > 0) {
                return@withContext targetFile.absolutePath
            }

            val url = java.net.URL(cleanUrl)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.doInput = true
            conn.connect()
            if (conn.responseCode != 200) return@withContext null
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) return@withContext null

            // Compute downsample bounds
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val maxDimension = 512
            var inSampleSize = 1
            if (boundsOptions.outHeight > maxDimension || boundsOptions.outWidth > maxDimension) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // 50% less RAM than ARGB_8888
            }
            val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return@withContext null

            val finalBitmap = if (decodedBitmap.width > maxDimension || decodedBitmap.height > maxDimension) {
                val ratio = minOf(maxDimension.toFloat() / decodedBitmap.width, maxDimension.toFloat() / decodedBitmap.height)
                val targetW = (decodedBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decodedBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
            } else {
                decodedBitmap
            }

            FileOutputStream(targetFile).use { out ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, out)
                } else {
                    @Suppress("DEPRECATION")
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP, 75, out)
                }
            }

            if (finalBitmap != decodedBitmap) finalBitmap.recycle()
            decodedBitmap.recycle()

            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun loadThumbnail(path: String, reqWidth: Int = 300, reqHeight: Int = 300): Bitmap? {
        return try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                val url = java.net.URL(path)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 10000
                conn.doInput = true
                conn.connect()
                val bytes = conn.inputStream.use { it.readBytes() }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                var inSampleSize = 1
                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                        inSampleSize *= 2
                    }
                }
                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            } else {
                val file = File(path)
                if (!file.exists()) return null

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(path, options)

                var inSampleSize = 1
                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                BitmapFactory.decodeFile(path, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteImage(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }
}
