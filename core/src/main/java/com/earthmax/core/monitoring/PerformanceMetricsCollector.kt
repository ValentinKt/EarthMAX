package com.earthmax.core.monitoring

import com.earthmax.core.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects and aggregates performance metrics from the Logger.
 * Provides real-time performance data for monitoring dashboard.
 */
@Singleton
class PerformanceMetricsCollector @Inject constructor() {
    
    data class AggregatedMetrics(
        val totalLogs: Long = 0,
        val errorCount: Long = 0,
        val warningCount: Long = 0,
        val networkRequests: Long = 0,
        val networkErrors: Long = 0,
        val userActions: Long = 0,
        val businessEvents: Long = 0,
        val averageResponseTime: Double = 0.0,
        val slowestOperations: List<Logger.PerformanceMetric> = emptyList(),
        val mostFrequentErrors: Map<String, Long> = emptyMap(),
        val performanceByTag: Map<String, Double> = emptyMap(),
        val hourlyMetrics: Map<String, Long> = emptyMap()
    )
    
    data class SystemHealth(
        val status: HealthStatus,
        val errorRate: Double,
        val averageResponseTime: Double,
        val slowOperationsCount: Int,
        val criticalIssues: List<String>
    )
    
    enum class HealthStatus {
        HEALTHY, WARNING, CRITICAL
    }
    
    private val _aggregatedMetrics = MutableStateFlow(AggregatedMetrics())
    val aggregatedMetrics: StateFlow<AggregatedMetrics> = _aggregatedMetrics.asStateFlow()
    
    private val _systemHealth = MutableStateFlow(SystemHealth(
        status = HealthStatus.HEALTHY,
        errorRate = 0.0,
        averageResponseTime = 0.0,
        slowOperationsCount = 0,
        criticalIssues = emptyList()
    ))
    val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()
    
    private val operationThresholds = mapOf(
        "network_response" to 5000L, // 5 seconds
        "database_query" to 3000L,   // 3 seconds
        "user_authentication" to 2000L, // 2 seconds
        "event_creation" to 1000L,   // 1 second
        "default" to 2000L           // 2 seconds default
    )
    
    /**
     * Collect and aggregate current metrics from Logger
     */
    fun collectMetrics() {
        val performanceMetrics = Logger.getPerformanceMetrics()
        val counters = Logger.getPerformanceCounters()
        
        val aggregated = AggregatedMetrics(
            totalLogs = counters["total_logs"] ?: 0,
            errorCount = counters["errors"] ?: 0,
            warningCount = counters["logs_warning"] ?: 0,
            networkRequests = counters["network_requests"] ?: 0,
            networkErrors = counters["network_responses_4xx"] ?: 0 + counters["network_responses_5xx"] ?: 0,
            userActions = counters["user_actions"] ?: 0,
            businessEvents = counters["business_events"] ?: 0,
            averageResponseTime = calculateAverageResponseTime(performanceMetrics),
            slowestOperations = getSlowestOperations(performanceMetrics),
            mostFrequentErrors = getMostFrequentErrors(counters),
            performanceByTag = getPerformanceByTag(performanceMetrics),
            hourlyMetrics = getHourlyMetrics(performanceMetrics)
        )
        
        _aggregatedMetrics.value = aggregated
        updateSystemHealth(aggregated, performanceMetrics)
    }
    
    /**
     * Get performance metrics for a specific time range
     */
    fun getMetricsForTimeRange(startTime: Long, endTime: Long): List<Logger.PerformanceMetric> {
        return Logger.getPerformanceMetrics().filter { metric ->
            metric.timestamp in startTime..endTime
        }
    }
    
    /**
     * Get performance metrics for a specific operation
     */
    fun getMetricsForOperation(operation: String): List<Logger.PerformanceMetric> {
        return Logger.getPerformanceMetrics().filter { metric ->
            metric.operation == operation
        }
    }
    
    /**
     * Get performance metrics for a specific tag
     */
    fun getMetricsForTag(tag: String): List<Logger.PerformanceMetric> {
        return Logger.getPerformanceMetrics().filter { metric ->
            metric.tag == tag
        }
    }
    
    /**
     * Get top performing operations (fastest)
     */
    fun getTopPerformingOperations(limit: Int = 10): List<Pair<String, Double>> {
        return Logger.getPerformanceMetrics()
            .groupBy { it.operation }
            .mapValues { (_, metrics) -> metrics.map { it.duration }.average() }
            .toList()
            .sortedBy { it.second }
            .take(limit)
    }
    
    /**
     * Get worst performing operations (slowest)
     */
    fun getWorstPerformingOperations(limit: Int = 10): List<Pair<String, Double>> {
        return Logger.getPerformanceMetrics()
            .groupBy { it.operation }
            .mapValues { (_, metrics) -> metrics.map { it.duration }.average() }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }
    
    /**
     * Clear all collected metrics
     */
    fun clearMetrics() {
        Logger.clearPerformanceMetrics()
        Logger.clearLogEntries()
        _aggregatedMetrics.value = AggregatedMetrics()
        _systemHealth.value = SystemHealth(
            status = HealthStatus.HEALTHY,
            errorRate = 0.0,
            averageResponseTime = 0.0,
            slowOperationsCount = 0,
            criticalIssues = emptyList()
        )
    }
    
    private fun calculateAverageResponseTime(metrics: List<Logger.PerformanceMetric>): Double {
        return if (metrics.isEmpty()) 0.0 else metrics.map { it.duration }.average()
    }
    
    private fun getSlowestOperations(metrics: List<Logger.PerformanceMetric>, limit: Int = 10): List<Logger.PerformanceMetric> {
        return metrics.sortedByDescending { it.duration }.take(limit)
    }
    
    private fun getMostFrequentErrors(counters: Map<String, Long>): Map<String, Long> {
        return counters.filterKeys { it.startsWith("error_") }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .toMap()
    }
    
    private fun getPerformanceByTag(metrics: List<Logger.PerformanceMetric>): Map<String, Double> {
        return metrics.groupBy { it.tag }
            .mapValues { (_, tagMetrics) -> tagMetrics.map { it.duration }.average() }
    }
    
    private fun getHourlyMetrics(metrics: List<Logger.PerformanceMetric>): Map<String, Long> {
        val now = System.currentTimeMillis()
        val hourlyBuckets = mutableMapOf<String, Long>()
        
        for (i in 0..23) {
            val hourStart = now - (i * 60 * 60 * 1000)
            val hourEnd = hourStart + (60 * 60 * 1000)
            val hourKey = String.format("%02d:00", (23 - i))
            
            val count = metrics.count { metric ->
                metric.timestamp in hourStart..hourEnd
            }.toLong()
            
            hourlyBuckets[hourKey] = count
        }
        
        return hourlyBuckets
    }
    
    private fun updateSystemHealth(metrics: AggregatedMetrics, performanceMetrics: List<Logger.PerformanceMetric>) {
        val totalRequests = metrics.totalLogs
        val errorRate = if (totalRequests > 0) {
            (metrics.errorCount.toDouble() / totalRequests.toDouble()) * 100
        } else 0.0
        
        val slowOperations = performanceMetrics.filter { metric ->
            val threshold = operationThresholds[metric.operation] ?: operationThresholds["default"]!!
            metric.duration > threshold
        }
        
        val criticalIssues = mutableListOf<String>()
        
        // Determine health status
        val status = when {
            errorRate > 10.0 -> {
                criticalIssues.add("High error rate: ${String.format("%.1f", errorRate)}%")
                HealthStatus.CRITICAL
            }
            errorRate > 5.0 -> {
                criticalIssues.add("Elevated error rate: ${String.format("%.1f", errorRate)}%")
                HealthStatus.WARNING
            }
            metrics.averageResponseTime > 3000 -> {
                criticalIssues.add("High average response time: ${String.format("%.0f", metrics.averageResponseTime)}ms")
                HealthStatus.WARNING
            }
            slowOperations.size > 10 -> {
                criticalIssues.add("Multiple slow operations detected: ${slowOperations.size}")
                HealthStatus.WARNING
            }
            else -> HealthStatus.HEALTHY
        }
        
        // Add specific critical issues
        if (metrics.networkErrors > metrics.networkRequests * 0.2) {
            criticalIssues.add("High network error rate")
        }
        
        val health = SystemHealth(
            status = status,
            errorRate = errorRate,
            averageResponseTime = metrics.averageResponseTime,
            slowOperationsCount = slowOperations.size,
            criticalIssues = criticalIssues
        )
        
        _systemHealth.value = health
    }
}