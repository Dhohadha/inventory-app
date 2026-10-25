package com.packready.shopkeeper.presentation.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Crop32
import androidx.compose.material.icons.filled.Crop54
import androidx.compose.material.icons.filled.Crop75
import androidx.compose.material.icons.filled.CropDin
import androidx.compose.material.icons.filled.CropOriginal
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.packready.shopkeeper.data.local.ProductImageStorage
import com.packready.shopkeeper.presentation.theme.BrownBorder
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMedium
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Cream50
import com.packready.shopkeeper.presentation.theme.Espresso800
import com.packready.shopkeeper.presentation.theme.ForestGreen100
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    SQUARE_1_1("1:1 Square", 1.0f),
    STANDARD_4_3("4:3 Standard", 4.0f / 3.0f),
    PORTRAIT_3_4("3:4 Portrait", 3.0f / 4.0f),
    ORIGINAL("Original", null)
}

/**
 * Intelligent Smart Color/Luminance Background Removal for Packaging & Products.
 * Samples border pixels to identify ambient background color and converts background to transparent alpha.
 */
fun removeProductBackground(source: Bitmap, tolerance: Float = 42f): Bitmap {
    val width = source.width
    val height = source.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    // Sample corner pixels to determine ambient background color
    val corners = listOf(
        pixels[0],
        pixels[min(width - 1, pixels.size - 1)],
        pixels[min((height - 1) * width, pixels.size - 1)],
        pixels[min(pixels.size - 1, (height - 1) * width + width - 1)]
    )

    var avgR = 0f
    var avgG = 0f
    var avgB = 0f
    for (c in corners) {
        avgR += AndroidColor.red(c)
        avgG += AndroidColor.green(c)
        avgB += AndroidColor.blue(c)
    }
    avgR /= corners.size
    avgG /= corners.size
    avgB /= corners.size

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = AndroidColor.red(pixel)
        val g = AndroidColor.green(pixel)
        val b = AndroidColor.blue(pixel)

        // Euclidean distance in RGB color space from background
        val dr = r - avgR
        val dg = g - avgG
        val db = b - avgB
        val distance = sqrt((dr * dr + dg * dg + db * db).toDouble()).toFloat()

        if (distance < tolerance) {
            // Near background color -> make transparent
            pixels[i] = AndroidColor.argb(0, r, g, b)
        } else if (distance < tolerance + 15f) {
            // Smooth edge blending ramp
            val alpha = (((distance - tolerance) / 15f) * 255f).toInt().coerceIn(0, 255)
            pixels[i] = AndroidColor.argb(alpha, r, g, b)
        }
    }

    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}

/**
 * Crops the bitmap to the designated aspect ratio around the center.
 */
fun cropBitmapToAspectRatio(source: Bitmap, targetRatio: Float?): Bitmap {
    if (targetRatio == null) return source
    val srcW = source.width
    val srcH = source.height
    val currentRatio = srcW.toFloat() / srcH.toFloat()

    val cropW: Int
    val cropH: Int
    if (currentRatio > targetRatio) {
        // Source is wider than target -> crop width
        cropH = srcH
        cropW = (srcH * targetRatio).toInt().coerceAtMost(srcW)
    } else {
        // Source is taller than target -> crop height
        cropW = srcW
        cropH = (srcW / targetRatio).toInt().coerceAtMost(srcH)
    }

    val startX = ((srcW - cropW) / 2).coerceAtLeast(0)
    val startY = ((srcH - cropH) / 2).coerceAtLeast(0)

    return Bitmap.createBitmap(source, startX, startY, cropW, cropH)
}

/**
 * Rotates a bitmap by degrees.
 */
fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    if (degrees % 360f == 0f) return source
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

@Composable
fun ImageEditorDialog(
    initialImagePath: String,
    onDismiss: () -> Unit,
    onImageSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(true) }

    // User options: Background removal is TRUE by default as requested!
    var removeBackground by remember { mutableStateOf(true) }
    var selectedCropRatio by remember { mutableStateOf(CropAspectRatio.SQUARE_1_1) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    // Load original bitmap
    LaunchedEffect(initialImagePath) {
        withContext(Dispatchers.IO) {
            val bmp = ProductImageStorage.loadThumbnail(initialImagePath, 1024, 1024)
            originalBitmap = bmp
        }
    }

    // Recompute processed bitmap whenever options change
    LaunchedEffect(originalBitmap, removeBackground, selectedCropRatio, rotationDegrees) {
        val base = originalBitmap ?: return@LaunchedEffect
        isProcessing = true
        withContext(Dispatchers.Default) {
            var current = base

            // 1. Rotation
            if (rotationDegrees != 0f) {
                current = rotateBitmap(current, rotationDegrees)
            }

            // 2. Aspect Ratio Crop
            selectedCropRatio.ratio?.let { ratio ->
                current = cropBitmapToAspectRatio(current, ratio)
            }

            // 3. Background Removal (enabled by default)
            if (removeBackground) {
                current = removeProductBackground(current)
            }

            processedBitmap = current
            isProcessing = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = Cream50,
            tonalElevation = 10.dp,
            border = BorderStroke(1.2.dp, Caramel500.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Caramel500.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Caramel600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Product Photo Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrownTextDark
                            )
                            Text(
                                text = "Crop & Remove Background",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrownTextMedium
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = BrownTextDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Preview Canvas with Transparency Checkerboard Effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, BrownBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = processedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Edited Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }

                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Caramel600, strokeWidth = 3.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Editing Controls Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, BrownBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    // 1. Remove Background Toggle (ON by default)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (removeBackground) ForestGreen600 else BrownTextMedium,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Remove Background (Auto Cutout)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (removeBackground) ForestGreen600 else BrownTextDark
                                )
                                Text(
                                    text = "Creates transparent backdrop for crisp catalog look",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = BrownTextMedium
                                )
                            }
                        }

                        Switch(
                            checked = removeBackground,
                            onCheckedChange = { removeBackground = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ForestGreen600,
                                checkedTrackColor = ForestGreen100
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Aspect Ratio Crop Selector Chips
                    Text(
                        text = "Aspect Ratio Crop",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrownTextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CropAspectRatio.values().forEach { option ->
                            val isSelected = selectedCropRatio == option
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Caramel600 else Cream100,
                                border = BorderStroke(1.dp, if (isSelected) Caramel600 else BrownBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedCropRatio = option }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (option) {
                                            CropAspectRatio.SQUARE_1_1 -> Icons.Default.CropSquare
                                            CropAspectRatio.STANDARD_4_3 -> Icons.Default.CropDin
                                            CropAspectRatio.PORTRAIT_3_4 -> Icons.Default.CropPortrait
                                            CropAspectRatio.ORIGINAL -> Icons.Default.CropOriginal
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else BrownTextMedium,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else BrownTextMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Rotate 90 deg button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Cream100,
                            border = BorderStroke(1.dp, BrownBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { rotationDegrees = (rotationDegrees + 90f) % 360f }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = "Rotate",
                                    tint = BrownTextMedium,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Rotate 90°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrownTextMedium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrownBorder)
                    ) {
                        Text("Cancel", color = BrownTextDark, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val bmp = processedBitmap ?: originalBitmap ?: return@Button
                            coroutineScope.launch {
                                val savedPath = withContext(Dispatchers.IO) {
                                    ProductImageStorage.saveBitmap(context, bmp)
                                }
                                onImageSaved(savedPath)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Espresso800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply & Use Photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
