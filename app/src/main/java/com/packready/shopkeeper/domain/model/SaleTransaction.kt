package com.packready.shopkeeper.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI QR"),
    KHATA("Khata / Udhar"),
    CARD("Card / POS")
}

@Serializable
data class SaleItem(
    val id: Long = 0,
    val transactionId: String,
    val productId: String,
    val productName: String,
    val barcode: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val costPrice: Double,
    val discount: Double = 0.0,
    val total: Double
)

@Serializable
data class SaleTransaction(
    val id: String,
    val invoiceNumber: String,
    val items: List<SaleItem>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val paymentMethod: PaymentMethod,
    val cashReceived: Double = 0.0,
    val changeReturned: Double = 0.0,
    val upiTransactionRef: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalProfit: Double
        get() = items.sumOf { (it.unitPrice - it.costPrice) * it.quantity } - discountAmount
}
