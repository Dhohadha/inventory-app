package com.packready.shopkeeper.data.vision

import android.graphics.Bitmap
import java.io.Closeable

interface ProductEmbeddingEngine : Closeable {
    val modelName: String
    val embeddingDimension: Int
    val modelInputWidth: Int
    val modelInputHeight: Int

    fun extractEmbedding(bitmap: Bitmap): FloatArray
    fun computeCosineSimilarity(a: FloatArray, b: FloatArray): Float
}
