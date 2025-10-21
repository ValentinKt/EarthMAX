package com.earthmax.core.sync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.earthmax.core.utils.Logger
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
        operationType: String,
        data: String,
        priority: String = "NORMAL"
    ) {
        try {
            val change = OfflineChange(
                id = UUID.randomUUID().toString(),
                entity_type = entityType,
                entity_id = entityId,
                operation_type = operationType,
                data = data,
                priority = priority,
                timestamp = java.time.LocalDateTime.now().toString(),
                retry_count = 0,
                status = "PENDING"
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
    fun getPendingChangesByPriority(priority: String): Flow<List<OfflineChange>> {
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
            offlineChangeDao.updateStatus(changeId, "SYNCED")
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
                    status = "FAILED",
                    retry_count = change.retry_count + 1
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
            offlineChangeDao.updateStatus(changeId, "PENDING")
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
            val deletedCount = offlineChangeDao.deleteOldSyncedChanges(olderThan.toString())
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
            val pending = offlineChangeDao.countByStatus("PENDING")
            val synced = offlineChangeDao.countByStatus("SYNCED")
            val failed = offlineChangeDao.countByStatus("FAILED")
            
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
            new.operation_type == "DELETE" -> new.copy(id = existing.id)
            
            // If existing is DELETE, keep it unless new is also DELETE
            existing.operation_type == "DELETE" -> existing
            
            // If both are CREATE or UPDATE, use the newer one
            existing.operation_type == new.operation_type -> new.copy(id = existing.id)
            
            // If existing is CREATE and new is UPDATE, merge them as CREATE
            existing.operation_type == "CREATE" && new.operation_type == "UPDATE" -> {
                new.copy(
                    id = existing.id,
                    operation_type = "CREATE",
                    data = new.data // Use newer data
                )
            }
            
            // Default: use the newer operation
            else -> new.copy(id = existing.id)
        }
    }
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
    fun getPendingChangesByPriority(priority: String): Flow<List<OfflineChange>>
    
    @Query("SELECT * FROM offline_changes WHERE status = 'FAILED' ORDER BY timestamp ASC")
    suspend fun getFailedChanges(): List<OfflineChange>
    
    @Query("SELECT * FROM offline_changes WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun getChangesForEntity(entityType: String, entityId: String): List<OfflineChange>
    
    @Query("SELECT * FROM offline_changes WHERE entity_type = :entityType AND entity_id = :entityId AND status = 'PENDING' LIMIT 1")
    suspend fun getChangeByEntity(entityType: String, entityId: String): OfflineChange?
    
    @androidx.room.Query("SELECT * FROM offline_changes WHERE entity_type = :entityType ORDER BY timestamp ASC")
    suspend fun getChangesByEntityType(entityType: String): List<OfflineChange>
    
    @androidx.room.Query("SELECT * FROM offline_changes WHERE id = :id")
    suspend fun getById(id: String): OfflineChange?
    
    @androidx.room.Query("SELECT COUNT(*) FROM offline_changes WHERE status = :status")
    suspend fun countByStatus(status: String): Int
    
    @androidx.room.Query("UPDATE offline_changes SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
    
    @androidx.room.Query("DELETE FROM offline_changes WHERE status = 'SYNCED' AND timestamp < :olderThan")
    suspend fun deleteOldSyncedChanges(olderThan: String): Int
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertChange(change: OfflineChange)
    
    @androidx.room.Update
    suspend fun updateChange(change: OfflineChange)
    
    @androidx.room.Delete
    suspend fun delete(change: OfflineChange)
}