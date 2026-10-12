package com.packready.shopkeeper.domain.usecase

import android.graphics.Bitmap
import com.packready.shopkeeper.domain.model.SlipReconciliation
import com.packready.shopkeeper.domain.repository.OcrRepository
import javax.inject.Inject

class ProcessHandwrittenSlipUseCase @Inject constructor(
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(bitmap: Bitmap, apiKey: String): Result<SlipReconciliation> {
        return ocrRepository.parseHandwrittenSlip(bitmap, apiKey)
    }
}
