package com.packready.shopkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.packready.shopkeeper.domain.model.DynamicAttributes
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["category"]),
        Index(value = ["salesCount"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String, // Barcode or PK-LOOSE-XXXX
    val barcode: String,
    val name: String,
    val brand: String,
    val category: String,
    val subCategory: String,
    val mrp: Double,
    val costPrice: Double,
    val sellingPrice: Double,
    val currentStock: Double,
    val minStockAlert: Double = 5.0,
    val unit: String, // pcs, kg, g, litre, ml
    val gstPercent: Double = 0.0,
    val salesCount: Int = 0,
    val attributesJson: String? = null, // Dynamic attributes based on Category
    val imagesJson: String? = null, // JSON List<String> of file paths/URIs
    val lastRestockedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Product {
        val parsedCategory = try {
            ProductCategory.valueOf(category)
        } catch (e: Exception) {
            ProductCategory.OTHER
        }

        val parsedAttributes = attributesJson?.let {
            try {
                Json.decodeFromString<DynamicAttributes>(it)
            } catch (e: Exception) {
                null
            }
        }

        val parsedImages = imagesJson?.let {
            try {
                Json.decodeFromString(ListSerializer(String.serializer()), it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        return Product(
            id = id,
            barcode = barcode,
            name = name,
            brand = brand,
            category = parsedCategory,
            subCategory = subCategory,
            mrp = mrp,
            costPrice = costPrice,
            sellingPrice = sellingPrice,
            currentStock = currentStock,
            minStockAlert = minStockAlert,
            unit = unit,
            gstPercent = gstPercent,
            salesCount = salesCount,
            attributes = parsedAttributes,
            images = parsedImages,
            lastRestockedAt = lastRestockedAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(product: Product): ProductEntity {
            val jsonAttr = product.attributes?.let {
                try {
                    Json.encodeToString(DynamicAttributes.serializer(), it)
                } catch (e: Exception) {
                    null
                }
            }

            val jsonImages = if (product.images.isNotEmpty()) {
                try {
                    Json.encodeToString(ListSerializer(String.serializer()), product.images)
                } catch (e: Exception) {
                    null
                }
            } else null

            return ProductEntity(
                id = product.id,
                barcode = product.barcode,
                name = product.name,
                brand = product.brand,
                category = product.category.name,
                subCategory = product.subCategory,
                mrp = product.mrp,
                costPrice = product.costPrice,
                sellingPrice = product.sellingPrice,
                currentStock = product.currentStock,
                minStockAlert = product.minStockAlert,
                unit = product.unit,
                gstPercent = product.gstPercent,
                salesCount = product.salesCount,
                attributesJson = jsonAttr,
                imagesJson = jsonImages,
                lastRestockedAt = product.lastRestockedAt,
                createdAt = product.createdAt
            )
        }
    }
}
