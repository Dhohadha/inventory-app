package com.packready.shopkeeper.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMuted
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Espresso800
import com.packready.shopkeeper.presentation.theme.Espresso900

/**
 * Custom-crafted high-performance geometric vector emblem for PackReady Shopkeeper Edition
 */
@Composable
fun PackReadyEmblem(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Outer Hexagonal / Rounded Badge Container
        val bgBrush = Brush.linearGradient(
            colors = listOf(Espresso800, Espresso900),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRoundRect(
            brush = bgBrush,
            cornerRadius = CornerRadius(w * 0.26f, h * 0.26f),
            size = Size(w, h)
        )

        // Subtle Gold-Caramel Border
        drawRoundRect(
            color = Caramel500.copy(alpha = 0.45f),
            cornerRadius = CornerRadius(w * 0.26f, h * 0.26f),
            size = Size(w, h),
            style = Stroke(width = w * 0.045f)
        )

        // Geometric PackReady Cube/Box Motif
        val boxWidth = w * 0.48f
        val boxHeight = h * 0.44f
        val boxLeft = (w - boxWidth) / 2f
        val boxTop = (h - boxHeight) / 2f

        // Front Face
        drawRoundRect(
            color = Caramel500,
            topLeft = Offset(boxLeft, boxTop + h * 0.08f),
            size = Size(boxWidth, boxHeight * 0.72f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )

        // Top Lid Fold
        val path = Path().apply {
            moveTo(boxLeft, boxTop + h * 0.08f)
            lineTo(w / 2f, boxTop)
            lineTo(boxLeft + boxWidth, boxTop + h * 0.08f)
            close()
        }
        drawPath(path, color = Color(0xFFE5A84B))

        // Center Laser Beam Accent
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = Offset(boxLeft + boxWidth * 0.15f, boxTop + boxHeight * 0.44f),
            end = Offset(boxLeft + boxWidth * 0.85f, boxTop + boxHeight * 0.44f),
            strokeWidth = w * 0.07f
        )
    }
}

@Composable
fun PackReadyHeaderBrand(
    subtitle: String = "Shopkeeper Edition",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PackReadyEmblem(size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PackReady",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.3).sp,
                    color = BrownTextDark
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Caramel100,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = "PRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Caramel700,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BrownTextMuted
            )
        }
    }
}
