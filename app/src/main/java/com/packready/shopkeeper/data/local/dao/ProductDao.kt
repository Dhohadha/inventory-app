package com.packready.shopkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.packready.shopkeeper.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY salesCount DESC LIMIT :limit")
    fun getTopSellingProductsFlow(limit: Int = 20): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE currentStock <= minStockAlert ORDER BY currentStock ASC")
    fun getLowStockProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategoryFlow(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id OR barcode = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("""
        SELECT * FROM products 
        WHERE name LIKE '%' || :query || '%' 
           OR brand LIKE '%' || :query || '%' 
           OR barcode LIKE '%' || :query || '%' 
           OR subCategory LIKE '%' || :query || '%'
        ORDER BY salesCount DESC, name ASC
    """)
    fun searchProductsFlow(query: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE name LIKE '%' || :query || '%' 
           OR brand LIKE '%' || :query || '%' 
           OR barcode LIKE '%' || :query || '%'
        ORDER BY salesCount DESC, name ASC
    """)
    suspend fun searchProductsSync(query: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET currentStock = currentStock + :delta, lastRestockedAt = :timestamp WHERE id = :id")
    suspend fun adjustStock(id: String, delta: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET salesCount = salesCount + :count WHERE id = :id")
    suspend fun incrementSalesCount(id: String, count: Int = 1)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}
