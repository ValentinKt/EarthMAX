package com.earthmax.core.sync

import com.earthmax.core.cache.AdvancedCacheManager
import com.earthmax.core.error.AdvancedErrorHandler
import com.earthmax.core.utils.Logger
import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.core.sync.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages offline-first architecture with intelligent sync capabilities
 */
@Singleton
class SyncManager @Inject constructor(
    private val cacheManager: AdvancedCacheManager,
    private val errorHandler: AdvancedErrorHandler,
    private val networkMonitor: NetworkMonitor,
    private val logger: Logger,
    private val metricsCollector: MetricsCollector
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncQueue = ConcurrentHashMap<String, SyncOperation>()
    private val conflictResolver = ConflictResolver(logger)
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _pendingOperations = MutableStateFlow<List<SyncOperation>>(emptyList())
    val pendingOperations: StateFlow<List<SyncOperation>> = _pendingOperations.asStateFlow()

    init {
        // Monitor network connectivity and trigger sync when online
        networkMonitor.isConnected
            .distinctUntilChanged()
            .filter { it } // Only when connected
            .onEach { 
                logger.d("SyncManager", "Network connected, starting sync")
                startSync()
            }
            .launchIn(scope)
    }

    /**
     * Queue an operation for sync when online
     */
    suspend fun queueOperation(operation: SyncOperation) {
        logger.d("SyncManager", "Queuing operation: ${operation.id}")
        
        syncQueue[operation.id] = operation
        updatePendingOperations()
        
        // Store in cache for persistence
        cacheManager.put<SyncOperation>(
            key = "sync_operation_${operation.id}",
            data = operation
        )
        
        metricsCollector.incrementCounter("sync_operations_queued")
        
        // Try immediate sync if online
        scope.launch {
            networkMonitor.isConnected.first { isConnected ->
                if (isConnected) {
                    startSync()
                }
                true
            }
        }
    }

    /**
     * Start synchronization process
     */
    suspend fun startSync() {
        if (_syncStatus.value == SyncStatus.SYNCING) {
            logger.d("SyncManager", "Sync already in progress")
            return
        }
        
        scope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                if (!isConnected) {
                    logger.d("SyncManager", "Cannot sync - offline")
                    return@collect
                }
                // Continue with sync process
                performActualSync()
            }
        }
    }
    
    private suspend fun performActualSync() {
        _syncStatus.value = SyncStatus.SYNCING
        logger.i("SyncManager", "Starting sync process")
        
        try {
            val operations = syncQueue.values.sortedBy { it.timestamp }
            
            for (operation in operations) {
                processSyncOperation(operation)
            }
            
            _syncStatus.value = SyncStatus.SUCCESS
            logger.i("SyncManager", "Sync completed successfully")
            metricsCollector.incrementCounter("sync_completed_success")
            
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            logger.e("SyncManager", "Sync failed", e)
            metricsCollector.incrementCounter("sync_completed_error")
            
            // Handle error using basic error handler
            errorHandler.handleError("sync_process", e)
        }
    }

    /**
     * Process individual sync operation
     */
    private suspend fun processSyncOperation(operation: SyncOperation) {
        logger.d("SyncManager", "Processing operation: ${operation.id}")
        
        try {
            when (operation.type) {
                SyncOperationType.CREATE -> handleCreateOperation(operation)
                SyncOperationType.UPDATE -> handleUpdateOperation(operation)
                SyncOperationType.DELETE -> handleDeleteOperation(operation)
            }
            
            // Remove from queue on success
            syncQueue.remove(operation.id)
            cacheManager.remove("sync_operation_${operation.id}")
            updatePendingOperations()
            
            metricsCollector.incrementCounter("sync_operation_success")
            
        } catch (e: Exception) {
            logger.e("SyncManager", "Failed to process operation ${operation.id}", e)
            
            // Handle retry logic
            val updatedOperation = operation.copy(
                retryCount = operation.retryCount + 1,
                lastError = e.message
            )
            
            if (updatedOperation.retryCount < operation.maxRetries) {
                syncQueue[operation.id] = updatedOperation
                cacheManager.put<SyncOperation>(
                    key = "sync_operation_${operation.id}",
                    data = updatedOperation
                )
            } else {
                // Max retries reached, move to failed operations
                logger.e("SyncManager", "Operation ${operation.id} failed after ${operation.maxRetries} retries")
                syncQueue.remove(operation.id)
                cacheManager.remove("sync_operation_${operation.id}")
            }
            
            updatePendingOperations()
            metricsCollector.incrementCounter("sync_operation_failed")
        }
    }

    /**
     * Handle create operations
     */
    private suspend fun handleCreateOperation(operation: SyncOperation) {
        // Check for conflicts with server data
        val serverData = fetchServerData(operation.entityType, operation.entityId)
        
        if (serverData != null) {
            // Conflict detected - entity already exists
            val resolution = conflictResolver.resolveCreateConflict(operation, serverData)
            applyConflictResolution(resolution)
        } else {
            // No conflict, proceed with creation
            createOnServer(operation)
        }
    }

    /**
     * Handle update operations
     */
    private suspend fun handleUpdateOperation(operation: SyncOperation) {
        val serverData = fetchServerData(operation.entityType, operation.entityId)
        
        if (serverData == null) {
            // Entity doesn't exist on server, convert to create
            val createOperation = operation.copy(type = SyncOperationType.CREATE)
            handleCreateOperation(createOperation)
        } else {
            // Check for conflicts
            val resolution = conflictResolver.resolveUpdateConflict(operation, serverData)
            applyConflictResolution(resolution)
        }
    }

    /**
     * Handle delete operations
     */
    private suspend fun handleDeleteOperation(operation: SyncOperation) {
        val serverData = fetchServerData(operation.entityType, operation.entityId)
        
        if (serverData != null) {
            // Entity exists, proceed with deletion
            deleteOnServer(operation)
        } else {
            // Entity already deleted or doesn't exist
            logger.d("SyncManager", "Entity ${operation.entityId} already deleted or doesn't exist")
        }
    }

    /**
     * Fetch data from server for conflict resolution
     */
    private suspend fun fetchServerData(entityType: String, entityId: String): Any? {
        return try {
            // This would be implemented based on your API structure
            when (entityType) {
                "Event" -> {
                    // Fetch event from API
                    null // Placeholder
                }
                "User" -> {
                    // Fetch user from API
                    null // Placeholder
                }
                else -> null
            }
        } catch (e: Exception) {
            logger.e("SyncManager", "Failed to fetch server data for $entityType:$entityId", e)
            null
        }
    }

    /**
     * Create entity on server
     */
    private suspend fun createOnServer(operation: SyncOperation) {
        // Implementation depends on your API structure
        logger.d("SyncManager", "Creating ${operation.entityType}:${operation.entityId} on server")
        
        // Update local cache with server response
        // TODO: Implement cache update after server response
    }

    /**
     * Update entity on server
     */
    private suspend fun updateOnServer(operation: SyncOperation) {
        logger.d("SyncManager", "Updating ${operation.entityType}:${operation.entityId} on server")
        
        // Update local cache with server response
        // TODO: Implement cache update after server response
    }

    /**
     * Delete entity on server
     */
    private suspend fun deleteOnServer(operation: SyncOperation) {
        logger.d("SyncManager", "Deleting ${operation.entityType}:${operation.entityId} on server")
        
        // Remove from local cache
        cacheManager.invalidate(com.earthmax.core.cache.InvalidationStrategy.Key("${operation.entityType}_${operation.entityId}"))
    }

    /**
     * Apply conflict resolution
     */
    private suspend fun applyConflictResolution(resolution: ConflictResolution) {
        when (resolution.strategy) {
            ConflictStrategy.USE_LOCAL -> {
                // Use local data, update server
                updateOnServer(resolution.operation)
            }
            ConflictStrategy.USE_SERVER -> {
                // Use server data, update local cache
                resolution.serverData?.let { data ->
                    cacheManager.put<Map<String, Any?>>(
                        key = "${resolution.operation.entityType}_${resolution.operation.entityId}",
                        data = data as? Map<String, Any?> ?: emptyMap()
                    )
                }
            }
            ConflictStrategy.MERGE -> {
                // Merge data and update both
                val mergedData = conflictResolver.mergeData(resolution.operation.data, resolution.serverData)
                
                // Update server with merged data
                val mergedOperation = resolution.operation.copy(data = mergedData as Map<String, Any?>)
                updateOnServer(mergedOperation)
                
                // Update local cache
                mergedData?.let { data ->
                    cacheManager.put<Map<String, Any?>>(
                        key = "${resolution.operation.entityType}_${resolution.operation.entityId}",
                        data = data as Map<String, Any?>
                    )
                }
            }
        }
    }

    /**
     * Update pending operations state
     */
    private fun updatePendingOperations() {
        _pendingOperations.value = syncQueue.values.toList()
    }

    /**
     * Clear all pending operations
     */
    suspend fun clearPendingOperations() {
        syncQueue.clear()
        
        // Clear from cache
        syncQueue.keys.forEach { operationId ->
            cacheManager.remove("sync_operation_${operationId}")
        }
        
        updatePendingOperations()
        logger.i("SyncManager", "Cleared all pending operations")
    }

    /**
     * Get sync statistics
     */
    fun getSyncStats(): SyncStats {
        return SyncStats(
            pendingOperations = syncQueue.size,
            lastSyncTime = LocalDateTime.now(), // This should be tracked
            syncStatus = _syncStatus.value,
            totalOperationsProcessed = metricsCollector.getCounter("sync_operation_success") ?: 0L,
            totalOperationsFailed = metricsCollector.getCounter("sync_operation_failed") ?: 0L
        )
    }

    fun cleanup() {
        scope.cancel()
    }
}