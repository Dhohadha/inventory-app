package com.packready.shopkeeper.presentation.checkout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.data.hardware.BarcodeAnalyzer
import com.packready.shopkeeper.data.hardware.BluetoothPrinterManager
import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.repository.ProductRepository
import com.packready.shopkeeper.domain.usecase.CheckoutResult
import com.packready.shopkeeper.domain.usecase.ExecuteCheckoutUseCase
import com.packready.shopkeeper.domain.usecase.GetTopSellingProductsUseCase
import com.packready.shopkeeper.domain.usecase.ProcessScanUseCase
import com.packready.shopkeeper.domain.usecase.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PosViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productRepository: ProductRepository,
    private val getTopSellingProductsUseCase: GetTopSellingProductsUseCase,
    private val processScanUseCase: ProcessScanUseCase,
    private val executeCheckoutUseCase: ExecuteCheckoutUseCase,
    private val bluetoothPrinterManager: BluetoothPrinterManager,
    private val soundPoolManager: SoundPoolManager,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    val barcodeAnalyzer: BarcodeAnalyzer by lazy {
        BarcodeAnalyzer(
            debounceWindowMs = 2000L,
            onBarcodeDetected = { barcode ->
                onEvent(PosUiEvent.OnBarcodeScanned(barcode))
            }
        )
    }

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
                val newActive = !_uiState.value.isCameraActive
                _uiState.update { it.copy(isCameraActive = newActive) }
            }
            is PosUiEvent.ToggleCameraLens -> {
                _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
            }
            is PosUiEvent.SetCameraActive -> {
                _uiState.update { it.copy(isCameraActive = event.isActive) }
            }
            is PosUiEvent.ToggleCameraScanMode -> {
                _uiState.update { it.copy(cameraScanMode = it.cameraScanMode.toggle()) }
            }
            is PosUiEvent.SetCameraScanMode -> {
                _uiState.update { it.copy(cameraScanMode = event.mode) }
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
            is PosUiEvent.ClearMessages -> {
                _uiState.update { it.copy(errorMessage = null, toastMessage = null) }
            }
        }
    }

    private fun handleBarcodeScan(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(lastScannedBarcode = barcode) }
            when (val result = processScanUseCase(_uiState.value.cart, barcode)) {
                is ScanResult.Success -> {
                    hapticManager.pulseScan()
                    soundPoolManager.playScanSuccess()
                    _uiState.update { state ->
                        state.copy(
                            cart = result.updatedCart,
                            toastMessage = "Added: ${result.product.name}"
                        )
                    }
                }
                is ScanResult.NotFound -> {
                    soundPoolManager.playErrorBuzz()
                    hapticManager.pulseError()
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = "Barcode '$barcode' not found in store catalog."
                        )
                    }
                }
                is ScanResult.Error -> {
                    soundPoolManager.playErrorBuzz()
                    hapticManager.pulseError()
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    private fun finalizeCheckout() {
        viewModelScope.launch {
            val state = _uiState.value
            val isAdjustment = state.activePaymentMethod == PaymentMethod.ADJUSTMENT
            val cashReceived = if (isAdjustment) 0.0 else (state.cashReceivedInput.toDoubleOrNull() ?: state.cart.grandTotal)

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
                    soundPoolManager.playSaleSuccess()
                    hapticManager.pulseSaleSuccess()
                    _uiState.update { s ->
                        s.copy(
                            completedSale = result.transaction,
                            isPaymentDialogOpen = false,
                            cart = s.cart.clear(),
                            cashReceivedInput = "",
                            customerNameInput = "",
                            customerPhoneInput = "",
                            toastMessage = if (isAdjustment) "Inventory Adjustment of ₹${result.transaction.grandTotal} Recorded!" else "Payment of ₹${result.transaction.grandTotal} Successful!"
                        )
                    }
                }
                is CheckoutResult.Error -> {
                    soundPoolManager.playErrorBuzz()
                    hapticManager.pulseError()
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
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
