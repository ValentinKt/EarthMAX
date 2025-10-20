package com.earthmax.core.sync

import androidx.room.*
import com.earthmax.core.monitoring.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks offline changes for sync operations
 */
@Singleton
class OfflineChangeTracker @Inject constructor(
    private val offlineChangeDao: OfflineChangeDao,
    private val logger: Logger
) {

    /**
     * Track a change made while offline
     */
    suspend fun trackChange(
        entityType: String,
        entityId: String,
        operationType: SyncOperationType,
        data: Map<String, Any?>,
        priority: SyncPriority = SyncPriority.NORMAL
    ) {
        try {
            val change = OfflineChange(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operationType = operationType,
                data = data,
                priority = priority,
                timestamp = LocalDateTime.now(),
                retryCount = 0,
                status = OfflineChangeStatus.PENDING
            )
            
            // Check if there's already a pending change for this entity
            val existingChange = offlineChangeDao.getChangeByEntity(entityType, entityId)
            
            if (existingChange != null) {
                // Merge or replace the existing change
                val mergedChange = mergeChanges(existingChange, change)
                offlineChangeDao.updateChange(mergedChange)
                logger.d("OfflineChangeTracker", "Updated existing change for $entityType:$entityId")
            } else {
                offlineChangeDao.insertChange(change)
                logger.d("OfflineChangeTracker", "Tracked new change for $entityType:$entityId")
            }
        } catch (e: Exception) {
            logger.e("OfflineChangeTracker", "Failed to track change for $entityType:$entityId", e)
        }
    }

    /**
     * Get all pending changes
     */
    suspend fun getPendingChanges(): List<OfflineChange> {
        return offlineChangeDao.getPendingChanges()
    }

    /**
     * Get pending changes by priority
     */
    fun getPendingChangesByPriority(priority: SyncPriority): Flow<List<OfflineChange>> {
        return offlineChangeDao.getPendingChangesByPriority(priority)
    }

    /**
     * Get changes for a specific entity
     */
    suspend fun getChangesForEntity(entityType: String, entityId: String): List<OfflineChange> {
        return offlineChangeDao.getChangesForEntity(entityType, entityId)
    }

    /**
     * Mark a change as synced
     */
    suspend fun markAsSynced(changeId: String) {
        try {
            offlineChangeDao.updateStatus(changeId, OfflineChangeStatus.SYNCED)
            logger.d("OfflineChangeTracker", "Marked change $changeId as synced")
        } catch (e: Exception) {
            logger.e("OfflineChangeTracker", "Failed to mark change $changeId as synced", e)
        }
    }

    /**
     * Mark a change as failed
     */
    suspend fun markAsFailed(changeId: String, error: String) {
        try {
            val change = offlineChangeDao.getById(changeId)
            if (change != null) {
                val updatedChange = change.copy(
                    status = OfflineChangeStatus.FAILED,
                    retryCount = change.retryCount + 1,
                    lastError = error,
                    lastAttempt = LocalDateTime.now()
                )
                offlineChangeDao.updateChange(updatedChange)
                logger.d("OfflineChangeTracker", "Marked change $changeId as failed: $error")
            }
        } catch (e: Exception) {
            logger.e("OfflineChangeTracker", "Failed to mark change $changeId as failed", e)
        }
    }

    /**
     * Retry a failed change
     */
    suspend fun retryChange(changeId: String) {
        try {
            offlineChangeDao.updateStatus(changeId, OfflineChangeStatus.PENDING)
            logger.d("OfflineChangeTracker", "Retrying change $changeId")
        } catch (e: Exception) {
            logger.e("OfflineChangeTracker", "Failed to retry change $changeId", e)
        }
    }

    /**
     * Get changes that need retry
     */
    suspend fun getFailedChanges(): List<OfflineChange> {
        return offlineChangeDao.getFailedChanges()
    }

    suspend fun getChangesByEntityType(entityType: String): List<OfflineChange> {
        return offlineChangeDao.getChangesByEntityType(entityType)
    }

    /**
     * Clean up old synced changes
     */
    suspend fun cleanupOldChanges(olderThan: LocalDateTime) {
        try {
            val deletedCount = offlineChangeDao.deleteOldSyncedChanges(olderThan)
            logger.d("OfflineChangeTracker", "Cleaned up $deletedCount old changes")
        } catch (e: Exception) {
            logger.e("OfflineChangeTracker", "Failed to cleanup old changes", e)
        }
    }

    /**
     * Get sync statistics
     */
    suspend fun getSyncStats(): OfflineChangeStats {
        return try {
            val pending = offlineChangeDao.countByStatus(OfflineChangeStatus.PENDING)
            val synced = offlineChangeDao.countByStatus(OfflineChangeStatus.SYNCED)
            val failed = offlineChangeDao.countByStatus(OfflineChangeStatus.FAILED)
            
            OfflineChangeStats(
                pendingCount = pending,
                syncedCount = synced,
                failedCount = failed,
                totalCount = pending + synced + failed
            )
        } catch (e: Exception) {
            logger.e("OfflineChangeTracker", "Failed to get sync stats", e)
            OfflineChangeStats(0, 0, 0, 0)
        }
    }

    /**
     * Merge two changes for the same entity
     */
    private fun mergeChanges(existing: OfflineChange, new: OfflineChange): OfflineChange {
        return when {
            // If new operation is DELETE, it overrides everything
            new.operationType == SyncOperationType.DELETE -> new.copy(id = existing.id)
            
            // If existing is DELETE, keep it unless new is also DELETE
            existing.operationType == SyncOperationType.DELETE -> existing
            
            // If both are CREATE or UPDATE, use the newer one
            existing.operationType == new.operationType -> new.copy(id = existing.id)
            
            // If existing is CREATE and new is UPDATE, merge them as CREATE
            existing.operationType == SyncOperationType.CREATE && new.operationType == SyncOperationType.UPDATE -> {
                new.copy(
                    id = existing.id,
                    operationType = SyncOperationType.CREATE,
                    data = new.data // Use newer data
                )
            }
            
            // Default: use the newer operation
            else -> new.copy(id = existing.id)
        }
    }
}

/**
 * Room entity for offline changes
 */
@Entity(
    tableName = "offline_changes",
    indices = [
        Index(value = ["entityType", "entityId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["priority"]),
        Index(value = ["timestamp"])
    ]
)
data class OfflineChange(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    
    @ColumnInfo(name = "operation_type")
    val operationType: SyncOperationType,
    
    @ColumnInfo(name = "data")
    val data: Map<String, Any?>,
    
    @ColumnInfo(name = "priority")
    val priority: SyncPriority,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: LocalDateTime,
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "status")
    val status: OfflineChangeStatus,
    
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    
    @ColumnInfo(name = "last_attempt")
    val lastAttempt: LocalDateTime? = null
)

/**
 * Status of offline changes
 */
enum class OfflineChangeStatus {
    PENDING,
    SYNCED,
    FAILED
}

/**
 * Statistics for offline changes
 */
data class OfflineChangeStats(
    val pendingCount: Int,
    val syncedCount: Int,
    val failedCount: Int,
    val totalCount: Int
)

/**
 * Room DAO for offline changes
 */
@Dao
interface OfflineChangeDao {
    
    @Query("SELECT * FROM offline_changes WHERE status = 'PENDING' ORDER BY priority DESC, timestamp ASC")
    suspend fun getPendingChanges(): List<OfflineChange>
    
    @Query("SELECT * FROM offline_changes WHERE status = 'PENDING' AND priority = :priority ORDER BY timestamp ASC")
    fun getPendingChangesByPriority(priority: SyncPriority): Flow<List<OfflineChange>>
    
    @Query("SELECT * FROM offline_changes WHERE status = 'FAILED' ORDER BY timestamp ASC")
    suspend fun getFailedChanges(): List<OfflineChange>
    
    @Query("SELECT * FROM offline_changes WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun getChangesForEntity(entityType: String, entityId: String): List<OfflineChange>
    
    @Query("SELECT * FROM offline_changes WHERE entity_type = :entityType AND entity_id = :entityId AND status = 'PENDING' LIMIT 1")
    suspend fun getChangeByEntity(entityType: String, entityId: String): OfflineChange?
    
    @Query("SELECT * FROM offline_changes WHERE entity_type = :entityType ORDER BY timestamp ASC")
    suspend fun getChangesByEntityType(entityType: String): List<OfflineChange>
    
    @Query("SELECT * FROM offline_changes WHERE id = :id")
    suspend fun getById(id: String): OfflineChange?
    
    @Query("SELECT COUNT(*) FROM offline_changes WHERE status = :status")
    suspend fun countByStatus(status: OfflineChangeStatus): Int
    
    @Query("UPDATE offline_changes SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: OfflineChangeStatus)
    
    @Query("DELETE FROM offline_changes WHERE status = 'SYNCED' AND timestamp < :olderThan")
    suspend fun deleteOldSyncedChanges(olderThan: LocalDateTime): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChange(change: OfflineChange)
    
    @Update
    suspend fun updateChange(change: OfflineChange)
    
    @Delete
    suspend fun delete(change: OfflineChange)
}