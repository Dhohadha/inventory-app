package com.packready.shopkeeper.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProductCategory(val displayName: String) {
    FOOD_GROCERY("Food & Grocery"),
    BEVERAGES("Beverages"),
    PERSONAL_CARE("Personal Care & Hygiene"),
    HOUSEHOLD_CLEANING("Household & Cleaning"),
    ELECTRONICS_ELECTRICAL("Electronics & Electrical"),
    APPAREL_FOOTWEAR("Apparel & Footwear"),
    LOOSE_STAPLES("Loose Staples"),
    OTC_WELLNESS("OTC & Wellness"),
    OTHER("Other")
}

@Serializable
data class DynamicAttributes(
    // Food / Grocery / Beverage
    val netWeight: String? = null,
    val isVegetarian: Boolean? = true,
    val bestBeforeMonths: Int? = null,
    val packagingType: String? = null, // Can, Bottle, Tetra, Pouch, Box
    
    // Electronics
    val wattage: String? = null,
    val voltageRange: String? = null,
    val warrantyMonths: Int? = null,
    val pinCapType: String? = null, // B22, E27, Type-C

    // Personal / Household Care
    val fragrance: String? = null,
    val formFactor: String? = null, // Liquid, Powder, Bar, Gel

    // Apparel
    val size: String? = null, // S, M, L, XL, 32, 34
    val color: String? = null,
    val fabric: String? = null,

    // Loose Staples
    val grainGrade: String? = null, // Superfine, Desi, Raw, Boiled
    val origin: String? = null,

    // Wellness
    val dosageForm: String? = null, // Tablet, Syrup, Capsule
    val packCount: Int? = null
)

@Serializable
data class Product(
    val id: String,
    val barcode: String,
    val name: String,
    val brand: String,
    val category: ProductCategory,
    val subCategory: String,
    val mrp: Double,
    val costPrice: Double,
    val sellingPrice: Double,
    val currentStock: Double,
    val minStockAlert: Double = 5.0,
    val unit: String, // pcs, kg, g, litre, ml, pack, box
    val gstPercent: Double = 0.0,
    val salesCount: Int = 0,
    val attributes: DynamicAttributes? = null,
    val lastRestockedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = currentStock <= minStockAlert

    val marginPercent: Double
        get() = if (sellingPrice > 0) ((sellingPrice - costPrice) / sellingPrice) * 100.0 else 0.0

    val isLooseItem: Boolean
        get() = barcode.startsWith("PK-LOOSE-") || category == ProductCategory.LOOSE_STAPLES
}
