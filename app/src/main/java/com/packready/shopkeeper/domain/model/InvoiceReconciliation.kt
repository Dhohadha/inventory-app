package com.packready.shopkeeper.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class InvoiceStatus {
    DRAFT,
    PROCESSED,
    RECONCILED
}

@Serializable
data class InvoiceLineItem(
    val id: String,
    val rawExtractedText: String,
    val matchedProductId: String? = null,
    val matchedProductName: String,
    val barcode: String? = null,
    val quantity: Double,
    val unit: String = "pcs",
    val purchaseRate: Double,
    val sellingPrice: Double,
    val totalAmount: Double,
    val isConfirmed: Boolean = true
)

@Serializable
data class InvoiceDocument(
    val id: String,
    val vendorName: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val totalAmount: Double,
    val items: List<InvoiceLineItem>,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis()
)
