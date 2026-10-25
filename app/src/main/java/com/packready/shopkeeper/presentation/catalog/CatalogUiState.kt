package com.packready.shopkeeper.presentation.catalog

import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory

data class CatalogUiState(
    val allProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedProductForDetail: Product? = null,
    val productToEdit: Product? = null,
    val productToDelete: Product? = null,
    val isAddProductDialogOpen: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface CatalogUiEvent {
    data class OnSearchQueryChange(val query: String) : CatalogUiEvent
    data class OnCategorySelect(val category: String) : CatalogUiEvent
    data class OnProductClick(val product: Product) : CatalogUiEvent
    object DismissProductDetail : CatalogUiEvent

    data class OpenEditProductDialog(val product: Product) : CatalogUiEvent
    object DismissEditProductDialog : CatalogUiEvent
    data class SaveEditedProduct(val product: Product) : CatalogUiEvent

    data class OpenDeleteProductDialog(val product: Product) : CatalogUiEvent
    object DismissDeleteProductDialog : CatalogUiEvent
    object ConfirmDeleteProduct : CatalogUiEvent

    data class AdjustStock(val productId: String, val delta: Double) : CatalogUiEvent

    object OpenAddProductDialog : CatalogUiEvent
    object DismissAddProductDialog : CatalogUiEvent
    data class AddNewProduct(val product: Product) : CatalogUiEvent

    object ClearMessages : CatalogUiEvent
}
