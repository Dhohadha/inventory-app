package com.packready.shopkeeper.domain.repository

import com.packready.shopkeeper.domain.model.KhataCustomer
import com.packready.shopkeeper.domain.model.KhataEntryType
import com.packready.shopkeeper.domain.model.KhataTransaction
import kotlinx.coroutines.flow.Flow

interface KhataRepository {
    fun getAllCustomers(): Flow<List<KhataCustomer>>
    suspend fun getCustomer(phone: String): KhataCustomer?
    fun searchCustomers(query: String): Flow<List<KhataCustomer>>
    fun getCustomerTransactions(phone: String): Flow<List<KhataTransaction>>
    suspend fun recordCreditSale(
        customerName: String,
        customerPhone: String,
        amount: Double,
        saleId: String
    )
    suspend fun recordPaymentReceived(
        customerPhone: String,
        customerName: String,
        amount: Double,
        note: String? = null
    )
    suspend fun getTotalOutstanding(): Double
}
