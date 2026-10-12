package com.packready.shopkeeper.domain.model

import java.net.URLEncoder
import java.util.Locale

object UpiQrGenerator {
    /**
     * Builds a standard EMVCo-compliant UPI deep-link URI
     * Format: upi://pay?pa={vpa}&pn={name}&am={amount}&cu=INR&tn={ref}
     */
    fun generateUpiUri(
        vpa: String = "packready.mart@upi",
        merchantName: String = "PACKREADY MART",
        amount: Double,
        transactionRef: String
    ): String {
        val encodedName = URLEncoder.encode(merchantName, "UTF-8")
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val encodedRef = URLEncoder.encode(transactionRef, "UTF-8")
        return "upi://pay?pa=$vpa&pn=$encodedName&am=$formattedAmount&cu=INR&tn=$encodedRef"
    }
}
