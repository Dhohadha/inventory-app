package com.packready.shopkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.packready.shopkeeper.data.local.entity.ProductEmbeddingEntity

@Dao
interface EmbeddingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: ProductEmbeddingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<ProductEmbeddingEntity>)

    @Query("SELECT * FROM product_embeddings WHERE productId = :productId")
    suspend fun getEmbeddingsForProduct(productId: String): List<ProductEmbeddingEntity>

    @Query("SELECT * FROM product_embeddings")
    suspend fun getAllEmbeddings(): List<ProductEmbeddingEntity>

    @Query("DELETE FROM product_embeddings WHERE productId = :productId")
    suspend fun deleteForProduct(productId: String)

    @Query("SELECT COUNT(*) FROM product_embeddings")
    suspend fun getCount(): Int
}
