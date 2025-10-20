package com.earthmax.core.monitoring

import com.earthmax.core.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring utility for tracking application metrics
 */
@Singleton
class PerformanceMonitor @Inject constructor() {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val SLOW_OPERATION_THRESHOLD_MS = 1000L
        private const val VERY_SLOW_OPERATION_THRESHOLD_MS = 5000L
    }

    private val operationTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val operationCounts = ConcurrentHashMap<String, AtomicLong>()
    private val activeOperations = ConcurrentHashMap<String, Long>()

    /**
     * Start tracking an operation
     */
    fun startOperation(operationName: String): String {
        val operationId = "${operationName}_${System.currentTimeMillis()}_${Thread.currentThread().id}"
        activeOperations[operationId] = System.currentTimeMillis()
        
        Logger.d(TAG, "Started operation: $operationName (ID: $operationId)")
        return operationId
    }

    /**
     * End tracking an operation and log performance metrics
     */
    fun endOperation(operationId: String, operationName: String? = null) {
        val startTime = activeOperations.remove(operationId)
        if (startTime == null) {
            Logger.w(TAG, "Operation not found: $operationId")
            return
        }

        val duration = System.currentTimeMillis() - startTime
        val name = operationName ?: operationId.substringBefore("_")

        // Record the operation time
        operationTimes.computeIfAbsent(name) { mutableListOf() }.add(duration)
        operationCounts.computeIfAbsent(name) { AtomicLong(0) }.incrementAndGet()

        // Log based on performance thresholds
        when {
            duration > VERY_SLOW_OPERATION_THRESHOLD_MS -> {
                Logger.w(TAG, "Very slow operation: $name took ${duration}ms")
            }
            duration > SLOW_OPERATION_THRESHOLD_MS -> {
                Logger.i(TAG, "Slow operation: $name took ${duration}ms")
            }
            else -> {
                Logger.d(TAG, "Operation completed: $name took ${duration}ms")
            }
        }

        // Log performance metrics periodically
        val count = operationCounts[name]?.get() ?: 0
        if (count % 10 == 0L) { // Every 10 operations
            logOperationStats(name)
        }
    }

    /**
     * Execute a block of code with performance monitoring
     */
    inline fun <T> measureOperation(operationName: String, block: () -> T): T {
        val operationId = startOperation(operationName)
        return try {
            block()
        } finally {
            endOperation(operationId, operationName)
        }
    }

    /**
     * Execute a suspend block of code with performance monitoring
     */
    suspend inline fun <T> measureSuspendOperation(operationName: String, crossinline block: suspend () -> T): T {
        val operationId = startOperation(operationName)
        return try {
            block()
        } finally {
            endOperation(operationId, operationName)
        }
    }

    /**
     * Get performance statistics for an operation
     */
    fun getOperationStats(operationName: String): OperationStats? {
        val times = operationTimes[operationName] ?: return null
        val count = operationCounts[operationName]?.get() ?: 0

        if (times.isEmpty()) return null

        val sortedTimes = times.sorted()
        return OperationStats(
            operationName = operationName,
            totalCount = count,
            averageTime = times.average(),
            minTime = sortedTimes.first(),
            maxTime = sortedTimes.last(),
            medianTime = if (sortedTimes.size % 2 == 0) {
                (sortedTimes[sortedTimes.size / 2 - 1] + sortedTimes[sortedTimes.size / 2]) / 2.0
            } else {
                sortedTimes[sortedTimes.size / 2].toDouble()
            },
            p95Time = sortedTimes[(sortedTimes.size * 0.95).toInt().coerceAtMost(sortedTimes.size - 1)],
            p99Time = sortedTimes[(sortedTimes.size * 0.99).toInt().coerceAtMost(sortedTimes.size - 1)]
        )
    }

    /**
     * Log statistics for an operation
     */
    private fun logOperationStats(operationName: String) {
        val stats = getOperationStats(operationName) ?: return
        
        Logger.i(TAG, "Performance stats for $operationName: " +
                "count=${stats.totalCount}, " +
                "avg=${String.format("%.2f", stats.averageTime)}ms, " +
                "min=${stats.minTime}ms, " +
                "max=${stats.maxTime}ms, " +
                "median=${String.format("%.2f", stats.medianTime)}ms, " +
                "p95=${stats.p95Time}ms, " +
                "p99=${stats.p99Time}ms")
    }

    /**
     * Get all operation statistics
     */
    fun getAllStats(): Map<String, OperationStats> {
        return operationTimes.keys.mapNotNull { operationName ->
            getOperationStats(operationName)?.let { operationName to it }
        }.toMap()
    }

    /**
     * Clear all performance data
     */
    fun clearStats() {
        operationTimes.clear()
        operationCounts.clear()
        activeOperations.clear()
        Logger.i(TAG, "Performance statistics cleared")
    }

    /**
     * Log a summary of all performance statistics
     */
    fun logSummary() {
        CoroutineScope(Dispatchers.IO).launch {
            Logger.i(TAG, "=== Performance Summary ===")
            getAllStats().forEach { (name, stats) ->
                logOperationStats(name)
            }
            Logger.i(TAG, "=== End Performance Summary ===")
        }
    }

    /**
     * Data class representing operation performance statistics
     */
    data class OperationStats(
        val operationName: String,
        val totalCount: Long,
        val averageTime: Double,
        val minTime: Long,
        val maxTime: Long,
        val medianTime: Double,
        val p95Time: Long,
        val p99Time: Long
    )
}