package com.packready.shopkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.packready.shopkeeper.data.local.entity.SaleItemEntity
import com.packready.shopkeeper.data.local.entity.SaleTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SaleTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Transaction
    suspend fun insertSale(transaction: SaleTransactionEntity, items: List<SaleItemEntity>) {
        insertTransaction(transaction)
        insertItems(items)
    }

    @Query("SELECT * FROM sale_transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<SaleTransactionEntity>>

    @Query("SELECT * FROM sale_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): SaleTransactionEntity?

    @Query("SELECT * FROM sale_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetweenFlow(startTime: Long, endTime: Long): Flow<List<SaleTransactionEntity>>

    @Query("SELECT * FROM sale_transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsBetweenSync(startTime: Long, endTime: Long): List<SaleTransactionEntity>

    @Query("SELECT * FROM sale_transactions WHERE customerPhone = :phone ORDER BY timestamp DESC")
    fun getTransactionsForCustomerFlow(phone: String): Flow<List<SaleTransactionEntity>>

    @Query("SELECT * FROM sale_items WHERE transactionId IN (SELECT id FROM sale_transactions WHERE timestamp BETWEEN :startTime AND :endTime)")
    suspend fun getItemsBetweenDates(startTime: Long, endTime: Long): List<SaleItemEntity>

    @Query("SELECT COUNT(*) FROM sale_transactions")
    suspend fun getTotalTransactionCount(): Int

    @Query("SELECT SUM(grandTotal) FROM sale_transactions WHERE timestamp >= :startTime")
    suspend fun getRevenueSince(startTime: Long): Double?
}
