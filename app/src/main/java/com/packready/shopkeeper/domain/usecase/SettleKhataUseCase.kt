package com.packready.shopkeeper.domain.usecase

import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.repository.KhataRepository
import javax.inject.Inject

class SettleKhataUseCase @Inject constructor(
    private val khataRepository: KhataRepository,
    private val soundPoolManager: SoundPoolManager,
    private val hapticManager: HapticManager
) {
    suspend operator fun invoke(
        customerPhone: String,
        customerName: String,
        amount: Double,
        note: String? = null
    ) {
        khataRepository.recordPaymentReceived(
            customerPhone = customerPhone,
            customerName = customerName,
            amount = amount,
            note = note
        )
        soundPoolManager.playSaleSuccess()
        hapticManager.pulseSaleSuccess()
    }
}
