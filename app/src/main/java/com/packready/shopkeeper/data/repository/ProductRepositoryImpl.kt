package com.packready.shopkeeper.data.repository

import com.packready.shopkeeper.data.local.InitialCatalogData
import com.packready.shopkeeper.data.local.dao.ProductDao
import com.packready.shopkeeper.data.local.entity.ProductEntity
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProductsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTopSellingProducts(limit: Int): Flow<List<Product>> {
        return productDao.getTopSellingProductsFlow(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLowStockProducts(): Flow<List<Product>> {
        return productDao.getLowStockProductsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProductsByCategory(category: ProductCategory): Flow<List<Product>> {
        return productDao.getProductsByCategoryFlow(category.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProductsFlow(query.trim()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    override suspend fun getProductByBarcode(barcode: String): Product? {
        val local = productDao.getProductByBarcode(barcode)
        if (local != null) return local.toDomain()

        // Fallback: check global Kirana reference catalog
        val fallback = lookupGlobalCatalog(barcode)
        if (fallback != null) {
            // Auto-cache into local DB
            saveProduct(fallback)
            return fallback
        }
        return null
    }

    override suspend fun saveProduct(product: Product) {
        productDao.insertOrUpdate(ProductEntity.fromDomain(product))
    }

    override suspend fun adjustStock(productId: String, delta: Double) {
        productDao.adjustStock(productId, delta)
    }

    override suspend fun incrementSales(productId: String, count: Int) {
        productDao.incrementSalesCount(productId, count)
    }

    override suspend fun deleteProduct(productId: String) {
        productDao.deleteById(productId)
    }

    override suspend fun lookupGlobalCatalog(barcode: String): Product? {
        return InitialCatalogData.sampleProducts.find { it.barcode == barcode || it.id == barcode }
    }
}
