package com.packready.shopkeeper.presentation.checkout

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.FlipCameraAndroid
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
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMuted
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Cream50
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.Espresso900
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.ui.components.CameraXScannerView
import com.packready.shopkeeper.presentation.ui.components.CartBottomDrawer
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem
import com.packready.shopkeeper.presentation.ui.components.PackReadyHeaderBrand
import com.packready.shopkeeper.presentation.ui.components.PulseDot
import com.packready.shopkeeper.presentation.ui.components.ReceiptThermalPreviewDialog
import com.packready.shopkeeper.presentation.ui.components.ScanReticleOverlay
import com.packready.shopkeeper.presentation.ui.components.SpeedDialGrid
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCheckoutScreen(
    isActiveTab: Boolean = true,
    viewModel: PosViewModel = hiltViewModel(),
    onOpenProfile: (() -> Unit)? = null
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

    // Snackbar notifications - dismiss previous immediately to prevent stacking queue
    LaunchedEffect(uiState.toastMessage, uiState.errorMessage) {
        val toast = uiState.toastMessage
        val err = uiState.errorMessage
        if (toast != null || err != null) {
            val msg = toast ?: err ?: ""
            viewModel.onEvent(PosUiEvent.ClearMessages)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = msg,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
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

            val effectiveCameraActive = uiState.isCameraActive && isActiveTab

            if (isTabletLandscape) {
                // Tablet/Wide screen: Side-by-Side Dual Pane Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: Camera Scanner or Standby Panel (45% width)
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .padding(8.dp)
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (effectiveCameraActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(uiState.cameraScanMode.ratioWidthOverHeight)
                                    .clip(RoundedCornerShape(18.dp))
                            ) {
                                CameraXScannerView(
                                    isFrontCamera = uiState.isFrontCamera,
                                    isCameraActive = effectiveCameraActive,
                                    onToggleLens = { viewModel.onEvent(PosUiEvent.ToggleCameraLens) },
                                    customAnalyzer = viewModel.barcodeAnalyzer,
                                    onBarcodeDetected = { barcode ->
                                        viewModel.onEvent(PosUiEvent.OnBarcodeScanned(barcode))
                                    }
                                )
                                ScanReticleOverlay(
                                    isScanningActive = true,
                                    lastScannedBarcode = uiState.lastScannedBarcode,
                                    showStatusBadge = false
                                )

                                // Top Floating Action Controls: Live Status + Lens Flip + Ratio Toggle Pill
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Cam Live",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald500
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .bouncingClickable { viewModel.onEvent(PosUiEvent.ToggleCameraLens) },
                                            shape = RoundedCornerShape(20.dp),
                                            color = Roast900.copy(alpha = 0.90f),
                                            shadowElevation = 6.dp,
                                            border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FlipCameraAndroid,
                                                    contentDescription = "Switch Camera",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (uiState.isFrontCamera) "Front" else "Rear",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        CameraRatioTogglePill(
                                            activeMode = uiState.cameraScanMode,
                                            onSelectMode = { viewModel.onEvent(PosUiEvent.SetCameraScanMode(it)) }
                                        )
                                    }
                                }
                            }
                        } else {
                            TabletCameraStandbyPane(
                                onTurnOnCamera = { viewModel.onEvent(PosUiEvent.SetCameraActive(true)) }
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
                            isCameraActive = effectiveCameraActive,
                            onSearchQueryChange = { viewModel.onEvent(PosUiEvent.OnSearchQueryChange(it)) },
                            onToggleCamera = { viewModel.onEvent(PosUiEvent.ToggleCamera) },
                            onOpenProfile = onOpenProfile
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
                // Mobile Portrait: Top Header -> Camera Preview (if active) -> Quick Sale Catalog
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                ) {
                    // 1. Top Header: PackReady Logo, Profile, Camera On/Off & Desk Scanner Paused Tile
                    PosHeaderAndSearchBar(
                        searchQuery = uiState.searchQuery,
                        isCameraActive = effectiveCameraActive,
                        onSearchQueryChange = { viewModel.onEvent(PosUiEvent.OnSearchQueryChange(it)) },
                        onToggleCamera = { viewModel.onEvent(PosUiEvent.ToggleCamera) },
                        onOpenProfile = onOpenProfile
                    )

                    // 2. Camera Preview when Camera is Active
                    if (effectiveCameraActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(uiState.cameraScanMode.ratioWidthOverHeight)
                                .clip(RoundedCornerShape(16.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            CameraXScannerView(
                                isFrontCamera = uiState.isFrontCamera,
                                isCameraActive = effectiveCameraActive,
                                onToggleLens = { viewModel.onEvent(PosUiEvent.ToggleCameraLens) },
                                customAnalyzer = viewModel.barcodeAnalyzer,
                                onBarcodeDetected = { barcode ->
                                    viewModel.onEvent(PosUiEvent.OnBarcodeScanned(barcode))
                                }
                            )
                            ScanReticleOverlay(
                                isScanningActive = true,
                                lastScannedBarcode = uiState.lastScannedBarcode,
                                showStatusBadge = false
                            )

                            // Top Floating Action Controls: Cam Live status + Lens Flip + Mode toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Cam Live",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald500
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .bouncingClickable { viewModel.onEvent(PosUiEvent.ToggleCameraLens) },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Roast900.copy(alpha = 0.90f),
                                        shadowElevation = 6.dp,
                                        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlipCameraAndroid,
                                                contentDescription = "Switch Camera",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (uiState.isFrontCamera) "Front" else "Rear",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    CameraRatioTogglePill(
                                        activeMode = uiState.cameraScanMode,
                                        onSelectMode = { viewModel.onEvent(PosUiEvent.SetCameraScanMode(it)) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Lower Content Area: Speed Dial Grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
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

    // Completed Thermal Receipt Preview Dialog
    val completedSale = uiState.completedSale
    if (completedSale != null) {
        ReceiptThermalPreviewDialog(
            sale = completedSale,
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
    onOpenProfile: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Top Brand Header Row: Logo, Profile, Camera On/Off & Desk Scanner Paused Tile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PackReadyHeaderBrand(subtitle = "")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Unified Scanner Live / Paused Status & Quick-Toggle Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .bouncingClickable { onToggleCamera() },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isCameraActive) Color(0xFFE8F5E9) else Cream100,
                    border = BorderStroke(
                        1.dp,
                        if (isCameraActive) ForestGreen600.copy(alpha = 0.5f) else Color(0xFFE2D6CB)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCameraActive) {
                            PulseDot(color = ForestGreen600, size = 6.dp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Scanner Live",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen600,
                                fontSize = 11.5.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = Caramel700,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Scanner Paused",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Caramel700,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                // Sleek Circular Profile Avatar Button
                if (onOpenProfile != null) {
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .bouncingClickable { onOpenProfile() },
                        shape = CircleShape,
                        color = Cream100,
                        border = BorderStroke(1.dp, Color(0xFFE2D6CB))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = Caramel700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Search item name, brand, or barcode...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = BrownTextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Caramel600,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = BrownTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Caramel600,
                unfocusedBorderColor = Color(0xFFE4DAD0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun MobileCameraStandbyBanner(
    onTurnOnCamera: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
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
                        text = "Camera off • Tap Turn ON to scan barcodes",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onTurnOnCamera,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .bouncingClickable(onClick = onTurnOnCamera)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Turn ON",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TabletCameraStandbyPane(
    onTurnOnCamera: () -> Unit
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
                text = "The live camera stream is turned off to save battery. Tap below to re-enable hands-free 4:3 barcode scanning.",
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
        }
    }
}

@Composable
private fun CameraRatioTogglePill(
    activeMode: CameraScanMode,
    onSelectMode: (CameraScanMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Roast900.copy(alpha = 0.92f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mount (4:3) Option (vertical:horizontal => 4:3)
            val isMount = activeMode == CameraScanMode.MOUNT_4_3
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .bouncingClickable { onSelectMode(CameraScanMode.MOUNT_4_3) },
                shape = RoundedCornerShape(16.dp),
                color = if (isMount) Caramel400 else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CropPortrait,
                        contentDescription = "Mount 4:3",
                        tint = if (isMount) Espresso900 else Cream50.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mount 4:3",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isMount) FontWeight.Bold else FontWeight.Medium,
                        color = if (isMount) Espresso900 else Cream50.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Hand (1:2) Option (vertical:horizontal => 1:2)
            val isHand = activeMode == CameraScanMode.HAND_1_2
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .bouncingClickable { onSelectMode(CameraScanMode.HAND_1_2) },
                shape = RoundedCornerShape(16.dp),
                color = if (isHand) Caramel400 else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CropLandscape,
                        contentDescription = "Hand 1:2",
                        tint = if (isHand) Espresso900 else Cream50.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hand 1:2",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isHand) FontWeight.Bold else FontWeight.Medium,
                        color = if (isHand) Espresso900 else Cream50.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

