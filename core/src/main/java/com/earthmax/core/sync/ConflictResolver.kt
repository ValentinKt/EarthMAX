package com.earthmax.core.sync

import com.earthmax.core.utils.Logger
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles conflict resolution for sync operations
 */
@Singleton
class ConflictResolver @Inject constructor(
    private val logger: Logger
) {

    /**
     * Resolve conflicts for create operations
     */
    fun resolveCreateConflict(operation: SyncOperation, serverData: Any?): ConflictResolution {
        logger.d("ConflictResolver", "Resolving create conflict for ${operation.entityType}:${operation.entityId}")
        
        return when {
            serverData == null -> {
                // No conflict, proceed with creation
                ConflictResolution(
                    operation = operation,
                    serverData = null,
                    strategy = ConflictStrategy.USE_LOCAL
                )
            }
            isLocalDataNewer(operation.data, serverData) -> {
                // Local data is newer, use local
                ConflictResolution(
                    operation = operation,
                    serverData = serverData,
                    strategy = ConflictStrategy.USE_LOCAL
                )
            }
            else -> {
                // Server data is newer or same, use server
                ConflictResolution(
                    operation = operation,
                    serverData = serverData,
                    strategy = ConflictStrategy.USE_SERVER
                )
            }
        }
    }

    /**
     * Resolve conflicts for update operations
     */
    fun resolveUpdateConflict(operation: SyncOperation, serverData: Any?): ConflictResolution {
        logger.d("ConflictResolver", "Resolving update conflict for ${operation.entityType}:${operation.entityId}")
        
        return when {
            serverData == null -> {
                // Entity doesn't exist on server, convert to create
                ConflictResolution(
                    operation = operation.copy(type = SyncOperationType.CREATE),
                    serverData = null,
                    strategy = ConflictStrategy.USE_LOCAL
                )
            }
            hasConflictingChanges(operation.data, serverData) -> {
                // Conflicting changes detected, try to merge
                val mergedData = mergeData(operation.data, serverData)
                ConflictResolution(
                    operation = operation,
                    serverData = serverData,
                    strategy = ConflictStrategy.MERGE,
                    mergedData = mergedData
                )
            }
            isLocalDataNewer(operation.data, serverData) -> {
                // Local data is newer, use local
                ConflictResolution(
                    operation = operation,
                    serverData = serverData,
                    strategy = ConflictStrategy.USE_LOCAL
                )
            }
            else -> {
                // Server data is newer, use server
                ConflictResolution(
                    operation = operation,
                    serverData = serverData,
                    strategy = ConflictStrategy.USE_SERVER
                )
            }
        }
    }

    /**
     * Merge local and server data intelligently
     */
    fun mergeData(localData: Any?, serverData: Any?): Any? {
        if (localData == null) return serverData
        if (serverData == null) return localData
        
        return try {
            when {
                localData is Map<*, *> && serverData is Map<*, *> -> {
                    mergeMaps(localData, serverData)
                }
                else -> {
                    // For complex objects, use reflection or specific merge logic
                    mergeObjects(localData, serverData)
                }
            }
        } catch (e: Exception) {
            logger.e("ConflictResolver", "Failed to merge data", e)
            // Fallback to local data
            localData
        }
    }

    /**
     * Merge two maps, preferring newer values
     */
    private fun mergeMaps(localMap: Map<*, *>, serverMap: Map<*, *>): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        
        // Add all server data first
        serverMap.forEach { (key, value) ->
            if (key is String) {
                result[key] = value
            }
        }
        
        // Override with local data for newer or non-conflicting fields
        localMap.forEach { (key, value) ->
            if (key is String) {
                when (key) {
                    "updatedAt", "modifiedAt", "lastModified" -> {
                        // For timestamp fields, use the newer one
                        val localTime = parseTimestamp(value)
                        val serverTime = parseTimestamp(serverMap[key])
                        
                        if (localTime != null && serverTime != null) {
                            result[key] = if (localTime.isAfter(serverTime)) value else serverMap[key]
                        } else {
                            result[key] = value
                        }
                    }
                    "createdAt", "id" -> {
                        // For immutable fields, prefer server data
                        result[key] = serverMap[key] ?: value
                    }
                    else -> {
                        // For other fields, prefer local changes
                        result[key] = value
                    }
                }
            }
        }
        
        return result
    }

    /**
     * Merge complex objects using reflection or specific logic
     */
    private fun mergeObjects(localData: Any, serverData: Any): Any {
        // This would need to be implemented based on your specific data models
        // For now, return local data as fallback
        logger.d("ConflictResolver", "Using local data for object merge (not implemented)")
        return localData
    }

    /**
     * Check if local data is newer than server data
     */
    private fun isLocalDataNewer(localData: Any?, serverData: Any?): Boolean {
        if (localData == null || serverData == null) return localData != null
        
        return try {
            val localTimestamp = extractTimestamp(localData)
            val serverTimestamp = extractTimestamp(serverData)
            
            when {
                localTimestamp != null && serverTimestamp != null -> {
                    localTimestamp.isAfter(serverTimestamp)
                }
                else -> false // Default to server data if timestamps can't be compared
            }
        } catch (e: Exception) {
            logger.e("ConflictResolver", "Failed to compare timestamps", e)
            false
        }
    }

    /**
     * Check if there are conflicting changes between local and server data
     */
    private fun hasConflictingChanges(localData: Any?, serverData: Any?): Boolean {
        if (localData == null || serverData == null) return false
        
        return try {
            when {
                localData is Map<*, *> && serverData is Map<*, *> -> {
                    hasMapConflicts(localData, serverData)
                }
                else -> {
                    // For complex objects, assume conflict if they're different
                    localData != serverData
                }
            }
        } catch (e: Exception) {
            logger.e("ConflictResolver", "Failed to detect conflicts", e)
            true // Assume conflict on error
        }
    }

    /**
     * Check for conflicts in map data
     */
    private fun hasMapConflicts(localMap: Map<*, *>, serverMap: Map<*, *>): Boolean {
        val commonKeys = localMap.keys.intersect(serverMap.keys)
        
        return commonKeys.any { key ->
            val localValue = localMap[key]
            val serverValue = serverMap[key]
            
            // Skip timestamp fields for conflict detection
            when (key) {
                "updatedAt", "modifiedAt", "lastModified", "createdAt" -> false
                else -> localValue != serverValue
            }
        }
    }

    /**
     * Extract timestamp from data for comparison
     */
    private fun extractTimestamp(data: Any): LocalDateTime? {
        return try {
            when (data) {
                is Map<*, *> -> {
                    val updatedAt = data["updatedAt"] ?: data["modifiedAt"] ?: data["lastModified"]
                    parseTimestamp(updatedAt)
                }
                else -> {
                    // Use reflection to find timestamp fields
                    null // Placeholder - implement based on your data models
                }
            }
        } catch (e: Exception) {
            logger.e("ConflictResolver", "Failed to extract timestamp", e)
            null
        }
    }

    /**
     * Parse timestamp from various formats
     */
    private fun parseTimestamp(value: Any?): LocalDateTime? {
        return try {
            when (value) {
                is LocalDateTime -> value
                is String -> LocalDateTime.parse(value)
                is Long -> LocalDateTime.ofEpochSecond(value / 1000, 0, java.time.ZoneOffset.UTC)
                else -> null
            }
        } catch (e: Exception) {
            logger.e("ConflictResolver", "Failed to parse timestamp: $value", e)
            null
        }
    }
}