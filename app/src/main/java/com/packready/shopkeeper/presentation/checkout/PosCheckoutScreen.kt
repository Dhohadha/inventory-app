package com.packready.shopkeeper.presentation.checkout

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.ui.components.CameraXScannerView
import com.packready.shopkeeper.presentation.ui.components.CartBottomDrawer
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem
import com.packready.shopkeeper.presentation.ui.components.PackReadyHeaderBrand
import com.packready.shopkeeper.presentation.ui.components.PulseDot
import com.packready.shopkeeper.presentation.ui.components.ReceiptThermalPreviewDialog
import com.packready.shopkeeper.presentation.ui.components.MultiObjectBoundingBoxOverlay
import com.packready.shopkeeper.presentation.ui.components.ScanReticleOverlay
import com.packready.shopkeeper.presentation.ui.components.SpeedDialGrid
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCheckoutScreen(
    viewModel: PosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Screen Wake-Lock: Keep display active indefinitely during desk checkout
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Snackbar notifications
    LaunchedEffect(uiState.toastMessage, uiState.errorMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(PosUiEvent.ClearMessages)
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(PosUiEvent.ClearMessages)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CartBottomDrawer(
                cart = uiState.cart,
                onQuantityChange = { id, qty ->
                    viewModel.onEvent(PosUiEvent.UpdateCartItemQuantity(id, qty))
                },
                onRemoveItem = { id ->
                    viewModel.onEvent(PosUiEvent.RemoveCartItem(id))
                },
                onClearCart = {
                    viewModel.onEvent(PosUiEvent.ClearCart)
                },
                onProceedToPay = {
                    viewModel.onEvent(PosUiEvent.OpenPaymentDialog)
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isTabletLandscape = maxWidth > 680.dp

            if (isTabletLandscape) {
                // Tablet/Wide screen: Side-by-Side Dual Pane Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: Camera Scanner or Standby Panel (45% width)
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                    ) {
                        if (uiState.isCameraActive) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CameraXScannerView(
                                    customAnalyzer = viewModel.posAnalyzer,
                                    onBarcodeDetected = { barcode ->
                                        viewModel.onEvent(PosUiEvent.OnBarcodeScanned(barcode))
                                    },
                                    showShutterButton = true,
                                    shutterButtonLabel = "Snap to Bill",
                                    onPhotoCaptured = { bitmap ->
                                        viewModel.onEvent(PosUiEvent.ProcessCapturedPosPhoto(bitmap))
                                    }
                                )
                                MultiObjectBoundingBoxOverlay(overlays = uiState.trackedOverlays)
                                if (uiState.trackedOverlays.isEmpty()) {
                                    ScanReticleOverlay(
                                        isScanningActive = true,
                                        lastScannedBarcode = uiState.lastScannedBarcode,
                                        showStatusBadge = false
                                    )
                                }

                                // Closest Visual Match Suggestion Card
                                if (uiState.suggestedProduct != null) {
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Closest Visual Match (${(uiState.suggestionConfidence * 100).toInt()}%)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = uiState.suggestedProduct!!.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "₹${uiState.suggestedProduct!!.sellingPrice}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Emerald500,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.onEvent(PosUiEvent.DismissSuggestedProduct) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.onEvent(PosUiEvent.AddProductToCart(uiState.suggestedProduct!!))
                                                        viewModel.onEvent(PosUiEvent.DismissSuggestedProduct)
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                                                ) {
                                                    Text("+ Add", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Top Floating Controls Overlay
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Camera Toggle Pill (Active State)
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .bouncingClickable { viewModel.onEvent(PosUiEvent.ToggleCamera) },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Roast900.copy(alpha = 0.90f),
                                        shadowElevation = 6.dp,
                                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PulseDot(color = Emerald500, size = 7.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Camera Live",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald500
                                            )
                                            if (uiState.telemetry != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• ${uiState.telemetry!!.fps} FPS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 11.sp
                                                )
                                                if (uiState.telemetry!!.objectCount > 0) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "(${uiState.telemetry!!.objectCount})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Caramel400,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // OCR Slip Scanner Pill
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .bouncingClickable { viewModel.onEvent(PosUiEvent.OpenSlipOcrDialog) },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Roast900.copy(alpha = 0.90f),
                                        shadowElevation = 6.dp,
                                        border = BorderStroke(1.dp, Caramel500.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DocumentScanner,
                                                contentDescription = "Scan Slip",
                                                tint = Caramel500,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Scan Slip",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Caramel500
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            TabletCameraStandbyPane(
                                onTurnOnCamera = { viewModel.onEvent(PosUiEvent.SetCameraActive(true)) },
                                onOpenSlipOcr = { viewModel.onEvent(PosUiEvent.OpenSlipOcrDialog) }
                            )
                        }
                    }

                    // Right Pane: Catalog and Fast Speed Dial (55% width)
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    ) {
                        PosHeaderAndSearchBar(
                            searchQuery = uiState.searchQuery,
                            isCameraActive = uiState.isCameraActive,
                            onSearchQueryChange = { viewModel.onEvent(PosUiEvent.OnSearchQueryChange(it)) },
                            onToggleCamera = { viewModel.onEvent(PosUiEvent.ToggleCamera) },
                            onOpenSlipOcr = { viewModel.onEvent(PosUiEvent.OpenSlipOcrDialog) }
                        )

                        val displayList = if (uiState.isSearching) uiState.searchResults else uiState.topSellingProducts

                        SpeedDialGrid(
                            products = displayList,
                            onProductTap = { product ->
                                viewModel.onEvent(PosUiEvent.AddProductToCart(product))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // Mobile Portrait: Stacked Layout with Smooth Animation on Camera Toggle
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                ) {
                    if (uiState.isCameraActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(245.dp)
                        ) {
                            CameraXScannerView(
                                customAnalyzer = viewModel.posAnalyzer,
                                onBarcodeDetected = { barcode ->
                                    viewModel.onEvent(PosUiEvent.OnBarcodeScanned(barcode))
                                },
                                showShutterButton = true,
                                shutterButtonLabel = "Snap to Bill",
                                onPhotoCaptured = { bitmap ->
                                    viewModel.onEvent(PosUiEvent.ProcessCapturedPosPhoto(bitmap))
                                }
                            )
                            MultiObjectBoundingBoxOverlay(overlays = uiState.trackedOverlays)
                            if (uiState.trackedOverlays.isEmpty()) {
                                ScanReticleOverlay(
                                    isScanningActive = true,
                                    lastScannedBarcode = uiState.lastScannedBarcode,
                                    showStatusBadge = false
                                )
                            }

                            // Closest Visual Match Suggestion Card
                            if (uiState.suggestedProduct != null) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Closest Match (${(uiState.suggestionConfidence * 100).toInt()}%)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = uiState.suggestedProduct!!.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.onEvent(PosUiEvent.DismissSuggestedProduct) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.onEvent(PosUiEvent.AddProductToCart(uiState.suggestedProduct!!))
                                                    viewModel.onEvent(PosUiEvent.DismissSuggestedProduct)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                                            ) {
                                                Text("+ Add", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Top Floating Action Controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Camera Toggle Pill (Active State)
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .bouncingClickable { viewModel.onEvent(PosUiEvent.ToggleCamera) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Roast900.copy(alpha = 0.90f),
                                    shadowElevation = 6.dp,
                                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PulseDot(color = Emerald500, size = 6.dp)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Cam Live",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald500
                                        )
                                        if (uiState.telemetry != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${uiState.telemetry!!.fps} FPS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 11.sp
                                            )
                                            if (uiState.telemetry!!.objectCount > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "(${uiState.telemetry!!.objectCount})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Caramel400,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // OCR Slip Scanner Pill
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .bouncingClickable { viewModel.onEvent(PosUiEvent.OpenSlipOcrDialog) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Roast900.copy(alpha = 0.90f),
                                    shadowElevation = 6.dp,
                                    border = BorderStroke(1.dp, Caramel500.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DocumentScanner,
                                            contentDescription = "Scan Slip",
                                            tint = Caramel500,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Scan Slip",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Caramel500
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        MobileCameraStandbyBanner(
                            onTurnOnCamera = { viewModel.onEvent(PosUiEvent.SetCameraActive(true)) },
                            onOpenSlipOcr = { viewModel.onEvent(PosUiEvent.OpenSlipOcrDialog) }
                        )
                    }

                    // Lower Content Area: Speed Dial Grid & Search
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PosHeaderAndSearchBar(
                                searchQuery = uiState.searchQuery,
                                isCameraActive = uiState.isCameraActive,
                                onSearchQueryChange = { viewModel.onEvent(PosUiEvent.OnSearchQueryChange(it)) },
                                onToggleCamera = { viewModel.onEvent(PosUiEvent.ToggleCamera) },
                                onOpenSlipOcr = { viewModel.onEvent(PosUiEvent.OpenSlipOcrDialog) }
                            )

                            val displayList = if (uiState.isSearching) uiState.searchResults else uiState.topSellingProducts

                            SpeedDialGrid(
                                products = displayList,
                                onProductTap = { product ->
                                    viewModel.onEvent(PosUiEvent.AddProductToCart(product))
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Payment Dialog
    if (uiState.isPaymentDialogOpen) {
        PaymentDialog(
            cart = uiState.cart,
            activeMethod = uiState.activePaymentMethod,
            cashReceivedInput = uiState.cashReceivedInput,
            customerName = uiState.customerNameInput,
            customerPhone = uiState.customerPhoneInput,
            onMethodSelect = { viewModel.onEvent(PosUiEvent.SelectPaymentMethod(it)) },
            onCashReceivedChange = { viewModel.onEvent(PosUiEvent.OnCashReceivedChange(it)) },
            onCustomerNameChange = { viewModel.onEvent(PosUiEvent.OnCustomerNameChange(it)) },
            onCustomerPhoneChange = { viewModel.onEvent(PosUiEvent.OnCustomerPhoneChange(it)) },
            onConfirm = { viewModel.onEvent(PosUiEvent.ConfirmCheckout) },
            onDismiss = { viewModel.onEvent(PosUiEvent.ClosePaymentDialog) }
        )
    }

    // Handwritten Slip OCR Dialog
    if (uiState.isSlipOcrDialogOpen) {
        SlipOcrDialog(
            isProcessing = uiState.isOcrProcessing,
            reconciliation = uiState.slipReconciliation,
            onProcessImage = { bitmap -> viewModel.onEvent(PosUiEvent.ProcessSlipImage(bitmap)) },
            onToggleItem = { index -> viewModel.onEvent(PosUiEvent.ToggleSlipItemSelection(index)) },
            onAddSelectedToCart = { viewModel.onEvent(PosUiEvent.AddMatchedSlipItemsToCart) },
            onDismiss = { viewModel.onEvent(PosUiEvent.CloseSlipOcrDialog) }
        )
    }

    // Completed Thermal Receipt Preview Dialog
    uiState.completedSale?.let { sale ->
        ReceiptThermalPreviewDialog(
            sale = sale,
            onPrintBluetooth = { viewModel.onEvent(PosUiEvent.PrintReceiptBluetooth) },
            onDismiss = { viewModel.onEvent(PosUiEvent.DismissCompletedReceipt) }
        )
    }
}

@Composable
private fun PosHeaderAndSearchBar(
    searchQuery: String,
    isCameraActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleCamera: () -> Unit,
    onOpenSlipOcr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Top Brand Header Row with Camera Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PackReadyHeaderBrand(subtitle = "POS Checkout Counter")

            // Top Camera Quick Toggle Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .bouncingClickable { onToggleCamera() },
                shape = RoundedCornerShape(8.dp),
                color = if (isCameraActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(
                    1.dp,
                    if (isCameraActive) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCameraActive) {
                        PulseDot(color = Emerald500, size = 6.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "Toggle Camera",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCameraActive) "Cam ON" else "Cam OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCameraActive) Emerald500 else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Quick Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Search item name, brand, or enter barcode...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}

@Composable
private fun MobileCameraStandbyBanner(
    onTurnOnCamera: () -> Unit,
    onOpenSlipOcr: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Desk Scanner Paused",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Camera off • Battery saving",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Turn On Camera Button
                Button(
                    onClick = onTurnOnCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .bouncingClickable(onClick = onTurnOnCamera)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Turn ON",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // OCR Slip Button
                OutlinedButton(
                    onClick = onOpenSlipOcr,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .bouncingClickable(onClick = onOpenSlipOcr)
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Slip",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletCameraStandbyPane(
    onTurnOnCamera: () -> Unit,
    onOpenSlipOcr: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PackReadyEmblem(size = 64.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Barcode Scanner in Standby",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The live camera stream is turned off to save battery. Tap below to re-enable hands-free barcode scanning.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onTurnOnCamera,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(46.dp)
                    .bouncingClickable(onClick = onTurnOnCamera)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Turn On Camera Scanner",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenSlipOcr,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .height(46.dp)
                    .bouncingClickable(onClick = onOpenSlipOcr)
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan Handwritten Slip (OCR)",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
