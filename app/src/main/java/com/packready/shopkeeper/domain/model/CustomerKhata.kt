package com.packready.shopkeeper.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class KhataEntryType {
    DEBIT_PURCHASE,  // Customer bought items on credit (Dues Increase)
    CREDIT_PAYMENT   // Customer paid cash/UPI to settle dues (Dues Decrease)
}

@Serializable
data class KhataTransaction(
    val id: String,
    val customerPhone: String,
    val amount: Double,
    val entryType: KhataEntryType,
    val note: String? = null,
    val relatedSaleId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class KhataCustomer(
    val customerPhone: String,
    val customerName: String,
    val totalOutstanding: Double = 0.0,
    val lastTransactionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
