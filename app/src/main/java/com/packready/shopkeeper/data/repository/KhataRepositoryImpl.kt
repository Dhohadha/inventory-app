package com.packready.shopkeeper.data.repository

import com.packready.shopkeeper.data.local.dao.KhataDao
import com.packready.shopkeeper.data.local.dao.OutboxDao
import com.packready.shopkeeper.data.local.entity.KhataTransactionEntity
import com.packready.shopkeeper.data.local.entity.SyncOutboxEntity
import com.packready.shopkeeper.domain.model.KhataCustomer
import com.packready.shopkeeper.domain.model.KhataEntryType
import com.packready.shopkeeper.domain.model.KhataTransaction
import com.packready.shopkeeper.domain.repository.KhataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KhataRepositoryImpl @Inject constructor(
    private val khataDao: KhataDao,
    private val outboxDao: OutboxDao
) : KhataRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllCustomers(): Flow<List<KhataCustomer>> {
        return khataDao.getAllCustomersFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCustomer(phone: String): KhataCustomer? {
        return khataDao.getCustomerByPhone(phone)?.toDomain()
    }

    override fun searchCustomers(query: String): Flow<List<KhataCustomer>> {
        return khataDao.searchCustomersFlow(query.trim()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCustomerTransactions(phone: String): Flow<List<KhataTransaction>> {
        return khataDao.getTransactionsForCustomerFlow(phone).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordCreditSale(
        customerName: String,
        customerPhone: String,
        amount: Double,
        saleId: String
    ) {
        val txn = KhataTransactionEntity(
            id = UUID.randomUUID().toString(),
            customerPhone = customerPhone,
            amount = amount,
            entryType = KhataEntryType.DEBIT_PURCHASE.name,
            note = "Store Purchase (Credit)",
            relatedSaleId = saleId,
            timestamp = System.currentTimeMillis()
        )

        khataDao.recordKhataEntry(
            customerName = customerName,
            customerPhone = customerPhone,
            amountDelta = amount, // Dues increase
            transaction = txn
        )

        // Sync Outbox Event
        val payload = json.encodeToString(txn.toDomain())
        outboxDao.insertEvent(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                eventType = "KHATA_LEDGER_MUTATED",
                payloadJson = payload,
                status = "PENDING",
                retryCount = 0,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun recordPaymentReceived(
        customerPhone: String,
        customerName: String,
        amount: Double,
        note: String?
    ) {
        val txn = KhataTransactionEntity(
            id = UUID.randomUUID().toString(),
            customerPhone = customerPhone,
            amount = amount,
            entryType = KhataEntryType.CREDIT_PAYMENT.name,
            note = note ?: "Payment Received",
            relatedSaleId = null,
            timestamp = System.currentTimeMillis()
        )

        khataDao.recordKhataEntry(
            customerName = customerName,
            customerPhone = customerPhone,
            amountDelta = -amount, // Dues decrease
            transaction = txn
        )

        val payload = json.encodeToString(txn.toDomain())
        outboxDao.insertEvent(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                eventType = "KHATA_LEDGER_MUTATED",
                payloadJson = payload,
                status = "PENDING",
                retryCount = 0,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getTotalOutstanding(): Double {
        return khataDao.getTotalOutstandingDues() ?: 0.0
    }
}
