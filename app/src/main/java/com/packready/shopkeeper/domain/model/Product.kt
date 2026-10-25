package com.packready.shopkeeper.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProductCategory(val displayName: String) {
    FOOD_GROCERY("Food & Grocery"),
    SNACKS_PACKAGED("Snacks & Packaged Food"),
    BEVERAGES("Beverages"),
    DAIRY_REFRIGERATED("Dairy & Refrigerated"),
    LOOSE_STAPLES("Loose Staples"),
    SPICES_MASALAS("Spices & Masalas"),
    INSTANT_FOOD("Instant & Ready Food"),
    PERSONAL_CARE("Personal Care & Hygiene"),
    HOUSEHOLD_CLEANING("Cleaning & Household"),
    OTC_WELLNESS("Pharmacy & Wellness"),
    ELECTRONICS_ELECTRICAL("Electronics & Electrical"),
    STATIONERY_OFFICE("Stationery & Office"),
    BABY_CARE("Baby Care"),
    PET_CARE("Pet Care"),
    APPAREL_FOOTWEAR("Apparel & Footwear"),
    OTHER("Other")
}

@Serializable
data class DynamicAttributes(
    // Food / Grocery / Beverage
    val netWeight: String? = null,
    val isVegetarian: Boolean? = true,
    val bestBeforeMonths: Int? = null,
    val expiryDate: String? = null,
    val mfgDate: String? = null,
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
    val images: List<String> = emptyList(),
    val lastRestockedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = currentStock <= (if (minStockAlert <= 0.0) 5.0 else minStockAlert)

    val marginPercent: Double
        get() = if (sellingPrice > 0) ((sellingPrice - costPrice) / sellingPrice) * 100.0 else 0.0

    val isLooseItem: Boolean
        get() = barcode.startsWith("PK-LOOSE-") || category == ProductCategory.LOOSE_STAPLES
}
