package com.packready.shopkeeper.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packready.shopkeeper.presentation.analytics.DailyReportScreen
import com.packready.shopkeeper.presentation.checkout.PosCheckoutScreen
import com.packready.shopkeeper.presentation.khata.KhataLedgerScreen
import com.packready.shopkeeper.presentation.loosegoods.LabelGeneratorScreen
import com.packready.shopkeeper.presentation.restock.InwardRestockScreen

@Composable
fun PackReadyNavigation() {
    val items = Screen.bottomNavItems
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val saveableStateHolder = rememberSaveableStateHolder()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
            ) {
                items.forEachIndexed { index, screen ->
                    val isSelected = selectedIndex == index
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tab_icon_scale"
                    )

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (selectedIndex != index) {
                                selectedIndex = index
                            }
                        },
                        icon = {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> (fullWidth * 0.15f * direction).toInt() }
                ) + fadeIn(animationSpec = tween(durationMillis = 180))).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> (-fullWidth * 0.15f * direction).toInt() }
                    ) + fadeOut(animationSpec = tween(durationMillis = 150))
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "main_screen_tab_transition"
        ) { targetIndex ->
            saveableStateHolder.SaveableStateProvider(key = targetIndex) {
                when (targetIndex) {
                    0 -> PosCheckoutScreen()
                    1 -> InwardRestockScreen()
                    2 -> LabelGeneratorScreen()
                    3 -> KhataLedgerScreen()
                    4 -> DailyReportScreen()
                    else -> PosCheckoutScreen()
                }
            }
        }
    }
}

