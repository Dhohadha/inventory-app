package com.packready.shopkeeper.data.repository

import com.packready.shopkeeper.data.local.dao.OutboxDao
import com.packready.shopkeeper.data.local.entity.SyncOutboxEntity
import com.packready.shopkeeper.domain.repository.OutboxEvent
import com.packready.shopkeeper.domain.repository.OutboxRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboxRepositoryImpl @Inject constructor(
    private val outboxDao: OutboxDao
) : OutboxRepository {

    override suspend fun scheduleEvent(eventType: String, payloadJson: String) {
        val entity = SyncOutboxEntity(
            id = UUID.randomUUID().toString(),
            eventType = eventType,
            payloadJson = payloadJson,
            status = "PENDING",
            retryCount = 0,
            createdAt = System.currentTimeMillis()
        )
        outboxDao.insertEvent(entity)
    }

    override fun getPendingCount(): Flow<Int> {
        return outboxDao.getPendingCountFlow()
    }

    override suspend fun getPendingEvents(limit: Int): List<OutboxEvent> {
        return outboxDao.getPendingEventsSync(limit).map { entity ->
            OutboxEvent(
                id = entity.id,
                eventType = entity.eventType,
                payloadJson = entity.payloadJson,
                status = entity.status,
                retryCount = entity.retryCount,
                createdAt = entity.createdAt
            )
        }
    }

    override suspend fun markSynced(eventId: String) {
        outboxDao.updateEventStatus(eventId, "SYNCED")
    }

    override suspend fun markFailed(eventId: String) {
        outboxDao.updateEventStatus(eventId, "FAILED")
    }
}
