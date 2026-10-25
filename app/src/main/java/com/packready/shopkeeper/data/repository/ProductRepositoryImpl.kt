package com.packready.shopkeeper.data.repository

import com.packready.shopkeeper.data.local.InitialCatalogData
import com.packready.shopkeeper.data.local.MasterCatalogDataSource
import com.packready.shopkeeper.data.local.dao.EmbeddingDao
import com.packready.shopkeeper.data.local.dao.ProductDao
import com.packready.shopkeeper.data.local.entity.ProductEmbeddingEntity
import com.packready.shopkeeper.data.local.entity.ProductEntity
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val embeddingDao: EmbeddingDao,
    private val masterCatalogDataSource: MasterCatalogDataSource,
    private val catalogApiService: com.packready.shopkeeper.data.remote.CatalogApiService,
    private val sessionManager: com.packready.shopkeeper.data.local.UserSessionManager
) : ProductRepository {

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val sampleBarcodes = InitialCatalogData.sampleProducts.map { it.barcode }
                productDao.deleteByBarcodes(sampleBarcodes)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

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
        // 1. Check local store inventory in Room SQLite
        val local = productDao.getProductByBarcode(barcode)
        if (local != null) return local.toDomain()

        // 2. Query Central Master Catalog Server (FastAPI + Redis)
        try {
            val response = catalogApiService.getProductByBarcode(barcode)
            if (response.isSuccessful) {
                val serverProduct = response.body()?.toDomain()
                if (serverProduct != null) {
                    saveProduct(serverProduct)
                    return serverProduct
                }
            }
        } catch (e: Exception) {
            // Server offline or network unreachable, fall back to offline catalog
        }

        // 3. Fallback: check offline 10,000 FMCG master catalog
        val fallback = lookupGlobalCatalog(barcode)
        if (fallback != null) {
            // Auto-cache into local DB so subsequent scans hit local store inventory directly
            saveProduct(fallback)
            return fallback
        }
        return null
    }

    override suspend fun saveProduct(product: Product) {
        productDao.insertOrUpdate(ProductEntity.fromDomain(product))
        try {
            val storeId = sessionManager.getActiveStoreId()
            val primaryImg = product.images.firstOrNull()
            catalogApiService.enrollProduct(
                com.packready.shopkeeper.data.remote.dto.ProductEnrollRequestDto(
                    barcode = product.barcode,
                    name = product.name,
                    brand = product.brand,
                    category = product.category.displayName,
                    standardMrp = product.mrp,
                    sellingPrice = product.sellingPrice,
                    costPrice = product.costPrice,
                    unit = product.unit,
                    imageUrl = primaryImg,
                    storeId = storeId
                )
            )
        } catch (_: Exception) {
            // Local store retains item safely even if offline
        }
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
        return masterCatalogDataSource.lookupByBarcode(barcode)
    }

    override suspend fun searchGlobalCatalog(query: String, limit: Int): List<Product> {
        return masterCatalogDataSource.searchMasterCatalog(query, limit)
    }

    override suspend fun getGlobalCatalogCount(): Int {
        return masterCatalogDataSource.getMasterCatalogCount()
    }

    override suspend fun saveProductEmbeddings(productId: String, embeddings: List<Pair<String, FloatArray>>) {
        val entities = embeddings.map { (angle, vector) ->
            ProductEmbeddingEntity(
                productId = productId,
                angleTag = angle,
                vector = vector
            )
        }
        embeddingDao.insertAll(entities)
    }

    override suspend fun getProductEmbeddings(productId: String): List<Pair<String, FloatArray>> {
        return embeddingDao.getEmbeddingsForProduct(productId).map {
            Pair(it.angleTag, it.vector)
        }
    }
}
