package com.packready.shopkeeper.domain.usecase

import android.graphics.Bitmap
import com.packready.shopkeeper.domain.model.InvoiceDocument
import com.packready.shopkeeper.domain.repository.InvoiceRepository
import com.packready.shopkeeper.domain.repository.OcrRepository
import javax.inject.Inject

class ProcessInvoiceOcrUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(bitmap: Bitmap, apiKey: String): Result<InvoiceDocument> {
        val result = ocrRepository.parseDistributorInvoice(bitmap, apiKey)
        if (result.isSuccess) {
            val doc = result.getOrNull()
            if (doc != null) {
                invoiceRepository.saveInvoice(doc)
            }
        }
        return result
    }
}
