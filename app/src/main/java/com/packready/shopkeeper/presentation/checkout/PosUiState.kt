package com.packready.shopkeeper.presentation.checkout

import android.graphics.Bitmap
import com.packready.shopkeeper.domain.model.Cart
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.SaleTransaction
import com.packready.shopkeeper.domain.model.SlipReconciliation

data class PosUiState(
    val cart: Cart = Cart(),
    val isCameraActive: Boolean = true,
    val topSellingProducts: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val isSearching: Boolean = false,
    val lastScannedBarcode: String? = null,
    val isPaymentDialogOpen: Boolean = false,
    val isSlipOcrDialogOpen: Boolean = false,
    val isOcrProcessing: Boolean = false,
    val slipReconciliation: SlipReconciliation? = null,
    val completedSale: SaleTransaction? = null,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val activePaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val cashReceivedInput: String = "",
    val customerNameInput: String = "",
    val customerPhoneInput: String = "",
    val discountInput: String = "",
    val geminiApiKey: String = ""
)

sealed interface PosUiEvent {
    // Camera Control
    object ToggleCamera : PosUiEvent
    data class SetCameraActive(val isActive: Boolean) : PosUiEvent

    data class OnBarcodeScanned(val barcode: String) : PosUiEvent
    data class AddProductToCart(val product: Product, val qtyDelta: Double = 1.0) : PosUiEvent
    data class UpdateCartItemQuantity(val productId: String, val newQty: Double) : PosUiEvent
    data class RemoveCartItem(val productId: String) : PosUiEvent
    object ClearCart : PosUiEvent
    data class OnSearchQueryChange(val query: String) : PosUiEvent
    
    // Payment Dialog
    object OpenPaymentDialog : PosUiEvent
    object ClosePaymentDialog : PosUiEvent
    data class SelectPaymentMethod(val method: PaymentMethod) : PosUiEvent
    data class OnCashReceivedChange(val input: String) : PosUiEvent
    data class OnCustomerNameChange(val name: String) : PosUiEvent
    data class OnCustomerPhoneChange(val phone: String) : PosUiEvent
    data class ApplyOverallDiscount(val discountAmount: Double) : PosUiEvent
    object ConfirmCheckout : PosUiEvent
    object DismissCompletedReceipt : PosUiEvent
    object PrintReceiptBluetooth : PosUiEvent

    // Handwritten Slip OCR
    object OpenSlipOcrDialog : PosUiEvent
    object CloseSlipOcrDialog : PosUiEvent
    data class ProcessSlipImage(val bitmap: Bitmap) : PosUiEvent
    data class ToggleSlipItemSelection(val index: Int) : PosUiEvent
    object AddMatchedSlipItemsToCart : PosUiEvent

    // Toast/Error
    object ClearMessages : PosUiEvent
    data class SetApiKey(val key: String) : PosUiEvent
}
