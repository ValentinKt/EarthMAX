package com.earthmax.performance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks network performance metrics including response times, errors, and data usage
 */
@Singleton
class NetworkTracker @Inject constructor() {
    
    private val _networkMetrics = MutableStateFlow(NetworkMetrics())
    val networkMetrics: StateFlow<NetworkMetrics> = _networkMetrics.asStateFlow()
    
    // Network request tracking
    private val requestTimes = ConcurrentLinkedQueue<NetworkRequest>()
    private val endpointMetrics = ConcurrentHashMap<String, EndpointMetrics>()
    
    // Counters
    private val totalRequests = AtomicLong(0)
    private val successfulRequests = AtomicLong(0)
    private val failedRequests = AtomicLong(0)
    private val bytesReceived = AtomicLong(0)
    private val bytesSent = AtomicLong(0)
    
    // Configuration
    private val maxStoredRequests = 1000
    private val slowRequestThreshold = 2000L // 2 seconds

    // Tracking state
    private var tracking = false
    
    /**
     * Record a network request
     */
    fun recordRequest(
        url: String,
        method: String,
        responseTime: Long,
        responseCode: Int,
        requestSize: Long = 0,
        responseSize: Long = 0,
        error: String? = null
    ) {
        val request = NetworkRequest(
            timestamp = System.currentTimeMillis(),
            url = url,
            method = method,
            responseTime = responseTime,
            responseCode = responseCode,
            requestSize = requestSize,
            responseSize = responseSize,
            error = error,
            isSuccess = responseCode in 200..299
        )
        
        // Add to request queue
        requestTimes.offer(request)
        
        // Remove old requests to maintain size limit
        while (requestTimes.size > maxStoredRequests) {
            requestTimes.poll()
        }
        
        // Update counters
        totalRequests.incrementAndGet()
        if (request.isSuccess) {
            successfulRequests.incrementAndGet()
        } else {
            failedRequests.incrementAndGet()
        }
        
        bytesReceived.addAndGet(responseSize)
        bytesSent.addAndGet(requestSize)
        
        // Update endpoint metrics
        updateEndpointMetrics(request)
        
        // Update overall metrics
        updateNetworkMetrics()
    }

    /**
     * Compatibility overload: record request with simplified signature used in tests
     */
    fun recordRequest(
        url: String,
        responseTime: Long,
        requestSize: Long,
        responseSize: Long,
        success: Boolean
    ) {
        val responseCode = if (success) 200 else 500
        val error = if (success) null else "RequestFailed"
        recordRequest(url = url, method = "GET", responseTime = responseTime, responseCode = responseCode, requestSize = requestSize, responseSize = responseSize, error = error)
    }
    
    /**
     * Update endpoint-specific metrics
     */
    private fun updateEndpointMetrics(request: NetworkRequest) {
        val endpoint = extractEndpoint(request.url)
        val metrics = endpointMetrics.getOrPut(endpoint) { EndpointMetrics(endpoint) }
        
        metrics.totalRequests++
        if (request.isSuccess) {
            metrics.successfulRequests++
        } else {
            metrics.failedRequests++
        }
        
        metrics.totalResponseTime += request.responseTime
        metrics.averageResponseTime = metrics.totalResponseTime / metrics.totalRequests
        
        if (request.responseTime > slowRequestThreshold) {
            metrics.slowRequests++
        }
        
        metrics.totalBytesReceived += request.responseSize
        metrics.totalBytesSent += request.requestSize
        
        // Update min/max response times
        if (metrics.minResponseTime == 0L || request.responseTime < metrics.minResponseTime) {
            metrics.minResponseTime = request.responseTime
        }
        if (request.responseTime > metrics.maxResponseTime) {
            metrics.maxResponseTime = request.responseTime
        }
        
        // Track error types
        request.error?.let { error ->
            metrics.errorTypes[error] = metrics.errorTypes.getOrDefault(error, 0) + 1
        }
    }
    
    /**
     * Extract endpoint from URL for grouping (compatibility format)
     */
    private fun extractEndpoint(url: String): String {
        return try {
            val afterProtocol = url.substringAfter("://", missingDelimiterValue = url)
            val pathPart = afterProtocol.substringAfter("/", missingDelimiterValue = "")
            val noQuery = pathPart.substringBefore("?")
            if (noQuery.isBlank()) return "/"
            val parts = noQuery.split("/")
                .filter { it.isNotBlank() }
                .filterNot { part ->
                    // Drop pure numeric or UUID-like segments
                    part.matches(Regex("\\d+")) || part.matches(Regex("[a-f0-9-]{36}", RegexOption.IGNORE_CASE))
                }
            
            "/" + parts.joinToString("/")
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * Update overall network metrics
     */
    private fun updateNetworkMetrics() {
        val requests = requestTimes.toList()
        if (requests.isEmpty()) {
            _networkMetrics.value = NetworkMetrics()
            return
        }
        
        val recentRequests = requests.takeLast(100) // Last 100 requests
        val averageResponseTime = recentRequests.map { it.responseTime }.average().toLong()
        
        val metrics = NetworkMetrics(
            averageResponseTime = averageResponseTime,
            successfulRequests = successfulRequests.get().toInt(),
            failedRequests = failedRequests.get().toInt(),
            bytesReceived = bytesReceived.get(),
            bytesSent = bytesSent.get(),
            totalRequests = totalRequests.get().toInt(),
            slowRequests = recentRequests.count { it.responseTime > slowRequestThreshold },
            errorRate = if (totalRequests.get() > 0) {
                (failedRequests.get().toDouble() / totalRequests.get() * 100).toInt()
            } else 0
        )
        
        _networkMetrics.value = metrics
    }
    
    /**
     * Get current network information
     */
    fun getNetworkInfo(): NetworkMetrics {
        return _networkMetrics.value
    }

    /**
     * Get average response time (compat API)
     */
    fun getAverageResponseTime(): Long {
        val requests = requestTimes.toList()
        return if (requests.isEmpty()) 0L else requests.map { it.responseTime }.average().toLong()
    }
    
    /**
     * Get detailed network statistics
     */
    fun getDetailedStats(): NetworkStats {
        val requests = requestTimes.toList()
        if (requests.isEmpty()) {
            return NetworkStats()
        }
        
        val responseTimes = requests.map { it.responseTime }
        val successfulRequests = requests.filter { it.isSuccess }
        val failedRequests = requests.filter { !it.isSuccess }
        
        // Calculate percentiles
        val sortedTimes = responseTimes.sorted()
        val p50 = getPercentile(sortedTimes, 0.5)
        val p95 = getPercentile(sortedTimes, 0.95)
        val p99 = getPercentile(sortedTimes, 0.99)
        
        // Calculate error breakdown
        val errorBreakdown = failedRequests.groupBy { it.error ?: "Unknown" }
            .mapValues { it.value.size }
        
        // Calculate method breakdown
        val methodBreakdown = requests.groupBy { it.method }
            .mapValues { it.value.size }
        
        return NetworkStats(
            totalRequests = requests.size,
            successfulRequests = successfulRequests.size,
            failedRequests = failedRequests.size,
            averageResponseTime = responseTimes.average().toLong(),
            minResponseTime = responseTimes.minOrNull() ?: 0L,
            maxResponseTime = responseTimes.maxOrNull() ?: 0L,
            p50ResponseTime = p50,
            p95ResponseTime = p95,
            p99ResponseTime = p99,
            totalBytesReceived = bytesReceived.get(),
            totalBytesSent = bytesSent.get(),
            errorRate = if (requests.isNotEmpty()) {
                (failedRequests.size.toDouble() / requests.size * 100)
            } else 0.0,
            slowRequestsCount = requests.count { it.responseTime > slowRequestThreshold },
            errorBreakdown = errorBreakdown,
            methodBreakdown = methodBreakdown,
            endpointMetrics = endpointMetrics.values.toList()
        )
    }
    
    /**
     * Calculate percentile from sorted list
     */
    private fun getPercentile(sortedList: List<Long>, percentile: Double): Long {
        if (sortedList.isEmpty()) return 0L
        
        val index = (percentile * (sortedList.size - 1)).toInt()
        return sortedList[maxOf(0, index)]
    }
    
    /**
     * Get network performance recommendations
     */
    fun getNetworkRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val stats = getDetailedStats()
        
        // Check average response time
        if (stats.averageResponseTime > 2000) {
            recommendations.add("Average response time is high (${stats.averageResponseTime}ms). Consider optimizing API calls.")
        }
        
        // Check error rate
        if (stats.errorRate > 5.0) {
            recommendations.add("High error rate (${stats.errorRate.toInt()}%). Implement better error handling and retry logic.")
        }
        
        // Check slow requests
        if (stats.slowRequestsCount > stats.totalRequests * 0.1) {
            recommendations.add("Many slow requests detected. Consider implementing request caching.")
        }
        
        // Check data usage
        val totalDataMB = (stats.totalBytesReceived + stats.totalBytesSent) / (1024 * 1024)
        if (totalDataMB > 100) {
            recommendations.add("High data usage (${totalDataMB}MB). Consider implementing data compression.")
        }
        
        // Check endpoint performance
        endpointMetrics.values.forEach { endpoint ->
            if (endpoint.averageResponseTime > 3000) {
                recommendations.add("Endpoint '${endpoint.endpoint}' is slow (${endpoint.averageResponseTime}ms average).")
            }
            if (endpoint.failedRequests > endpoint.totalRequests * 0.1) {
                recommendations.add("Endpoint '${endpoint.endpoint}' has high failure rate.")
            }
        }
        
        return recommendations
    }

    /**
     * Compatibility alias for recommendations
     */
    fun getOptimizationRecommendations(): List<String> = getNetworkRecommendations()

    /**
     * Compute overall network performance score (0-100)
     */
    fun getNetworkPerformanceScore(): Float {
        val stats = getDetailedStats()
        if (stats.totalRequests == 0) return 50.0f
        
        // Score components
        val responseScore = when {
            stats.averageResponseTime <= 300 -> 95.0f
            stats.averageResponseTime <= 800 -> 80.0f
            stats.averageResponseTime <= 1500 -> 65.0f
            stats.averageResponseTime <= 3000 -> 50.0f
            else -> 30.0f
        }
        val errorPenalty = stats.errorRate.toFloat().coerceIn(0f, 100f) // percentage
        val slowRatio = if (stats.totalRequests > 0) stats.slowRequestsCount.toFloat() / stats.totalRequests.toFloat() else 0f
        val slowPenalty = (slowRatio * 50f).coerceIn(0f, 50f)
        
        val score = (responseScore + (100f - errorPenalty) + (100f - slowPenalty)) / 3f
        return score.coerceIn(0f, 100f)
    }

    /**
     * Get summary stats (compat API expected by tests)
     */
    fun getNetworkStats(): NetworkStatsCompat {
        val requests = requestTimes.toList()
        val avg = if (requests.isEmpty()) 0.0 else requests.map { it.responseTime }.average()
        val total = totalRequests.get().toInt()
        val success = successfulRequests.get().toInt()
        val failed = failedRequests.get().toInt()
        val errRate = if (total > 0) ((failed.toDouble() / total.toDouble()) * 100.0).toFloat() else 0.0f
        return NetworkStatsCompat(
            totalRequests = total,
            successfulRequests = success,
            failedRequests = failed,
            averageResponseTime = avg,
            errorRate = errRate,
            totalDataSent = bytesSent.get(),
            totalDataReceived = bytesReceived.get()
        )
    }

    /**
     * Endpoint stats (compat API)
     */
    fun getEndpointStats(endpointOrUrl: String): EndpointStats? {
        val key = extractEndpoint(endpointOrUrl)
        val metrics = endpointMetrics[key] ?: return null
        return EndpointStats(
            endpoint = metrics.endpoint,
            requestCount = metrics.totalRequests,
            successCount = metrics.successfulRequests,
            failureCount = metrics.failedRequests,
            averageResponseTime = metrics.averageResponseTime.toDouble()
        )
    }

    /**
     * Identify slow endpoints by average response time threshold
     */
    fun getSlowEndpoints(thresholdMs: Long): List<EndpointStats> {
        return endpointMetrics.values
            .filter { it.averageResponseTime > thresholdMs }
            .map {
                EndpointStats(
                    endpoint = it.endpoint,
                    requestCount = it.totalRequests,
                    successCount = it.successfulRequests,
                    failureCount = it.failedRequests,
                    averageResponseTime = it.averageResponseTime.toDouble()
                )
            }
    }

    /**
     * Identify failed endpoints by error rate threshold (percentage)
     */
    fun getFailedEndpoints(errorRateThresholdPercent: Float): List<EndpointStats> {
        return endpointMetrics.values
            .filter { it.totalRequests > 0 }
            .filter { (it.failedRequests.toFloat() / it.totalRequests.toFloat()) * 100f >= errorRateThresholdPercent }
            .map {
                EndpointStats(
                    endpoint = it.endpoint,
                    requestCount = it.totalRequests,
                    successCount = it.successfulRequests,
                    failureCount = it.failedRequests,
                    averageResponseTime = it.averageResponseTime.toDouble()
                )
            }
    }
    
    /**
     * Reset all network tracking data
     */
    fun reset() {
        requestTimes.clear()
        endpointMetrics.clear()
        totalRequests.set(0)
        successfulRequests.set(0)
        failedRequests.set(0)
        bytesReceived.set(0)
        bytesSent.set(0)
        _networkMetrics.value = NetworkMetrics()
        tracking = false
    }

    /**
     * Start/Stop tracking (compat APIs)
     */
    fun startTracking() { tracking = true }
    fun stopTracking() { tracking = false }
    fun isTracking(): Boolean = tracking
}

/**
 * Individual network request data
 */
data class NetworkRequest(
    val timestamp: Long,
    val url: String,
    val method: String,
    val responseTime: Long,
    val responseCode: Int,
    val requestSize: Long,
    val responseSize: Long,
    val error: String?,
    val isSuccess: Boolean
)

/**
 * Enhanced network metrics data class
 */
data class NetworkMetrics(
    val averageResponseTime: Long = 0L,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val totalRequests: Int = 0,
    val slowRequests: Int = 0,
    val errorRate: Int = 0
)

/**
 * Detailed network statistics
 */
data class NetworkStats(
    val totalRequests: Int = 0,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val averageResponseTime: Long = 0L,
    val minResponseTime: Long = 0L,
    val maxResponseTime: Long = 0L,
    val p50ResponseTime: Long = 0L,
    val p95ResponseTime: Long = 0L,
    val p99ResponseTime: Long = 0L,
    val totalBytesReceived: Long = 0L,
    val totalBytesSent: Long = 0L,
    val errorRate: Double = 0.0,
    val slowRequestsCount: Int = 0,
    val errorBreakdown: Map<String, Int> = emptyMap(),
    val methodBreakdown: Map<String, Int> = emptyMap(),
    val endpointMetrics: List<EndpointMetrics> = emptyList()
)

/**
 * Endpoint-specific metrics
 */
data class EndpointMetrics(
    val endpoint: String,
    var totalRequests: Int = 0,
    var successfulRequests: Int = 0,
    var failedRequests: Int = 0,
    var totalResponseTime: Long = 0L,
    var averageResponseTime: Long = 0L,
    var minResponseTime: Long = 0L,
    var maxResponseTime: Long = 0L,
    var slowRequests: Int = 0,
    var totalBytesReceived: Long = 0L,
    var totalBytesSent: Long = 0L,
    val errorTypes: MutableMap<String, Int> = mutableMapOf()
)

/**
 * Compatibility summary stats type used by tests
 */
data class NetworkStatsCompat(
    val totalRequests: Int,
    val successfulRequests: Int,
    val failedRequests: Int,
    val averageResponseTime: Double,
    val errorRate: Float,
    val totalDataSent: Long,
    val totalDataReceived: Long
)

/**
 * Compatibility endpoint stats type used by tests
 */
data class EndpointStats(
    val endpoint: String,
    val requestCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val averageResponseTime: Double
)