package com.packready.shopkeeper.presentation.checkout

import com.packready.shopkeeper.domain.model.Cart
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.SaleTransaction

enum class CameraScanMode(
    val label: String,
    val subtitle: String,
    val ratioWidthOverHeight: Float,
    val verticalRatio: Int,
    val horizontalRatio: Int
) {
    MOUNT_4_3(
        label = "Mount (4:3)",
        subtitle = "Desk / Counter Stand",
        ratioWidthOverHeight = 3f / 4f,
        verticalRatio = 4,
        horizontalRatio = 3
    ),
    HAND_1_2(
        label = "Hand Scan (1:2)",
        subtitle = "One-Hand Barcode",
        ratioWidthOverHeight = 2f / 1f,
        verticalRatio = 1,
        horizontalRatio = 2
    );

    fun toggle(): CameraScanMode = if (this == MOUNT_4_3) HAND_1_2 else MOUNT_4_3
}

data class PosUiState(
    val cart: Cart = Cart(),
    val isCameraActive: Boolean = true,
    val isFrontCamera: Boolean = false,
    val cameraScanMode: CameraScanMode = CameraScanMode.MOUNT_4_3,
    val topSellingProducts: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val isSearching: Boolean = false,
    val lastScannedBarcode: String? = null,
    val isPaymentDialogOpen: Boolean = false,
    val completedSale: SaleTransaction? = null,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val activePaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val cashReceivedInput: String = "",
    val customerNameInput: String = "",
    val customerPhoneInput: String = "",
    val discountInput: String = ""
)

sealed interface PosUiEvent {
    // Camera Control
    object ToggleCamera : PosUiEvent
    object ToggleCameraLens : PosUiEvent
    data class SetCameraActive(val isActive: Boolean) : PosUiEvent
    object ToggleCameraScanMode : PosUiEvent
    data class SetCameraScanMode(val mode: CameraScanMode) : PosUiEvent

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

    // Toast/Error
    object ClearMessages : PosUiEvent
}
