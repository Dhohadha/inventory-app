package com.packready.shopkeeper.presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.theme.Terracotta400

@Composable
fun ScanReticleOverlay(
    isScanningActive: Boolean = true,
    lastScannedBarcode: String? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Dimensions of scanning window (responsive to aspect ratio)
            val boxWidth = canvasWidth * 0.80f
            val boxHeight = canvasHeight * 0.68f
            val left = (canvasWidth - boxWidth) / 2f
            val top = (canvasHeight - boxHeight) / 2f
            val right = left + boxWidth
            val bottom = top + boxHeight

            // Dark dimmed backdrop outside the scan window
            drawRect(
                color = Color.Black.copy(alpha = 0.48f),
                size = size
            )

            // Clear the inside rect
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Precision Corner Reticles (Warm Amber Gold)
            val cornerLength = 28.dp.toPx()
            val strokeWidth = 3.5.dp.toPx()
            val cornerRadius = 14.dp.toPx()
            val bracketColor = Caramel400

            // Top-Left
            drawLine(bracketColor, Offset(left, top + cornerRadius), Offset(left, top + cornerLength), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, Offset(left + cornerRadius, top), Offset(left + cornerLength, top), strokeWidth, StrokeCap.Round)
            drawArc(bracketColor, 180f, 90f, false, topLeft = Offset(left, top), size = Size(cornerRadius * 2, cornerRadius * 2), style = Stroke(strokeWidth))

            // Top-Right
            drawLine(bracketColor, Offset(right, top + cornerRadius), Offset(right, top + cornerLength), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, Offset(right - cornerRadius, top), Offset(right - cornerLength, top), strokeWidth, StrokeCap.Round)
            drawArc(bracketColor, 270f, 90f, false, topLeft = Offset(right - cornerRadius * 2, top), size = Size(cornerRadius * 2, cornerRadius * 2), style = Stroke(strokeWidth))

            // Bottom-Left
            drawLine(bracketColor, Offset(left, bottom - cornerRadius), Offset(left, bottom - cornerLength), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, Offset(left + cornerRadius, bottom), Offset(left + cornerLength, bottom), strokeWidth, StrokeCap.Round)
            drawArc(bracketColor, 90f, 90f, false, topLeft = Offset(left, bottom - cornerRadius * 2), size = Size(cornerRadius * 2, cornerRadius * 2), style = Stroke(strokeWidth))

            // Bottom-Right
            drawLine(bracketColor, Offset(right, bottom - cornerRadius), Offset(right, bottom - cornerLength), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, Offset(right - cornerRadius, bottom), Offset(right - cornerLength, bottom), strokeWidth, StrokeCap.Round)
            drawArc(bracketColor, 0f, 90f, false, topLeft = Offset(right - cornerRadius * 2, bottom - cornerRadius * 2), size = Size(cornerRadius * 2, cornerRadius * 2), style = Stroke(strokeWidth))

            // Animated Caramel Laser Sweep Line with Glow
            if (isScanningActive) {
                val laserY = top + (boxHeight * laserPosition)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Caramel400.copy(alpha = 0.0f),
                            Caramel400.copy(alpha = 0.45f * pulseAlpha),
                            Caramel400.copy(alpha = 0.0f)
                        ),
                        startY = laserY - 14.dp.toPx(),
                        endY = laserY + 14.dp.toPx()
                    ),
                    topLeft = Offset(left + 6.dp.toPx(), laserY - 7.dp.toPx()),
                    size = Size(boxWidth - 12.dp.toPx(), 14.dp.toPx())
                )
                drawLine(
                    color = Caramel400,
                    start = Offset(left + 10.dp.toPx(), laserY),
                    end = Offset(right - 10.dp.toPx(), laserY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Live Scanning Status Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp),
            shape = RoundedCornerShape(20.dp),
            color = Roast900.copy(alpha = 0.90f),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Caramel500.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (lastScannedBarcode != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scanned: $lastScannedBarcode",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Emerald500
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Caramel400.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Caramel400,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Desk Scanner Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
