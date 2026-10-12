package com.packready.shopkeeper.domain.usecase

import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.model.Cart
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.repository.ProductRepository
import javax.inject.Inject

sealed class ScanResult {
    data class Success(val product: Product, val updatedCart: Cart) : ScanResult()
    data class NotFound(val rawBarcode: String) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

class ProcessScanUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val soundPoolManager: SoundPoolManager,
    private val hapticManager: HapticManager
) {
    suspend operator fun invoke(currentCart: Cart, barcode: String): ScanResult {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) {
            return ScanResult.Error("Empty barcode")
        }

        val product = productRepository.getProductByBarcode(trimmed)
        return if (product != null) {
            // Play crisp chime & light haptic pulse
            soundPoolManager.playScanSuccess()
            hapticManager.pulseScan()
            val newCart = currentCart.addItem(product, 1.0)
            ScanResult.Success(product, newCart)
        } else {
            // Unrecognized barcode
            soundPoolManager.playErrorBuzz()
            hapticManager.pulseError()
            ScanResult.NotFound(trimmed)
        }
    }
}
