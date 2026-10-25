package com.packready.shopkeeper.presentation.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.packready.shopkeeper.domain.model.Cart
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.presentation.theme.BrownTextDark
import com.packready.shopkeeper.presentation.theme.BrownTextMuted
import com.packready.shopkeeper.presentation.theme.Caramel100
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel600
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Cream100
import com.packready.shopkeeper.presentation.theme.Espresso800
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import com.packready.shopkeeper.presentation.theme.PackReadyShopkeeperTheme
import com.packready.shopkeeper.presentation.ui.components.DynamicUpiQrCard
import com.packready.shopkeeper.presentation.ui.components.PackReadyEmblem
import com.packready.shopkeeper.presentation.ui.components.bouncingClickable
import java.util.Locale

@Composable
fun PaymentDialog(
    cart: Cart,
    activeMethod: PaymentMethod,
    cashReceivedInput: String,
    customerName: String = "",
    customerPhone: String = "",
    onMethodSelect: (PaymentMethod) -> Unit,
    onCashReceivedChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit = {},
    onCustomerPhoneChange: (String) -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val grandTotal = cart.grandTotal
    val cashReceived = cashReceivedInput.toDoubleOrNull() ?: 0.0
    val changeReturned = (cashReceived - grandTotal).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PackReadyShopkeeperTheme(darkTheme = false) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .imePadding()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with PackReady Emblem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PackReadyEmblem(size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Settlement & Payment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Grand Total: ₹${String.format(Locale.US, "%.2f", grandTotal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Method Selector Tabs (Cash, UPI QR, and Adjustment)
                    PaymentMethodTabs(
                        selected = activeMethod,
                        onSelect = onMethodSelect
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content depending on selected method
                    when (activeMethod) {
                        PaymentMethod.UPI -> {
                            DynamicUpiQrCard(
                                amount = grandTotal,
                                transactionRef = "TXN-${System.currentTimeMillis() % 100000}"
                            )
                        }
                        PaymentMethod.ADJUSTMENT -> {
                            AdjustmentSection(
                                grandTotal = grandTotal,
                                itemCount = cart.items.size,
                                reason = customerName,
                                onReasonChange = onCustomerNameChange
                            )
                        }
                        else -> {
                            CashPaymentSection(
                                grandTotal = grandTotal,
                                cashInput = cashReceivedInput,
                                changeReturned = changeReturned,
                                onCashChange = onCashReceivedChange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Confirmation Button
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMethod == PaymentMethod.ADJUSTMENT) Caramel600 else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .bouncingClickable(onClick = onConfirm)
                    ) {
                        Text(
                            text = if (activeMethod == PaymentMethod.ADJUSTMENT)
                                "Confirm Inventory Adjustment (₹${String.format(Locale.US, "%.2f", grandTotal)})"
                            else
                                "Complete Sale (₹${String.format(Locale.US, "%.2f", grandTotal)})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodTabs(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
) {
    val tabs = listOf(
        Pair(PaymentMethod.CASH, Icons.Default.LocalAtm),
        Pair(PaymentMethod.UPI, Icons.Default.QrCode),
        Pair(PaymentMethod.ADJUSTMENT, Icons.Default.Inventory2)
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { (method, icon) ->
                val isSelected = selected == method
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Espresso800 else Color.Transparent,
                    shadowElevation = if (isSelected) 3.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(method) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = method.label,
                            tint = if (isSelected) Caramel400 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = method.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSection(
    grandTotal: Double,
    itemCount: Int,
    reason: String,
    onReasonChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Cream100),
        border = BorderStroke(1.dp, Caramel400.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Caramel100,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Caramel700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Inventory Stock Adjustment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrownTextDark
                    )
                    Text(
                        text = "Non-cash inventory stock deduction",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = BrownTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEADBCE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• Deducts $itemCount item(s) from current store stock",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = BrownTextDark
                    )
                    Text(
                        text = "• Adjustment Value: ₹${String.format(Locale.US, "%.2f", grandTotal)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = Caramel700
                    )
                    Text(
                        text = "• Listed in Transaction History as 'Adjustment'",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = BrownTextDark
                    )
                    Text(
                        text = "• EXCLUDED from Cash Count & Cash Drawer balance",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = ForestGreen600
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                label = { Text("Adjustment Reason (Optional)", style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)) },
                placeholder = { Text("e.g. Damaged, Expired, Store Use, Audit", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Caramel600,
                    unfocusedBorderColor = Color(0xFFEADBCE)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CashPaymentSection(
    grandTotal: Double,
    cashInput: String,
    changeReturned: Double,
    onCashChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = cashInput,
            onValueChange = onCashChange,
            label = { Text("Cash Received from Customer") },
            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Note denomination pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val denominations = listOf(
                Pair("Exact", grandTotal.toInt().toString()),
                Pair("₹100", "100"),
                Pair("₹200", "200"),
                Pair("₹500", "500"),
                Pair("₹2000", "2000")
            )
            denominations.forEach { (label, amount) ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .bouncingClickable { onCashChange(amount) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Change Return Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (changeReturned > 0)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(
                1.dp,
                if (changeReturned > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cash Change to Return:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", changeReturned)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (changeReturned > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
