package com.packready.shopkeeper.presentation.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packready.shopkeeper.data.remote.CatalogApiService
import com.packready.shopkeeper.presentation.auth.AuthViewModel
import com.packready.shopkeeper.presentation.theme.AmberWarning100
import com.packready.shopkeeper.presentation.theme.AmberWarning600
import com.packready.shopkeeper.presentation.theme.BrownBorder
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMedium
import com.packready.shopkeeper.presentation.theme.BrownTextMuted
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel50
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Cream200
import com.packready.shopkeeper.presentation.theme.Cream50
import com.packready.shopkeeper.presentation.theme.Espresso900
import com.packready.shopkeeper.presentation.theme.ForestGreen100
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.theme.RustError100
import com.packready.shopkeeper.presentation.theme.RustError600
import com.packready.shopkeeper.presentation.ui.components.GoogleSignInButton
import com.packready.shopkeeper.presentation.ui.components.ProductThumbnailImage
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProfileSettingsEntryPoint {
    fun catalogApiService(): CatalogApiService
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onNavigateBack: () -> Unit,
    onSignOutSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val entryPoint = remember(context) {
        try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ProfileSettingsEntryPoint::class.java
            )
        } catch (e: Exception) {
            null
        }
    }
    val catalogApiService = entryPoint?.catalogApiService()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var isCheckingHealth by remember { mutableStateOf(false) }
    var healthStatusMessage by remember { mutableStateOf<String?>(null) }
    var isHealthSuccess by remember { mutableStateOf(false) }

    val user = uiState.currentUser
    val isGuest = uiState.isGuestMode || (user == null && !uiState.isLoggedIn)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Store & Profile Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrownTextDark
                        )
                        Text(
                            text = "PackReady Shopkeeper Pro • Terminal #1",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrownTextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrownTextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Cream50
                )
            )
        },
        containerColor = Cream100
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Account Profile Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrownBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Caramel100)
                        ) {
                            if (!user?.avatarUrl.isNullOrBlank()) {
                                ProductThumbnailImage(
                                    imagePath = user!!.avatarUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    reqSize = 160
                                ) {
                                    InitialsAvatar(name = user.name ?: "Shopkeeper")
                                }
                            } else {
                                InitialsAvatar(name = user?.name ?: if (isGuest) "Guest" else "Shopkeeper")
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.name ?: if (isGuest) "Offline Guest User" else "Shopkeeper Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrownTextDark,
                                fontSize = 18.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = user?.email ?: "Offline Device Session",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrownTextMedium,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Badges Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Caramel50,
                                    border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "👑 ${user?.role ?: "SHOPKEEPER"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Caramel700,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        fontSize = 10.5.sp
                                    )
                                }

                                if (user?.isGoogleAuth == true) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ForestGreen100
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = ForestGreen600,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "Google Verified",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = ForestGreen600,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AmberWarning100
                                    ) {
                                        Text(
                                            text = "Offline Mode",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberWarning600,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // If Guest Mode: Show prominent upgrade prompt
                    if (isGuest) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BrownBorder.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Caramel50,
                            border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "✨ Link Your Google Account",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Caramel700
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sync your product database, access master barcodes, and store packaging photos on Cloudinary CDN.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrownTextMedium,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                GoogleSignInButton(
                                    isLoading = uiState.isLoading,
                                    text = "Sign in with Google to Sync",
                                    onClick = {
                                        authViewModel.signInWithGoogle(context) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Account linked successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Store Registration Check (Whitelisting & Prefilled Info)
            if (!isGuest && user != null) {
                if (uiState.storeInfo.isRegistered) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.2.dp, ForestGreen600.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen600, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verified Store Profile (Admin Approved)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen600
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.storeInfo.storeName ?: "Registered Store",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrownTextDark
                            )
                            if (!uiState.storeInfo.address.isNullOrBlank() || !uiState.storeInfo.city.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.storeInfo.address ?: ""} ${uiState.storeInfo.city ?: ""} ${uiState.storeInfo.pincode ?: ""}".trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrownTextMedium
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = RustError100.copy(alpha = 0.4f)),
                        border = BorderStroke(1.5.dp, RustError600.copy(alpha = 0.7f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = RustError600, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Account Not Registered",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RustError600
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Google account (${user.email ?: ""}) is not registered by the administrator.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrownTextDark,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Please contact: dhohadhanaryana@gmail.com to get your store registered and activated.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = RustError600,
                                fontSize = 12.5.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:dhohadhanaryana@gmail.com?subject=Store%20Registration%20Request%20(${user.email ?: ""})")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Contact: dhohadhanaryana@gmail.com", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RustError600),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Contact Administrator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Store Configuration Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrownBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Store & Hardware Terminal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Caramel700
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingInfoRow(
                        icon = Icons.Default.Store,
                        label = "Store Identifier",
                        value = uiState.storeInfo.storeName ?: "default-store (Master Counter)"
                    )
                    SettingInfoRow(
                        icon = Icons.Default.PhoneAndroid,
                        label = "Device Hardware",
                        value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
                    )
                    SettingInfoRow(
                        icon = Icons.Default.CloudDone,
                        label = "Media & CDN Storage",
                        value = "Cloudinary CDN (Cloud: c0j8mq7b)"
                    )
                    SettingInfoRow(
                        icon = Icons.Default.VerifiedUser,
                        label = "Local Database Engine",
                        value = "SQLite Room v2.6 (Offline-First ACID)"
                    )
                }
            }

            // 3. Master Server & Sync Diagnostics
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrownBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Server Connectivity & Cache",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Caramel700
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Cream200
                        ) {
                            Text(
                                text = "Port 8000",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrownTextMedium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Active Server URL: http://13.239.252.110/ (AWS Production Cloud)",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrownTextMuted,
                        fontSize = 11.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ping Server Diagnostics Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .bouncingClickable {
                                if (!isCheckingHealth && catalogApiService != null) {
                                    isCheckingHealth = true
                                    healthStatusMessage = null
                                    coroutineScope.launch {
                                        val startMs = System.currentTimeMillis()
                                        try {
                                            val resp = catalogApiService.checkHealth()
                                            val latency = System.currentTimeMillis() - startMs
                                            if (resp.isSuccessful) {
                                                val body = resp.body()
                                                val pg = body?.get("postgres") ?: "up"
                                                val rd = body?.get("redis") ?: "up"
                                                healthStatusMessage = "✓ Server Online (${latency}ms) • PostgreSQL: $pg • Redis: $rd"
                                                isHealthSuccess = true
                                            } else {
                                                healthStatusMessage = "Server returned HTTP ${resp.code()}"
                                                isHealthSuccess = false
                                            }
                                        } catch (e: Exception) {
                                            healthStatusMessage = "Connection failed: ${e.localizedMessage ?: "timeout"}"
                                            isHealthSuccess = false
                                        } finally {
                                            isCheckingHealth = false
                                        }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = Caramel50,
                        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCheckingHealth) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Caramel600,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Testing Connection Latency...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Caramel700
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Caramel600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Test Server Health & Latency",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Caramel700
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = healthStatusMessage != null) {
                        healthStatusMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isHealthSuccess) ForestGreen100 else RustError100,
                                border = BorderStroke(1.dp, if (isHealthSuccess) ForestGreen600.copy(alpha = 0.3f) else RustError600.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isHealthSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isHealthSuccess) ForestGreen600 else RustError600,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isHealthSuccess) ForestGreen600 else RustError600
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Session & Sign Out Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrownBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Session Management",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrownTextDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RustError600
                        ),
                        border = BorderStroke(1.dp, RustError600.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                tint = RustError600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGuest) "Reset Guest Session" else "Sign Out of Store",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "PackReady Shopkeeper PRO • Version 2.4 (Build 2026.10)\nLocal catalog and cached records remain intact on this terminal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrownTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out Confirmation",
                    fontWeight = FontWeight.Bold,
                    color = BrownTextDark
                )
            },
            text = {
                Text(
                    text = if (isGuest) {
                        "Are you sure you want to reset your local session and return to the login screen?"
                    } else {
                        "Are you sure you want to sign out from ${user?.email ?: "this account"}? Your local data will be preserved, but you'll need to sign in again to sync with the server."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrownTextMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        authViewModel.logout {
                            onSignOutSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RustError600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = BrownTextMedium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun InitialsAvatar(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Espresso900),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "S",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Caramel400
        )
    }
}

@Composable
private fun SettingInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Cream100,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Caramel700,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = BrownTextMuted,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrownTextDark,
                fontSize = 13.sp
            )
        }
    }
}
