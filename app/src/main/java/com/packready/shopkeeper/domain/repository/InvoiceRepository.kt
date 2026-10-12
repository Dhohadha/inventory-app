package com.packready.shopkeeper.domain.repository

import com.packready.shopkeeper.domain.model.InvoiceDocument
import com.packready.shopkeeper.domain.model.InvoiceLineItem
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    fun getAllInvoices(): Flow<List<InvoiceDocument>>
    suspend fun getInvoiceById(id: String): InvoiceDocument?
    suspend fun saveInvoice(invoice: InvoiceDocument)
    suspend fun updateLineItem(item: InvoiceLineItem, invoiceId: String)
    suspend fun reconcileInvoiceAndApplyStock(invoiceId: String)
    suspend fun deleteInvoice(id: String)
}
