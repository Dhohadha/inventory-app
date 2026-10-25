package com.packready.shopkeeper.presentation.restock

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.packready.shopkeeper.data.local.ProductImageStorage
import com.packready.shopkeeper.data.remote.CatalogApiService
import com.packready.shopkeeper.data.remote.UploadApiService
import com.packready.shopkeeper.domain.model.DynamicAttributes
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.presentation.theme.AmberWarning100
import com.packready.shopkeeper.presentation.theme.AmberWarning600
import com.packready.shopkeeper.presentation.theme.BrownBorder
import com.packready.shopkeeper.presentation.theme.BrownDivider
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMedium
import com.packready.shopkeeper.presentation.theme.BrownTextMuted
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel50
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Cream200
import com.packready.shopkeeper.presentation.theme.Cream50
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.Espresso800
import com.packready.shopkeeper.presentation.theme.Espresso900
import com.packready.shopkeeper.presentation.theme.ForestGreen100
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.theme.RustError100
import com.packready.shopkeeper.presentation.theme.RustError600
import com.packready.shopkeeper.presentation.ui.components.CameraCaptureMode
import com.packready.shopkeeper.presentation.ui.components.ImageEditorDialog
import com.packready.shopkeeper.presentation.ui.components.InAppCameraCaptureDialog
import com.packready.shopkeeper.presentation.ui.components.MultiImageEditorRow
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

import com.packready.shopkeeper.data.local.GeminiUsageTracker
import com.packready.shopkeeper.data.local.GeminiUsageStats

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuickAddEntryPoint {
    fun catalogApiService(): CatalogApiService
    fun uploadApiService(): UploadApiService
    fun geminiUsageTracker(): GeminiUsageTracker
}

private fun matchCategory(serverCategory: String): ProductCategory {
    return ProductCategory.values().firstOrNull {
        it.name.equals(serverCategory, ignoreCase = true) ||
        it.displayName.equals(serverCategory, ignoreCase = true) ||
        it.name.replace("_", " ").contains(serverCategory, ignoreCase = true) ||
        serverCategory.contains(it.name.replace("_", " "), ignoreCase = true)
    } ?: ProductCategory.FOOD_GROCERY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddProductDialog(
    initialBarcode: String? = null,
    onConfirm: (
        name: String,
        barcode: String,
        brand: String,
        category: ProductCategory,
        unit: String,
        costPrice: Double,
        sellingPrice: Double,
        mrp: Double,
        initialStock: Double,
        attributes: DynamicAttributes?,
        visualEmbeddings: List<Pair<String, FloatArray>>,
        images: List<String>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val productImages = remember { mutableStateListOf<String>() }
    val uploadingImages = remember { mutableStateListOf<String>() }
    var isCameraCaptureOpen by remember { mutableStateOf(false) }
    var editingImagePath by remember { mutableStateOf<String?>(null) }
    var editingImageIndex by remember { mutableStateOf<Int?>(null) }
    var isSavingAndUploading by remember { mutableStateOf(false) }

    val entryPoint = remember(context) {
        try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                QuickAddEntryPoint::class.java
            )
        } catch (e: Exception) {
            null
        }
    }
    val catalogApiService = entryPoint?.catalogApiService()
    val uploadApiService = entryPoint?.uploadApiService()
    val geminiTracker = entryPoint?.geminiUsageTracker()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        coroutineScope.launch {
            uris.forEachIndexed { idx, uri ->
                val savedPath = ProductImageStorage.saveUri(context, uri)
                if (savedPath != null) {
                    productImages.add(savedPath)
                    if (editingImagePath == null && idx == 0) {
                        editingImagePath = savedPath
                        editingImageIndex = productImages.size - 1
                    }
                }
            }
        }
    }

    var barcode by remember { mutableStateOf(initialBarcode ?: "890${(1000000000..9999999999).random()}") }
    var isExtractingDetails by remember { mutableStateOf(false) }
    var extractionStatusMessage by remember { mutableStateOf<String?>(null) }
    var extractionStatusIsError by remember { mutableStateOf(false) }
    var showGeminiOption by remember { mutableStateOf(false) }
    var isGeminiExtracting by remember { mutableStateOf(false) }
    var usageStats by remember { mutableStateOf<GeminiUsageStats?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProductCategory.FOOD_GROCERY) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf("pcs") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("24") }

    // Dynamic Attribute Fields
    var netWeight by remember { mutableStateOf("") }
    var isVegetarian by remember { mutableStateOf(true) }
    var wattage by remember { mutableStateOf("") }
    var warrantyMonths by remember { mutableStateOf("") }
    var fragrance by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var grainGrade by remember { mutableStateOf("") }

    // Profit & Margin calculations
    val costDouble = costPrice.toDoubleOrNull()
    val sellDouble = sellingPrice.toDoubleOrNull()
    val profitVal = if (costDouble != null && sellDouble != null) sellDouble - costDouble else null
    val marginPercent = if (profitVal != null && sellDouble != null && sellDouble > 0) {
        (profitVal / sellDouble) * 100
    } else null

    Dialog(
        onDismissRequest = { if (!isSavingAndUploading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .imePadding()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(26.dp),
            color = Cream50,
            tonalElevation = 10.dp,
            border = BorderStroke(1.2.dp, Caramel400.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PackReadyEmblem(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Add New Product SKU",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrownTextDark,
                                fontSize = 17.sp
                            )
                            Surface(
                                color = Caramel100,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = Caramel700,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "EAN: $barcode",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = Caramel700,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Cream200,
                        modifier = Modifier.size(34.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = BrownTextDark,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Card 1: Product Photos Studio Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrownBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Multi-Image Editor Component with automatic Cloudinary upload & status
                        MultiImageEditorRow(
                            images = productImages,
                            uploadingImages = uploadingImages.toSet(),
                            onAddViaCamera = { isCameraCaptureOpen = true },
                            onAddViaGallery = { galleryLauncher.launch("image/*") },
                            onEditImage = { index, path ->
                                editingImagePath = path
                                editingImageIndex = index
                            },
                            onRemoveImage = { index ->
                                if (index in productImages.indices) {
                                    productImages.removeAt(index)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 2: Identification & Basic Info Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrownBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Item Identity & Details",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Caramel700
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode / EAN") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Caramel600, modifier = Modifier.size(18.dp))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Caramel600,
                                unfocusedBorderColor = BrownBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button to Extract / Autofill Details from Server
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .bouncingClickable {
                                    if (!isExtractingDetails && barcode.isNotBlank() && catalogApiService != null) {
                                        isExtractingDetails = true
                                        extractionStatusMessage = null
                                        showGeminiOption = false
                                        coroutineScope.launch {
                                            try {
                                                // 1. Check Master Catalog ONLY (autoAiFallback = false)
                                                val response = catalogApiService.getProductByBarcode(barcode.trim(), autoAiFallback = false)
                                                if (response.isSuccessful && response.body() != null) {
                                                    val prod = response.body()!!
                                                    name = prod.name
                                                    brand = prod.brand
                                                    selectedCategory = matchCategory(prod.category)
                                                    unit = prod.unit
                                                    if (prod.standardMrp > 0) {
                                                        mrp = prod.standardMrp.toString()
                                                        sellingPrice = prod.standardMrp.toString()
                                                        costPrice = (prod.standardMrp * 0.85).toString()
                                                    }
                                                    prod.dynamicAttributes?.let { da ->
                                                        da.netWeight?.let { netWeight = it }
                                                        da.isVegetarian?.let { isVegetarian = it }
                                                        da.wattage?.let { wattage = it }
                                                        da.warrantyMonths?.let { warrantyMonths = it.toString() }
                                                        da.fragrance?.let { fragrance = it }
                                                        da.size?.let { size = it }
                                                        da.grainGrade?.let { grainGrade = it }
                                                    }
                                                    prod.images.forEach { imgDto ->
                                                        val remoteUrl = imgDto.secureUrl
                                                        if (!productImages.contains(remoteUrl)) {
                                                            productImages.add(remoteUrl)
                                                            coroutineScope.launch {
                                                                ProductImageStorage.saveRemoteUrlLowSpace(context, remoteUrl, barcode)
                                                            }
                                                        }
                                                    }
                                                    extractionStatusMessage = "✓ Verified in Master Catalog: ${prod.name}"
                                                    extractionStatusIsError = false
                                                    showGeminiOption = false
                                                } else {
                                                    // Product not found in Master Catalog: reveal Gemini AI option with live rate-limit tracking
                                                    usageStats = geminiTracker?.getUsageStats()
                                                    extractionStatusMessage = "Product not found in Master Catalog or Open Food Facts. You can extract with Gemini AI below."
                                                    extractionStatusIsError = true
                                                    showGeminiOption = true
                                                }
                                            } catch (e: Exception) {
                                                usageStats = geminiTracker?.getUsageStats()
                                                extractionStatusMessage = "Product not found in Master Catalog or Open Food Facts. You can extract with Gemini AI below."
                                                extractionStatusIsError = true
                                                showGeminiOption = true
                                            } finally {
                                                isExtractingDetails = false
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (barcode.isNotBlank()) Caramel50 else Cream100,
                            border = BorderStroke(1.dp, if (barcode.isNotBlank()) Caramel500.copy(alpha = 0.7f) else BrownBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isExtractingDetails) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Caramel600,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Extracting Details from Server...",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Caramel700
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (barcode.isNotBlank()) Caramel600 else BrownTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Extract Details from Server",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (barcode.isNotBlank()) Caramel700 else BrownTextMuted
                                    )
                                }
                            }
                        }

                        // Status Feedback Banner
                        AnimatedVisibility(visible = extractionStatusMessage != null) {
                            extractionStatusMessage?.let { msg ->
                                Column {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (extractionStatusIsError) AmberWarning100 else ForestGreen100,
                                        border = BorderStroke(1.dp, if (extractionStatusIsError) AmberWarning600.copy(alpha = 0.3f) else ForestGreen600.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (extractionStatusIsError) Icons.Default.Info else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (extractionStatusIsError) AmberWarning600 else ForestGreen600,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = msg,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (extractionStatusIsError) AmberWarning600 else ForestGreen600
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Gemini AI Rate-Tracked Extraction Card
                        AnimatedVisibility(visible = showGeminiOption) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF5F3FF))
                                    .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Gemini AI Assistant",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5B21B6)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Key: ${usageStats?.maskedApiKey ?: "Not Set"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6B7280)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Edit Key",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7C3AED),
                                            modifier = Modifier.bouncingClickable {
                                                tempApiKey = geminiTracker?.getApiKey() ?: ""
                                                showApiKeyDialog = true
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Live Rate Tracking Meters (RPM & RPD)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // RPM Box
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFEDE9FE))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Requests/Min:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF4B5563)
                                                )
                                                Text(
                                                    text = "${usageStats?.currentRpm ?: 0} / 15",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if ((usageStats?.currentRpm ?: 0) >= 14) Color.Red else Color(0xFF1F2937)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { ((usageStats?.currentRpm ?: 0) / 15f).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = if ((usageStats?.currentRpm ?: 0) >= 14) Color.Red else Color(0xFF7C3AED),
                                                trackColor = Color(0xFFEDE9FE)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${usageStats?.rpmRemaining ?: 15} left this min",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = Color(0xFF6B7280)
                                            )
                                        }
                                    }

                                    // RPD Box
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFEDE9FE))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Daily Quota:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF4B5563)
                                                )
                                                Text(
                                                    text = "${usageStats?.currentRpd ?: 0} / 500",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if ((usageStats?.currentRpd ?: 0) >= 495) Color.Red else Color(0xFF1F2937)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { ((usageStats?.currentRpd ?: 0) / 500f).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = if ((usageStats?.currentRpd ?: 0) >= 495) Color.Red else Color(0xFF059669),
                                                trackColor = Color(0xFFD1FAE5)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${usageStats?.rpdRemaining ?: 500} left today",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = Color(0xFF6B7280)
                                            )
                                        }
                                    }
                                }

                                if (usageStats?.isBlocked == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFFB91C1C),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = usageStats?.blockReason ?: "Rate limit active.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF991B1B),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // AI Extract Button
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .bouncingClickable {
                                            if (usageStats?.isBlocked != true && !isGeminiExtracting && catalogApiService != null) {
                                                isGeminiExtracting = true
                                                coroutineScope.launch {
                                                    try {
                                                        val userKey = geminiTracker?.getApiKey()
                                                        val response = catalogApiService.extractProductWithGemini(
                                                            com.packready.shopkeeper.data.remote.dto.GeminiExtractRequestDto(
                                                                barcode = barcode.trim(),
                                                                apiKey = if (!userKey.isNullOrBlank()) userKey else null
                                                            ),
                                                            apiKey = if (!userKey.isNullOrBlank()) userKey else null
                                                        )
                                                        if (response.isSuccessful && response.body() != null) {
                                                            val prod = response.body()!!
                                                            name = prod.name
                                                            brand = prod.brand
                                                            selectedCategory = matchCategory(prod.category)
                                                            unit = prod.unit
                                                            if (prod.standardMrp > 0) {
                                                                mrp = prod.standardMrp.toString()
                                                                sellingPrice = prod.standardMrp.toString()
                                                                costPrice = (prod.standardMrp * 0.85).toString()
                                                            }
                                                            prod.dynamicAttributes?.let { da ->
                                                                da.netWeight?.let { netWeight = it }
                                                                da.isVegetarian?.let { isVegetarian = it }
                                                                da.wattage?.let { wattage = it }
                                                                da.warrantyMonths?.let { warrantyMonths = it.toString() }
                                                                da.fragrance?.let { fragrance = it }
                                                                da.size?.let { size = it }
                                                                da.grainGrade?.let { grainGrade = it }
                                                            }
                                                            prod.images.forEach { imgDto ->
                                                                val remoteUrl = imgDto.secureUrl
                                                                if (!productImages.contains(remoteUrl)) {
                                                                    productImages.add(remoteUrl)
                                                                    coroutineScope.launch {
                                                                        ProductImageStorage.saveRemoteUrlLowSpace(context, remoteUrl, barcode)
                                                                    }
                                                                }
                                                            }
                                                            geminiTracker?.recordRequest()
                                                            usageStats = geminiTracker?.getUsageStats()
                                                            extractionStatusMessage = "✨ AI Auto-Extracted (Gemini): ${prod.name}"
                                                            extractionStatusIsError = false
                                                            showGeminiOption = false
                                                        } else {
                                                            usageStats = geminiTracker?.getUsageStats()
                                                            extractionStatusMessage = if (response.code() == 404) {
                                                                "Product not found in Open Food Facts or Gemini AI. Please enter details manually."
                                                            } else {
                                                                "Gemini could not identify barcode: ${response.message()}"
                                                            }
                                                            extractionStatusIsError = true
                                                        }
                                                    } catch (e: Exception) {
                                                        usageStats = geminiTracker?.getUsageStats()
                                                        extractionStatusMessage = "Product not found in Open Food Facts or Gemini AI. Please enter details manually."
                                                        extractionStatusIsError = true
                                                    } finally {
                                                        isGeminiExtracting = false
                                                    }
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (usageStats?.isBlocked == true) Color(0xFFE5E7EB) else Color(0xFF7C3AED),
                                    border = BorderStroke(1.dp, if (usageStats?.isBlocked == true) Color(0xFFD1D5DB) else Color(0xFF6D28D9))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isGeminiExtracting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Extracting via Gemini...",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (usageStats?.isBlocked == true) Color(0xFF9CA3AF) else Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (usageStats?.hasApiKey == false) {
                                                    "Set API Key to Extract"
                                                } else if (usageStats?.isBlocked == true) {
                                                    "Rate Limit Safety Lock Active"
                                                } else {
                                                    "Extract with Gemini AI ✨"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (usageStats?.isBlocked == true) Color(0xFF9CA3AF) else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showApiKeyDialog) {
                            AlertDialog(
                                onDismissRequest = { showApiKeyDialog = false },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Gemini API Key Settings",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Espresso900
                                        )
                                    }
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = "Enter your Google AI Studio API key to utilize your personal free tier (500 RPD, 15 RPM).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrownTextMuted
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(
                                            value = tempApiKey,
                                            onValueChange = { tempApiKey = it },
                                            label = { Text("Gemini API Key") },
                                            placeholder = { Text("AIzaSy...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF7C3AED),
                                                unfocusedBorderColor = BrownBorder
                                            )
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            geminiTracker?.setApiKey(tempApiKey)
                                            usageStats = geminiTracker?.getUsageStats()
                                            showApiKeyDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                                    ) {
                                        Text("Save Key")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showApiKeyDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Product Display Name *") },
                            placeholder = { Text("e.g. Parle-G Gold Biscuit 250g", color = BrownTextMuted) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Caramel600,
                                unfocusedBorderColor = BrownBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = brand,
                                onValueChange = { brand = it },
                                label = { Text("Brand / Mfr") },
                                placeholder = { Text("e.g. Parle, Amul", color = BrownTextMuted) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Caramel600,
                                    unfocusedBorderColor = BrownBorder
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Unit") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Caramel600,
                                    unfocusedBorderColor = BrownBorder
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // Quick Unit Choice Pills
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("pcs", "kg", "g", "ltr", "ml", "pack", "box", "bottle").forEach { unitChoice ->
                                val isSelected = unit.equals(unitChoice, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Caramel600 else Cream200,
                                    border = BorderStroke(1.dp, if (isSelected) Caramel600 else BrownBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { unit = unitChoice }
                                ) {
                                    Text(
                                        text = unitChoice,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else BrownTextMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 3: Category Selector with Quick Category Chips
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrownBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Catalog Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Caramel700
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Caramel600,
                                    unfocusedBorderColor = BrownBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                ProductCategory.entries.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.displayName) },
                                        onClick = {
                                            selectedCategory = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Category Selection Chips Row
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                ProductCategory.FOOD_GROCERY to "Grocery 🛒",
                                ProductCategory.BEVERAGES to "Beverages 🥤",
                                ProductCategory.PERSONAL_CARE to "Personal Care 🧼",
                                ProductCategory.HOUSEHOLD_CLEANING to "Cleaning 🧹",
                                ProductCategory.ELECTRONICS_ELECTRICAL to "Electronics ⚡",
                                ProductCategory.LOOSE_STAPLES to "Staples 🌾",
                                ProductCategory.APPAREL_FOOTWEAR to "Apparel 👕"
                            ).forEach { (cat, label) ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Caramel600 else Cream200,
                                    border = BorderStroke(1.dp, if (isSelected) Caramel600 else BrownBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedCategory = cat }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else BrownTextMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 4: Pricing & Profit Margin Card (Kirana Favorite Feature)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrownBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Pricing & Inventory Stock",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Caramel700
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costPrice,
                                onValueChange = { costPrice = it },
                                label = { Text("Cost (₹)") },
                                placeholder = { Text("0.00", color = BrownTextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Caramel600,
                                    unfocusedBorderColor = BrownBorder
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sellingPrice,
                                onValueChange = { sellingPrice = it },
                                label = { Text("Sell (₹)") },
                                placeholder = { Text("0.00", color = BrownTextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Caramel600,
                                    unfocusedBorderColor = BrownBorder
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = stock,
                                onValueChange = { stock = it },
                                label = { Text("Stock") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Caramel600,
                                    unfocusedBorderColor = BrownBorder
                                ),
                                modifier = Modifier.weight(0.9f),
                                singleLine = true
                            )
                        }

                        // Live Profit Margin Pill
                        if (profitVal != null && marginPercent != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isProfitable = profitVal > 0
                            val isLoss = profitVal < 0
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isProfitable -> ForestGreen100
                                    isLoss -> RustError100
                                    else -> Caramel100
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isProfitable -> ForestGreen600.copy(alpha = 0.4f)
                                        isLoss -> RustError600.copy(alpha = 0.4f)
                                        else -> Caramel500.copy(alpha = 0.4f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isProfitable) Icons.Default.CheckCircle else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = when {
                                            isProfitable -> ForestGreen600
                                            isLoss -> RustError600
                                            else -> Caramel700
                                        },
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isProfitable -> "Net Margin: +₹${String.format("%.2f", profitVal)} / unit (${String.format("%.1f", marginPercent)}% margin)"
                                            isLoss -> "Selling at ₹${String.format("%.2f", -profitVal)} loss below cost!"
                                            else -> "Breakeven (0% margin)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isProfitable -> ForestGreen600
                                            isLoss -> RustError600
                                            else -> Caramel700
                                        },
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 5: Category Specialized Attributes
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrownBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Specialized Attributes (${selectedCategory.displayName})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Caramel700
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        when (selectedCategory) {
                            ProductCategory.FOOD_GROCERY, ProductCategory.BEVERAGES -> {
                                // Authentic Indian Green Veg / Brown Non-Veg Toggle Card
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { isVegetarian = !isVegetarian },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isVegetarian) ForestGreen100.copy(alpha = 0.5f) else Color(0xFFF7ECE8),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isVegetarian) ForestGreen600.copy(alpha = 0.4f) else Color(0xFF8D6E63).copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Authentic FSSAI Veg Green Box or Non-Veg Brown Box
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .border(
                                                        1.6.dp,
                                                        if (isVegetarian) ForestGreen600 else Color(0xFF8D6E63),
                                                        RoundedCornerShape(3.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isVegetarian) ForestGreen600 else Color(0xFF8D6E63))
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isVegetarian) "100% Pure Vegetarian" else "Non-Vegetarian / Contains Egg",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isVegetarian) ForestGreen600 else Color(0xFF5D4037)
                                                )
                                                Text(
                                                    text = if (isVegetarian) "FSSAI Green Veg mark applied on receipts" else "Brown Non-Veg mark applied on receipts",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = BrownTextMuted
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = isVegetarian,
                                            onCheckedChange = { isVegetarian = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = ForestGreen600,
                                                checkedTrackColor = ForestGreen100,
                                                uncheckedThumbColor = Color(0xFF8D6E63),
                                                uncheckedTrackColor = Color(0xFFEFE8E4)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = netWeight,
                                    onValueChange = { netWeight = it },
                                    label = { Text("Net Weight / Volume (e.g. 500g, 1L)") },
                                    placeholder = { Text("500g, 1 kg, 250ml", color = BrownTextMuted) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Caramel600,
                                        unfocusedBorderColor = BrownBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // Quick suggestion chips for weight
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("100g", "250g", "500g", "1 kg", "2 kg", "5 kg", "200ml", "500ml", "1 L").forEach { wChoice ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Cream200,
                                            border = BorderStroke(1.dp, BrownBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { netWeight = wChoice }
                                        ) {
                                            Text(
                                                text = wChoice,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BrownTextMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                            ProductCategory.ELECTRONICS_ELECTRICAL -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = wattage,
                                        onValueChange = { wattage = it },
                                        label = { Text("Wattage (e.g. 9W)") },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = Caramel600,
                                            unfocusedBorderColor = BrownBorder
                                        ),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = warrantyMonths,
                                        onValueChange = { warrantyMonths = it },
                                        label = { Text("Warranty (Months)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = Caramel600,
                                            unfocusedBorderColor = BrownBorder
                                        ),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            ProductCategory.PERSONAL_CARE, ProductCategory.HOUSEHOLD_CLEANING -> {
                                OutlinedTextField(
                                    value = fragrance,
                                    onValueChange = { fragrance = it },
                                    label = { Text("Variant / Fragrance (e.g. Lemon, Menthol)") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Caramel600,
                                        unfocusedBorderColor = BrownBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            ProductCategory.APPAREL_FOOTWEAR -> {
                                OutlinedTextField(
                                    value = size,
                                    onValueChange = { size = it },
                                    label = { Text("Size (S, M, L, XL, 32, 34)") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Caramel600,
                                        unfocusedBorderColor = BrownBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            ProductCategory.LOOSE_STAPLES -> {
                                OutlinedTextField(
                                    value = grainGrade,
                                    onValueChange = { grainGrade = it },
                                    label = { Text("Grain Grade / Quality (Superfine, Raw, Desi)") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Caramel600,
                                        unfocusedBorderColor = BrownBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Buttons (pinned at bottom so they never get cut off)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSavingAndUploading,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BrownBorder)
                ) {
                    Text("Cancel", color = BrownTextDark, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (isSavingAndUploading) return@Button
                        coroutineScope.launch {
                            isSavingAndUploading = true
                            val finalImageUrls = mutableListOf<String>()
                            val cleanName = (name.ifBlank { "item" }).trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val cleanBarcode = barcode.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val baseLabel = "${cleanBarcode}_${cleanName}".take(60)

                            productImages.forEachIndexed { idx, pathOrUrl ->
                                if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                                    finalImageUrls.add(pathOrUrl)
                                } else {
                                    var resolvedUrl = pathOrUrl
                                    try {
                                        val file = File(pathOrUrl)
                                        if (file.exists() && uploadApiService != null) {
                                            val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                            val part = MultipartBody.Part.createFormData("file", file.name, reqFile)
                                            val folderPart = "products".toRequestBody("text/plain".toMediaTypeOrNull())
                                            val barcodePart = barcode.toRequestBody("text/plain".toMediaTypeOrNull())
                                            val publicIdPart = "${baseLabel}_${idx + 1}".toRequestBody("text/plain".toMediaTypeOrNull())
                                            val response = uploadApiService.uploadProductImage(part, folderPart, barcodePart, publicIdPart)
                                            if (response.isSuccessful && !response.body()?.secureUrl.isNullOrBlank()) {
                                                resolvedUrl = response.body()!!.secureUrl!!
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Keep local path on network failure
                                    }
                                    finalImageUrls.add(resolvedUrl)
                                }
                            }

                            val cp = costPrice.toDoubleOrNull() ?: 0.0
                            val sp = sellingPrice.toDoubleOrNull() ?: (cp * 1.15)
                            val m = mrp.toDoubleOrNull() ?: (sp * 1.05)
                            val st = stock.toDoubleOrNull() ?: 12.0

                            val dynAttrs = DynamicAttributes(
                                netWeight = netWeight.ifBlank { null },
                                isVegetarian = isVegetarian,
                                wattage = wattage.ifBlank { null },
                                warrantyMonths = warrantyMonths.toIntOrNull(),
                                fragrance = fragrance.ifBlank { null },
                                size = size.ifBlank { null },
                                grainGrade = grainGrade.ifBlank { null }
                            )

                            isSavingAndUploading = false
                            onConfirm(
                                name.ifBlank { "Item $barcode" },
                                barcode,
                                brand.ifBlank { "General" },
                                selectedCategory,
                                unit,
                                cp,
                                sp,
                                m,
                                st,
                                dynAttrs,
                                emptyList(),
                                finalImageUrls
                            )
                        }
                    },
                    enabled = !isSavingAndUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Espresso800),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                ) {
                    if (isSavingAndUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Uploading & Saving...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save & Add to Catalog",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

    if (isCameraCaptureOpen) {
        InAppCameraCaptureDialog(
            mode = CameraCaptureMode.MULTI_PHOTO,
            title = "Capture Packaging Photos",
            instructionText = "Capture front, back, and detail packaging photos",
            onPhotosCaptured = { bitmaps ->
                coroutineScope.launch {
                    val newPaths = mutableListOf<String>()
                    bitmaps.forEach { bitmap ->
                        val path = ProductImageStorage.saveBitmap(context, bitmap)
                        productImages.add(path)
                        newPaths.add(path)
                    }
                    if (newPaths.isNotEmpty()) {
                        editingImagePath = newPaths.first()
                        editingImageIndex = productImages.indexOf(newPaths.first())
                    }
                }
                isCameraCaptureOpen = false
            },
            onDismiss = { isCameraCaptureOpen = false }
        )
    }

    if (editingImagePath != null) {
        ImageEditorDialog(
            initialImagePath = editingImagePath!!,
            onDismiss = {
                editingImagePath = null
                editingImageIndex = null
            },
            onImageSaved = { newSavedPath ->
                val idx = editingImageIndex
                if (idx != null && idx in productImages.indices) {
                    productImages[idx] = newSavedPath
                } else {
                    productImages.add(newSavedPath)
                }
                editingImagePath = null
                editingImageIndex = null
            }
        )
    }
}
