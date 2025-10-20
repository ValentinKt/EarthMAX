package com.earthmax.data.repository

import com.earthmax.core.monitoring.LogFilterManager
import com.earthmax.core.monitoring.PerformanceMetricsCollector
import com.earthmax.core.utils.Logger
import com.earthmax.data.local.dao.PerformanceDao
import com.earthmax.data.local.entity.LogEntryEntity
import com.earthmax.data.local.entity.PerformanceMetricEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing performance data persistence and retrieval.
 * Handles storing logs and metrics to local database for historical analysis.
 */
@Singleton
class PerformanceRepository @Inject constructor(
    private val performanceDao: PerformanceDao
) {
    
    /**
     * Store current performance metrics to database
     */
    suspend fun storeCurrentMetrics() {
        Logger.enter("PerformanceRepository", "storeCurrentMetrics")
        
        try {
            val performanceMetrics = Logger.getPerformanceMetrics()
            val logEntries = Logger.getLogEntries()
            
            // Convert and store performance metrics
            val metricEntities = performanceMetrics.map { metric ->
                PerformanceMetricEntity(
                    id = 0, // Auto-generated
                    operation = metric.operation,
                    tag = metric.tag,
                    duration = metric.duration.toDouble(),
                    timestamp = metric.timestamp,
                    metadata = metric.additionalMetrics.entries.joinToString(";") { "${it.key}=${it.value}" }
                )
            }
            
            // Convert and store log entries
            val logEntities = logEntries.map { log ->
                LogEntryEntity(
                    id = 0, // Auto-generated
                    level = log.level.name,
                    tag = log.tag,
                    message = log.message,
                    timestamp = log.timestamp,
                    exception = log.throwable?.toString(),
                    metadata = "" // LogEntry doesn't have metadata property
                )
            }
            
            performanceDao.insertMetrics(metricEntities)
            performanceDao.insertLogs(logEntities)
            
            Logger.logBusinessEvent(
                "PerformanceRepository",
                "performance_data_stored",
                mapOf(
                    "metrics_count" to metricEntities.size,
                    "logs_count" to logEntities.size
                )
            )
            
        } catch (e: Exception) {
            Logger.logError("PerformanceRepository", "Failed to store performance metrics", e, mapOf(
                "operation" to "storeCurrentMetrics"
            ))
        } finally {
            Logger.exit("PerformanceRepository", "storeCurrentMetrics")
        }
    }
    
    /**
     * Get performance metrics for a specific time range
     */
    suspend fun getMetricsForTimeRange(startTime: Long, endTime: Long): List<Logger.PerformanceMetric> {
        Logger.enter("PerformanceRepository", "getMetricsForTimeRange", 
            "startTime" to startTime,
            "endTime" to endTime
        )
        
        return try {
            val entities = performanceDao.getMetricsInTimeRange(startTime, endTime)
            entities.map { entity ->
                Logger.PerformanceMetric(
                    operation = entity.operation,
                    tag = entity.tag,
                    duration = entity.duration.toLong(),
                    timestamp = entity.timestamp,
                    additionalMetrics = parseMetadata(entity.metadata)
                )
            }.also {
                Logger.logBusinessEvent(
                    "PerformanceRepository",
                    "metrics_retrieved",
                    mapOf(
                        "count" to it.size,
                        "time_range_hours" to ((endTime - startTime) / (1000 * 60 * 60))
                    )
                )
            }
        } catch (e: Exception) {
            Logger.logError("PerformanceRepository", "Failed to retrieve metrics for time range", e, mapOf(
                "startTime" to startTime,
                "endTime" to endTime
            ))
            emptyList()
        } finally {
            Logger.exit("PerformanceRepository", "getMetricsForTimeRange")
        }
    }
    
    /**
     * Get log entries for a specific time range
     */
    suspend fun getLogsForTimeRange(startTime: Long, endTime: Long): List<Logger.LogEntry> {
        Logger.enter("PerformanceRepository", "getLogsForTimeRange", 
            "startTime" to startTime,
            "endTime" to endTime
        )
        
        return try {
            val entities = performanceDao.getLogsInTimeRange(startTime, endTime)
            entities.map { entity ->
                Logger.LogEntry(
                    level = Logger.Level.valueOf(entity.level),
                    tag = entity.tag,
                    message = entity.message,
                    timestamp = entity.timestamp,
                    threadName = Thread.currentThread().name,
                    throwable = entity.exception?.let { Exception(it) }
                )
            }.also {
                Logger.logBusinessEvent(
                    "PerformanceRepository",
                    "logs_retrieved",
                    mapOf(
                        "count" to it.size,
                        "time_range_hours" to ((endTime - startTime) / (1000 * 60 * 60))
                    )
                )
            }
        } catch (e: Exception) {
            Logger.logError("PerformanceRepository", "Failed to retrieve logs for time range", e, mapOf(
                "startTime" to startTime,
                "endTime" to endTime
            ))
            emptyList()
        } finally {
            Logger.exit("PerformanceRepository", "getLogsForTimeRange")
        }
    }
    
    /**
     * Get performance metrics by operation
     */
    suspend fun getMetricsByOperation(operation: String): List<Logger.PerformanceMetric> {
        Logger.enter("PerformanceRepository", "getMetricsByOperation", 
            "operation" to operation
        )
        
        return try {
            val entities = performanceDao.getMetricsByOperation(operation)
            entities.map { entity ->
                Logger.PerformanceMetric(
                    operation = entity.operation,
                    tag = entity.tag,
                    duration = entity.duration.toLong(),
                    timestamp = entity.timestamp,
                    additionalMetrics = parseMetadata(entity.metadata)
                )
            }.also {
                Logger.logBusinessEvent(
                    "PerformanceRepository",
                    "operation_metrics_retrieved",
                    mapOf(
                        "operation" to operation,
                        "count" to it.size
                    )
                )
            }
        } catch (e: Exception) {
            Logger.logError("PerformanceRepository", "Failed to retrieve metrics by operation", e, mapOf(
                "operation" to operation
            ))
            emptyList()
        } finally {
            Logger.exit("PerformanceRepository", "getMetricsByOperation")
        }
    }
    
    /**
     * Get log entries by level
     */
    suspend fun getLogsByLevel(level: Logger.Level): List<Logger.LogEntry> {
        Logger.enter("PerformanceRepository", "getLogsByLevel", 
            "level" to level.name
        )
        
        return try {
            val entities = performanceDao.getLogsByLevel(level.name)
            entities.map { entity ->
                Logger.LogEntry(
                    level = Logger.Level.valueOf(entity.level),
                    tag = entity.tag,
                    message = entity.message,
                    timestamp = entity.timestamp,
                    threadName = Thread.currentThread().name,
                    throwable = entity.exception?.let { Exception(it) }
                )
            }.also {
                Logger.logBusinessEvent(
                    "PerformanceRepository",
                    "level_logs_retrieved",
                    mapOf(
                        "level" to level.name,
                        "count" to it.size
                    )
                )
            }
        } catch (e: Exception) {
            Logger.logError("PerformanceRepository", "Failed to retrieve logs by level", e, mapOf(
                "level" to level.name
            ))
            emptyList()
        } finally {
            Logger.exit("PerformanceRepository", "getLogsByLevel")
        }
    }
    
    /**
     * Get aggregated performance statistics
     */
    suspend fun getPerformanceStatistics(startTime: Long, endTime: Long): PerformanceStatistics {
        Logger.enter("PerformanceRepository", "getPerformanceStatistics",
            "startTime" to startTime,
            "endTime" to endTime
        )
        
        return try {
            val metrics = performanceDao.getMetricsInTimeRange(startTime, endTime)
            val logs = performanceDao.getLogsInTimeRange(startTime, endTime)
            
            val avgResponseTime = metrics.map { it.duration }.average().takeIf { !it.isNaN() } ?: 0.0
            val errorCount = logs.count { it.level == Logger.Level.ERROR.name }
            val warningCount = logs.count { it.level == Logger.Level.WARNING.name }
            val slowOperations = metrics.filter { it.duration > 2000 }
            
            val operationStats = metrics.groupBy { it.operation }
                .mapValues { (_, operationMetrics) ->
                    OperationStatistics(
                        operation = operationMetrics.first().operation,
                        totalCalls = operationMetrics.size,
                        averageDuration = operationMetrics.map { it.duration }.average(),
                        minDuration = operationMetrics.minOf { it.duration },
                        maxDuration = operationMetrics.maxOf { it.duration },
                        errorCount = logs.count { log -> 
                            log.level == Logger.Level.ERROR.name && 
                            log.message.contains(operationMetrics.first().operation, ignoreCase = true)
                        }
                    )
                }
            
            PerformanceStatistics(
                timeRange = TimeRange(startTime, endTime),
                totalMetrics = metrics.size,
                totalLogs = logs.size,
                averageResponseTime = avgResponseTime,
                errorCount = errorCount,
                warningCount = warningCount,
                slowOperationsCount = slowOperations.size,
                operationStatistics = operationStats.values.toList(),
                hourlyDistribution = getHourlyDistribution(metrics, startTime, endTime)
            ).also { stats ->
                Logger.logBusinessEvent(
                    "PerformanceRepository",
                    "performance_statistics_calculated",
                    mapOf(
                        "total_metrics" to stats.totalMetrics,
                        "total_logs" to stats.totalLogs,
                        "avg_response_time" to stats.averageResponseTime,
                        "error_count" to stats.errorCount
                    )
                )
            }
        } catch (e: Exception) {
            Logger.logError("PerformanceRepository", "Failed to calculate performance statistics", e, mapOf(
                "startTime" to startTime,
                "endTime" to endTime
            ))
            PerformanceStatistics(
                timeRange = TimeRange(startTime, endTime),
                totalMetrics = 0,
                totalLogs = 0,
                averageResponseTime = 0.0,
                errorCount = 0,
                warningCount = 0,
                slowOperationsCount = 0,
                operationStatistics = emptyList(),
                hourlyDistribution = emptyMap()
            )
        } finally {
            Logger.exit("PerformanceRepository", "getPerformanceStatistics")
        }
    }
    
    /**
     * Clean up old performance data
     */
    suspend fun cleanupOldData(olderThanDays: Int = 7) {
        Logger.enter("PerformanceRepository", "cleanupOldData", 
            "olderThanDays" to olderThanDays
        )
        
        try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            
            val deletedMetrics = performanceDao.deleteMetricsOlderThan(cutoffTime)
            val deletedLogs = performanceDao.deleteLogsOlderThan(cutoffTime)
            
            Logger.logBusinessEvent(
                "PerformanceRepository",
                "old_data_cleaned",
                mapOf(
                    "deleted_metrics" to deletedMetrics,
                    "deleted_logs" to deletedLogs,
                    "cutoff_days" to olderThanDays
                )
            )
            
        } catch (e: Exception) {
            Logger.logError("PerformanceRepository", "Failed to cleanup old data", e, mapOf(
                "olderThanDays" to olderThanDays
            ))
        } finally {
            Logger.exit("PerformanceRepository", "cleanupOldData")
        }
    }
    
    /**
     * Export performance data as JSON
     */
    suspend fun exportPerformanceData(startTime: Long, endTime: Long): String {
        Logger.enter("PerformanceRepository", "exportPerformanceData", 
            "startTime" to startTime,
            "endTime" to endTime
        )
        
        return try {
            val metrics = getMetricsForTimeRange(startTime, endTime)
            val logs = getLogsForTimeRange(startTime, endTime)
            val statistics = getPerformanceStatistics(startTime, endTime)
            
            val exportData = PerformanceExport(
                exportTimestamp = System.currentTimeMillis(),
                timeRange = TimeRange(startTime, endTime),
                statistics = statistics,
                metrics = metrics,
                logs = logs
            )
            
            // Simple JSON serialization (in a real app, use a proper JSON library)
            buildString {
                appendLine("{")
                appendLine("  \"exportTimestamp\": ${exportData.exportTimestamp},")
                appendLine("  \"timeRange\": {")
                appendLine("    \"startTime\": ${exportData.timeRange.startTime},")
                appendLine("    \"endTime\": ${exportData.timeRange.endTime}")
                appendLine("  },")
                appendLine("  \"statistics\": {")
                appendLine("    \"totalMetrics\": ${exportData.statistics.totalMetrics},")
                appendLine("    \"totalLogs\": ${exportData.statistics.totalLogs},")
                appendLine("    \"averageResponseTime\": ${exportData.statistics.averageResponseTime},")
                appendLine("    \"errorCount\": ${exportData.statistics.errorCount}")
                appendLine("  },")
                appendLine("  \"metricsCount\": ${exportData.metrics.size},")
                appendLine("  \"logsCount\": ${exportData.logs.size}")
                appendLine("}")
            }.also { json ->
                Logger.logBusinessEvent(
                    "PerformanceRepository",
                    "performance_data_exported",
                    mapOf(
                        "metrics_count" to metrics.size,
                        "logs_count" to logs.size,
                        "json_size" to json.length
                    )
                )
            }
        } catch (e: Exception) {
            Logger.logError("PerformanceRepository", "Failed to export performance data", e, mapOf(
                "startTime" to startTime,
                "endTime" to endTime
            ))
            "{\"error\": \"Failed to export data\"}"
        } finally {
            Logger.exit("PerformanceRepository", "exportPerformanceData")
        }
    }
    
    /**
     * Get real-time performance metrics flow
     */
    fun getRealtimeMetrics(metricsCollector: PerformanceMetricsCollector): Flow<PerformanceMetricsCollector.AggregatedMetrics> {
        return metricsCollector.aggregatedMetrics
    }
    
    /**
     * Get real-time system health flow
     */
    fun getRealtimeSystemHealth(metricsCollector: PerformanceMetricsCollector): Flow<PerformanceMetricsCollector.SystemHealth> {
        return metricsCollector.systemHealth
    }
    
    /**
     * Get filtered logs flow
     */
    fun getFilteredLogs(logFilterManager: LogFilterManager): Flow<List<Logger.LogEntry>> {
        return logFilterManager.getFilteredLogs()
    }
    
    /**
     * Get filter statistics flow
     */
    fun getFilterStats(logFilterManager: LogFilterManager): Flow<LogFilterManager.FilterStats> {
        return logFilterManager.filterStats
    }
    
    private fun parseMetadata(metadataString: String): Map<String, Any> {
        if (metadataString.isBlank()) return emptyMap()
        
        return try {
            metadataString.split(";")
                .mapNotNull { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun getHourlyDistribution(
        metrics: List<PerformanceMetricEntity>,
        startTime: Long,
        endTime: Long
    ): Map<String, Int> {
        val hourlyBuckets = mutableMapOf<String, Int>()
        val timeSpan = endTime - startTime
        val hours = (timeSpan / (1000 * 60 * 60)).coerceAtMost(24)
        
        for (i in 0 until hours) {
            val hourStart = startTime + (i * 60 * 60 * 1000)
            val hourEnd = hourStart + (60 * 60 * 1000)
            val hourKey = String.format("%02d:00", i)
            
            val count = metrics.count { metric ->
                metric.timestamp in hourStart..hourEnd
            }
            
            hourlyBuckets[hourKey] = count
        }
        
        return hourlyBuckets
    }
    
    data class TimeRange(
        val startTime: Long,
        val endTime: Long
    )
    
    data class OperationStatistics(
        val operation: String,
        val totalCalls: Int,
        val averageDuration: Double,
        val minDuration: Double,
        val maxDuration: Double,
        val errorCount: Int
    )
    
    data class PerformanceStatistics(
        val timeRange: TimeRange,
        val totalMetrics: Int,
        val totalLogs: Int,
        val averageResponseTime: Double,
        val errorCount: Int,
        val warningCount: Int,
        val slowOperationsCount: Int,
        val operationStatistics: List<OperationStatistics>,
        val hourlyDistribution: Map<String, Int>
    )
    
    data class PerformanceExport(
        val exportTimestamp: Long,
        val timeRange: TimeRange,
        val statistics: PerformanceStatistics,
        val metrics: List<Logger.PerformanceMetric>,
        val logs: List<Logger.LogEntry>
    )
}