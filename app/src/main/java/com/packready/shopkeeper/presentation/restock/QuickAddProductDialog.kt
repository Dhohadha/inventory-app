package com.packready.shopkeeper.presentation.restock

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.Brush
import com.packready.shopkeeper.presentation.ui.components.ProductThumbnailImage
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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import android.graphics.Bitmap
import androidx.compose.ui.text.style.TextAlign
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import com.packready.shopkeeper.presentation.ui.components.BarcodeRenderer
import com.packready.shopkeeper.data.hardware.BluetoothPrinterManager
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
    fun bluetoothPrinterManager(): BluetoothPrinterManager
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
    val bluetoothPrinterManager = entryPoint?.bluetoothPrinterManager()

    var isBarcodeGenerated by remember { mutableStateOf(false) }
    var isTwoPhotoCameraOpen by remember { mutableStateOf(false) }
    var isAnalyzingTwoPhotos by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var labelCopies by remember { mutableStateOf(1) }
    var printStatusMessage by remember { mutableStateOf<String?>(null) }
    var expiryDate by remember { mutableStateOf("") }
    var mfgDate by remember { mutableStateOf("") }

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

    var barcode by remember { mutableStateOf(initialBarcode ?: "") }

    fun generateInternalEan(): String {
        val prefix = (20..29).random().toString()
        val randomMiddle = (1000000000L..9999999999L).random().toString().take(10)
        val raw12 = prefix + randomMiddle
        var sumOdd = 0
        var sumEven = 0
        raw12.forEachIndexed { i, ch ->
            val digit = ch - '0'
            if (i % 2 == 0) sumOdd += digit else sumEven += digit
        }
        val checkDigit = (10 - ((sumOdd + sumEven * 3) % 10)) % 10
        return "$raw12$checkDigit"
    }
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

    fun processTwoPhotos(frontBitmap: Bitmap, backBitmap: Bitmap?) {
        coroutineScope.launch {
            isAnalyzingTwoPhotos = true
            extractionStatusMessage = "Analyzing packaging with Gemini Vision & ML Kit..."
            extractionStatusIsError = false

            val savedFront = ProductImageStorage.saveBitmap(context, frontBitmap)
            if (!productImages.contains(savedFront)) productImages.add(savedFront)
            if (backBitmap != null) {
                val savedBack = ProductImageStorage.saveBitmap(context, backBitmap)
                if (!productImages.contains(savedBack)) productImages.add(savedBack)
            }

            var detectedCode: String? = null
            try {
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                )
                val targetBmp = backBitmap ?: frontBitmap
                val inputImg = InputImage.fromBitmap(targetBmp, 0)
                val barcodes = kotlinx.coroutines.suspendCancellableCoroutine<List<Barcode>> { cont ->
                    scanner.process(inputImg)
                        .addOnSuccessListener { if (cont.isActive) cont.resume(it) {} }
                        .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) {} }
                }
                detectedCode = barcodes.firstOrNull()?.rawValue

                if (detectedCode.isNullOrBlank() && backBitmap != null) {
                    val frontInput = InputImage.fromBitmap(frontBitmap, 0)
                    val frontBarcodes = kotlinx.coroutines.suspendCancellableCoroutine<List<Barcode>> { cont ->
                        scanner.process(frontInput)
                            .addOnSuccessListener { if (cont.isActive) cont.resume(it) {} }
                            .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) {} }
                    }
                    detectedCode = frontBarcodes.firstOrNull()?.rawValue
                }
            } catch (e: Exception) {
                android.util.Log.w("QuickAdd", "ML kit detection error: ${e.message}")
            }

            try {
                val frontStream = java.io.ByteArrayOutputStream()
                frontBitmap.compress(Bitmap.CompressFormat.JPEG, 85, frontStream)
                val frontBytes = frontStream.toByteArray()
                val frontPart = MultipartBody.Part.createFormData(
                    "front_image",
                    "front_${System.currentTimeMillis()}.jpg",
                    frontBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )

                val backPart = backBitmap?.let { bmp ->
                    val backStream = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, backStream)
                    val backBytes = backStream.toByteArray()
                    MultipartBody.Part.createFormData(
                        "back_image",
                        "back_${System.currentTimeMillis()}.jpg",
                        backBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                }

                val barcodeReqBody = detectedCode?.toRequestBody("text/plain".toMediaTypeOrNull())
                val userKey = geminiTracker?.getApiKey()

                if (catalogApiService != null) {
                    val response = catalogApiService.extractByPhotos(
                        frontImage = frontPart,
                        backImage = backPart,
                        detectedBarcode = barcodeReqBody,
                        apiKey = if (!userKey.isNullOrBlank()) userKey else null
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val prod = response.body()!!
                        barcode = prod.barcode
                        isBarcodeGenerated = prod.isBarcodeGenerated
                        name = prod.name
                        brand = prod.brand
                        selectedCategory = matchCategory(prod.category)
                        unit = prod.unit
                        if (prod.standardMrp > 0) {
                            mrp = prod.standardMrp.toString()
                            sellingPrice = if (prod.sellingPrice > 0) prod.sellingPrice.toString() else prod.standardMrp.toString()
                            costPrice = if (prod.costPrice > 0) prod.costPrice.toString() else (prod.standardMrp * 0.85).toString()
                        }

                        prod.dynamicAttributes?.let { da ->
                            da.netWeight?.let { netWeight = it }
                            da.isVegetarian?.let { isVegetarian = it }
                            da.expiryDate?.let { expiryDate = it }
                            da.mfgDate?.let { mfgDate = it }
                            da.wattage?.let { wattage = it }
                            da.warrantyMonths?.let { warrantyMonths = it.toString() }
                            da.fragrance?.let { fragrance = it }
                            da.size?.let { size = it }
                            da.grainGrade?.let { grainGrade = it }
                        }

                        geminiTracker?.recordRequest()
                        usageStats = geminiTracker?.getUsageStats()

                        if (prod.isBarcodeGenerated) {
                            extractionStatusMessage = "⚡ Auto-generated internal EAN barcode: ${prod.barcode} (Ready to print!)"
                        } else {
                            extractionStatusMessage = "✨ Extracted from packaging! Barcode: ${prod.barcode} (Type: ${prod.codeType})"
                        }
                        extractionStatusIsError = false
                        showGeminiOption = false
                    } else {
                        val err = response.errorBody()?.string() ?: response.message()
                        extractionStatusMessage = "Photo AI extraction failed: $err"
                        extractionStatusIsError = true
                    }
                }
            } catch (e: Exception) {
                extractionStatusMessage = "Photo AI extraction error: ${e.message}"
                extractionStatusIsError = true
            } finally {
                isAnalyzingTwoPhotos = false
            }
        }
    }


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
                            if (barcode.isNotBlank()) {
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
                                            text = if (isBarcodeGenerated) "Internal EAN: $barcode" else "Barcode: $barcode",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = Caramel700,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "AI Vision Auto-Fill or Manual Entry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrownTextMuted,
                                    fontSize = 11.sp
                                )
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
                    // Card 1: Packaging Studio & AI Auto-Enroll
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BrownBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (productImages.isEmpty()) {
                                // Hero State: No photos yet -> Inviting 2-Photo Vision AI Banner
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .bouncingClickable { isTwoPhotoCameraOpen = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Transparent
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF4C1D95),
                                                        Color(0xFF6D28D9),
                                                        Color(0xFF7C3AED)
                                                    )
                                                )
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Top Header Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0x33FFFFFF),
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.AutoAwesome,
                                                                contentDescription = null,
                                                                tint = Color(0xFFFDE047),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "Smart 2-Photo AI Auto-Enroll",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            text = "Multimodal Vision & ML Kit",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFFDDD6FE),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0x35FFFFFF)
                                                ) {
                                                    Text(
                                                        text = "GEMINI AI",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // 2 Step Guided Slots
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Step 1: Front
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0x22FFFFFF),
                                                    border = BorderStroke(1.dp, Color(0x44FFFFFF))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CameraAlt,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                text = "1. Front View",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                fontSize = 11.sp
                                                            )
                                                            Text(
                                                                text = "Title & Brand",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                                                color = Color(0xFFDDD6FE)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Step 2: Back
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0x22FFFFFF),
                                                    border = BorderStroke(1.dp, Color(0x44FFFFFF))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.QrCode,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                text = "2. Back View",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                fontSize = 11.sp
                                                            )
                                                            Text(
                                                                text = "Barcode & Expiry",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                                                color = Color(0xFFDDD6FE)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Action Buttons Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = { isTwoPhotoCameraOpen = true },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CameraAlt,
                                                        contentDescription = null,
                                                        tint = Color(0xFF6D28D9),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Snap Packaging ⚡",
                                                        color = Color(0xFF6D28D9),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                Surface(
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                    .bouncingClickable { galleryLauncher.launch("image/*") },
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0x33FFFFFF),
                                                border = BorderStroke(1.dp, Color(0x66FFFFFF))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PhotoLibrary,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Gallery",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Photos captured state: Sleek gallery strip with labeled thumbnails
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Caramel600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Packaging Photos",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrownTextDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ForestGreen100
                                    ) {
                                        Text(
                                            text = "${productImages.size} captured",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreen600,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF3E8FF),
                                        border = BorderStroke(1.dp, Color(0xFFC084FC)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .bouncingClickable { isTwoPhotoCameraOpen = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFF7C3AED),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Retake AI",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7C3AED),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Cream200,
                                        border = BorderStroke(1.dp, BrownBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .bouncingClickable { galleryLauncher.launch("image/*") }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = BrownTextDark,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Add",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BrownTextDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal thumbnail row
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(productImages) { idx, path ->
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.2.dp, if (idx == 0) Color(0xFF7C3AED) else BrownBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                editingImagePath = path
                                                editingImageIndex = idx
                                            }
                                    ) {
                                        ProductThumbnailImage(
                                            imagePath = path,
                                            modifier = Modifier.fillMaxSize(),
                                            reqSize = 160
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Cream200),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BrownTextMedium
                                                )
                                            }
                                        }

                                        // Badge: 1. FRONT or 2. BACK
                                        Surface(
                                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                                            color = when (idx) {
                                                0 -> Color(0xFF7C3AED)
                                                1 -> Color(0xFF2563EB)
                                                else -> Caramel700
                                            },
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = when (idx) {
                                                    0 -> "FRONT"
                                                    1 -> "BACK"
                                                    else -> "#${idx + 1}"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        // Delete button
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(18.dp)
                                                .clickable {
                                                    if (idx in productImages.indices) {
                                                        productImages.removeAt(idx)
                                                    }
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.padding(3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item Identity & Barcode",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Caramel700
                            )
                            if (isBarcodeGenerated) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                ) {
                                    Text(
                                        text = "⚡ System EAN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = barcode,
                            onValueChange = {
                                barcode = it
                                if (isBarcodeGenerated) isBarcodeGenerated = false
                            },
                            label = { Text("Barcode / EAN / QR") },
                            placeholder = { Text("Scan packaging or auto-generate", color = BrownTextMuted) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Caramel600, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (barcode.isNotBlank()) {
                                    IconButton(onClick = { barcode = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = BrownTextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
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

                        // Quick Action Bar beneath Barcode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (barcode.isNotBlank()) {
                                // Print Sticker Button
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .bouncingClickable { showPrintDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Caramel100,
                                    border = BorderStroke(1.dp, Caramel400)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Caramel700, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Print Sticker 🏷️", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Caramel700)
                                    }
                                }

                                // Verify / Extract from Master Catalog
                                Surface(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .bouncingClickable {
                                            if (!isExtractingDetails && barcode.isNotBlank() && catalogApiService != null) {
                                                isExtractingDetails = true
                                                extractionStatusMessage = null
                                                showGeminiOption = false
                                                coroutineScope.launch {
                                                    try {
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
                                                                da.expiryDate?.let { expiryDate = it }
                                                                da.mfgDate?.let { mfgDate = it }
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
                                                            usageStats = geminiTracker?.getUsageStats()
                                                            extractionStatusMessage = "Product not in Master Catalog. You can extract with Gemini AI below."
                                                            extractionStatusIsError = true
                                                            showGeminiOption = true
                                                        }
                                                    } catch (e: Exception) {
                                                        usageStats = geminiTracker?.getUsageStats()
                                                        extractionStatusMessage = "Product not found. You can extract with Gemini AI below."
                                                        extractionStatusIsError = true
                                                        showGeminiOption = true
                                                    } finally {
                                                        isExtractingDetails = false
                                                    }
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Cream100,
                                    border = BorderStroke(1.dp, BrownBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isExtractingDetails) {
                                            CircularProgressIndicator(modifier = Modifier.size(13.dp), color = Caramel700, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Checking...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Caramel700)
                                        } else {
                                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Caramel700, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Search Catalog 🔍", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Caramel700)
                                        }
                                    }
                                }
                            } else {
                                // When barcode is blank: Option to auto-generate internal EAN-13
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .bouncingClickable {
                                            val newEan = generateInternalEan()
                                            barcode = newEan
                                            isBarcodeGenerated = true
                                            extractionStatusMessage = "⚡ Generated internal EAN barcode: $newEan"
                                            extractionStatusIsError = false
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Caramel50,
                                    border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Caramel700, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Auto-Generate Internal EAN-13 Barcode ⚡",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Caramel700
                                        )
                                    }
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

                        // Expiry and Mfg Dates (Extracted by Gemini Vision & ML Kit)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = { expiryDate = it },
                                label = { Text("Expiry / Use By") },
                                placeholder = { Text("e.g. 2026-12-31 or 12/26", color = BrownTextMuted) },
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
                                value = mfgDate,
                                onValueChange = { mfgDate = it },
                                label = { Text("Mfg / Pkg Date") },
                                placeholder = { Text("e.g. 2026-06-15", color = BrownTextMuted) },
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
                                expiryDate = expiryDate.ifBlank { null },
                                mfgDate = mfgDate.ifBlank { null },
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

    if (isTwoPhotoCameraOpen) {
        InAppCameraCaptureDialog(
            mode = CameraCaptureMode.ENROLL_FRONT_BACK,
            title = "Smart 2-Photo AI Auto-Enroll",
            instructionText = "Step 1: Capture Front (Name/Brand). Step 2: Capture Back (Barcode & Expiry)",
            onPhotosCaptured = { bitmaps ->
                isTwoPhotoCameraOpen = false
                if (bitmaps.isNotEmpty()) {
                    val front = bitmaps[0]
                    val back = bitmaps.getOrNull(1)
                    processTwoPhotos(front, back)
                }
            },
            onDismiss = { isTwoPhotoCameraOpen = false }
        )
    }

    if (isAnalyzingTwoPhotos) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                border = BorderStroke(1.2.dp, Color(0xFFC084FC))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.88f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF3E8FF),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                color = Color(0xFF7C3AED),
                                strokeWidth = 3.dp
                            )
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing Packaging Photos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B21B6)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gemini Multimodal AI & ML Kit are scanning packaging for Barcode, QR, Name, Brand, Category, MRP, Net Weight & Expiry Date...",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrownTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showPrintDialog) {
        Dialog(
            onDismissRequest = {
                showPrintDialog = false
                printStatusMessage = null
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Caramel700,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Print Barcode Sticker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrownTextDark
                            )
                        }
                        IconButton(
                            onClick = {
                                showPrintDialog = false
                                printStatusMessage = null
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = BrownTextDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barcode Preview Card (Thermal Sticker simulation)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, Color.Black.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PACKREADY RETAIL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = (name.ifBlank { "Product Item" }).take(26),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            BarcodeRenderer(
                                barcodeData = barcode.trim(),
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(0.88f),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val mrpVal = mrp.toDoubleOrNull() ?: sellingPrice.toDoubleOrNull() ?: 0.0
                                Text(
                                    text = "MRP: ₹${String.format(java.util.Locale.US, "%.2f", mrpVal)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                                if (expiryDate.isNotBlank()) {
                                    Text(
                                        text = "Exp: $expiryDate",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quantity / Copies Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sticker Copies:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrownTextDark
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Cream200,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { if (labelCopies > 1) labelCopies-- }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = BrownTextDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = "$labelCopies",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Espresso900,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            Surface(
                                shape = CircleShape,
                                color = Caramel100,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { if (labelCopies < 50) labelCopies++ }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = Caramel700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Status Message
                    if (printStatusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = printStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (printStatusMessage!!.startsWith("✓")) ForestGreen600 else RustError600,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Print Action Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (bluetoothPrinterManager == null) {
                                    printStatusMessage = "Printer service not initialized"
                                    return@launch
                                }
                                printStatusMessage = "Sending to Bluetooth thermal printer..."
                                val mrpVal = mrp.toDoubleOrNull() ?: sellingPrice.toDoubleOrNull() ?: 0.0
                                val result = bluetoothPrinterManager.printStickerLabel(
                                    barcode = barcode.trim(),
                                    productName = name.ifBlank { "Item $barcode" },
                                    mrp = mrpVal,
                                    copies = labelCopies
                                )
                                if (result.isSuccess) {
                                    printStatusMessage = "✓ $labelCopies sticker(s) printed successfully!"
                                    Toast.makeText(context, "Printed $labelCopies sticker(s)", Toast.LENGTH_SHORT).show()
                                } else {
                                    val err = result.exceptionOrNull()?.message ?: "Printing failed"
                                    printStatusMessage = "Printing failed: $err"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Espresso800),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Print $labelCopies Label(s)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
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
