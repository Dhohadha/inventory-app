package com.packready.shopkeeper.presentation.checkout

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.data.hardware.BluetoothPrinterManager
import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.repository.ProductRepository
import com.packready.shopkeeper.domain.usecase.CheckoutResult
import com.packready.shopkeeper.domain.usecase.ExecuteCheckoutUseCase
import com.packready.shopkeeper.domain.usecase.GetTopSellingProductsUseCase
import com.packready.shopkeeper.domain.usecase.ProcessHandwrittenSlipUseCase
import com.packready.shopkeeper.domain.usecase.ProcessScanUseCase
import com.packready.shopkeeper.domain.usecase.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val getTopSellingProductsUseCase: GetTopSellingProductsUseCase,
    private val processScanUseCase: ProcessScanUseCase,
    private val executeCheckoutUseCase: ExecuteCheckoutUseCase,
    private val processHandwrittenSlipUseCase: ProcessHandwrittenSlipUseCase,
    private val bluetoothPrinterManager: BluetoothPrinterManager,
    private val soundPoolManager: SoundPoolManager,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        observeTopSellingProducts()
        observeAllProducts()
    }

    private fun observeTopSellingProducts() {
        viewModelScope.launch {
            getTopSellingProductsUseCase(20).collectLatest { products ->
                _uiState.update { it.copy(topSellingProducts = products) }
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

    fun onEvent(event: PosUiEvent) {
        when (event) {
            is PosUiEvent.ToggleCamera -> {
                _uiState.update { it.copy(isCameraActive = !it.isCameraActive) }
            }
            is PosUiEvent.SetCameraActive -> {
                _uiState.update { it.copy(isCameraActive = event.isActive) }
            }
            is PosUiEvent.OnBarcodeScanned -> handleBarcodeScan(event.barcode)
            is PosUiEvent.AddProductToCart -> {
                hapticManager.pulseScan()
                soundPoolManager.playScanSuccess()
                _uiState.update { state ->
                    state.copy(cart = state.cart.addItem(event.product, event.qtyDelta))
                }
            }
            is PosUiEvent.UpdateCartItemQuantity -> {
                _uiState.update { state ->
                    state.copy(cart = state.cart.updateQuantity(event.productId, event.newQty))
                }
            }
            is PosUiEvent.RemoveCartItem -> {
                _uiState.update { state ->
                    state.copy(cart = state.cart.removeItem(event.productId))
                }
            }
            is PosUiEvent.ClearCart -> {
                _uiState.update { state -> state.copy(cart = state.cart.clear()) }
            }
            is PosUiEvent.OnSearchQueryChange -> {
                val query = event.query
                _uiState.update { state ->
                    val filtered = if (query.isBlank()) emptyList() else {
                        state.allProducts.filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.barcode.contains(query, ignoreCase = true) ||
                            it.brand.contains(query, ignoreCase = true)
                        }
                    }
                    state.copy(
                        searchQuery = query,
                        searchResults = filtered,
                        isSearching = query.isNotBlank()
                    )
                }
            }
            is PosUiEvent.OpenPaymentDialog -> {
                _uiState.update { state ->
                    state.copy(
                        isPaymentDialogOpen = true,
                        cashReceivedInput = String.format("%.0f", state.cart.grandTotal)
                    )
                }
            }
            is PosUiEvent.ClosePaymentDialog -> {
                _uiState.update { it.copy(isPaymentDialogOpen = false) }
            }
            is PosUiEvent.SelectPaymentMethod -> {
                _uiState.update { it.copy(activePaymentMethod = event.method) }
            }
            is PosUiEvent.OnCashReceivedChange -> {
                _uiState.update { it.copy(cashReceivedInput = event.input) }
            }
            is PosUiEvent.OnCustomerNameChange -> {
                _uiState.update { it.copy(customerNameInput = event.name) }
            }
            is PosUiEvent.OnCustomerPhoneChange -> {
                _uiState.update { it.copy(customerPhoneInput = event.phone) }
            }
            is PosUiEvent.ApplyOverallDiscount -> {
                _uiState.update { state ->
                    state.copy(cart = state.cart.copy(overallDiscountAmount = event.discountAmount))
                }
            }
            is PosUiEvent.ConfirmCheckout -> finalizeCheckout()
            is PosUiEvent.DismissCompletedReceipt -> {
                _uiState.update { it.copy(completedSale = null) }
            }
            is PosUiEvent.PrintReceiptBluetooth -> printLastReceipt()
            is PosUiEvent.OpenSlipOcrDialog -> {
                _uiState.update { it.copy(isSlipOcrDialogOpen = true, slipReconciliation = null) }
            }
            is PosUiEvent.CloseSlipOcrDialog -> {
                _uiState.update { it.copy(isSlipOcrDialogOpen = false) }
            }
            is PosUiEvent.ProcessSlipImage -> handleSlipOcr(event.bitmap)
            is PosUiEvent.ToggleSlipItemSelection -> {
                _uiState.update { state ->
                    val recon = state.slipReconciliation ?: return@update state
                    val updatedItems = recon.parsedItems.mapIndexed { idx, item ->
                        if (idx == event.index) item.copy(isSelected = !item.isSelected) else item
                    }
                    state.copy(slipReconciliation = recon.copy(parsedItems = updatedItems))
                }
            }
            is PosUiEvent.AddMatchedSlipItemsToCart -> addSelectedSlipItemsToCart()
            is PosUiEvent.ClearMessages -> {
                _uiState.update { it.copy(errorMessage = null, toastMessage = null) }
            }
            is PosUiEvent.SetApiKey -> {
                _uiState.update { it.copy(geminiApiKey = event.key) }
            }
        }
    }

    private fun handleBarcodeScan(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(lastScannedBarcode = barcode) }
            when (val result = processScanUseCase(_uiState.value.cart, barcode)) {
                is ScanResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            cart = result.updatedCart,
                            toastMessage = "Added: ${result.product.name}"
                        )
                    }
                }
                is ScanResult.NotFound -> {
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = "Barcode '$barcode' not found in store catalog."
                        )
                    }
                }
                is ScanResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    private fun finalizeCheckout() {
        viewModelScope.launch {
            val state = _uiState.value
            val cashReceived = state.cashReceivedInput.toDoubleOrNull() ?: state.cart.grandTotal

            val result = executeCheckoutUseCase(
                cart = state.cart,
                paymentMethod = state.activePaymentMethod,
                cashReceived = cashReceived,
                upiTransactionRef = if (state.activePaymentMethod == PaymentMethod.UPI) "UPI-${System.currentTimeMillis() % 100000}" else null,
                customerName = state.customerNameInput,
                customerPhone = state.customerPhoneInput
            )

            when (result) {
                is CheckoutResult.Success -> {
                    _uiState.update { s ->
                        s.copy(
                            completedSale = result.transaction,
                            isPaymentDialogOpen = false,
                            cart = s.cart.clear(),
                            cashReceivedInput = "",
                            customerNameInput = "",
                            customerPhoneInput = "",
                            toastMessage = "Payment of ₹${result.transaction.grandTotal} Successful!"
                        )
                    }
                }
                is CheckoutResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    private fun handleSlipOcr(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOcrProcessing = true, errorMessage = null) }
            val apiKey = _uiState.value.geminiApiKey
            val result = processHandwrittenSlipUseCase(bitmap, apiKey)
            if (result.isSuccess) {
                val recon = result.getOrNull()
                _uiState.update { it.copy(isOcrProcessing = false, slipReconciliation = recon) }
                soundPoolManager.playScanSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isOcrProcessing = false,
                        errorMessage = "Failed to parse slip. Please ensure good lighting."
                    )
                }
                soundPoolManager.playErrorBuzz()
            }
        }
    }

    private fun addSelectedSlipItemsToCart() {
        val recon = _uiState.value.slipReconciliation ?: return
        var mutableCart = _uiState.value.cart

        for (item in recon.parsedItems) {
            if (item.isSelected && item.matchedProduct != null) {
                mutableCart = mutableCart.addItem(item.matchedProduct, item.suggestedQuantity)
            }
        }

        soundPoolManager.playSaleSuccess()
        hapticManager.pulseSaleSuccess()

        _uiState.update {
            it.copy(
                cart = mutableCart,
                isSlipOcrDialogOpen = false,
                slipReconciliation = null,
                toastMessage = "Added items from Handwritten List to Cart!"
            )
        }
    }

    private fun printLastReceipt() {
        viewModelScope.launch {
            val sale = _uiState.value.completedSale ?: return@launch
            val devices = bluetoothPrinterManager.getPairedPrinters()
            if (devices.isNotEmpty()) {
                val firstPrinter = devices.first().second
                val printResult = bluetoothPrinterManager.printReceipt(firstPrinter, sale)
                if (printResult.isSuccess) {
                    _uiState.update { it.copy(toastMessage = "Receipt printed via Bluetooth!") }
                } else {
                    _uiState.update { it.copy(errorMessage = "Printing error: ${printResult.exceptionOrNull()?.message}") }
                }
            } else {
                _uiState.update { it.copy(toastMessage = "No paired thermal printer found. Showing digital preview.") }
            }
        }
    }
}
