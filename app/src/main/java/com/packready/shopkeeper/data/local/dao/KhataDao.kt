package com.packready.shopkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.packready.shopkeeper.data.local.entity.KhataCustomerEntity
import com.packready.shopkeeper.data.local.entity.KhataTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {

    @Query("SELECT * FROM khata_customers ORDER BY totalOutstanding DESC, customerName ASC")
    fun getAllCustomersFlow(): Flow<List<KhataCustomerEntity>>

    @Query("SELECT * FROM khata_customers WHERE customerPhone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): KhataCustomerEntity?

    @Query("SELECT * FROM khata_customers WHERE customerName LIKE '%' || :query || '%' OR customerPhone LIKE '%' || :query || '%'")
    fun searchCustomersFlow(query: String): Flow<List<KhataCustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCustomer(customer: KhataCustomerEntity)

    @Update
    suspend fun updateCustomer(customer: KhataCustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: KhataTransactionEntity)

    @Query("SELECT * FROM khata_transactions WHERE customerPhone = :phone ORDER BY timestamp DESC")
    fun getTransactionsForCustomerFlow(phone: String): Flow<List<KhataTransactionEntity>>

    @Query("SELECT * FROM khata_transactions WHERE customerPhone = :phone ORDER BY timestamp DESC")
    suspend fun getTransactionsForCustomerSync(phone: String): List<KhataTransactionEntity>

    @Query("UPDATE khata_customers SET totalOutstanding = totalOutstanding + :amountDelta, lastTransactionAt = :timestamp WHERE customerPhone = :phone")
    suspend fun updateCustomerBalance(phone: String, amountDelta: Double, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun recordKhataEntry(
        customerName: String,
        customerPhone: String,
        amountDelta: Double,
        transaction: KhataTransactionEntity
    ) {
        val existing = getCustomerByPhone(customerPhone)
        if (existing == null) {
            val newCustomer = KhataCustomerEntity(
                customerPhone = customerPhone,
                customerName = customerName,
                totalOutstanding = amountDelta,
                lastTransactionAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            insertOrUpdateCustomer(newCustomer)
        } else {
            updateCustomerBalance(customerPhone, amountDelta, System.currentTimeMillis())
        }
        insertTransaction(transaction)
    }

    @Query("SELECT SUM(totalOutstanding) FROM khata_customers WHERE totalOutstanding > 0")
    suspend fun getTotalOutstandingDues(): Double?
}
