package com.packready.shopkeeper.data.repository

import com.packready.shopkeeper.data.local.dao.OutboxDao
import com.packready.shopkeeper.data.local.dao.ProductDao
import com.packready.shopkeeper.data.local.dao.SaleDao
import com.packready.shopkeeper.data.local.entity.SaleItemEntity
import com.packready.shopkeeper.data.local.entity.SaleTransactionEntity
import com.packready.shopkeeper.data.local.entity.SyncOutboxEntity
import com.packready.shopkeeper.domain.model.DailyAnalytics
import com.packready.shopkeeper.domain.model.PaymentBreakdown
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.model.SaleTransaction
import com.packready.shopkeeper.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val outboxDao: OutboxDao
) : SaleRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllTransactions(): Flow<List<SaleTransaction>> {
        return saleDao.getAllTransactionsFlow().map { entities ->
            entities.map { entity ->
                val items = saleDao.getItemsForTransaction(entity.id).map { it.toDomain() }
                entity.toDomain(items)
            }
        }
    }

    override suspend fun getTransactionById(id: String): SaleTransaction? {
        val entity = saleDao.getTransactionById(id) ?: return null
        val items = saleDao.getItemsForTransaction(id).map { it.toDomain() }
        return entity.toDomain(items)
    }

    override suspend fun recordSale(sale: SaleTransaction) {
        // 1. Save Transaction & Items
        val txnEntity = SaleTransactionEntity.fromDomain(sale)
        val itemEntities = sale.items.map { SaleItemEntity.fromDomain(it, sale.id) }
        saleDao.insertSale(txnEntity, itemEntities)

        // 2. Decrement Stock and Increment Popularity count for each product
        for (item in sale.items) {
            productDao.adjustStock(item.productId, -item.quantity)
            productDao.incrementSalesCount(item.productId, item.quantity.toInt().coerceAtLeast(1))
        }

        // 3. Queue to Sync Outbox for cloud synchronization
        val outboxPayload = json.encodeToString(sale)
        val outboxEvent = SyncOutboxEntity(
            id = UUID.randomUUID().toString(),
            eventType = "SALE_CREATED",
            payloadJson = outboxPayload,
            status = "PENDING",
            retryCount = 0,
            createdAt = System.currentTimeMillis()
        )
        outboxDao.insertEvent(outboxEvent)
    }

    override fun getTransactionsForDate(startTime: Long, endTime: Long): Flow<List<SaleTransaction>> {
        return saleDao.getTransactionsBetweenFlow(startTime, endTime).map { entities ->
            entities.map { entity ->
                val items = saleDao.getItemsForTransaction(entity.id).map { it.toDomain() }
                entity.toDomain(items)
            }
        }
    }

    override suspend fun getDailyAnalytics(dayStartTimestamp: Long, dayEndTimestamp: Long): DailyAnalytics {
        val transactions = saleDao.getTransactionsBetweenSync(dayStartTimestamp, dayEndTimestamp)
        val items = saleDao.getItemsBetweenDates(dayStartTimestamp, dayEndTimestamp)

        val totalRev = transactions.sumOf { it.grandTotal }
        val totalCost = items.sumOf { it.costPrice * it.quantity }
        val grossProfit = (totalRev - totalCost).coerceAtLeast(0.0)
        val margin = if (totalRev > 0) (grossProfit / totalRev) * 100.0 else 0.0

        var cashSum = 0.0
        var upiSum = 0.0
        var khataSum = 0.0
        var cardSum = 0.0

        for (tx in transactions) {
            when (tx.paymentMethod) {
                PaymentMethod.CASH.name -> cashSum += tx.grandTotal
                PaymentMethod.UPI.name -> upiSum += tx.grandTotal
                PaymentMethod.KHATA.name -> khataSum += tx.grandTotal
                PaymentMethod.CARD.name -> cardSum += tx.grandTotal
            }
        }

        val productSalesGrouped = items.groupBy { it.productName }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date(dayStartTimestamp))

        return DailyAnalytics(
            dateString = dateStr,
            totalRevenue = totalRev,
            totalCost = totalCost,
            grossProfit = grossProfit,
            profitMarginPercent = margin,
            totalTransactionsCount = transactions.size,
            totalUnitsSold = items.sumOf { it.quantity },
            paymentBreakdown = PaymentBreakdown(
                cashTotal = cashSum,
                upiTotal = upiSum,
                khataTotal = khataSum,
                cardTotal = cardSum
            ),
            topSellingProducts = productSalesGrouped
        )
    }
}
