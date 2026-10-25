package com.packready.shopkeeper.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GoogleSignInButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String = "Sign in with Google",
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFF4285F4),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Authenticating...",
                    color = Color(0xFF3C4043),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                // Vector Canvas drawing official 4-color Google 'G' logo
                GoogleLogoIcon(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = text,
                    color = Color(0xFF3C4043),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w * 0.46f

        // Blue (#4285F4)
        val blue = Color(0xFF4285F4)
        // Red (#EA4335)
        val red = Color(0xFFEA4335)
        // Yellow (#FBBC05)
        val yellow = Color(0xFFFBBC05)
        // Green (#34A853)
        val green = Color(0xFF34A853)

        // Draw Google G geometry
        val blueBar = Path().apply {
            moveTo(cx, cy - radius * 0.22f)
            lineTo(w, cy - radius * 0.22f)
            lineTo(w, cy + radius * 0.22f)
            lineTo(cx, cy + radius * 0.22f)
            close()
        }
        drawPath(blueBar, blue)

        // Outer circular rings
        drawArc(
            color = red,
            startAngle = 180f,
            sweepAngle = 140f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.2f),
            topLeft = Offset(w * 0.1f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f)
        )
        drawArc(
            color = yellow,
            startAngle = 120f,
            sweepAngle = 60f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.2f),
            topLeft = Offset(w * 0.1f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f)
        )
        drawArc(
            color = green,
            startAngle = 45f,
            sweepAngle = 75f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.2f),
            topLeft = Offset(w * 0.1f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f)
        )
        drawArc(
            color = blue,
            startAngle = 0f,
            sweepAngle = 45f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.2f),
            topLeft = Offset(w * 0.1f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f)
        )
    }
}
