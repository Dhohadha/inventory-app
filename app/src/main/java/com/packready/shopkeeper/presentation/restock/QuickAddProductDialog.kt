package com.packready.shopkeeper.presentation.restock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.packready.shopkeeper.domain.model.DynamicAttributes
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.packready.shopkeeper.data.vision.MobileNetV4EmbeddingEngine
import com.packready.shopkeeper.data.vision.PackagingOcrParser
import com.packready.shopkeeper.data.vision.RoiProcessor
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.ui.components.CameraCaptureMode
import com.packready.shopkeeper.presentation.ui.components.InAppCameraCaptureDialog
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        visualEmbeddings: List<Pair<String, FloatArray>>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var barcode by remember { mutableStateOf(initialBarcode ?: "890${(1000000000..9999999999).random()}") }
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

    // AI Photo Extraction State
    var isAiScanning by remember { mutableStateOf(false) }
    var aiStatusMessage by remember { mutableStateOf<String?>(null) }
    var visualEmbeddings by remember { mutableStateOf<List<Pair<String, FloatArray>>>(emptyList()) }
    var isCameraCaptureOpen by remember { mutableStateOf(false) }
    var capturedPhotos by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var hasSubmittedPhotos by remember { mutableStateOf(false) }

    fun processPackagingBitmaps(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        isAiScanning = true
        aiStatusMessage = "Detecting & segmenting packaging objects, then extracting OCR & visual embeddings..."
        coroutineScope.launch {
            try {
                // Step 1: Object detection segmentation to isolate product packaging from background
                val segmentedBitmaps = withContext(Dispatchers.Default) {
                    bitmaps.map { bmp -> RoiProcessor.segmentProductObject(bmp) }
                }

                // Step 2: OCR on segmented packaging images
                val parser = PackagingOcrParser()
                val parsed = parser.parsePackagingImages(segmentedBitmaps)

                parsed.detectedProductName?.let { name = it }
                parsed.detectedBrand?.let { brand = it }
                parsed.detectedMrp?.let { mrp = it.toString() }
                parsed.detectedCostPrice?.let { costPrice = it.toString() }
                parsed.detectedSellingPrice?.let { sellingPrice = it.toString() }
                parsed.detectedNetQuantity?.let { netWeight = it }
                unit = parsed.detectedUnit
                parsed.detectedCategory?.let { selectedCategory = it }
                isVegetarian = parsed.isVegetarian

                // Step 3: Extract low-spec 512-D MobileNetV4 ArcFace embeddings on segmented object
                val embeddingEngine = MobileNetV4EmbeddingEngine(context)
                val embeddings = withContext(Dispatchers.Default) {
                    segmentedBitmaps.mapIndexed { idx, bmp ->
                        val tag = when (idx) {
                            0 -> "FRONT"
                            1 -> "BACK"
                            else -> "DETAIL_$idx"
                        }
                        Pair(tag, embeddingEngine.extractEmbedding(bmp))
                    }
                }
                visualEmbeddings = embeddings
                hasSubmittedPhotos = true

                aiStatusMessage = "✓ Extracted: ${parsed.detectedProductName ?: "Product"} • ${embeddings.size} Object-Segmented Visual Embeddings Enrolled"
            } catch (e: Exception) {
                aiStatusMessage = "Error processing photos: ${e.message}"
            } finally {
                isAiScanning = false
            }
        }
    }

    if (isCameraCaptureOpen) {
        InAppCameraCaptureDialog(
            mode = CameraCaptureMode.MULTI_PHOTO,
            title = "Snap Product Photos",
            instructionText = "Step 1: Snap Front packaging • Step 2: Snap Back packaging",
            onPhotosCaptured = { bitmaps ->
                capturedPhotos = bitmaps
                isCameraCaptureOpen = false
                hasSubmittedPhotos = false
                aiStatusMessage = "Captured ${bitmaps.size} packaging photo(s). Review and tap 'Submit Photos' to run OCR & Object Segmentation."
            },
            onDismiss = { isCameraCaptureOpen = false }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val bitmaps = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                }
                capturedPhotos = bitmaps
                hasSubmittedPhotos = false
                aiStatusMessage = "Selected ${bitmaps.size} packaging photo(s). Review and tap 'Submit Photos' to run OCR & Object Segmentation."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PackReadyEmblem(size = 28.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add New Product SKU",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI SMART AUTO-FILL CARD
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Photo Auto-Fill & Visual Enrollment",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Snap or select packaging photos (Front, Back). The system extracts title, brand, MRP, net qty via OCR and saves visual embeddings for POS recognition.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Captured Photo Thumbnails
                        if (capturedPhotos.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(capturedPhotos) { idx, bmp ->
                                    val tag = when (idx) {
                                        0 -> "FRONT"
                                        1 -> "BACK"
                                        else -> "DETAIL ${idx + 1}"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.5.dp, Emerald500, RoundedCornerShape(10.dp))
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = tag,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Surface(
                                            color = Roast900.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                        ) {
                                            Text(
                                                text = tag,
                                                color = Color.White,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons: Explicit Submit Photos + Camera & Gallery Capture
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (capturedPhotos.isNotEmpty()) {
                                // Explicit Submit Photos Button - Only runs OCR and Segmentation on tap
                                Button(
                                    onClick = { processPackagingBitmaps(capturedPhotos) },
                                    enabled = !isAiScanning,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .bouncingClickable { processPackagingBitmaps(capturedPhotos) }
                                ) {
                                    if (isAiScanning) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Segmenting Object & Extracting OCR...",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (hasSubmittedPhotos) "Re-Submit Photos & Re-Analyze" else "Submit Photos (Run OCR & Object Segmentation)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isCameraCaptureOpen = true },
                                    enabled = !isAiScanning,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (capturedPhotos.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .bouncingClickable { isCameraCaptureOpen = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = if (capturedPhotos.isEmpty()) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (capturedPhotos.isEmpty()) "Open Camera" else "Retake Camera",
                                        color = if (capturedPhotos.isEmpty()) Color.White else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    enabled = !isAiScanning,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .bouncingClickable { photoPickerLauncher.launch("image/*") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("From Gallery", fontSize = 12.sp)
                                }
                            }
                        }

                        if (aiStatusMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (aiStatusMessage!!.startsWith("✓")) Emerald500 else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode / EAN") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Display Name *") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs/kg/g)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Selector
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
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
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

                Spacer(modifier = Modifier.height(8.dp))

                // Pricing Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Sell (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamic Category Specialized Attributes
                Text(
                    text = "Specialized Attributes (${selectedCategory.displayName})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                when (selectedCategory) {
                    ProductCategory.FOOD_GROCERY, ProductCategory.BEVERAGES -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isVegetarian = !isVegetarian },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isVegetarian) ForestGreen600.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (isVegetarian) ForestGreen600.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Authentic Indian Green Veg / Brown Non-Veg Box Symbol
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .border(
                                                1.5.dp,
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
                                            color = if (isVegetarian) ForestGreen600 else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isVegetarian) "Green Veg mark applied on receipts" else "Brown Non-Veg mark applied",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isVegetarian,
                                    onCheckedChange = { isVegetarian = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ForestGreen600,
                                        checkedTrackColor = ForestGreen600.copy(alpha = 0.25f),
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = netWeight,
                            onValueChange = { netWeight = it },
                            label = { Text("Net Weight / Volume (e.g. 500g, 1L)") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    ProductCategory.ELECTRONICS_ELECTRICAL -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = wattage,
                                onValueChange = { wattage = it },
                                label = { Text("Wattage (e.g. 9W)") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
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
                            visualEmbeddings
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bouncingClickable {
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
                                visualEmbeddings
                            )
                        }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Add to Catalog", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
