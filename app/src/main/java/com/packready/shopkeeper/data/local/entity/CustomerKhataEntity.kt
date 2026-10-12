package com.packready.shopkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.packready.shopkeeper.domain.model.KhataCustomer
import com.packready.shopkeeper.domain.model.KhataEntryType
import com.packready.shopkeeper.domain.model.KhataTransaction

@Entity(
    tableName = "khata_customers",
    indices = [
        Index(value = ["customerPhone"], unique = true),
        Index(value = ["customerName"])
    ]
)
data class KhataCustomerEntity(
    @PrimaryKey val customerPhone: String,
    val customerName: String,
    val totalOutstanding: Double = 0.0,
    val lastTransactionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): KhataCustomer {
        return KhataCustomer(
            customerPhone = customerPhone,
            customerName = customerName,
            totalOutstanding = totalOutstanding,
            lastTransactionAt = lastTransactionAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(customer: KhataCustomer): KhataCustomerEntity {
            return KhataCustomerEntity(
                customerPhone = customer.customerPhone,
                customerName = customer.customerName,
                totalOutstanding = customer.totalOutstanding,
                lastTransactionAt = customer.lastTransactionAt,
                createdAt = customer.createdAt
            )
        }
    }
}

@Entity(
    tableName = "khata_transactions",
    indices = [
        Index(value = ["customerPhone"]),
        Index(value = ["timestamp"])
    ]
)
data class KhataTransactionEntity(
    @PrimaryKey val id: String,
    val customerPhone: String,
    val amount: Double,
    val entryType: String, // DEBIT_PURCHASE, CREDIT_PAYMENT
    val note: String? = null,
    val relatedSaleId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): KhataTransaction {
        val parsedType = try {
            KhataEntryType.valueOf(entryType)
        } catch (e: Exception) {
            KhataEntryType.DEBIT_PURCHASE
        }

        return KhataTransaction(
            id = id,
            customerPhone = customerPhone,
            amount = amount,
            entryType = parsedType,
            note = note,
            relatedSaleId = relatedSaleId,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(transaction: KhataTransaction): KhataTransactionEntity {
            return KhataTransactionEntity(
                id = transaction.id,
                customerPhone = transaction.customerPhone,
                amount = transaction.amount,
                entryType = transaction.entryType.name,
                note = transaction.note,
                relatedSaleId = transaction.relatedSaleId,
                timestamp = transaction.timestamp
            )
        }
    }
}
