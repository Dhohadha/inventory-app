package com.packready.shopkeeper.presentation.restock

import com.packready.shopkeeper.domain.model.DynamicAttributes
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory

data class RestockUiState(
    val scannedBarcode: String? = null,
    val matchedProduct: Product? = null,
    val quantityToAdd: Double = 12.0,
    val costPriceInput: String = "",
    val sellingPriceInput: String = "",
    val mrpInput: String = "",
    val isQuickAddDialogOpen: Boolean = false,
    val isCameraScanningActive: Boolean = false,
    val searchQuery: String = "",
    val lowStockProducts: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val searchResults: List<Product> = emptyList(),
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface RestockUiEvent {
    data class OnBarcodeScanned(val barcode: String) : RestockUiEvent
    data class SelectProduct(val product: Product) : RestockUiEvent
    object CancelSelection : RestockUiEvent
    data class OnSearchQueryChange(val query: String) : RestockUiEvent
    object ToggleCameraScanning : RestockUiEvent
    data class OnQuantityChange(val qty: Double) : RestockUiEvent
    data class OnCostPriceChange(val cost: String) : RestockUiEvent
    data class OnSellingPriceChange(val sp: String) : RestockUiEvent
    data class OnMrpChange(val mrp: String) : RestockUiEvent
    object ConfirmRestock : RestockUiEvent
    object OpenQuickAddDialog : RestockUiEvent
    object CloseQuickAddDialog : RestockUiEvent
    data class CreateNewProduct(
        val name: String,
        val barcode: String,
        val brand: String,
        val category: ProductCategory,
        val unit: String,
        val costPrice: Double,
        val sellingPrice: Double,
        val mrp: Double,
        val initialStock: Double,
        val attributes: DynamicAttributes?,
        val visualEmbeddings: List<Pair<String, FloatArray>> = emptyList()
    ) : RestockUiEvent
    object ClearMessages : RestockUiEvent
}
