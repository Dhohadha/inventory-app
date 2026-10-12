package com.packready.shopkeeper.data.repository

import com.packready.shopkeeper.data.local.dao.InvoiceDao
import com.packready.shopkeeper.data.local.dao.OutboxDao
import com.packready.shopkeeper.data.local.dao.ProductDao
import com.packready.shopkeeper.data.local.entity.InvoiceDocumentEntity
import com.packready.shopkeeper.data.local.entity.InvoiceLineItemEntity
import com.packready.shopkeeper.data.local.entity.ProductEntity
import com.packready.shopkeeper.data.local.entity.SyncOutboxEntity
import com.packready.shopkeeper.domain.model.InvoiceDocument
import com.packready.shopkeeper.domain.model.InvoiceLineItem
import com.packready.shopkeeper.domain.model.InvoiceStatus
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepositoryImpl @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao,
    private val outboxDao: OutboxDao
) : InvoiceRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllInvoices(): Flow<List<InvoiceDocument>> {
        return invoiceDao.getAllInvoicesFlow().map { list ->
            list.map { docEntity ->
                val items = invoiceDao.getLineItemsForInvoice(docEntity.id).map { it.toDomain() }
                docEntity.toDomain(items)
            }
        }
    }

    override suspend fun getInvoiceById(id: String): InvoiceDocument? {
        val docEntity = invoiceDao.getInvoiceById(id) ?: return null
        val items = invoiceDao.getLineItemsForInvoice(id).map { it.toDomain() }
        return docEntity.toDomain(items)
    }

    override suspend fun saveInvoice(invoice: InvoiceDocument) {
        val docEntity = InvoiceDocumentEntity.fromDomain(invoice)
        val itemEntities = invoice.items.map { InvoiceLineItemEntity.fromDomain(it, invoice.id) }
        invoiceDao.saveFullInvoice(docEntity, itemEntities)
    }

    override suspend fun updateLineItem(item: InvoiceLineItem, invoiceId: String) {
        invoiceDao.updateLineItem(InvoiceLineItemEntity.fromDomain(item, invoiceId))
    }

    override suspend fun reconcileInvoiceAndApplyStock(invoiceId: String) {
        val invoice = getInvoiceById(invoiceId) ?: return
        if (invoice.status == InvoiceStatus.RECONCILED) return

        for (item in invoice.items) {
            if (item.isConfirmed) {
                // If matched to an existing product
                if (!item.matchedProductId.isNullOrBlank()) {
                    val product = productDao.getProductById(item.matchedProductId)
                    if (product != null) {
                        // Update stock and cost price
                        productDao.adjustStock(product.id, item.quantity)
                        if (item.purchaseRate > 0) {
                            val updated = product.copy(
                                costPrice = item.purchaseRate,
                                sellingPrice = if (item.sellingPrice > 0) item.sellingPrice else product.sellingPrice,
                                lastRestockedAt = System.currentTimeMillis()
                            )
                            productDao.update(updated)
                        }
                    }
                } else if (!item.barcode.isNullOrBlank()) {
                    // Check by barcode
                    val existing = productDao.getProductByBarcode(item.barcode)
                    if (existing != null) {
                        productDao.adjustStock(existing.id, item.quantity)
                    } else {
                        // Create brand new product
                        val newProduct = Product(
                            id = item.barcode,
                            barcode = item.barcode,
                            name = item.matchedProductName.ifBlank { item.rawExtractedText },
                            brand = invoice.vendorName,
                            category = ProductCategory.FOOD_GROCERY,
                            subCategory = "Restocked",
                            mrp = item.sellingPrice.coerceAtLeast(item.purchaseRate * 1.2),
                            costPrice = item.purchaseRate,
                            sellingPrice = item.sellingPrice.coerceAtLeast(item.purchaseRate * 1.15),
                            currentStock = item.quantity,
                            unit = item.unit,
                            lastRestockedAt = System.currentTimeMillis()
                        )
                        productDao.insertOrUpdate(ProductEntity.fromDomain(newProduct))
                    }
                }
            }
        }

        invoiceDao.updateInvoiceStatus(invoiceId, InvoiceStatus.RECONCILED.name)

        // Queue outbox sync
        val payload = json.encodeToString(invoice.copy(status = InvoiceStatus.RECONCILED))
        outboxDao.insertEvent(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                eventType = "INVOICE_RECONCILED",
                payloadJson = payload,
                status = "PENDING",
                retryCount = 0,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteInvoice(id: String) {
        invoiceDao.deleteInvoice(id)
    }
}
