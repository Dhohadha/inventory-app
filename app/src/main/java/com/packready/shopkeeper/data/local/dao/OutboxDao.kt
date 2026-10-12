package com.packready.shopkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.packready.shopkeeper.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SyncOutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<SyncOutboxEntity>)

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingEventsFlow(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingEventsSync(limit: Int = 50): List<SyncOutboxEntity>

    @Query("UPDATE sync_outbox SET status = :status, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateEventStatus(id: String, status: String)

    @Update
    suspend fun updateEvent(event: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE status = 'SYNCED' AND createdAt < :timestamp")
    suspend fun purgeSyncedEventsBefore(timestamp: Long)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>
}
