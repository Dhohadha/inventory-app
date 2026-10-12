package com.packready.shopkeeper.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceOcrResponseDto(
    val vendorName: String = "",
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val totalAmount: Double = 0.0,
    val items: List<InvoiceItemOcrDto> = emptyList()
)

@Serializable
data class InvoiceItemOcrDto(
    val description: String = "",
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val purchaseRate: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val barcode: String? = null
)

@Serializable
data class HandwrittenSlipResponseDto(
    val items: List<SlipItemOcrDto> = emptyList(),
    val notes: String? = null
)

@Serializable
data class SlipItemOcrDto(
    val rawText: String = "",
    val itemName: String = "",
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val confidence: Float = 1.0f
)
