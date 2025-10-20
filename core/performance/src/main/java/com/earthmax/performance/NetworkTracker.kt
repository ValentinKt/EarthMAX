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
     * Extract endpoint from URL for grouping
     */
    private fun extractEndpoint(url: String): String {
        return try {
            val path = url.substringAfter("://").substringAfter("/")
            val pathParts = path.split("/")
            
            // Group similar endpoints (replace IDs with placeholders)
            pathParts.map { part ->
                when {
                    part.matches(Regex("\\d+")) -> "{id}"
                    part.matches(Regex("[a-f0-9-]{36}")) -> "{uuid}"
                    else -> part
                }
            }.joinToString("/")
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * Update overall network metrics
     */
    private fun updateNetworkMetrics() {
        val requests = requestTimes.toList()
        if (requests.isEmpty()) return
        
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
    }
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