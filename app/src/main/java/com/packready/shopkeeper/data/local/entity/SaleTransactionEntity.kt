package com.packready.shopkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.model.SaleItem
import com.packready.shopkeeper.domain.model.SaleTransaction

@Entity(
    tableName = "sale_transactions",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["paymentMethod"])
    ]
)
data class SaleTransactionEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val paymentMethod: String, // CASH, UPI, KHATA, CARD
    val cashReceived: Double = 0.0,
    val changeReturned: Double = 0.0,
    val upiTransactionRef: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(items: List<SaleItem>): SaleTransaction {
        val method = try {
            PaymentMethod.valueOf(paymentMethod)
        } catch (e: Exception) {
            PaymentMethod.CASH
        }

        return SaleTransaction(
            id = id,
            invoiceNumber = invoiceNumber,
            items = items,
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            paymentMethod = method,
            cashReceived = cashReceived,
            changeReturned = changeReturned,
            upiTransactionRef = upiTransactionRef,
            customerName = customerName,
            customerPhone = customerPhone,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(sale: SaleTransaction): SaleTransactionEntity {
            return SaleTransactionEntity(
                id = sale.id,
                invoiceNumber = sale.invoiceNumber,
                subtotal = sale.subtotal,
                discountAmount = sale.discountAmount,
                taxAmount = sale.taxAmount,
                grandTotal = sale.grandTotal,
                paymentMethod = sale.paymentMethod.name,
                cashReceived = sale.cashReceived,
                changeReturned = sale.changeReturned,
                upiTransactionRef = sale.upiTransactionRef,
                customerName = sale.customerName,
                customerPhone = sale.customerPhone,
                timestamp = sale.timestamp
            )
        }
    }
}

@Entity(
    tableName = "sale_items",
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["productId"])
    ]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val productId: String,
    val productName: String,
    val barcode: String,
    val quantity: Double,
    val unit: String = "pcs",
    val unitPrice: Double,
    val costPrice: Double,
    val discount: Double = 0.0,
    val total: Double
) {
    fun toDomain(): SaleItem {
        return SaleItem(
            id = id,
            transactionId = transactionId,
            productId = productId,
            productName = productName,
            barcode = barcode,
            quantity = quantity,
            unit = unit,
            unitPrice = unitPrice,
            costPrice = costPrice,
            discount = discount,
            total = total
        )
    }

    companion object {
        fun fromDomain(item: SaleItem, txnId: String): SaleItemEntity {
            return SaleItemEntity(
                id = item.id,
                transactionId = txnId,
                productId = item.productId,
                productName = item.productName,
                barcode = item.barcode,
                quantity = item.quantity,
                unit = item.unit,
                unitPrice = item.unitPrice,
                costPrice = item.costPrice,
                discount = item.discount,
                total = item.total
            )
        }
    }
}

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val eventType: String, // SALE_CREATED, STOCK_UPDATED, KHATA_MUTATED, INVOICE_SAVED
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
