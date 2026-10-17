package com.packready.shopkeeper.domain.repository

import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getTopSellingProducts(limit: Int = 20): Flow<List<Product>>
    fun getLowStockProducts(): Flow<List<Product>>
    fun getProductsByCategory(category: ProductCategory): Flow<List<Product>>
    fun searchProducts(query: String): Flow<List<Product>>
    suspend fun getProductById(id: String): Product?
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun saveProduct(product: Product)
    suspend fun adjustStock(productId: String, delta: Double)
    suspend fun incrementSales(productId: String, count: Int = 1)
    suspend fun deleteProduct(productId: String)
    suspend fun lookupGlobalCatalog(barcode: String): Product?
    suspend fun searchGlobalCatalog(query: String, limit: Int = 25): List<Product>
    suspend fun getGlobalCatalogCount(): Int
    suspend fun saveProductEmbeddings(productId: String, embeddings: List<Pair<String, FloatArray>>)
    suspend fun getProductEmbeddings(productId: String): List<Pair<String, FloatArray>>
}
