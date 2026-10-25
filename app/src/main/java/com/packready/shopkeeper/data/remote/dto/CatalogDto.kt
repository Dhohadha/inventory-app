package com.packready.shopkeeper.data.remote.dto

import com.packready.shopkeeper.domain.model.DynamicAttributes
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MasterProductResponseDto(
    val id: String,
    val barcode: String,
    val name: String,
    val brand: String,
    val category: String,
    @SerialName("sub_category") val subCategory: String? = null,
    @SerialName("standard_mrp") val standardMrp: Double = 0.0,
    val unit: String = "pcs",
    @SerialName("gst_percent") val gstPercent: Double = 0.0,
    val description: String? = null,
    @SerialName("dynamic_attributes") val dynamicAttributes: DynamicAttributes? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("popularity_score") val popularityScore: Int = 0,
    val images: List<ProductImageDto> = emptyList()
) {
    fun toDomain(): Product {
        val parsedCat = try {
            ProductCategory.valueOf(category)
        } catch (e: Exception) {
            ProductCategory.OTHER
        }

        return Product(
            id = id,
            barcode = barcode,
            name = name,
            brand = brand,
            category = parsedCat,
            subCategory = subCategory ?: "",
            mrp = standardMrp,
            costPrice = standardMrp * 0.85, // Default 15% estimated margin if cost price unknown
            sellingPrice = standardMrp,
            currentStock = 0.0,
            minStockAlert = 5.0,
            unit = unit,
            gstPercent = gstPercent,
            salesCount = 0,
            attributes = dynamicAttributes,
            images = images.map { it.secureUrl }
        )
    }
}

@Serializable
data class ProductImageDto(
    val id: String,
    @SerialName("cloudinary_public_id") val cloudinaryPublicId: String,
    @SerialName("secure_url") val secureUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false
)

@Serializable
data class PaginatedMasterProductsDto(
    val total: Int,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val items: List<MasterProductResponseDto>
)

@Serializable
data class StockAdjustmentRequestDto(
    @SerialName("delta_quantity") val deltaQuantity: Double,
    val reason: String = "SALE",
    @SerialName("reference_id") val referenceId: String? = null,
    val notes: String? = null
)

@Serializable
data class ProductEnrollRequestDto(
    val barcode: String,
    val name: String,
    val brand: String = "Generic",
    val category: String = "General",
    @SerialName("sub_category") val subCategory: String? = null,
    @SerialName("standard_mrp") val standardMrp: Double = 0.0,
    @SerialName("selling_price") val sellingPrice: Double? = null,
    @SerialName("cost_price") val costPrice: Double? = null,
    val unit: String = "pcs",
    @SerialName("gst_percent") val gstPercent: Double = 0.0,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("store_id") val storeId: String? = null
)

@Serializable
data class GeminiExtractRequestDto(
    val barcode: String,
    @SerialName("api_key") val apiKey: String? = null
)

@Serializable
data class ProductPhotoExtractionResponseDto(
    val barcode: String,
    @SerialName("is_barcode_generated") val isBarcodeGenerated: Boolean = false,
    @SerialName("code_type") val codeType: String = "EAN_13",
    val name: String,
    val brand: String,
    val category: String,
    @SerialName("sub_category") val subCategory: String? = null,
    @SerialName("standard_mrp") val standardMrp: Double = 0.0,
    @SerialName("cost_price") val costPrice: Double = 0.0,
    @SerialName("selling_price") val sellingPrice: Double = 0.0,
    val unit: String = "pcs",
    @SerialName("gst_percent") val gstPercent: Double = 0.0,
    val description: String? = null,
    @SerialName("dynamic_attributes") val dynamicAttributes: DynamicAttributes? = null,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList()
)


