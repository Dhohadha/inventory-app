package com.packready.shopkeeper.domain.repository

import android.graphics.Bitmap
import com.packready.shopkeeper.domain.model.InvoiceDocument
import com.packready.shopkeeper.domain.model.SlipReconciliation

interface OcrRepository {
    suspend fun parseHandwrittenSlip(bitmap: Bitmap, apiKey: String): Result<SlipReconciliation>
    suspend fun parseDistributorInvoice(bitmap: Bitmap, apiKey: String): Result<InvoiceDocument>
}
