package com.packready.shopkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.packready.shopkeeper.domain.model.InvoiceDocument
import com.packready.shopkeeper.domain.model.InvoiceLineItem
import com.packready.shopkeeper.domain.model.InvoiceStatus

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoiceNumber"]),
        Index(value = ["vendorName"]),
        Index(value = ["status"])
    ]
)
data class InvoiceDocumentEntity(
    @PrimaryKey val id: String,
    val vendorName: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val totalAmount: Double,
    val status: String = "DRAFT", // DRAFT, PROCESSED, RECONCILED
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(items: List<InvoiceLineItem>): InvoiceDocument {
        val parsedStatus = try {
            InvoiceStatus.valueOf(status)
        } catch (e: Exception) {
            InvoiceStatus.DRAFT
        }

        return InvoiceDocument(
            id = id,
            vendorName = vendorName,
            invoiceNumber = invoiceNumber,
            invoiceDate = invoiceDate,
            totalAmount = totalAmount,
            items = items,
            status = parsedStatus,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(doc: InvoiceDocument): InvoiceDocumentEntity {
            return InvoiceDocumentEntity(
                id = doc.id,
                vendorName = doc.vendorName,
                invoiceNumber = doc.invoiceNumber,
                invoiceDate = doc.invoiceDate,
                totalAmount = doc.totalAmount,
                status = doc.status.name,
                createdAt = doc.createdAt
            )
        }
    }
}

@Entity(
    tableName = "invoice_line_items",
    indices = [
        Index(value = ["invoiceId"]),
        Index(value = ["matchedProductId"])
    ]
)
data class InvoiceLineItemEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val rawExtractedText: String,
    val matchedProductId: String? = null,
    val matchedProductName: String,
    val barcode: String? = null,
    val quantity: Double,
    val unit: String = "pcs",
    val purchaseRate: Double,
    val sellingPrice: Double,
    val totalAmount: Double,
    val isConfirmed: Boolean = true
) {
    fun toDomain(): InvoiceLineItem {
        return InvoiceLineItem(
            id = id,
            rawExtractedText = rawExtractedText,
            matchedProductId = matchedProductId,
            matchedProductName = matchedProductName,
            barcode = barcode,
            quantity = quantity,
            unit = unit,
            purchaseRate = purchaseRate,
            sellingPrice = sellingPrice,
            totalAmount = totalAmount,
            isConfirmed = isConfirmed
        )
    }

    companion object {
        fun fromDomain(item: InvoiceLineItem, invId: String): InvoiceLineItemEntity {
            return InvoiceLineItemEntity(
                id = item.id,
                invoiceId = invId,
                rawExtractedText = item.rawExtractedText,
                matchedProductId = item.matchedProductId,
                matchedProductName = item.matchedProductName,
                barcode = item.barcode,
                quantity = item.quantity,
                unit = item.unit,
                purchaseRate = item.purchaseRate,
                sellingPrice = item.sellingPrice,
                totalAmount = item.totalAmount,
                isConfirmed = item.isConfirmed
            )
        }
    }
}
