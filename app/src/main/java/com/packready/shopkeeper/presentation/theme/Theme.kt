package com.packready.shopkeeper.presentation.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Espresso800,
    onPrimary = Cream50,
    primaryContainer = Caramel100,
    onPrimaryContainer = Espresso900,
    secondary = Caramel600,
    onSecondary = Color.White,
    secondaryContainer = Caramel50,
    onSecondaryContainer = Espresso800,
    tertiary = ForestGreen600,
    onTertiary = Color.White,
    tertiaryContainer = ForestGreen100,
    onTertiaryContainer = ForestGreen600,
    background = Cream50,
    onBackground = BrownTextDark,
    surface = Color.White,
    onSurface = BrownTextDark,
    surfaceVariant = Cream100,
    onSurfaceVariant = BrownTextMedium,
    outline = BrownBorder,
    outlineVariant = BrownDivider,
    error = RustError600,
    onError = Color.White,
    errorContainer = RustError100,
    onErrorContainer = RustError600
)

private val DarkColorScheme = darkColorScheme(
    primary = Caramel400,
    onPrimary = Espresso900,
    primaryContainer = Espresso700,
    onPrimaryContainer = Caramel100,
    secondary = Caramel200,
    onSecondary = Espresso900,
    background = Color(0xFF1B140E),
    onBackground = Cream100,
    surface = Color(0xFF261D15),
    onSurface = Cream50,
    surfaceVariant = Color(0xFF33271D),
    onSurfaceVariant = Cream300,
    outline = Color(0xFF4D3C2E),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF3E0A0A)
)

@Composable
fun PackReadyShopkeeperTheme(
    darkTheme: Boolean = false, // Default to professional Light Theme Brown
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is ContextWrapper) {
                if (context is Activity) {
                    val window = context.window
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.surfaceVariant.toArgb()
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                    break
                }
                context = context.baseContext
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
