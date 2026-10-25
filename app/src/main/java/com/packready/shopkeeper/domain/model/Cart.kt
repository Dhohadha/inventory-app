package com.packready.shopkeeper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val customUnitPrice: Double? = null,
    val discount: Double = 0.0
) {
    val effectiveUnitPrice: Double
        get() = customUnitPrice ?: product.sellingPrice

    val total: Double
        get() = ((effectiveUnitPrice * quantity) - discount).coerceAtLeast(0.0)

    val itemCostTotal: Double
        get() = product.costPrice * quantity

    val itemMargin: Double
        get() = total - itemCostTotal
}

@Serializable
data class Cart(
    val items: List<CartItem> = emptyList(),
    val overallDiscountAmount: Double = 0.0,
    val customerName: String? = null,
    val customerPhone: String? = null
) {
    val subtotal: Double
        get() = items.sumOf { it.total }

    val itemLevelDiscounts: Double
        get() = items.sumOf { it.discount }

    val totalDiscount: Double
        get() = itemLevelDiscounts + overallDiscountAmount

    val taxAmount: Double
        get() = items.sumOf { item ->
            val total = item.total
            if (item.product.gstPercent > 0.0) {
                total - (total / (1.0 + item.product.gstPercent / 100.0))
            } else 0.0
        }

    val grandTotal: Double
        get() = (subtotal - overallDiscountAmount).coerceAtLeast(0.0)

    val totalUnitsCount: Double
        get() = items.sumOf { it.quantity }

    val distinctItemsCount: Int
        get() = items.size

    val estimatedProfit: Double
        get() = items.sumOf { it.itemMargin } - overallDiscountAmount

    fun addItem(product: Product, quantityDelta: Double = 1.0): Cart {
        val existingIndex = items.indexOfFirst { it.product.id == product.id }
        return if (existingIndex >= 0) {
            val existing = items[existingIndex]
            val newQty = existing.quantity + quantityDelta
            val updatedItems = items.toMutableList()
            if (newQty <= 0.0) {
                updatedItems.removeAt(existingIndex)
            } else {
                updatedItems[existingIndex] = existing.copy(quantity = newQty)
            }
            copy(items = updatedItems)
        } else {
            if (quantityDelta > 0.0) {
                copy(items = items + CartItem(product = product, quantity = quantityDelta))
            } else {
                this
            }
        }
    }

    fun updateQuantity(productId: String, newQuantity: Double): Cart {
        if (newQuantity <= 0.0) {
            return copy(items = items.filterNot { it.product.id == productId })
        }
        val updated = items.map { item ->
            if (item.product.id == productId) item.copy(quantity = newQuantity) else item
        }
        return copy(items = updated)
    }

    fun removeItem(productId: String): Cart {
        return copy(items = items.filterNot { it.product.id == productId })
    }

    fun clear(): Cart = Cart()
}
