package com.packready.shopkeeper.presentation.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packready.shopkeeper.domain.model.DailyAnalytics
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.model.SaleTransaction
import com.packready.shopkeeper.presentation.theme.Amber500
import com.packready.shopkeeper.presentation.theme.BrownTextMuted
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.ui.components.MetricCard
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem
import com.packready.shopkeeper.presentation.ui.components.ReceiptThermalPreviewDialog
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(AnalyticsUiEvent.ClearMessages)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PackReadyEmblem(size = 30.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Daily Reports & Store Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daily Revenue & Transaction Breakdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(AnalyticsUiEvent.Refresh) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val stats = uiState.dailyAnalytics

            Text(
                text = stats?.dateString ?: "Today's Business Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2 Metric Cards Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Today's Revenue",
                    value = "₹${String.format(Locale.US, "%.2f", stats?.totalRevenue ?: 0.0)}",
                    subtitle = "${stats?.totalTransactionsCount ?: 0} Invoices Generated",
                    icon = Icons.Default.AttachMoney,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Units Sold",
                    value = "${stats?.totalUnitsSold?.toInt() ?: 0} Units",
                    subtitle = "Across ${stats?.totalTransactionsCount ?: 0} Transactions",
                    icon = Icons.Default.ShoppingBag,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Split Horizontal Breakdown
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Payment Mode Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val cash = stats?.paymentBreakdown?.cashTotal ?: 0.0
                    val upi = stats?.paymentBreakdown?.upiTotal ?: 0.0
                    val adjustment = stats?.paymentBreakdown?.adjustmentTotal ?: 0.0
                    val total = (cash + upi).coerceAtLeast(1.0)

                    // Proportional Visual Progress Bar
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (cash > 0) {
                            Box(
                                modifier = Modifier
                                    .weight((cash / total).toFloat().coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(ForestGreen600)
                            )
                        }
                        if (upi > 0) {
                            Box(
                                modifier = Modifier
                                    .weight((upi / total).toFloat().coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PaymentSplitPill(
                            label = "Cash Count",
                            amount = cash,
                            icon = Icons.Default.LocalAtm,
                            color = ForestGreen600
                        )
                        PaymentSplitPill(
                            label = "UPI QR",
                            amount = upi,
                            icon = Icons.Default.QrCode,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (adjustment > 0) {
                            PaymentSplitPill(
                                label = "Adjustments",
                                amount = adjustment,
                                icon = Icons.Default.Inventory2,
                                color = Caramel700
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Completed Sales Feed
            Text(
                text = "Chronological Sales Feed (${uiState.recentTransactions.size} transactions)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            uiState.recentTransactions.forEach { sale ->
                RecentSaleRow(
                    sale = sale,
                    onClick = { viewModel.onEvent(AnalyticsUiEvent.SelectSaleReceipt(sale)) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    // Receipt Thermal Dialog
    uiState.selectedSaleForReceipt?.let { sale ->
        ReceiptThermalPreviewDialog(
            sale = sale,
            onPrintBluetooth = { viewModel.onEvent(AnalyticsUiEvent.PrintReceiptBluetooth) },
            onDismiss = { viewModel.onEvent(AnalyticsUiEvent.DismissReceipt) }
        )
    }
}

@Composable
private fun PaymentSplitPill(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "₹${String.format(Locale.US, "%.0f", amount)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun RecentSaleRow(
    sale: SaleTransaction,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val isAdjustment = sale.paymentMethod == PaymentMethod.ADJUSTMENT

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdjustment) Cream100 else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isAdjustment) Caramel400.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bouncingClickable(scaleDownFactor = 0.98f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.invoiceNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isAdjustment) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Caramel100,
                            border = BorderStroke(0.5.dp, Caramel500)
                        ) {
                            Text(
                                text = "ADJUSTMENT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Caramel700,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${sdf.format(Date(sale.timestamp))} • ${sale.items.size} items • ${if (isAdjustment) "Stock Deduction (Non-Cash)" else sale.paymentMethod.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAdjustment) Caramel700 else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isAdjustment && !sale.customerName.isNullOrBlank() && sale.customerName != "Store Inventory Adjustment") {
                    Text(
                        text = "Reason: ${sale.customerName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = BrownTextMuted
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", sale.grandTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAdjustment) Caramel700 else ForestGreen600
                )
                Text(
                    text = if (isAdjustment) "View Voucher" else "View Thermal Bill",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAdjustment) Caramel700 else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
