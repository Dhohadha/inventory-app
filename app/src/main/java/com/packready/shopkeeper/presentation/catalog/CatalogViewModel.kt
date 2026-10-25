package com.packready.shopkeeper.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val hapticManager: HapticManager,
    private val soundPoolManager: SoundPoolManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.getAllProducts().collectLatest { products ->
                _uiState.update { state ->
                    val filtered = applyFilter(products, state.searchQuery, state.selectedCategory)
                    state.copy(
                        allProducts = products,
                        filteredProducts = filtered,
                        selectedProductForDetail = state.selectedProductForDetail?.let { current ->
                            products.find { it.id == current.id }
                        }
                    )
                }
            }
        }
    }

    fun onEvent(event: CatalogUiEvent) {
        when (event) {
            is CatalogUiEvent.OnSearchQueryChange -> {
                _uiState.update { state ->
                    val filtered = applyFilter(state.allProducts, event.query, state.selectedCategory)
                    state.copy(searchQuery = event.query, filteredProducts = filtered)
                }
            }
            is CatalogUiEvent.OnCategorySelect -> {
                _uiState.update { state ->
                    val filtered = applyFilter(state.allProducts, state.searchQuery, event.category)
                    state.copy(selectedCategory = event.category, filteredProducts = filtered)
                }
            }
            is CatalogUiEvent.OnProductClick -> {
                hapticManager.pulseScan()
                _uiState.update { it.copy(selectedProductForDetail = event.product) }
            }
            is CatalogUiEvent.DismissProductDetail -> {
                _uiState.update { it.copy(selectedProductForDetail = null) }
            }
            is CatalogUiEvent.OpenEditProductDialog -> {
                _uiState.update { it.copy(productToEdit = event.product) }
            }
            is CatalogUiEvent.DismissEditProductDialog -> {
                _uiState.update { it.copy(productToEdit = null) }
            }
            is CatalogUiEvent.SaveEditedProduct -> {
                viewModelScope.launch {
                    try {
                        productRepository.saveProduct(event.product)
                        soundPoolManager.playScanSuccess()
                        hapticManager.pulseScan()
                        _uiState.update {
                            it.copy(
                                productToEdit = null,
                                selectedProductForDetail = event.product,
                                toastMessage = "Product '${event.product.name}' updated successfully!"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Failed to update product: ${e.message}") }
                    }
                }
            }
            is CatalogUiEvent.OpenDeleteProductDialog -> {
                _uiState.update { it.copy(productToDelete = event.product) }
            }
            is CatalogUiEvent.DismissDeleteProductDialog -> {
                _uiState.update { it.copy(productToDelete = null) }
            }
            is CatalogUiEvent.ConfirmDeleteProduct -> {
                val product = _uiState.value.productToDelete ?: return
                viewModelScope.launch {
                    try {
                        productRepository.deleteProduct(product.id)
                        soundPoolManager.playErrorBuzz()
                        hapticManager.pulseError()
                        _uiState.update {
                            it.copy(
                                productToDelete = null,
                                selectedProductForDetail = null,
                                toastMessage = "Deleted '${product.name}' from catalog"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
                    }
                }
            }
            is CatalogUiEvent.AdjustStock -> {
                viewModelScope.launch {
                    try {
                        productRepository.adjustStock(event.productId, event.delta)
                        hapticManager.pulseScan()
                        soundPoolManager.playScanSuccess()
                        val updated = productRepository.getProductById(event.productId)
                        _uiState.update {
                            it.copy(
                                selectedProductForDetail = updated ?: it.selectedProductForDetail,
                                toastMessage = "Stock updated (${if (event.delta > 0) "+${event.delta.toInt()}" else event.delta.toInt()})"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Error adjusting stock: ${e.message}") }
                    }
                }
            }
            is CatalogUiEvent.OpenAddProductDialog -> {
                _uiState.update { it.copy(isAddProductDialogOpen = true) }
            }
            is CatalogUiEvent.DismissAddProductDialog -> {
                _uiState.update { it.copy(isAddProductDialogOpen = false) }
            }
            is CatalogUiEvent.AddNewProduct -> {
                viewModelScope.launch {
                    try {
                        productRepository.saveProduct(event.product)
                        soundPoolManager.playSaleSuccess()
                        hapticManager.pulseSaleSuccess()
                        _uiState.update {
                            it.copy(
                                isAddProductDialogOpen = false,
                                toastMessage = "Added '${event.product.name}' to Catalog!"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Failed to add product: ${e.message}") }
                    }
                }
            }
            is CatalogUiEvent.ClearMessages -> {
                _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
            }
        }
    }

    private fun applyFilter(products: List<Product>, query: String, category: String): List<Product> {
        val q = query.trim().lowercase()
        return products.filter { product ->
            val matchesQuery = q.isEmpty() ||
                    product.name.lowercase().contains(q) ||
                    product.brand.lowercase().contains(q) ||
                    product.barcode.lowercase().contains(q) ||
                    product.subCategory.lowercase().contains(q)
            val matchesCategory = category == "All" || product.category.displayName == category
            matchesQuery && matchesCategory
        }
    }
}
