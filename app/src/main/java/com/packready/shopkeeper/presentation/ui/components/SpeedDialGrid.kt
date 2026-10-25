package com.packready.shopkeeper.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import com.packready.shopkeeper.presentation.theme.Amber500
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMuted
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Cream50
import com.packready.shopkeeper.presentation.theme.Espresso800
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.theme.RustError600
import java.util.Locale

@Composable
fun SpeedDialGrid(
    products: List<Product>,
    onProductTap: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }

    // Derive unique categories dynamically using ProductCategory enum display names
    val categories = remember(products) {
        val uniqueCats = products.map { it.category.displayName }.distinct()
        listOf("All") + uniqueCats
    }

    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "All") {
            products
        } else {
            products.filter { it.category.displayName == selectedCategory }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth
        val columnsCount = when {
            availableWidth < 600.dp -> 2
            availableWidth < 900.dp -> 3
            else -> 4
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Caramel100,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Caramel700,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Sale Catalog",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrownTextDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Cream100,
                    border = BorderStroke(1.dp, Color(0xFFEADBCE))
                ) {
                    Text(
                        text = "${filteredProducts.size} Items",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = Caramel700,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Category Filter Chips Row (Scrollable horizontally)
            if (categories.size > 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val count = if (cat == "All") products.size else products.count { it.category.displayName == cat }
                        CategoryFilterChip(
                            text = cat,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            count = count
                        )
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Cream100,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Caramel600,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrownTextDark
                        )
                        Text(
                            text = "Scan a barcode or use Inward Restock to add items",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrownTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Products Grid with responsive 2-column layout on phones
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        SpeedDialTile(
                            product = product,
                            onTap = { onProductTap(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedDialTile(
    product: Product,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .bouncingClickable(scaleDownFactor = 0.96f, onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE8DFD5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cream100)
                    .border(1.dp, Color(0xFFEDE3D5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                ProductThumbnailImage(
                    imagePath = product.images.firstOrNull(),
                    modifier = Modifier.fillMaxSize(),
                    reqSize = 160
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Caramel600.copy(alpha = 0.5f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 15.sp
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${String.format(Locale.US, "%.0f", product.sellingPrice)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = ForestGreen600,
                        maxLines = 1
                    )

                    Surface(
                        shape = CircleShape,
                        color = Espresso800,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
