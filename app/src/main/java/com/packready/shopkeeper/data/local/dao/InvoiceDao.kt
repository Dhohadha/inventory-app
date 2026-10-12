package com.packready.shopkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.packready.shopkeeper.data.local.entity.InvoiceDocumentEntity
import com.packready.shopkeeper.data.local.entity.InvoiceLineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<InvoiceLineItemEntity>)

    @Transaction
    suspend fun saveFullInvoice(invoice: InvoiceDocumentEntity, items: List<InvoiceLineItemEntity>) {
        insertInvoice(invoice)
        insertLineItems(items)
    }

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoicesFlow(): Flow<List<InvoiceDocumentEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: String): InvoiceDocumentEntity?

    @Query("SELECT * FROM invoice_line_items WHERE invoiceId = :invoiceId")
    suspend fun getLineItemsForInvoice(invoiceId: String): List<InvoiceLineItemEntity>

    @Query("SELECT * FROM invoice_line_items WHERE invoiceId = :invoiceId")
    fun getLineItemsForInvoiceFlow(invoiceId: String): Flow<List<InvoiceLineItemEntity>>

    @Update
    suspend fun updateLineItem(item: InvoiceLineItemEntity)

    @Query("UPDATE invoices SET status = :status WHERE id = :id")
    suspend fun updateInvoiceStatus(id: String, status: String)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: String)
}
