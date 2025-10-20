package com.earthmax.core.sync

import java.time.LocalDateTime

/**
 * Represents a sync operation to be performed when online
 */
data class SyncOperation(
    val id: String,
    val type: SyncOperationType,
    val entityType: String,
    val entityId: String,
    val data: Any,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val lastError: String? = null,
    val priority: SyncPriority = SyncPriority.NORMAL
)

/**
 * Types of sync operations
 */
enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Priority levels for sync operations
 */
enum class SyncPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * Current sync status
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

/**
 * Conflict resolution strategies
 */
enum class ConflictStrategy {
    USE_LOCAL,      // Use local data, overwrite server
    USE_SERVER,     // Use server data, overwrite local
    MERGE          // Merge both datasets
}

/**
 * Represents a conflict resolution decision
 */
data class ConflictResolution(
    val operation: SyncOperation,
    val serverData: Any?,
    val strategy: ConflictStrategy,
    val mergedData: Any? = null
)

/**
 * Sync statistics for monitoring
 */
data class SyncStats(
    val pendingOperations: Int,
    val lastSyncTime: LocalDateTime?,
    val syncStatus: SyncStatus,
    val totalOperationsProcessed: Long,
    val totalOperationsFailed: Long
)

/**
 * Offline data change tracking
 */
data class OfflineChange(
    val id: String,
    val entityType: String,
    val entityId: String,
    val changeType: ChangeType,
    val oldValue: Any?,
    val newValue: Any?,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val userId: String? = null
)

/**
 * Types of offline changes
 */
enum class ChangeType {
    INSERT,
    UPDATE,
    DELETE
}

/**
 * Sync configuration
 */
data class SyncConfig(
    val autoSyncEnabled: Boolean = true,
    val syncInterval: Long = 300_000, // 5 minutes in milliseconds
    val maxRetries: Int = 3,
    val batchSize: Int = 50,
    val conflictResolutionStrategy: ConflictStrategy = ConflictStrategy.USE_LOCAL,
    val priorityOrder: List<String> = listOf("User", "Event", "Other")
)

/**
 * Sync event for monitoring and logging
 */
sealed class SyncEvent {
    data class Started(val operationCount: Int) : SyncEvent()
    data class OperationCompleted(val operation: SyncOperation) : SyncEvent()
    data class OperationFailed(val operation: SyncOperation, val error: Throwable) : SyncEvent()
    data class ConflictDetected(val operation: SyncOperation, val serverData: Any?) : SyncEvent()
    data class ConflictResolved(val resolution: ConflictResolution) : SyncEvent()
    object Completed : SyncEvent()
    data class Failed(val error: Throwable) : SyncEvent()
}