package com.packready.shopkeeper.presentation.loosegoods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.data.hardware.BluetoothPrinterManager
import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.domain.repository.ProductRepository
import com.packready.shopkeeper.domain.usecase.GenerateLooseGoodsBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LooseGoodsUiState(
    val looseProducts: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val itemNameInput: String = "",
    val unitInput: String = "kg",
    val costPriceInput: String = "",
    val sellingPriceInput: String = "",
    val stockInput: String = "50",
    val grainGradeInput: String = "Superfine",
    val originInput: String = "",
    val labelCopiesCount: Int = 4,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface LooseGoodsUiEvent {
    data class OnNameChange(val name: String) : LooseGoodsUiEvent
    data class OnUnitChange(val unit: String) : LooseGoodsUiEvent
    data class OnCostChange(val cost: String) : LooseGoodsUiEvent
    data class OnSellingPriceChange(val sp: String) : LooseGoodsUiEvent
    data class OnStockChange(val stock: String) : LooseGoodsUiEvent
    data class OnGrainGradeChange(val grade: String) : LooseGoodsUiEvent
    data class OnOriginChange(val origin: String) : LooseGoodsUiEvent
    data class SetCopiesCount(val count: Int) : LooseGoodsUiEvent
    data class SelectProductForLabel(val product: Product) : LooseGoodsUiEvent
    object GenerateCustomBarcode : LooseGoodsUiEvent
    object PrintStickerLabels : LooseGoodsUiEvent
    object ClearMessages : LooseGoodsUiEvent
}

@HiltViewModel
class LooseGoodsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val generateLooseGoodsBarcodeUseCase: GenerateLooseGoodsBarcodeUseCase,
    private val bluetoothPrinterManager: BluetoothPrinterManager,
    private val soundPoolManager: SoundPoolManager,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LooseGoodsUiState())
    val uiState: StateFlow<LooseGoodsUiState> = _uiState.asStateFlow()

    init {
        observeLooseProducts()
    }

    private fun observeLooseProducts() {
        viewModelScope.launch {
            productRepository.getProductsByCategory(ProductCategory.LOOSE_STAPLES).collectLatest { list ->
                _uiState.update {
                    it.copy(
                        looseProducts = list,
                        selectedProduct = it.selectedProduct ?: list.firstOrNull()
                    )
                }
            }
        }
    }

    fun onEvent(event: LooseGoodsUiEvent) {
        when (event) {
            is LooseGoodsUiEvent.OnNameChange -> _uiState.update { it.copy(itemNameInput = event.name) }
            is LooseGoodsUiEvent.OnUnitChange -> _uiState.update { it.copy(unitInput = event.unit) }
            is LooseGoodsUiEvent.OnCostChange -> _uiState.update { it.copy(costPriceInput = event.cost) }
            is LooseGoodsUiEvent.OnSellingPriceChange -> _uiState.update { it.copy(sellingPriceInput = event.sp) }
            is LooseGoodsUiEvent.OnStockChange -> _uiState.update { it.copy(stockInput = event.stock) }
            is LooseGoodsUiEvent.OnGrainGradeChange -> _uiState.update { it.copy(grainGradeInput = event.grade) }
            is LooseGoodsUiEvent.OnOriginChange -> _uiState.update { it.copy(originInput = event.origin) }
            is LooseGoodsUiEvent.SetCopiesCount -> _uiState.update { it.copy(labelCopiesCount = event.count) }
            is LooseGoodsUiEvent.SelectProductForLabel -> _uiState.update { it.copy(selectedProduct = event.product) }
            is LooseGoodsUiEvent.GenerateCustomBarcode -> generateBarcode()
            is LooseGoodsUiEvent.PrintStickerLabels -> printLabels()
            is LooseGoodsUiEvent.ClearMessages -> _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
        }
    }

    private fun generateBarcode() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.itemNameInput.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter staple name (e.g. Basmati Rice, Sugar)") }
                return@launch
            }

            val cp = state.costPriceInput.toDoubleOrNull() ?: 40.0
            val sp = state.sellingPriceInput.toDoubleOrNull() ?: 50.0
            val stock = state.stockInput.toDoubleOrNull() ?: 50.0

            val product = generateLooseGoodsBarcodeUseCase(
                name = state.itemNameInput,
                unit = state.unitInput,
                costPrice = cp,
                sellingPrice = sp,
                currentStock = stock,
                grainGrade = state.grainGradeInput,
                origin = state.originInput
            )

            soundPoolManager.playSaleSuccess()
            hapticManager.pulseSaleSuccess()

            _uiState.update {
                it.copy(
                    selectedProduct = product,
                    itemNameInput = "",
                    costPriceInput = "",
                    sellingPriceInput = "",
                    toastMessage = "Generated Container Identifier: ${product.barcode}"
                )
            }
        }
    }

    private fun printLabels() {
        val product = _uiState.value.selectedProduct ?: return
        soundPoolManager.playScanSuccess()
        hapticManager.pulseScan()
        _uiState.update {
            it.copy(toastMessage = "Sent ${_uiState.value.labelCopiesCount} sticker copies of ${product.barcode} to printer!")
        }
    }
}
