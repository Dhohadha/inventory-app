package com.packready.shopkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["angleTag"])
    ]
)
data class ProductEmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: String,
    val angleTag: String, // "FRONT", "BACK", "TOP", "DETAIL"
    val vector: FloatArray, // 512-D L2-normalized vector
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProductEmbeddingEntity
        if (id != other.id) return false
        if (productId != other.productId) return false
        if (angleTag != other.angleTag) return false
        if (!vector.contentEquals(other.vector)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + productId.hashCode()
        result = 31 * result + angleTag.hashCode()
        result = 31 * result + vector.contentHashCode()
        return result
    }
}
