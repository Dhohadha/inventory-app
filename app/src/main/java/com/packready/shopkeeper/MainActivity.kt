package com.packready.shopkeeper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packready.shopkeeper.presentation.auth.AuthViewModel
import com.packready.shopkeeper.presentation.auth.LoginScreen
import com.packready.shopkeeper.presentation.auth.UnregisteredStoreGateScreen
import com.packready.shopkeeper.presentation.navigation.PackReadyNavigation
import com.packready.shopkeeper.presentation.profile.ProfileSettingsScreen
import com.packready.shopkeeper.presentation.theme.PackReadyShopkeeperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle permissions granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()

        setContent {
            PackReadyShopkeeperTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PackReadyAppRoot()
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionRequestLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

private enum class RootDestination {
    Auth,
    UnregisteredGate,
    Profile,
    Main
}

@Composable
fun PackReadyAppRoot(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var isProfileOpen by rememberSaveable { mutableStateOf(false) }

    // Intercept back button when profile is open
    BackHandler(enabled = isProfileOpen) {
        isProfileOpen = false
    }

    // Determine target destination
    val showAuth = !authUiState.isLoggedIn && !authUiState.isGuestMode
    val isUnregistered = authUiState.isLoggedIn && !authUiState.storeInfo.isRegistered && !authUiState.isGuestMode

    val currentDestination = when {
        showAuth -> RootDestination.Auth
        isUnregistered -> RootDestination.UnregisteredGate
        isProfileOpen -> RootDestination.Profile
        else -> RootDestination.Main
    }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) + slideInVertically(
                animationSpec = tween(220),
                initialOffsetY = { fullHeight -> (fullHeight * 0.08f).toInt() }
            )).togetherWith(
                fadeOut(animationSpec = tween(180)) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { fullHeight -> (-fullHeight * 0.08f).toInt() }
                )
            )
        },
        label = "root_app_navigation"
    ) { destination ->
        when (destination) {
            RootDestination.Auth -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        isProfileOpen = false
                    }
                )
            }
            RootDestination.UnregisteredGate -> {
                UnregisteredStoreGateScreen(
                    authViewModel = authViewModel,
                    onEnterOfflineMode = {
                        isProfileOpen = false
                    }
                )
            }
            RootDestination.Profile -> {
                ProfileSettingsScreen(
                    onNavigateBack = {
                        isProfileOpen = false
                    },
                    onSignOutSuccess = {
                        isProfileOpen = false
                    },
                    authViewModel = authViewModel
                )
            }
            RootDestination.Main -> {
                PackReadyNavigation(
                    onOpenProfile = {
                        isProfileOpen = true
                    }
                )
            }
        }
    }
}
