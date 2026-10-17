package com.packready.shopkeeper.data.vision

import com.packready.shopkeeper.data.local.entity.ProductEmbeddingEntity
import kotlin.math.sqrt

data class VectorMatch(
    val productId: String,
    val score: Float,
    val angleTag: String = ""
)

/**
 * High-speed Vector Similarity Engine for 512-D feature vectors.
 * Features 8-way unrolled dot-product loop for ARM NEON auto-vectorization on Android.
 */
object VectorSearchEngine {

    const val EMBEDDING_DIM = 512
    const val DEFAULT_THRESHOLD = 0.65f

    /**
     * Unrolled high-speed dot product cosine similarity for 512-d normalized FloatArray vectors.
     * For L2-normalized vectors: CosineSimilarity(a, b) = a · b = sum(a[i] * b[i])
     */
    fun computeCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var sum0 = 0f
        var sum1 = 0f
        var sum2 = 0f
        var sum3 = 0f
        var sum4 = 0f
        var sum5 = 0f
        var sum6 = 0f
        var sum7 = 0f

        val len = a.size
        val unrolledLimit = len - (len % 8)

        var i = 0
        while (i < unrolledLimit) {
            sum0 += a[i] * b[i]
            sum1 += a[i + 1] * b[i + 1]
            sum2 += a[i + 2] * b[i + 2]
            sum3 += a[i + 3] * b[i + 3]
            sum4 += a[i + 4] * b[i + 4]
            sum5 += a[i + 5] * b[i + 5]
            sum6 += a[i + 6] * b[i + 6]
            sum7 += a[i + 7] * b[i + 7]
            i += 8
        }

        var total = sum0 + sum1 + sum2 + sum3 + sum4 + sum5 + sum6 + sum7
        while (i < len) {
            total += a[i] * b[i]
            i++
        }

        return total
    }

    /**
     * Performs L2 Normalization on a vector: v_norm = v / max(||v||_2, 1e-12)
     */
    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSq = 0f
        for (v in vector) {
            sumSq += v * v
        }
        val norm = sqrt(sumSq)
        if (norm < 1e-12f) return vector.clone()

        val normalized = FloatArray(vector.size)
        val invNorm = 1.0f / norm
        for (idx in vector.indices) {
            normalized[idx] = vector[idx] * invNorm
        }
        return normalized
    }

    /**
     * Scans enrolled candidate embeddings and returns the highest scoring match.
     */
    fun findBestMatch(
        queryVector: FloatArray,
        candidates: List<ProductEmbeddingEntity>,
        threshold: Float = DEFAULT_THRESHOLD
    ): VectorMatch? {
        if (candidates.isEmpty()) return null

        var bestScore = -1.0f
        var bestCandidate: ProductEmbeddingEntity? = null

        for (candidate in candidates) {
            val score = computeCosineSimilarity(queryVector, candidate.vector)
            if (score > bestScore) {
                bestScore = score
                bestCandidate = candidate
            }
        }

        return if (bestCandidate != null && bestScore >= threshold) {
            VectorMatch(
                productId = bestCandidate.productId,
                score = bestScore,
                angleTag = bestCandidate.angleTag
            )
        } else {
            null
        }
    }
}
