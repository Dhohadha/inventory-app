package com.packready.shopkeeper.presentation.restock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
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
class RestockViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val soundPoolManager: SoundPoolManager,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestockUiState())
    val uiState: StateFlow<RestockUiState> = _uiState.asStateFlow()

    init {
        observeLowStock()
        observeAllProducts()
    }

    private fun observeLowStock() {
        viewModelScope.launch {
            productRepository.getLowStockProducts().collectLatest { list ->
                _uiState.update { it.copy(lowStockProducts = list) }
            }
        }
    }

    private fun observeAllProducts() {
        viewModelScope.launch {
            productRepository.getAllProducts().collectLatest { list ->
                _uiState.update { it.copy(allProducts = list) }
            }
        }
    }

    fun onEvent(event: RestockUiEvent) {
        when (event) {
            is RestockUiEvent.OnBarcodeScanned -> handleScan(event.barcode)
            is RestockUiEvent.SelectProduct -> selectProduct(event.product)
            is RestockUiEvent.CancelSelection -> _uiState.update { it.copy(matchedProduct = null, scannedBarcode = null) }
            is RestockUiEvent.OnSearchQueryChange -> handleSearch(event.query)
            is RestockUiEvent.ToggleCameraScanning -> _uiState.update { it.copy(isCameraScanningActive = !it.isCameraScanningActive) }
            is RestockUiEvent.OnQuantityChange -> _uiState.update { it.copy(quantityToAdd = event.qty) }
            is RestockUiEvent.OnCostPriceChange -> _uiState.update { it.copy(costPriceInput = event.cost) }
            is RestockUiEvent.OnSellingPriceChange -> _uiState.update { it.copy(sellingPriceInput = event.sp) }
            is RestockUiEvent.OnMrpChange -> _uiState.update { it.copy(mrpInput = event.mrp) }
            is RestockUiEvent.ConfirmRestock -> confirmRestock()
            is RestockUiEvent.OpenQuickAddDialog -> _uiState.update { it.copy(isQuickAddDialogOpen = true) }
            is RestockUiEvent.CloseQuickAddDialog -> _uiState.update { it.copy(isQuickAddDialogOpen = false) }
            is RestockUiEvent.CreateNewProduct -> createProduct(event)
            is RestockUiEvent.ClearMessages -> _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
        }
    }

    private fun handleSearch(query: String) {
        _uiState.update { state ->
            val results = if (query.isBlank()) {
                emptyList()
            } else {
                state.allProducts.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.barcode.contains(query, ignoreCase = true) ||
                    it.brand.contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, searchResults = results)
        }
    }

    private fun selectProduct(product: Product) {
        _uiState.update {
            it.copy(
                scannedBarcode = product.barcode,
                matchedProduct = product,
                costPriceInput = product.costPrice.toString(),
                sellingPriceInput = product.sellingPrice.toString(),
                mrpInput = product.mrp.toString(),
                quantityToAdd = 12.0
            )
        }
    }

    private fun handleScan(barcode: String) {
        viewModelScope.launch {
            val product = productRepository.getProductByBarcode(barcode)
            if (product != null) {
                soundPoolManager.playScanSuccess()
                hapticManager.pulseScan()
                _uiState.update {
                    it.copy(
                        scannedBarcode = barcode,
                        matchedProduct = product,
                        costPriceInput = product.costPrice.toString(),
                        sellingPriceInput = product.sellingPrice.toString(),
                        mrpInput = product.mrp.toString(),
                        isCameraScanningActive = false
                    )
                }
            } else {
                soundPoolManager.playErrorBuzz()
                hapticManager.pulseError()
                _uiState.update {
                    it.copy(
                        scannedBarcode = barcode,
                        matchedProduct = null,
                        isQuickAddDialogOpen = true,
                        isCameraScanningActive = false,
                        toastMessage = "New barcode $barcode detected. Please add product details."
                    )
                }
            }
        }
    }

    private fun confirmRestock() {
        viewModelScope.launch {
            val state = _uiState.value
            val product = state.matchedProduct ?: return@launch
            val qty = state.quantityToAdd
            val cost = state.costPriceInput.toDoubleOrNull() ?: product.costPrice
            val sp = state.sellingPriceInput.toDoubleOrNull() ?: product.sellingPrice
            val mrp = state.mrpInput.toDoubleOrNull() ?: product.mrp

            // Update product prices if changed
            val updatedProduct = product.copy(
                costPrice = cost,
                sellingPrice = sp,
                mrp = mrp,
                lastRestockedAt = System.currentTimeMillis()
            )
            productRepository.saveProduct(updatedProduct)
            productRepository.adjustStock(product.id, qty)

            soundPoolManager.playSaleSuccess()
            hapticManager.pulseSaleSuccess()

            _uiState.update {
                it.copy(
                    matchedProduct = null,
                    scannedBarcode = null,
                    toastMessage = "Restocked +${qty.toInt()} units of ${product.name}!"
                )
            }
        }
    }

    private fun createProduct(event: RestockUiEvent.CreateNewProduct) {
        viewModelScope.launch {
            val product = Product(
                id = event.barcode,
                barcode = event.barcode,
                name = event.name,
                brand = event.brand,
                category = event.category,
                subCategory = event.category.displayName,
                mrp = event.mrp,
                costPrice = event.costPrice,
                sellingPrice = event.sellingPrice,
                currentStock = event.initialStock,
                minStockAlert = 5.0,
                unit = event.unit,
                gstPercent = 5.0,
                salesCount = 0,
                attributes = event.attributes,
                lastRestockedAt = System.currentTimeMillis()
            )

            productRepository.saveProduct(product)
            if (event.visualEmbeddings.isNotEmpty()) {
                productRepository.saveProductEmbeddings(product.id, event.visualEmbeddings)
            }
            soundPoolManager.playSaleSuccess()
            hapticManager.pulseSaleSuccess()

            _uiState.update {
                it.copy(
                    isQuickAddDialogOpen = false,
                    matchedProduct = null,
                    scannedBarcode = null,
                    toastMessage = "Added ${product.name} to store catalog!"
                )
            }
        }
    }
}
