package com.packready.shopkeeper.presentation.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packready.shopkeeper.data.local.ProductImageStorage
import com.packready.shopkeeper.presentation.theme.BrownBorder
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMedium
import com.packready.shopkeeper.presentation.theme.Caramel50
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream50
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Cream200
import com.packready.shopkeeper.presentation.theme.Espresso900
import com.packready.shopkeeper.presentation.theme.ForestGreen100
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.theme.RustError600
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance image composable that safely decodes local product images on Dispatchers.IO.
 */
@Composable
fun ProductThumbnailImage(
    imagePath: String?,
    modifier: Modifier = Modifier,
    reqSize: Int = 300,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit
) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = imagePath) {
        if (imagePath.isNullOrBlank()) {
            value = null
        } else {
            value = withContext(Dispatchers.IO) {
                ProductImageStorage.loadThumbnail(imagePath, reqSize, reqSize)
            }
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        placeholder()
    }
}

/**
 * Interactive Multi-Image Editor Row used during Product Enrollment and Catalog Product Editing.
 */
@Composable
fun MultiImageEditorRow(
    images: List<String>,
    onAddViaCamera: () -> Unit,
    onAddViaGallery: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onEditImage: ((Int, String) -> Unit)? = null,
    onSetPrimary: ((Int) -> Unit)? = null,
    uploadingImages: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Consolidated Single Header Row: Title + Count Badge on left, Camera & Gallery Actions on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Caramel100.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Caramel600,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "Product Photos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrownTextDark
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (images.isNotEmpty()) ForestGreen100 else Cream200
                ) {
                    Text(
                        text = "${images.size} added",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (images.isNotEmpty()) ForestGreen600 else BrownTextMedium,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            // Quick Action Buttons (Prevent text wrapping)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .bouncingClickable(onClick = onAddViaCamera),
                    shape = RoundedCornerShape(8.dp),
                    color = Caramel50,
                    border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Caramel700,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Camera",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Caramel700,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .bouncingClickable(onClick = onAddViaGallery),
                    shape = RoundedCornerShape(8.dp),
                    color = Cream100,
                    border = BorderStroke(1.dp, BrownBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = BrownTextDark,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Gallery",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BrownTextDark,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (images.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .bouncingClickable(onClick = onAddViaCamera),
                shape = RoundedCornerShape(12.dp),
                color = Cream50.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, BrownBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Caramel100.copy(alpha = 0.6f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Caramel600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Take front/back packaging photos or pick from gallery",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrownTextMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(images) { index, path ->
                    val isUploading = uploadingImages.contains(path)
                    val isCloudUrl = path.startsWith("http://") || path.startsWith("https://")

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (index == 0) 2.dp else 1.dp,
                                color = if (index == 0) Caramel500 else BrownBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
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
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrownTextMedium
                                )
                            }
                        }

                        // Primary Tag on Index 0
                        if (index == 0) {
                            Surface(
                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                color = Roast900.copy(alpha = 0.88f),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "Cover",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Caramel400,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Uploading overlay or Cloud Sync badge
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        } else if (isCloudUrl) {
                            Surface(
                                shape = RoundedCornerShape(topEnd = 6.dp),
                                color = ForestGreen600.copy(alpha = 0.9f),
                                modifier = Modifier.align(Alignment.BottomStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Text(
                                        text = "Cloud",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Edit / Studio Badge (Bottom End)
                        if (onEditImage != null) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .bouncingClickable { onEditImage(index, path) },
                                shape = CircleShape,
                                color = Roast900.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, Caramel400)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit / Crop",
                                        tint = Caramel400,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        // Delete Badge (Top End)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .bouncingClickable { onRemoveImage(index) },
                            shape = CircleShape,
                            color = RustError600
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                // Add More slot at the end
                item {
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .bouncingClickable(onClick = onAddViaCamera),
                        shape = RoundedCornerShape(12.dp),
                        color = Cream50,
                        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add More",
                                tint = Caramel600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Add More",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = BrownTextDark
                            )
                        }
                    }
                }
            }
        }
    }
}
