package com.packready.shopkeeper.data.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.packready.shopkeeper.domain.model.SaleTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothPrinterManager @Inject constructor() {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<Pair<String, String>> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            adapter.bondedDevices?.map { Pair(it.name ?: "Unknown Device", it.address) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        deviceAddress: String,
        sale: SaleTransaction,
        storeName: String = "PACKREADY MART",
        storeAddress: String = "Main Market Road, Kirana Bazaar"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter unavailable"))

            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            val out: OutputStream = socket.outputStream

            // 1. ESC/POS Init
            out.write(byteArrayOf(0x1B, 0x40))

            // 2. Center Align & Header
            out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
            out.write("\n$storeName\n".toByteArray(Charsets.US_ASCII))
            out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold OFF
            out.write("$storeAddress\n".toByteArray(Charsets.US_ASCII))
            out.write("GSTIN: 27AABCP1234F1Z9\n".toByteArray(Charsets.US_ASCII))
            out.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))

            // 3. Invoice Meta
            out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left Align
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
            out.write("Inv: ${sale.invoiceNumber}\n".toByteArray(Charsets.US_ASCII))
            out.write("Date: ${sdf.format(Date(sale.timestamp))}\n".toByteArray(Charsets.US_ASCII))
            if (!sale.customerName.isNullOrBlank()) {
                out.write("Cust: ${sale.customerName} (${sale.customerPhone ?: ""})\n".toByteArray(Charsets.US_ASCII))
            }
            out.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))

            // 4. Line Items Table (32 Column standard for 58mm)
            out.write(byteArrayOf(0x1B, 0x45, 0x01))
            out.write("ITEM             QTY  RATE  TOTAL\n".toByteArray(Charsets.US_ASCII))
            out.write(byteArrayOf(0x1B, 0x45, 0x00))
            out.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))

            for (item in sale.items) {
                val namePart = item.productName.take(15).padEnd(15)
                val qtyPart = (if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity)).padStart(4)
                val ratePart = String.format(Locale.US, "%.0f", item.unitPrice).padStart(5)
                val totalPart = String.format(Locale.US, "%.1f", item.total).padStart(6)

                out.write("$namePart $qtyPart $ratePart $totalPart\n".toByteArray(Charsets.US_ASCII))
            }

            // 5. Total Calculations
            out.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))
            out.write("Subtotal:               Rs.${String.format(Locale.US, "%.2f", sale.subtotal)}\n".toByteArray(Charsets.US_ASCII))
            if (sale.discountAmount > 0) {
                out.write("Discount:              -Rs.${String.format(Locale.US, "%.2f", sale.discountAmount)}\n".toByteArray(Charsets.US_ASCII))
            }
            if (sale.taxAmount > 0) {
                out.write("GST Tax:               +Rs.${String.format(Locale.US, "%.2f", sale.taxAmount)}\n".toByteArray(Charsets.US_ASCII))
            }

            out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
            out.write("GRAND TOTAL:            Rs.${String.format(Locale.US, "%.2f", sale.grandTotal)}\n".toByteArray(Charsets.US_ASCII))
            out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold OFF

            // 6. Payment Information
            out.write("Paid via: ${sale.paymentMethod.label}\n".toByteArray(Charsets.US_ASCII))
            if (sale.paymentMethod.name == "CASH" && sale.cashReceived > 0) {
                out.write("Cash Given: Rs.${String.format(Locale.US, "%.2f", sale.cashReceived)} | Change: Rs.${String.format(Locale.US, "%.2f", sale.changeReturned)}\n".toByteArray(Charsets.US_ASCII))
            }

            // 7. Footer
            out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            out.write("\n*** THANK YOU VISIT AGAIN ***\n".toByteArray(Charsets.US_ASCII))
            out.write("Powered by PackReady Smart POS\n\n\n\n".toByteArray(Charsets.US_ASCII))

            // 8. Cut Paper Command (GS V 65 16)
            out.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10))

            out.flush()
            socket.close()
            Result.success(true)
        } catch (e: Exception) {
            try {
                socket?.close()
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    /**
     * Builds plain-text formatted receipt for preview or WhatsApp sharing
     */
    fun formatReceiptText(sale: SaleTransaction, storeName: String = "PACKREADY MART"): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine("       $storeName       ")
        sb.appendLine("  Main Market, Kirana Bazaar")
        sb.appendLine("================================")
        sb.appendLine("Invoice: ${sale.invoiceNumber}")
        sb.appendLine("Date: ${sdf.format(Date(sale.timestamp))}")
        if (!sale.customerName.isNullOrBlank()) {
            sb.appendLine("Customer: ${sale.customerName} (${sale.customerPhone ?: ""})")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("ITEM             QTY  RATE TOTAL")
        sb.appendLine("--------------------------------")
        for (item in sale.items) {
            val namePart = item.productName.take(15).padEnd(15)
            val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity)
            val total = String.format(Locale.US, "%.1f", item.total)
            sb.appendLine("$namePart ${qty.padStart(3)}x ${total.padStart(7)}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("Subtotal:               ₹${String.format(Locale.US, "%.2f", sale.subtotal)}")
        if (sale.discountAmount > 0) {
            sb.appendLine("Discount:              -₹${String.format(Locale.US, "%.2f", sale.discountAmount)}")
        }
        if (sale.taxAmount > 0) {
            sb.appendLine("GST Tax:               +₹${String.format(Locale.US, "%.2f", sale.taxAmount)}")
        }
        sb.appendLine("GRAND TOTAL:            ₹${String.format(Locale.US, "%.2f", sale.grandTotal)}")
        sb.appendLine("Payment: ${sale.paymentMethod.label}")
        if (sale.paymentMethod.name == "CASH" && sale.cashReceived > 0) {
            sb.appendLine("Cash Paid: ₹${String.format(Locale.US, "%.2f", sale.cashReceived)} | Change: ₹${String.format(Locale.US, "%.2f", sale.changeReturned)}")
        }
        sb.appendLine("================================")
        sb.appendLine("  Thank You! Visit Again :)     ")
        sb.appendLine("================================")
        return sb.toString()
    }
}
