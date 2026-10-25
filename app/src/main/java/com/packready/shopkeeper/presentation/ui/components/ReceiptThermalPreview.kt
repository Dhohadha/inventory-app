package com.packready.shopkeeper.presentation.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.model.SaleTransaction
import com.packready.shopkeeper.presentation.theme.Caramel700
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.ForestGreen600
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptThermalPreviewDialog(
    sale: SaleTransaction,
    onPrintBluetooth: () -> Unit,
    onDismiss: () -> Unit,
    storeName: String = "PACKREADY MART"
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    val isAdjustment = sale.paymentMethod == PaymentMethod.ADJUSTMENT

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .imePadding()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAdjustment) Caramel700.copy(alpha = 0.12f) else ForestGreen600.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = if (isAdjustment) Caramel700 else ForestGreen600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isAdjustment) "Adjustment Recorded" else "Sale Completed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdjustment) Caramel700 else ForestGreen600
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thermal Paper Simulation Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFAF7EE) // Off-white authentic thermal receipt
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE5DECE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = storeName,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Main Market Road, Kirana Bazaar",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Inv: ${sale.invoiceNumber}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                            Text(sdf.format(Date(sale.timestamp)), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                        }
                        if (!sale.customerName.isNullOrBlank()) {
                            Text(
                                text = "Cust: ${sale.customerName} (${sale.customerPhone ?: ""})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black
                        )

                        // Item rows
                        for (item in sale.items) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.productName.take(15),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity}x",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    text = "₹${String.format(Locale.US, "%.1f", item.total)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GRAND TOTAL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("₹${String.format(Locale.US, "%.2f", sale.grandTotal)}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                        }
                        Text(
                            text = if (isAdjustment) "Type: Inventory Stock Adjustment" else "Paid via: ${sale.paymentMethod.label}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isAdjustment && !sale.customerName.isNullOrBlank() && sale.customerName != "Store Inventory Adjustment") {
                            Text(
                                text = "Reason: ${sale.customerName}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            text = if (isAdjustment) "\n*** INVENTORY ADJUSTMENT VOUCHER ***" else "\n*** THANK YOU VISIT AGAIN ***",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Print Thermal & Share WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareBillViaWhatsApp(context, sale, storeName)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp Bill")
                    }

                    Button(
                        onClick = onPrintBluetooth,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print ESC/POS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Builds clean, comprehensive WhatsApp formatted message listing every single product,
 * quantities, rates, subtotal, discounts, GST, and final payment details.
 */
fun buildWhatsAppBillMessage(sale: SaleTransaction, storeName: String = "PACKREADY MART"): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(sale.timestamp))
    val sb = StringBuilder()

    sb.appendLine("🧾 *$storeName*")
    sb.appendLine("📍 Retail Counter Invoice")
    sb.appendLine("--------------------------------")
    sb.appendLine("📄 *Invoice:* ${sale.invoiceNumber}")
    sb.appendLine("📅 *Date:* $dateStr")
    if (!sale.customerName.isNullOrBlank()) {
        val phoneSuffix = if (!sale.customerPhone.isNullOrBlank()) " (${sale.customerPhone})" else ""
        sb.appendLine("👤 *Customer:* ${sale.customerName}$phoneSuffix")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("🛒 *ITEMS PURCHASED (${sale.items.size}):*")

    sale.items.forEachIndexed { index, item ->
        val qty = if (item.quantity % 1.0 == 0.0) {
            item.quantity.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", item.quantity)
        }
        val unitLabel = if (item.unit.isNotBlank() && item.unit != "pcs") " ${item.unit}" else ""
        val rate = String.format(Locale.US, "%.2f", item.unitPrice)
        val itemTotal = String.format(Locale.US, "%.2f", item.total)

        sb.appendLine("${index + 1}. *${item.productName}*")
        sb.appendLine("   $qty$unitLabel × ₹$rate = *₹$itemTotal*")
        if (item.discount > 0) {
            sb.appendLine("   (Item Discount: -₹${String.format(Locale.US, "%.2f", item.discount)})")
        }
    }

    sb.appendLine("--------------------------------")
    sb.appendLine("Subtotal: ₹${String.format(Locale.US, "%.2f", sale.subtotal)}")
    if (sale.discountAmount > 0) {
        sb.appendLine("Discount: -₹${String.format(Locale.US, "%.2f", sale.discountAmount)}")
    }
    if (sale.taxAmount > 0) {
        sb.appendLine("(Incl. GST / Tax: ₹${String.format(Locale.US, "%.2f", sale.taxAmount)})")
    }
    sb.appendLine("💰 *GRAND TOTAL: ₹${String.format(Locale.US, "%.2f", sale.grandTotal)}*")
    sb.appendLine("💳 *Payment Mode:* ${sale.paymentMethod.label}")
    if (sale.paymentMethod.name == "CASH" && sale.cashReceived > 0) {
        sb.appendLine("💵 Cash Received: ₹${String.format(Locale.US, "%.2f", sale.cashReceived)}")
        if (sale.changeReturned > 0) {
            sb.appendLine("🪙 Change Returned: ₹${String.format(Locale.US, "%.2f", sale.changeReturned)}")
        }
    }
    sb.appendLine("================================")
    sb.appendLine("🙏 *Thank you for shopping with us!*")
    sb.appendLine("Please visit again!")
    return sb.toString()
}

/**
 * Dispatches the formatted bill directly to WhatsApp, with graceful fallback to system share sheet.
 */
fun shareBillViaWhatsApp(context: Context, sale: SaleTransaction, storeName: String) {
    val message = buildWhatsAppBillMessage(sale, storeName)
    val phone = sale.customerPhone?.filter { it.isDigit() }?.takeLast(10) ?: ""
    val encodedMessage = Uri.encode(message)

    val uri = if (phone.length == 10) {
        Uri.parse("https://api.whatsapp.com/send?phone=91$phone&text=$encodedMessage")
    } else {
        Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
    }

    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Bill"))
    }
}
