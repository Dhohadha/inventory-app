package com.packready.shopkeeper.domain.repository

import kotlinx.coroutines.flow.Flow

data class OutboxEvent(
    val id: String,
    val eventType: String,
    val payloadJson: String,
    val status: String,
    val retryCount: Int,
    val createdAt: Long
)

interface OutboxRepository {
    suspend fun scheduleEvent(eventType: String, payloadJson: String)
    fun getPendingCount(): Flow<Int>
    suspend fun getPendingEvents(limit: Int = 50): List<OutboxEvent>
    suspend fun markSynced(eventId: String)
    suspend fun markFailed(eventId: String)
}
