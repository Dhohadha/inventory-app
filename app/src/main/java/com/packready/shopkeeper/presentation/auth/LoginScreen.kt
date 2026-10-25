package com.packready.shopkeeper.presentation.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Cream200
import com.packready.shopkeeper.presentation.theme.Cream50
import com.packready.shopkeeper.presentation.theme.Espresso900
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.ui.components.GoogleSignInButton
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isLoggedIn, uiState.isGuestMode) {
        if (uiState.isLoggedIn || uiState.isGuestMode) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1107), // Deep Obsidian Roast
                        Color(0xFF281608),
                        Color(0xFF331D0B),
                        Color(0xFF180D05)
                    )
                )
            )
    ) {
        // Decorative warm ambient radial glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .size(280.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Caramel500.copy(alpha = 0.22f),
                            Caramel600.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Brand identity section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Outer glowing emblem container
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.5.dp, Caramel400.copy(alpha = 0.4f)),
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PackReadyEmblem(size = 54.dp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PackReady",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp,
                        color = Cream50
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Caramel500
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Espresso900,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Next-Gen Retail OS & Master Catalog",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Caramel100.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Value Proposition Highlights
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeaturePillCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Instant Master Catalog",
                    subtitle = "Scan any barcode to extract product details & images"
                )
                FeaturePillCard(
                    icon = Icons.Default.ReceiptLong,
                    title = "AI Invoice OCR & Restock",
                    subtitle = "Automated bill reconciliation & instant stock adjustments"
                )
                FeaturePillCard(
                    icon = Icons.Default.CloudDone,
                    title = "Cloud & Offline Multi-Counter",
                    subtitle = "Real-time PostgreSQL sync with Cloudinary image CDN"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Action Card (Glassmorphic)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2C190B).copy(alpha = 0.85f)
                ),
                border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.28f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign in to your store account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cream50,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sync catalog, cashier billing, and supplier receipts",
                        style = MaterialTheme.typography.bodySmall,
                        color = Caramel100.copy(alpha = 0.7f),
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (uiState.errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF4A1818),
                            border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = Color(0xFFFFCDD2),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Google Sign-In Action
                    GoogleSignInButton(
                        isLoading = uiState.isLoading,
                        text = "Continue with Google",
                        onClick = {
                            viewModel.signInWithGoogle(context) { success ->
                                if (success) {
                                    onLoginSuccess()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Caramel400.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = Caramel100.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Caramel400.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Guest / Offline Mode Button
                    OutlinedButton(
                        onClick = {
                            viewModel.continueAsGuest {
                                onLoginSuccess()
                            }
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Caramel400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Explore in Offline Mode",
                                color = Caramel100,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Footnote
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Caramel400.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Encrypted Local Storage • Fast Master Server Ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = Caramel100.copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun FeaturePillCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF241407).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.16f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Caramel500.copy(alpha = 0.18f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Caramel400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Cream50,
                    fontSize = 13.5.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Caramel100.copy(alpha = 0.7f),
                    fontSize = 11.5.sp
                )
            }
        }
    }
}
