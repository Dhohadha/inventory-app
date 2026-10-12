package com.packready.shopkeeper.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Checkout : Screen("checkout", "Desk POS", Icons.Default.PointOfSale)
    object Restock : Screen("restock", "Inward Stock", Icons.Default.Inventory)
    object InvoiceReconciler : Screen("invoice_reconciler", "Invoice OCR", Icons.AutoMirrored.Filled.ReceiptLong)
    object LooseGoods : Screen("loose_goods", "Loose Goods", Icons.Default.QrCode2)
    object Khata : Screen("khata", "Khata Ledger", Icons.AutoMirrored.Filled.MenuBook)
    object Analytics : Screen("analytics", "Daily Reports", Icons.Default.Analytics)

    companion object {
        val bottomNavItems = listOf(
            Checkout,
            Restock,
            LooseGoods,
            Khata,
            Analytics
        )
    }
}
