package com.packready.shopkeeper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ParsedSlipItem(
    val rawText: String,
    val matchedProduct: Product? = null,
    val suggestedQuantity: Double = 1.0,
    val unit: String = "pcs",
    val confidence: Float = 1.0f,
    val isSelected: Boolean = true
)

@Serializable
data class SlipReconciliation(
    val rawImageUri: String? = null,
    val parsedItems: List<ParsedSlipItem> = emptyList(),
    val extractedTimestamp: Long = System.currentTimeMillis()
)
