package com.packready.shopkeeper.domain.usecase

import com.packready.shopkeeper.domain.model.DynamicAttributes
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.domain.repository.ProductRepository
import javax.inject.Inject

class GenerateLooseGoodsBarcodeUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        name: String,
        unit: String,
        costPrice: Double,
        sellingPrice: Double,
        currentStock: Double,
        grainGrade: String? = null,
        origin: String? = null
    ): Product {
        // Generate unique internal ID: PK-LOOSE-XXXX
        val randomNum = (1000..9999).random()
        val customBarcode = "PK-LOOSE-$randomNum"

        val product = Product(
            id = customBarcode,
            barcode = customBarcode,
            name = name.trim(),
            brand = "Store Bulk",
            category = ProductCategory.LOOSE_STAPLES,
            subCategory = "Loose Staples",
            mrp = sellingPrice * 1.1,
            costPrice = costPrice,
            sellingPrice = sellingPrice,
            currentStock = currentStock,
            minStockAlert = 10.0,
            unit = unit,
            gstPercent = 0.0,
            salesCount = 0,
            attributes = DynamicAttributes(grainGrade = grainGrade, origin = origin),
            lastRestockedAt = System.currentTimeMillis()
        )

        productRepository.saveProduct(product)
        return product
    }
}
