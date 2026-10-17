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
import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.packready.shopkeeper.data.local.dao.EmbeddingDao
import com.packready.shopkeeper.data.vision.MultiObjectPosAnalyzer
import com.packready.shopkeeper.data.vision.RoiProcessor
import com.packready.shopkeeper.data.vision.VectorSearchEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class PosViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productRepository: ProductRepository,
    private val embeddingDao: EmbeddingDao,
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

    val posAnalyzer: MultiObjectPosAnalyzer by lazy {
        MultiObjectPosAnalyzer(
            context = context,
            productRepository = productRepository,
            embeddingDao = embeddingDao,
            coroutineScope = viewModelScope,
            onProductMatched = { product, confidence, box, method ->
                onEvent(PosUiEvent.OnVisualProductMatched(product, confidence, method))
            },
            onProductSuggested = { product, confidence, box ->
                onEvent(PosUiEvent.OnProductSuggested(product, confidence))
            },
            onBarcodeScanned = { barcode ->
                onEvent(PosUiEvent.OnBarcodeScanned(barcode))
            },
            onTelemetryUpdated = { overlays, telemetry ->
                onEvent(PosUiEvent.UpdatePosTelemetry(overlays, telemetry))
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
                posAnalyzer.refreshEmbeddingsCache()
            }
        }
    }

    fun onEvent(event: PosUiEvent) {
        when (event) {
            is PosUiEvent.ToggleCamera -> {
                val newActive = !_uiState.value.isCameraActive
                _uiState.update { it.copy(isCameraActive = newActive) }
                if (newActive) {
                    viewModelScope.launch { posAnalyzer.refreshEmbeddingsCache() }
                }
            }
            is PosUiEvent.SetCameraActive -> {
                _uiState.update { it.copy(isCameraActive = event.isActive) }
                if (event.isActive) {
                    viewModelScope.launch { posAnalyzer.refreshEmbeddingsCache() }
                }
            }
            is PosUiEvent.OnBarcodeScanned -> handleBarcodeScan(event.barcode)
            is PosUiEvent.OnVisualProductMatched -> {
                hapticManager.pulseScan()
                soundPoolManager.playScanSuccess()
                _uiState.update { state ->
                    state.copy(
                        cart = state.cart.addItem(event.product, 1.0),
                        toastMessage = "Scanned: ${event.product.name} (${event.method})"
                    )
                }
            }
            is PosUiEvent.OnProductSuggested -> {
                _uiState.update { state ->
                    state.copy(
                        suggestedProduct = event.product,
                        suggestionConfidence = event.confidence
                    )
                }
            }
            is PosUiEvent.DismissSuggestedProduct -> {
                _uiState.update { it.copy(suggestedProduct = null) }
            }
            is PosUiEvent.UpdatePosTelemetry -> {
                _uiState.update {
                    it.copy(
                        trackedOverlays = event.overlays,
                        telemetry = event.telemetry
                    )
                }
            }
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
                posAnalyzer.clearResolvedObjects()
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
            is PosUiEvent.ProcessCapturedPosPhoto -> handleCapturedPosPhoto(event.bitmap)
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

    private fun handleCapturedPosPhoto(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(toastMessage = "Processing camera photo...") }

            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // 1. Scan for Barcode in the photo
            val barcodeScanner = BarcodeScanning.getClient()
            try {
                val barcodes = Tasks.await(barcodeScanner.process(inputImage), 1500L, TimeUnit.MILLISECONDS)
                val rawBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                if (rawBarcode != null) {
                    withContext(Dispatchers.Main) {
                        handleBarcodeScan(rawBarcode)
                    }
                    return@launch
                }
            } catch (_: Exception) {}

            // 2. Try Visual Match using MobileNetV4 ArcFace embeddings on segmented object
            val candidates = embeddingDao.getAllEmbeddings()
            if (candidates.isNotEmpty()) {
                val segmentedObject = RoiProcessor.segmentProductObject(bitmap)
                val queryEmbedding = posAnalyzer.embeddingEngine.extractEmbedding(segmentedObject)
                val bestMatch = VectorSearchEngine.findBestMatch(queryEmbedding, candidates, threshold = 0.55f)
                if (bestMatch != null) {
                    val product = productRepository.getProductById(bestMatch.productId)
                    if (product != null) {
                        withContext(Dispatchers.Main) {
                            hapticManager.pulseScan()
                            soundPoolManager.playScanSuccess()
                            _uiState.update { state ->
                                state.copy(
                                    cart = state.cart.addItem(product, 1.0),
                                    toastMessage = "✓ Matched: ${product.name} (${(bestMatch.score * 100).toInt()}% conf)"
                                )
                            }
                        }
                        return@launch
                    }
                }
            }

            // 3. Fallback: Check if photo contains multi-line handwriting / receipt text
            try {
                val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val textResult = Tasks.await(textRecognizer.process(inputImage), 1500L, TimeUnit.MILLISECONDS)
                if (textResult.text.trim().length > 15) {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isSlipOcrDialogOpen = true) }
                        handleSlipOcr(bitmap)
                    }
                    return@launch
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                soundPoolManager.playErrorBuzz()
                hapticManager.pulseError()
                _uiState.update { it.copy(toastMessage = "No product or barcode recognized in photo. Try closer.") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            posAnalyzer.embeddingEngine.close()
        } catch (_: Exception) {}
    }
}
