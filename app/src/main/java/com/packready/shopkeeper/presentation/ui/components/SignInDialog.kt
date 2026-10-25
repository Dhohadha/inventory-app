package com.packready.shopkeeper.presentation.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.packready.shopkeeper.data.remote.auth.GoogleSignInManager
import com.packready.shopkeeper.data.remote.auth.GoogleUserAccount
import kotlinx.coroutines.launch

@Composable
fun SignInDialog(
    googleSignInManager: GoogleSignInManager,
    onDismissRequest: () -> Unit,
    onSignInSuccess: (GoogleUserAccount) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isLoading) onDismissRequest() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo / Icon Header
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "PackReady Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to PackReady",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sign in to sync your inventory, access the global master catalog, and unlock AI receipt reconciliation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 13.5.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Google Sign In Button
                GoogleSignInButton(
                    isLoading = isLoading,
                    text = "Continue with Google",
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = googleSignInManager.signIn(context)
                            isLoading = false
                            result.onSuccess { account ->
                                Toast.makeText(context, "Welcome, ${account.displayName ?: account.email}!", Toast.LENGTH_SHORT).show()
                                onSignInSuccess(account)
                            }.onFailure { err ->
                                errorMessage = err.localizedMessage ?: "Failed to sign in with Google"
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismissRequest,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Skip for Now (Offline Mode)",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
