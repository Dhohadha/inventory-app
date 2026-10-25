package com.packready.shopkeeper.presentation.loosegoods

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.presentation.theme.Amber500
import com.packready.shopkeeper.presentation.theme.Roast800
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.theme.BrownBorder
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMedium
import com.packready.shopkeeper.presentation.theme.BrownTextMuted
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel50
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Cream200
import com.packready.shopkeeper.presentation.theme.Cream50
import com.packready.shopkeeper.presentation.theme.Espresso800
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.ui.components.BarcodeRenderer
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelGeneratorScreen(
    viewModel: LooseGoodsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.toastMessage, uiState.errorMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(LooseGoodsUiEvent.ClearMessages)
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(LooseGoodsUiEvent.ClearMessages)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PackReadyEmblem(size = 28.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Container Stickers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrownTextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Cream50
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Cream50)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // New Loose Goods Form
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrownBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "New Container Sticker",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrownTextDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.itemNameInput,
                        onValueChange = { viewModel.onEvent(LooseGoodsUiEvent.OnNameChange(it)) },
                        label = { Text("Item Name") },
                        placeholder = { Text("Loose Sugar, Basmati Rice") },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.sellingPriceInput,
                            onValueChange = { viewModel.onEvent(LooseGoodsUiEvent.OnSellingPriceChange(it)) },
                            label = { Text("Rate (₹)") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Caramel600,
                                unfocusedBorderColor = BrownBorder
                            ),
                            modifier = Modifier.weight(1.05f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.costPriceInput,
                            onValueChange = { viewModel.onEvent(LooseGoodsUiEvent.OnCostChange(it)) },
                            label = { Text("Cost (₹)") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Caramel600,
                                unfocusedBorderColor = BrownBorder
                            ),
                            modifier = Modifier.weight(0.95f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.unitInput,
                            onValueChange = { viewModel.onEvent(LooseGoodsUiEvent.OnUnitChange(it)) },
                            label = { Text("Unit") },
                            placeholder = { Text("kg") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Caramel600,
                                unfocusedBorderColor = BrownBorder
                            ),
                            modifier = Modifier.weight(0.85f),
                            singleLine = true
                        )
                    }

                    // Quick Unit Selection Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("kg", "g", "L", "ml", "pack", "pcs").forEach { unitOpt ->
                            val isSelected = uiState.unitInput.equals(unitOpt, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Caramel600 else Cream100,
                                border = if (isSelected) null else BorderStroke(1.dp, BrownBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onEvent(LooseGoodsUiEvent.OnUnitChange(unitOpt)) }
                            ) {
                                Text(
                                    text = unitOpt,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else BrownTextDark,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.onEvent(LooseGoodsUiEvent.GenerateCustomBarcode) },
                        colors = ButtonDefaults.buttonColors(containerColor = Espresso800),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bouncingClickable { viewModel.onEvent(LooseGoodsUiEvent.GenerateCustomBarcode) }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Barcode", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sticker Preview & Print Controls
            uiState.selectedProduct?.let { product ->
                StickerPreviewCard(
                    product = product,
                    copiesCount = uiState.labelCopiesCount,
                    onCopiesSelect = { viewModel.onEvent(LooseGoodsUiEvent.SetCopiesCount(it)) },
                    onPrint = { viewModel.onEvent(LooseGoodsUiEvent.PrintStickerLabels) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Existing Loose Items List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Registered Bulk Container Stickers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrownTextDark
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Caramel100,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${uiState.looseProducts.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = Caramel700,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.looseProducts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrownBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "No container stickers generated yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrownTextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                uiState.looseProducts.forEach { product ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BrownBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .bouncingClickable(scaleDownFactor = 0.98f) { viewModel.onEvent(LooseGoodsUiEvent.SelectProductForLabel(product)) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Caramel50,
                                    border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = null,
                                            tint = Caramel700,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrownTextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Cream200
                                        ) {
                                            Text(
                                                text = product.barcode,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = Caramel700,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = "₹${String.format(Locale.US, "%.0f", product.sellingPrice)}/${product.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ForestGreen600,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.onEvent(LooseGoodsUiEvent.SelectProductForLabel(product)) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Caramel600),
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .bouncingClickable { viewModel.onEvent(LooseGoodsUiEvent.SelectProductForLabel(product)) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = Caramel700,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sticker", color = Caramel700, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerPreviewCard(
    product: Product,
    copiesCount: Int,
    onCopiesSelect: (Int) -> Unit,
    onPrint: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Cream100),
        border = BorderStroke(1.dp, BrownBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sticker Preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BrownTextDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Sticker Mockup
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, BrownBorder),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PACKREADY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrownTextDark
                    )
                    Text(
                        text = product.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.0f", product.sellingPrice)} per ${product.unit}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                    BarcodeRenderer(
                        barcodeData = product.barcode,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Copies Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Copies:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrownTextDark
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 4, 6, 12, 24).forEach { count ->
                        val isSelected = copiesCount == count
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .bouncingClickable { onCopiesSelect(count) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Caramel600 else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Caramel600 else BrownBorder)
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else BrownTextDark,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onPrint,
                colors = ButtonDefaults.buttonColors(containerColor = Espresso800),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bouncingClickable(onClick = onPrint)
            ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Print Stickers ($copiesCount)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
