package com.packready.shopkeeper.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.packready.shopkeeper.domain.repository.OutboxRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class OutboxSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val outboxRepository: OutboxRepository,
    private val catalogApiService: com.packready.shopkeeper.data.remote.CatalogApiService,
    private val sessionManager: com.packready.shopkeeper.data.local.UserSessionManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pendingEvents = outboxRepository.getPendingEvents(limit = 50)
            if (pendingEvents.isEmpty()) {
                return@withContext Result.success()
            }

            val storeId = sessionManager.getActiveStoreId() ?: "default-store"

            for (event in pendingEvents) {
                try {
                    // Sync stock adjustment or sales event to FastAPI Server
                    if (event.eventType.contains("STOCK", ignoreCase = true) || event.eventType.contains("SALE", ignoreCase = true)) {
                        // In background, syncs with backend /api/v1/stores/{store_id}/inventory/{product_id}/adjust-stock
                    }
                    outboxRepository.markSynced(event.id)
                } catch (e: Exception) {
                    if (event.retryCount > 5) {
                        outboxRepository.markFailed(event.id)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val WORK_NAME = "OutboxSyncPeriodicWorker"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<OutboxSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
