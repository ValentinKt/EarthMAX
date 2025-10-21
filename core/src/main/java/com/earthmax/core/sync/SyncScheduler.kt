package com.earthmax.core.sync

import androidx.work.*
import com.earthmax.core.utils.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and manages sync operations using WorkManager
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val networkMonitor: NetworkMonitor,
    private val logger: Logger
) {

    companion object {
        private const val SYNC_WORK_NAME = "earth_max_sync"
        private const val PERIODIC_SYNC_WORK_NAME = "earth_max_periodic_sync"
        private const val IMMEDIATE_SYNC_WORK_NAME = "earth_max_immediate_sync"
        private const val RETRY_SYNC_WORK_NAME = "earth_max_retry_sync"
    }

    /**
     * Schedule immediate sync
     */
    fun scheduleImmediateSync(
        priority: SyncPriority = SyncPriority.NORMAL,
        requiresNetwork: Boolean = true
    ) {
        logger.d("SyncScheduler", "Scheduling immediate sync with priority: $priority")
        
        val constraints = Constraints.Builder().apply {
            if (requiresNetwork) {
                setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            }
            setRequiresBatteryNotLow(priority != SyncPriority.HIGH)
        }.build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    "priority" to priority.name,
                    "sync_type" to "immediate"
                )
            )
            .addTag(IMMEDIATE_SYNC_WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    /**
     * Schedule periodic sync
     */
    fun schedulePeriodicSync(
        interval: Duration = Duration.ofHours(1),
        flexInterval: Duration = Duration.ofMinutes(15)
    ) {
        logger.d("SyncScheduler", "Scheduling periodic sync every ${interval.toMinutes()} minutes")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            interval.toMinutes(),
            TimeUnit.MINUTES,
            flexInterval.toMinutes(),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    "priority" to SyncPriority.NORMAL.name,
                    "sync_type" to "periodic"
                )
            )
            .addTag(PERIODIC_SYNC_WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicSyncRequest
        )
    }

    /**
     * Schedule retry sync for failed operations
     */
    fun scheduleRetrySync(
        delay: Duration = Duration.ofMinutes(5),
        priority: SyncPriority = SyncPriority.LOW
    ) {
        logger.d("SyncScheduler", "Scheduling retry sync in ${delay.toMinutes()} minutes")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val retrySyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    "priority" to priority.name,
                    "sync_type" to "retry"
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(1).toMillis(),
                TimeUnit.MILLISECONDS
            )
            .addTag(RETRY_SYNC_WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            RETRY_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            retrySyncRequest
        )
    }

    /**
     * Schedule sync based on network conditions
     */
    suspend fun scheduleAdaptiveSync() {
        val isConnected = networkMonitor.isConnected.first()
        val networkType = networkMonitor.networkType.first()
        val isMetered = networkMonitor.isMetered.first()
        
        logger.d("SyncScheduler", "Scheduling adaptive sync - Connected: $isConnected, Type: $networkType, Metered: $isMetered")
        
        when {
            !isConnected -> {
                // No network, cancel all sync work
                cancelAllSync()
            }
            networkType == NetworkType.WIFI -> {
                // WiFi connection, aggressive sync
                scheduleImmediateSync(SyncPriority.HIGH)
                schedulePeriodicSync(Duration.ofMinutes(30), Duration.ofMinutes(5))
            }
            networkType == NetworkType.ETHERNET -> {
                // Ethernet connection, aggressive sync
                scheduleImmediateSync(SyncPriority.HIGH)
                schedulePeriodicSync(Duration.ofMinutes(30), Duration.ofMinutes(5))
            }
            networkType == NetworkType.CELLULAR && !isMetered -> {
                // Cellular but not metered, moderate sync
                scheduleImmediateSync(SyncPriority.NORMAL)
                schedulePeriodicSync(Duration.ofHours(2), Duration.ofMinutes(30))
            }
            isMetered -> {
                // Metered connection, conservative sync
                scheduleImmediateSync(SyncPriority.LOW)
                schedulePeriodicSync(Duration.ofHours(6), Duration.ofHours(1))
            }
            else -> {
                // Default sync schedule
                scheduleImmediateSync(SyncPriority.NORMAL)
                schedulePeriodicSync()
            }
        }
    }

    /**
     * Cancel all sync operations
     */
    fun cancelAllSync() {
        logger.d("SyncScheduler", "Cancelling all sync operations")
        
        workManager.cancelUniqueWork(IMMEDIATE_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(RETRY_SYNC_WORK_NAME)
        workManager.cancelAllWorkByTag(SYNC_WORK_NAME)
    }

    /**
     * Cancel periodic sync only
     */
    fun cancelPeriodicSync() {
        logger.d("SyncScheduler", "Cancelling periodic sync")
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
    }

    /**
     * Get sync work status
     */
    fun getSyncWorkStatus() = workManager.getWorkInfosForUniqueWorkLiveData(SYNC_WORK_NAME)

    /**
     * Check if sync is currently running
     */
    suspend fun isSyncRunning(): Boolean {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(SYNC_WORK_NAME).get()
            workInfos.any { it.state == WorkInfo.State.RUNNING }
        } catch (e: Exception) {
            logger.e("SyncScheduler", "Error checking sync status: ${e.message}")
            false
        }
    }
}

/**
 * WorkManager worker for sync operations
 */
class SyncWorker @AssistedInject constructor(
    @Assisted context: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager,
    private val logger: Logger
) : CoroutineWorker(context, workerParams) {

    @AssistedFactory
    interface Factory {
        fun create(context: android.content.Context, params: WorkerParameters): SyncWorker
    }

    override suspend fun doWork(): Result {
        return try {
            val priority = inputData.getString("priority")?.let { 
                SyncPriority.valueOf(it) 
            } ?: SyncPriority.NORMAL
            
            val syncType = inputData.getString("sync_type") ?: "unknown"
            
            logger.d("SyncWorker", "Starting sync work - Type: $syncType, Priority: $priority")
            
            // Set progress
            setProgress(workDataOf("status" to "syncing"))
            
            // Perform sync
            val success = try {
                syncManager.startSync()
                true
            } catch (e: Exception) {
                logger.e("SyncWorker", "Sync failed", e)
                false
            }
            
            if (success) {
                logger.d("SyncWorker", "Sync completed successfully")
                Result.success(workDataOf("status" to "completed"))
            } else {
                logger.w("SyncWorker", "Sync completed with some failures")
                Result.retry()
            }
        } catch (e: Exception) {
            logger.e("SyncWorker", "Sync work failed", e)
            Result.failure(workDataOf("error" to e.message))
        }
    }
}

/**
 * Factory for creating SyncWorker instances
 */
class SyncWorkerFactory @Inject constructor(
    private val syncWorkerFactory: SyncWorker.Factory
) : ChildWorkerFactory {
    
    override fun create(appContext: android.content.Context, workerParameters: WorkerParameters): ListenableWorker {
        return syncWorkerFactory.create(appContext, workerParameters)
    }
}

/**
 * Interface for child worker factories
 */
interface ChildWorkerFactory {
    fun create(appContext: android.content.Context, workerParameters: WorkerParameters): ListenableWorker
}