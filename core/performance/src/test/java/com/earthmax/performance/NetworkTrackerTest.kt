package com.earthmax.performance

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkTrackerTest {

    private lateinit var networkTracker: NetworkTracker
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        networkTracker = NetworkTracker()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startTracking should initialize network monitoring`() {
        // When
        networkTracker.startTracking()
        
        // Then
        assertTrue("Should be tracking", networkTracker.isTracking())
    }

    @Test
    fun `stopTracking should stop network monitoring`() {
        // Given
        networkTracker.startTracking()
        
        // When
        networkTracker.stopTracking()
        
        // Then
        assertFalse("Should not be tracking", networkTracker.isTracking())
    }

    @Test
    fun `recordRequest should track successful request`() {
        // Given
        val url = "https://api.example.com/users"
        val responseTime = 250L
        val requestSize = 1024L
        val responseSize = 2048L
        
        // When
        networkTracker.recordRequest(url, responseTime, requestSize, responseSize, true)
        
        // Then
        val stats = networkTracker.getNetworkStats()
        assertEquals("Total requests should be 1", 1, stats.totalRequests)
        assertEquals("Successful requests should be 1", 1, stats.successfulRequests)
        assertEquals("Failed requests should be 0", 0, stats.failedRequests)
        assertEquals("Average response time should match", responseTime.toDouble(), stats.averageResponseTime, 1.0)
        assertEquals("Total data sent should match", requestSize, stats.totalDataSent)
        assertEquals("Total data received should match", responseSize, stats.totalDataReceived)
    }

    @Test
    fun `recordRequest should track failed request`() {
        // Given
        val url = "https://api.example.com/users"
        val responseTime = 5000L // Slow response
        val requestSize = 1024L
        val responseSize = 0L // No response data
        
        // When
        networkTracker.recordRequest(url, responseTime, requestSize, responseSize, false)
        
        // Then
        val stats = networkTracker.getNetworkStats()
        assertEquals("Total requests should be 1", 1, stats.totalRequests)
        assertEquals("Successful requests should be 0", 0, stats.successfulRequests)
        assertEquals("Failed requests should be 1", 1, stats.failedRequests)
        assertEquals("Error rate should be 100%", 100.0f, stats.errorRate, 0.1f)
    }

    @Test
    fun `recordRequest should track multiple requests and calculate averages`() {
        // Given
        val requests = listOf(
            Triple("https://api.example.com/users", 200L, true),
            Triple("https://api.example.com/posts", 300L, true),
            Triple("https://api.example.com/comments", 400L, false),
            Triple("https://api.example.com/users", 250L, true)
        )
        
        // When
        requests.forEach { (url, responseTime, success) ->
            networkTracker.recordRequest(url, responseTime, 1024L, 2048L, success)
        }
        
        // Then
        val stats = networkTracker.getNetworkStats()
        assertEquals("Total requests should be 4", 4, stats.totalRequests)
        assertEquals("Successful requests should be 3", 3, stats.successfulRequests)
        assertEquals("Failed requests should be 1", 1, stats.failedRequests)
        assertEquals("Average response time should be calculated correctly", 
            287.5, stats.averageResponseTime, 1.0) // (200+300+400+250)/4
        assertEquals("Error rate should be 25%", 25.0f, stats.errorRate, 0.1f)
    }

    @Test
    fun `getEndpointStats should return endpoint-specific statistics`() {
        // Given
        val endpoint1 = "https://api.example.com/users"
        val endpoint2 = "https://api.example.com/posts"
        
        networkTracker.recordRequest(endpoint1, 200L, 1024L, 2048L, true)
        networkTracker.recordRequest(endpoint1, 300L, 1024L, 2048L, true)
        networkTracker.recordRequest(endpoint2, 150L, 512L, 1024L, false)
        
        // When
        val endpoint1Stats = networkTracker.getEndpointStats(endpoint1)
        val endpoint2Stats = networkTracker.getEndpointStats(endpoint2)
        
        // Then
        assertNotNull("Endpoint1 stats should not be null", endpoint1Stats)
        assertNotNull("Endpoint2 stats should not be null", endpoint2Stats)
        
        endpoint1Stats?.let { stats ->
            assertEquals("Endpoint1 should have 2 requests", 2, stats.requestCount)
            assertEquals("Endpoint1 should have 2 successful requests", 2, stats.successCount)
            assertEquals("Endpoint1 should have 0 failed requests", 0, stats.failureCount)
            assertEquals("Endpoint1 average response time should be 250ms", 
                250.0, stats.averageResponseTime, 1.0)
        }
        
        endpoint2Stats?.let { stats ->
            assertEquals("Endpoint2 should have 1 request", 1, stats.requestCount)
            assertEquals("Endpoint2 should have 0 successful requests", 0, stats.successCount)
            assertEquals("Endpoint2 should have 1 failed request", 1, stats.failureCount)
        }
    }

    @Test
    fun `getSlowEndpoints should identify endpoints with high response times`() {
        // Given
        networkTracker.recordRequest("https://api.example.com/fast", 100L, 1024L, 2048L, true)
        networkTracker.recordRequest("https://api.example.com/slow", 2000L, 1024L, 2048L, true)
        networkTracker.recordRequest("https://api.example.com/very-slow", 5000L, 1024L, 2048L, true)
        
        // When
        val slowEndpoints = networkTracker.getSlowEndpoints(1000L) // Threshold: 1 second
        
        // Then
        assertEquals("Should identify 2 slow endpoints", 2, slowEndpoints.size)
        assertTrue("Should contain slow endpoint", 
            slowEndpoints.any { it.endpoint.contains("slow") && !it.endpoint.contains("very") })
        assertTrue("Should contain very slow endpoint", 
            slowEndpoints.any { it.endpoint.contains("very-slow") })
    }

    @Test
    fun `getFailedEndpoints should identify endpoints with high error rates`() {
        // Given
        // Endpoint with 50% error rate
        networkTracker.recordRequest("https://api.example.com/unreliable", 200L, 1024L, 2048L, true)
        networkTracker.recordRequest("https://api.example.com/unreliable", 200L, 1024L, 2048L, false)
        
        // Endpoint with 100% error rate
        networkTracker.recordRequest("https://api.example.com/broken", 200L, 1024L, 2048L, false)
        
        // Endpoint with 0% error rate
        networkTracker.recordRequest("https://api.example.com/reliable", 200L, 1024L, 2048L, true)
        
        // When
        val failedEndpoints = networkTracker.getFailedEndpoints(30.0f) // Threshold: 30% error rate
        
        // Then
        assertEquals("Should identify 2 failed endpoints", 2, failedEndpoints.size)
        assertTrue("Should contain unreliable endpoint", 
            failedEndpoints.any { it.endpoint.contains("unreliable") })
        assertTrue("Should contain broken endpoint", 
            failedEndpoints.any { it.endpoint.contains("broken") })
    }

    @Test
    fun `getNetworkPerformanceScore should calculate performance score correctly`() {
        // Given - Good performance scenario
        repeat(10) {
            networkTracker.recordRequest("https://api.example.com/test", 200L, 1024L, 2048L, true)
        }
        
        // When
        val score = networkTracker.getNetworkPerformanceScore()
        
        // Then
        assertTrue("Score should be high for good performance", score >= 80.0f)
        assertTrue("Score should be within valid range", score >= 0.0f && score <= 100.0f)
    }

    @Test
    fun `getNetworkPerformanceScore should handle poor performance`() {
        // Given - Poor performance scenario
        repeat(5) {
            networkTracker.recordRequest("https://api.example.com/slow", 5000L, 1024L, 2048L, false)
        }
        
        // When
        val score = networkTracker.getNetworkPerformanceScore()
        
        // Then
        assertTrue("Score should be low for poor performance", score <= 50.0f)
        assertTrue("Score should be within valid range", score >= 0.0f && score <= 100.0f)
    }

    @Test
    fun `getOptimizationRecommendations should provide relevant suggestions`() {
        // Given - Mixed performance scenario
        networkTracker.recordRequest("https://api.example.com/slow", 3000L, 1024L, 2048L, true)
        networkTracker.recordRequest("https://api.example.com/failed", 1000L, 1024L, 2048L, false)
        networkTracker.recordRequest("https://api.example.com/large", 500L, 10240L, 20480L, true)
        
        // When
        val recommendations = networkTracker.getOptimizationRecommendations()
        
        // Then
        assertNotNull("Recommendations should not be null", recommendations)
        assertTrue("Should have recommendations", recommendations.isNotEmpty())
        assertTrue("Should contain performance-related recommendations", 
            recommendations.any { it.contains("response time", ignoreCase = true) ||
                                it.contains("error", ignoreCase = true) ||
                                it.contains("data", ignoreCase = true) })
    }

    @Test
    fun `extractEndpoint should correctly extract endpoint from URL`() {
        // Test cases for endpoint extraction
        val testCases = mapOf(
            "https://api.example.com/users/123" to "/users",
            "https://api.example.com/posts/456/comments" to "/posts/comments",
            "https://api.example.com/search?q=test" to "/search",
            "https://api.example.com/" to "/",
            "https://api.example.com" to "/",
            "invalid-url" to "invalid-url"
        )
        
        testCases.forEach { (url, expectedEndpoint) ->
            // When
            networkTracker.recordRequest(url, 200L, 1024L, 2048L, true)
            
            // Then
            val endpointStats = networkTracker.getEndpointStats(expectedEndpoint)
            assertNotNull("Should find stats for endpoint: $expectedEndpoint", endpointStats)
        }
    }

    @Test
    fun `reset should clear all tracking data`() {
        // Given
        networkTracker.startTracking()
        networkTracker.recordRequest("https://api.example.com/test", 200L, 1024L, 2048L, true)
        networkTracker.recordRequest("https://api.example.com/test2", 300L, 1024L, 2048L, false)
        
        // When
        networkTracker.reset()
        
        // Then
        val stats = networkTracker.getNetworkStats()
        assertEquals("Total requests should be 0", 0, stats.totalRequests)
        assertEquals("Successful requests should be 0", 0, stats.successfulRequests)
        assertEquals("Failed requests should be 0", 0, stats.failedRequests)
        assertEquals("Total data sent should be 0", 0L, stats.totalDataSent)
        assertEquals("Total data received should be 0", 0L, stats.totalDataReceived)
    }

    @Test
    fun `concurrent requests should be handled correctly`() = runTest {
        // Given
        networkTracker.startTracking()
        
        // When - Simulate concurrent requests
        repeat(100) { index ->
            networkTracker.recordRequest(
                "https://api.example.com/test$index", 
                (100..500).random().toLong(), 
                1024L, 
                2048L, 
                index % 10 != 0 // 90% success rate
            )
        }
        
        // Then
        val stats = networkTracker.getNetworkStats()
        assertEquals("Should track all requests", 100, stats.totalRequests)
        assertTrue("Should have mostly successful requests", stats.successfulRequests >= 85)
        assertTrue("Should have some failed requests", stats.failedRequests >= 5)
        assertTrue("Error rate should be around 10%", stats.errorRate >= 5.0f && stats.errorRate <= 15.0f)
    }

    @Test
    fun `edge cases should be handled gracefully`() {
        // Test with zero response time
        networkTracker.recordRequest("https://api.example.com/instant", 0L, 0L, 0L, true)
        
        // Test with very large response time
        networkTracker.recordRequest("https://api.example.com/timeout", Long.MAX_VALUE, 0L, 0L, false)
        
        // Test with empty URL
        networkTracker.recordRequest("", 200L, 1024L, 2048L, true)
        
        // Test with null-like URL
        networkTracker.recordRequest("null", 200L, 1024L, 2048L, true)
        
        // Then - Should not crash and should track all requests
        val stats = networkTracker.getNetworkStats()
        assertEquals("Should track all edge case requests", 4, stats.totalRequests)
        assertTrue("Should handle edge cases gracefully", stats.averageResponseTime >= 0)
    }

    @Test
    fun `multiple start and stop calls should not cause issues`() {
        // When
        networkTracker.startTracking()
        networkTracker.startTracking()
        networkTracker.stopTracking()
        networkTracker.stopTracking()
        networkTracker.startTracking()
        
        // Then
        assertTrue("Should be tracking after multiple calls", networkTracker.isTracking())
    }

    @Test
    fun `getNetworkStats with no data should return empty stats`() {
        // When
        val stats = networkTracker.getNetworkStats()
        
        // Then
        assertEquals("Total requests should be 0", 0, stats.totalRequests)
        assertEquals("Successful requests should be 0", 0, stats.successfulRequests)
        assertEquals("Failed requests should be 0", 0, stats.failedRequests)
        assertEquals("Average response time should be 0", 0.0, stats.averageResponseTime, 0.1)
        assertEquals("Error rate should be 0", 0.0f, stats.errorRate, 0.1f)
        assertEquals("Total data sent should be 0", 0L, stats.totalDataSent)
        assertEquals("Total data received should be 0", 0L, stats.totalDataReceived)
    }

    @Test
    fun `getEndpointStats with non-existent endpoint should return null`() {
        // When
        val stats = networkTracker.getEndpointStats("https://api.example.com/nonexistent")
        
        // Then
        assertNull("Should return null for non-existent endpoint", stats)
    }

    @Test
    fun `performance score calculation should handle division by zero`() {
        // Given - No requests recorded
        
        // When
        val score = networkTracker.getNetworkPerformanceScore()
        
        // Then
        assertTrue("Score should be valid even with no data", score >= 0.0f && score <= 100.0f)
    }
}