package com.packready.shopkeeper.domain.usecase

import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.model.Cart
import com.packready.shopkeeper.domain.model.PaymentMethod
import com.packready.shopkeeper.domain.model.SaleItem
import com.packready.shopkeeper.domain.model.SaleTransaction
import com.packready.shopkeeper.domain.repository.KhataRepository
import com.packready.shopkeeper.domain.repository.SaleRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

sealed class CheckoutResult {
    data class Success(val transaction: SaleTransaction) : CheckoutResult()
    data class Error(val message: String) : CheckoutResult()
}

class ExecuteCheckoutUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val khataRepository: KhataRepository,
    private val soundPoolManager: SoundPoolManager,
    private val hapticManager: HapticManager
) {
    suspend operator fun invoke(
        cart: Cart,
        paymentMethod: PaymentMethod,
        cashReceived: Double = 0.0,
        upiTransactionRef: String? = null,
        customerName: String? = null,
        customerPhone: String? = null
    ): CheckoutResult {
        if (cart.items.isEmpty()) {
            return CheckoutResult.Error("Cart is empty")
        }

        val transactionId = UUID.randomUUID().toString()
        val invoicePrefix = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val invoiceNumber = "INV-$invoicePrefix-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}"

        val changeReturned = if (paymentMethod == PaymentMethod.CASH && cashReceived > cart.grandTotal) {
            cashReceived - cart.grandTotal
        } else 0.0

        val saleItems = cart.items.map { cartItem ->
            SaleItem(
                transactionId = transactionId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                barcode = cartItem.product.barcode,
                quantity = cartItem.quantity,
                unit = cartItem.product.unit,
                unitPrice = cartItem.effectiveUnitPrice,
                costPrice = cartItem.product.costPrice,
                discount = cartItem.discount,
                total = cartItem.total
            )
        }

        val transaction = SaleTransaction(
            id = transactionId,
            invoiceNumber = invoiceNumber,
            items = saleItems,
            subtotal = cart.subtotal,
            discountAmount = cart.totalDiscount,
            taxAmount = cart.taxAmount,
            grandTotal = cart.grandTotal,
            paymentMethod = paymentMethod,
            cashReceived = cashReceived,
            changeReturned = changeReturned,
            upiTransactionRef = upiTransactionRef,
            customerName = customerName?.ifBlank { null } ?: cart.customerName,
            customerPhone = customerPhone?.ifBlank { null } ?: cart.customerPhone,
            timestamp = System.currentTimeMillis()
        )

        try {
            // 1. Record the sale in Room DB and deduct inventory
            saleRepository.recordSale(transaction)

            // 2. If Khata credit payment, record in customer ledger
            if (paymentMethod == PaymentMethod.KHATA) {
                val phone = transaction.customerPhone ?: "Walk-in"
                val name = transaction.customerName ?: "Customer"
                khataRepository.recordCreditSale(
                    customerName = name,
                    customerPhone = phone,
                    amount = transaction.grandTotal,
                    saleId = transaction.id
                )
            }

            // 3. Audio & Haptic cash register bell feedback
            soundPoolManager.playSaleSuccess()
            hapticManager.pulseSaleSuccess()

            return CheckoutResult.Success(transaction)
        } catch (e: Exception) {
            soundPoolManager.playErrorBuzz()
            hapticManager.pulseError()
            return CheckoutResult.Error(e.message ?: "Failed to record checkout")
        }
    }
}
