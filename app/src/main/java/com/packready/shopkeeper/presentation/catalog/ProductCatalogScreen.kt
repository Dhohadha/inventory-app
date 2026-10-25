package com.packready.shopkeeper.presentation.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.presentation.restock.QuickAddProductDialog
import com.packready.shopkeeper.presentation.theme.Amber500
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.theme.RustError600
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.packready.shopkeeper.data.local.ProductImageStorage
import com.packready.shopkeeper.presentation.ui.components.CameraCaptureMode
import com.packready.shopkeeper.presentation.ui.components.CategoryFilterChip
import com.packready.shopkeeper.presentation.ui.components.ImageEditorDialog
import com.packready.shopkeeper.presentation.ui.components.InAppCameraCaptureDialog
import com.packready.shopkeeper.presentation.ui.components.MultiImageEditorRow
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem
import com.packready.shopkeeper.presentation.ui.components.ProductThumbnailImage
import com.packready.shopkeeper.presentation.restock.QuickAddEntryPoint
import dagger.hilt.android.EntryPointAccessors
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCatalogScreen(
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.toastMessage, uiState.errorMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(CatalogUiEvent.ClearMessages)
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(CatalogUiEvent.ClearMessages)
        }
    }

    // Dynamic list of categories from enum plus "All"
    val categories = remember {
        listOf("All") + ProductCategory.values().map { it.displayName }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Title Bar with Add Product Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PackReadyEmblem(size = 30.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Product Catalog",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.allProducts.size} SKUs in Store Inventory",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.onEvent(CatalogUiEvent.OpenAddProductDialog) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .bouncingClickable { viewModel.onEvent(CatalogUiEvent.OpenAddProductDialog) }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Product", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-Time Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onEvent(CatalogUiEvent.OnSearchQueryChange(it)) },
                        placeholder = {
                            Text(
                                "Search product title, brand, or barcode...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
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
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onEvent(CatalogUiEvent.OnSearchQueryChange("")) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Carousel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val count = if (cat == "All") uiState.allProducts.size else uiState.allProducts.count { it.category.displayName == cat }
                            if (cat == "All" || count > 0) {
                                CategoryFilterChip(
                                    text = cat,
                                    isSelected = uiState.selectedCategory == cat,
                                    onClick = { viewModel.onEvent(CatalogUiEvent.OnCategorySelect(cat)) },
                                    count = count
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Count Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.searchQuery.isNotBlank()) "Search Results (${uiState.filteredProducts.size})" else "${uiState.selectedCategory} (${uiState.filteredProducts.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "3 In-A-Row View",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Products Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No items match '${uiState.searchQuery}'" else "No products found in this category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.onEvent(CatalogUiEvent.OpenAddProductDialog) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Item", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // 3 in a Row Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredProducts, key = { it.id }) { product ->
                        CatalogProductTile3Col(
                            product = product,
                            onClick = { viewModel.onEvent(CatalogUiEvent.OnProductClick(product)) }
                        )
                    }
                }
            }
        }
    }

    // Product Detail Modal
    uiState.selectedProductForDetail?.let { product ->
        ProductDetailModal(
            product = product,
            onDismiss = { viewModel.onEvent(CatalogUiEvent.DismissProductDetail) },
            onEdit = { viewModel.onEvent(CatalogUiEvent.OpenEditProductDialog(product)) },
            onDelete = { viewModel.onEvent(CatalogUiEvent.OpenDeleteProductDialog(product)) },
            onAdjustStock = { delta -> viewModel.onEvent(CatalogUiEvent.AdjustStock(product.id, delta)) }
        )
    }

    // Edit Product Dialog
    uiState.productToEdit?.let { product ->
        EditProductDialog(
            product = product,
            onDismiss = { viewModel.onEvent(CatalogUiEvent.DismissEditProductDialog) },
            onSave = { updated -> viewModel.onEvent(CatalogUiEvent.SaveEditedProduct(updated)) }
        )
    }

    // Delete Product Confirmation Dialog
    uiState.productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CatalogUiEvent.DismissDeleteProductDialog) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Delete Product?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently remove '${product.name}' (Barcode: ${product.barcode}) from your store catalog?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(CatalogUiEvent.ConfirmDeleteProduct) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(CatalogUiEvent.DismissDeleteProductDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add New Product Dialog
    if (uiState.isAddProductDialogOpen) {
        QuickAddProductDialog(
            onConfirm = { name, barcode, brand, cat, unit, cp, sp, mrp, stock, attrs, _, images ->
                val newProd = Product(
                    id = barcode.ifBlank { "PROD-${System.currentTimeMillis()}" },
                    barcode = barcode.ifBlank { "890${(1000000000..9999999999).random()}" },
                    name = name,
                    brand = brand.ifBlank { "General" },
                    category = cat,
                    subCategory = cat.displayName,
                    mrp = mrp,
                    costPrice = cp,
                    sellingPrice = sp,
                    currentStock = stock,
                    unit = unit,
                    attributes = attrs,
                    images = images
                )
                viewModel.onEvent(CatalogUiEvent.AddNewProduct(newProd))
            },
            onDismiss = { viewModel.onEvent(CatalogUiEvent.DismissAddProductDialog) }
        )
    }
}

/**
 * 3-in-a-Row Product Card
 */
@Composable
private fun CatalogProductTile3Col(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .bouncingClickable(scaleDownFactor = 0.94f, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Product Artwork / Photo Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                if (product.images.isNotEmpty()) {
                    ProductThumbnailImage(
                        imagePath = product.images.first(),
                        modifier = Modifier.fillMaxSize(),
                        reqSize = 180,
                        contentScale = ContentScale.Crop
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(getCategoryGradient(product.category)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(product.category),
                                contentDescription = product.name,
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(getCategoryGradient(product.category)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(product.category),
                            contentDescription = product.name,
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // If multiple photos, small badge at bottom-left
                if (product.images.size > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Roast900.copy(alpha = 0.80f)
                    ) {
                        Text(
                            text = "${product.images.size}📷",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = Caramel400,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }

                // Stock Badge Top Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    val stockColor = when {
                        product.currentStock <= 0 -> RustError600
                        product.isLowStock -> Amber500
                        else -> ForestGreen600
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Roast900.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "${product.currentStock.toInt()} ${product.unit}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = stockColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Pricing Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "₹${String.format(Locale.US, "%.0f", product.sellingPrice)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (product.mrp > product.sellingPrice) {
                        Text(
                            text = "₹${String.format(Locale.US, "%.0f", product.mrp)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full Product Detail Modal with image showcase and operations
 */
@Composable
private fun ProductDetailModal(
    product: Product,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Double) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                var selectedImageIndex by remember { mutableIntStateOf(0) }

                // Header Showcase with Photo / Category Artwork & Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.images.isNotEmpty()) {
                        val currentPath = product.images.getOrElse(selectedImageIndex) { product.images.first() }
                        ProductThumbnailImage(
                            imagePath = currentPath,
                            modifier = Modifier.fillMaxSize(),
                            reqSize = 600,
                            contentScale = ContentScale.Crop
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(getCategoryGradient(product.category)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(product.category),
                                    contentDescription = product.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        // Bottom Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                        startY = 100f
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(getCategoryGradient(product.category)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = getCategoryIcon(product.category),
                                    contentDescription = product.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = product.category.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Category Badge (Bottom Left)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Roast900.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = product.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Caramel400,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Image Counter Badge (Bottom Right if multiple)
                    if (product.images.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.70f)
                        ) {
                            Text(
                                text = "${selectedImageIndex + 1} / ${product.images.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // If Multiple Photos: Interactive Reel Strip
                if (product.images.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(product.images) { index, path ->
                            val isSelected = index == selectedImageIndex
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Caramel400 else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .bouncingClickable { selectedImageIndex = index }
                            ) {
                                ProductThumbnailImage(
                                    imagePath = path,
                                    modifier = Modifier.fillMaxSize(),
                                    reqSize = 120
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = (index + 1).toString())
                                    }
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Title & Brand
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Brand: ${product.brand} • Unit: ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barcode Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "EAN / Barcode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = product.barcode, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pricing Grid (4 Metrics)
                    Text(text = "Pricing & Margins", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(label = "Selling Price", value = "₹${product.sellingPrice}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        MetricPill(label = "MRP", value = "₹${product.mrp}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        MetricPill(label = "Cost Price", value = "₹${product.costPrice}", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                        MetricPill(label = "Profit Margin", value = "${String.format(Locale.US, "%.1f", product.marginPercent)}%", color = ForestGreen600, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stock Management Section
                    Text(text = "Inventory Stock: ${product.currentStock.toInt()} ${product.unit}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-5.0, -1.0, 1.0, 5.0, 10.0, 24.0).forEach { delta ->
                            OutlinedButton(
                                onClick = { onAdjustStock(delta) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Text(
                                    text = if (delta > 0) "+${delta.toInt()}" else delta.toInt().toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (delta > 0) ForestGreen600 else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Attributes Details
                    product.attributes?.let { attrs ->
                        Text(text = "Product Specifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                attrs.netWeight?.let { SpecRow("Net Quantity / Weight", it) }
                                attrs.packagingType?.let { SpecRow("Packaging", it) }
                                attrs.wattage?.let { SpecRow("Wattage", it) }
                                attrs.warrantyMonths?.let { SpecRow("Warranty", "$it Months") }
                                attrs.fragrance?.let { SpecRow("Fragrance", it) }
                                attrs.size?.let { SpecRow("Size", it) }
                                attrs.grainGrade?.let { SpecRow("Grade", it) }
                                if (attrs.isVegetarian != null) {
                                    SpecRow("Vegetarian", if (attrs.isVegetarian) "Yes (100% Veg)" else "No (Non-Veg)")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Operations & Actions Row (Edit, Delete, Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete SKU", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onEdit,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Product", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Edit Product Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProductDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val productImages = remember { mutableStateListOf<String>().apply { addAll(product.images) } }
    val uploadingImages = remember { mutableStateListOf<String>() }
    var isCameraCaptureOpen by remember { mutableStateOf(false) }

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
    val uploadApiService = entryPoint?.uploadApiService()

    fun triggerImageUpload(localPath: String) {
        if (uploadApiService == null) return
        uploadingImages.add(localPath)
        coroutineScope.launch {
            try {
                val file = File(localPath)
                if (file.exists()) {
                    val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", file.name, reqFile)
                    val folderPart = "products".toRequestBody("text/plain".toMediaTypeOrNull())
                    val response = uploadApiService.uploadProductImage(part, folderPart)
                    if (response.isSuccessful) {
                        val cloudUrl = response.body()?.secureUrl
                        if (!cloudUrl.isNullOrBlank()) {
                            val idx = productImages.indexOf(localPath)
                            if (idx != -1) {
                                productImages[idx] = cloudUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep local
            } finally {
                uploadingImages.remove(localPath)
            }
        }
    }

    var editingImagePath by remember { mutableStateOf<String?>(null) }
    var editingImageIndex by remember { mutableStateOf<Int?>(null) }
    var isSavingAndUploading by remember { mutableStateOf(false) }

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

    var name by remember { mutableStateOf(product.name) }
    var brand by remember { mutableStateOf(product.brand) }
    var category by remember { mutableStateOf(product.category) }
    var unit by remember { mutableStateOf(product.unit) }
    var sellingPrice by remember { mutableStateOf(product.sellingPrice.toString()) }
    var mrp by remember { mutableStateOf(product.mrp.toString()) }
    var costPrice by remember { mutableStateOf(product.costPrice.toString()) }
    var stock by remember { mutableStateOf(product.currentStock.toString()) }
    var minStock by remember { mutableStateOf(product.minStockAlert.toString()) }
    var categoryExpanded by remember { mutableStateOf(false) }

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
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Edit Product SKU", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, enabled = !isSavingAndUploading) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Photos Editor
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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs/kg)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ProductCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prices
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Selling Price (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mrp,
                        onValueChange = { mrp = it },
                        label = { Text("MRP (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Current Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pinned Bottom Button
                Button(
                    onClick = {
                        if (isSavingAndUploading) return@Button
                        coroutineScope.launch {
                            isSavingAndUploading = true
                            val finalImages = mutableListOf<String>()
                            val cleanName = (name.ifBlank { product.name }).trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val cleanBarcode = product.barcode.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val baseLabel = "${cleanBarcode}_${cleanName}".take(60)

                            productImages.forEachIndexed { idx, pathOrUrl ->
                                if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                                    finalImages.add(pathOrUrl)
                                } else {
                                    var resolvedUrl = pathOrUrl
                                    try {
                                        val file = File(pathOrUrl)
                                        if (file.exists() && uploadApiService != null) {
                                            val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                            val part = MultipartBody.Part.createFormData("file", file.name, reqFile)
                                            val folderPart = "products".toRequestBody("text/plain".toMediaTypeOrNull())
                                            val barcodePart = product.barcode.toRequestBody("text/plain".toMediaTypeOrNull())
                                            val publicIdPart = "${baseLabel}_${idx + 1}".toRequestBody("text/plain".toMediaTypeOrNull())
                                            val response = uploadApiService.uploadProductImage(part, folderPart, barcodePart, publicIdPart)
                                            if (response.isSuccessful && !response.body()?.secureUrl.isNullOrBlank()) {
                                                resolvedUrl = response.body()!!.secureUrl!!
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Keep local path
                                    }
                                    finalImages.add(resolvedUrl)
                                }
                            }

                            val sp = sellingPrice.toDoubleOrNull() ?: product.sellingPrice
                            val m = mrp.toDoubleOrNull() ?: product.mrp
                            val cp = costPrice.toDoubleOrNull() ?: product.costPrice
                            val st = stock.toDoubleOrNull() ?: product.currentStock
                            val mst = minStock.toDoubleOrNull() ?: product.minStockAlert

                            val updated = product.copy(
                                name = name.ifBlank { product.name },
                                brand = brand.ifBlank { product.brand },
                                category = category,
                                subCategory = category.displayName,
                                unit = unit.ifBlank { product.unit },
                                sellingPrice = sp,
                                mrp = m,
                                costPrice = cp,
                                currentStock = st,
                                minStockAlert = mst,
                                images = finalImages
                            )
                            isSavingAndUploading = false
                            onSave(updated)
                        }
                    },
                    enabled = !isSavingAndUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSavingAndUploading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading & Saving...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (isCameraCaptureOpen) {
        InAppCameraCaptureDialog(
            mode = CameraCaptureMode.MULTI_PHOTO,
            title = "Capture Product Photos",
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

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun getCategoryGradient(category: ProductCategory): Brush {
    return when (category) {
        ProductCategory.FOOD_GROCERY -> Brush.verticalGradient(listOf(Caramel500, Caramel400))
        ProductCategory.BEVERAGES -> Brush.verticalGradient(listOf(Color(0xFF0288D1), Color(0xFF26C6DA)))
        ProductCategory.PERSONAL_CARE -> Brush.verticalGradient(listOf(Color(0xFF8E24AA), Color(0xFFBA68C8)))
        ProductCategory.HOUSEHOLD_CLEANING -> Brush.verticalGradient(listOf(Color(0xFF3949AB), Color(0xFF5C6BC0)))
        ProductCategory.LOOSE_STAPLES -> Brush.verticalGradient(listOf(Amber500, Color(0xFFFFB74D)))
        ProductCategory.ELECTRONICS_ELECTRICAL -> Brush.verticalGradient(listOf(Color(0xFF00897B), Color(0xFF4DB6AC)))
        ProductCategory.APPAREL_FOOTWEAR -> Brush.verticalGradient(listOf(Color(0xFFE64A19), Color(0xFFFF7043)))
        ProductCategory.OTC_WELLNESS -> Brush.verticalGradient(listOf(Emerald500, ForestGreen600))
        ProductCategory.OTHER -> Brush.verticalGradient(listOf(Roast900, Color(0xFF5D4037)))
    }
}

private fun getCategoryIcon(category: ProductCategory): ImageVector {
    return when (category) {
        ProductCategory.FOOD_GROCERY -> Icons.Default.LocalMall
        ProductCategory.BEVERAGES -> Icons.Default.LocalDrink
        ProductCategory.PERSONAL_CARE -> Icons.Default.Category
        ProductCategory.HOUSEHOLD_CLEANING -> Icons.Default.Inventory2
        ProductCategory.LOOSE_STAPLES -> Icons.Default.LocalMall
        ProductCategory.ELECTRONICS_ELECTRICAL -> Icons.Default.Category
        ProductCategory.APPAREL_FOOTWEAR -> Icons.Default.LocalMall
        ProductCategory.OTC_WELLNESS -> Icons.Default.Category
        ProductCategory.OTHER -> Icons.Default.Category
    }
}
