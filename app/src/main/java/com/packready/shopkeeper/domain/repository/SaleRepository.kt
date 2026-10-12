package com.packready.shopkeeper.domain.repository

import com.packready.shopkeeper.domain.model.DailyAnalytics
import com.packready.shopkeeper.domain.model.SaleTransaction
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    fun getAllTransactions(): Flow<List<SaleTransaction>>
    suspend fun getTransactionById(id: String): SaleTransaction?
    suspend fun recordSale(sale: SaleTransaction)
    fun getTransactionsForDate(startTime: Long, endTime: Long): Flow<List<SaleTransaction>>
    suspend fun getDailyAnalytics(dayStartTimestamp: Long, dayEndTimestamp: Long): DailyAnalytics
}
