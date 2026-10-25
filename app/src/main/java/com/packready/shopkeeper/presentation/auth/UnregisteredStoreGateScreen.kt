package com.packready.shopkeeper.presentation.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packready.shopkeeper.presentation.theme.AmberWarning100
import com.packready.shopkeeper.presentation.theme.AmberWarning600
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Espresso900
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.theme.RustError100
import com.packready.shopkeeper.presentation.theme.RustError600
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem

/**
 * Access Control Gate Screen:
 * Displayed when an authenticated user's Google account is not whitelisted / registered
 * by the PackReady Administrator. Completely blocks access to main POS/Catalog UI.
 */
@Composable
fun UnregisteredStoreGateScreen(
    authViewModel: AuthViewModel,
    onEnterOfflineMode: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val user = uiState.currentUser
    val userEmail = user?.email ?: "Your Account"
    val contactEmail = uiState.storeInfo.contactEmail.ifBlank { "dhohadhanaryana@gmail.com" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Espresso900,
                        Roast900,
                        Color(0xFF150A02)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // PackReady Emblem
            PackReadyEmblem(modifier = Modifier.size(76.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Warning Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RustError100.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, RustError600.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = RustError600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Store Unregistered",
                        color = Color(0xFFFF8A80),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Heading Title
            Text(
                text = "Access Restricted",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your Google account is signed in, but your store has not been registered or approved by the Central Administrator yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Caramel100.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Signed-in Account Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF221307).copy(alpha = 0.75f),
                border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Caramel500.copy(alpha = 0.25f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (user?.name?.firstOrNull() ?: 'U').uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Caramel100,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.name ?: "Shopkeeper",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail,
                            color = Caramel100.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Red.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Not Whitelisted",
                            color = Color(0xFFFF8A80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Contact Administrator Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2B180A).copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Caramel400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contact Administrator to Register",
                            fontWeight = FontWeight.Bold,
                            color = Caramel100,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "To access the master catalog, cloud sync, and your inventory, ask the admin to register your store with this email:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Caramel100.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contact Email Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = contactEmail,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$contactEmail")
                                        putExtra(Intent.EXTRA_SUBJECT, "PackReady Store Whitelist Request ($userEmail)")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Hello Admin,\n\nPlease whitelist my store for PackReady Shopkeeper access:\n\nShopkeeper Email: $userEmail\nStore Name: \nStore Location: \nPhone Number: \n\nThank you!"
                                        )
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Contact Email: $contactEmail", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Caramel600
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Send Email", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Check Approval Button
                    OutlinedButton(
                        onClick = {
                            authViewModel.checkStoreRegistration { isRegistered ->
                                if (isRegistered) {
                                    Toast.makeText(context, "Store approved! Access granted.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Store is still pending registration.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.4f))
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Caramel400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Caramel400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "Check Approval Status",
                            color = Caramel100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action 1: Try Different Account Button (Mandatory Requirement)
            Button(
                onClick = {
                    authViewModel.switchGoogleAccount(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Caramel500
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwitchAccount,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Try Different Account",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 2: Open Offline Mode Button (Mandatory Requirement)
            OutlinedButton(
                onClick = {
                    authViewModel.continueAsGuest {
                        onEnterOfflineMode()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.35f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Caramel400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Open Offline Mode",
                        color = Caramel100,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Small footnote
            Text(
                text = "Offline Mode allows local counter billing without cloud sync.",
                style = MaterialTheme.typography.labelSmall,
                color = Caramel100.copy(alpha = 0.45f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
