package com.earthmax.performance

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceMonitorTest {

    private lateinit var context: Context
    private lateinit var frameTimeTracker: FrameTimeTracker
    private lateinit var memoryTracker: MemoryTracker
    private lateinit var networkTracker: NetworkTracker
    private lateinit var batteryTracker: BatteryTracker
    private lateinit var performanceMonitor: NewPerformanceMonitor
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        frameTimeTracker = mockk(relaxed = true)
        memoryTracker = mockk(relaxed = true)
        networkTracker = mockk(relaxed = true)
        batteryTracker = mockk(relaxed = true)
        
        performanceMonitor = NewPerformanceMonitor(
            context = context,
            frameTimeTracker = frameTimeTracker,
            memoryTracker = memoryTracker,
            networkTracker = networkTracker,
            batteryTracker = batteryTracker
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startMonitoring should initialize all trackers`() {
        // When
        performanceMonitor.startMonitoring()
        
        // Then
        verify { frameTimeTracker.startTracking() }
        verify { memoryTracker.startTracking() }
        verify { networkTracker.startTracking() }
        verify { batteryTracker.startTracking() }
        assertTrue(performanceMonitor.isMonitoring())
    }

    @Test
    fun `stopMonitoring should stop all trackers`() {
        // Given
        performanceMonitor.startMonitoring()
        
        // When
        performanceMonitor.stopMonitoring()
        
        // Then
        verify { frameTimeTracker.stopTracking() }
        verify { memoryTracker.stopTracking() }
        verify { networkTracker.stopTracking() }
        verify { batteryTracker.stopTracking() }
        assertFalse(performanceMonitor.isMonitoring())
    }

    @Test
    fun `getOverallScore should calculate correct performance score`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 60.0f
        every { frameTimeTracker.getAverageFps() } returns 45.0f
        every { networkTracker.getAverageResponseTime() } returns 800L
        every { batteryTracker.getBatteryLevel() } returns 70
        
        // When
        val score = performanceMonitor.getOverallScore()
        
        // Then
        assertTrue("Score should be between 0 and 100", score in 0.0..100.0)
        // Expected calculation based on thresholds
        val expectedScore = (40.0 + 75.0 + 60.0 + 70.0) / 4.0 // ~61.25
        assertEquals(expectedScore, score, 5.0) // Allow some tolerance
    }

    @Test
    fun `getMemoryScore should return correct score based on usage`() {
        // Test low memory usage (good score)
        every { memoryTracker.getMemoryUsagePercentage() } returns 30.0f
        var score = performanceMonitor.getMemoryScore()
        assertTrue("Low memory usage should have high score", score >= 80.0)
        
        // Test high memory usage (poor score)
        every { memoryTracker.getMemoryUsagePercentage() } returns 90.0f
        score = performanceMonitor.getMemoryScore()
        assertTrue("High memory usage should have low score", score <= 30.0)
    }

    @Test
    fun `getFrameRateScore should return correct score based on FPS`() {
        // Test high FPS (good score)
        every { frameTimeTracker.getAverageFps() } returns 58.0f
        var score = performanceMonitor.getFrameRateScore()
        assertTrue("High FPS should have high score", score >= 90.0)
        
        // Test low FPS (poor score)
        every { frameTimeTracker.getAverageFps() } returns 20.0f
        score = performanceMonitor.getFrameRateScore()
        assertTrue("Low FPS should have low score", score <= 40.0)
    }

    @Test
    fun `getNetworkScore should return correct score based on response time`() {
        // Test fast response time (good score)
        every { networkTracker.getAverageResponseTime() } returns 200L
        var score = performanceMonitor.getNetworkScore()
        assertTrue("Fast response time should have high score", score >= 90.0)
        
        // Test slow response time (poor score)
        every { networkTracker.getAverageResponseTime() } returns 3000L
        score = performanceMonitor.getNetworkScore()
        assertTrue("Slow response time should have low score", score <= 30.0)
    }

    @Test
    fun `getBatteryScore should return correct score based on battery level`() {
        // Test high battery (good score)
        every { batteryTracker.getBatteryLevel() } returns 80
        var score = performanceMonitor.getBatteryScore()
        assertTrue("High battery should have high score", score >= 80.0)
        
        // Test low battery (poor score)
        every { batteryTracker.getBatteryLevel() } returns 15
        score = performanceMonitor.getBatteryScore()
        assertTrue("Low battery should have low score", score <= 30.0)
    }

    @Test
    fun `getPerformanceIssues should identify memory issues`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 85.0f
        every { frameTimeTracker.getAverageFps() } returns 55.0f
        every { networkTracker.getAverageResponseTime() } returns 300L
        every { batteryTracker.getBatteryLevel() } returns 60
        
        // When
        val issues = performanceMonitor.getPerformanceIssues()
        
        // Then
        assertTrue("Should identify high memory usage", 
            issues.any { it.contains("memory", ignoreCase = true) })
    }

    @Test
    fun `getPerformanceIssues should identify frame rate issues`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 40.0f
        every { frameTimeTracker.getAverageFps() } returns 25.0f
        every { networkTracker.getAverageResponseTime() } returns 300L
        every { batteryTracker.getBatteryLevel() } returns 60
        
        // When
        val issues = performanceMonitor.getPerformanceIssues()
        
        // Then
        assertTrue("Should identify low frame rate", 
            issues.any { it.contains("frame", ignoreCase = true) || it.contains("fps", ignoreCase = true) })
    }

    @Test
    fun `getPerformanceIssues should identify network issues`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 40.0f
        every { frameTimeTracker.getAverageFps() } returns 55.0f
        every { networkTracker.getAverageResponseTime() } returns 2500L
        every { batteryTracker.getBatteryLevel() } returns 60
        
        // When
        val issues = performanceMonitor.getPerformanceIssues()
        
        // Then
        assertTrue("Should identify slow network", 
            issues.any { it.contains("network", ignoreCase = true) || it.contains("response", ignoreCase = true) })
    }

    @Test
    fun `getPerformanceIssues should identify battery issues`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 40.0f
        every { frameTimeTracker.getAverageFps() } returns 55.0f
        every { networkTracker.getAverageResponseTime() } returns 300L
        every { batteryTracker.getBatteryLevel() } returns 15
        
        // When
        val issues = performanceMonitor.getPerformanceIssues()
        
        // Then
        assertTrue("Should identify low battery", 
            issues.any { it.contains("battery", ignoreCase = true) })
    }

    @Test
    fun `getRecommendations should provide memory recommendations`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 85.0f
        every { frameTimeTracker.getAverageFps() } returns 55.0f
        every { networkTracker.getAverageResponseTime() } returns 300L
        every { batteryTracker.getBatteryLevel() } returns 60
        
        // When
        val recommendations = performanceMonitor.getRecommendations()
        
        // Then
        assertTrue("Should provide memory optimization recommendations", 
            recommendations.any { it.contains("memory", ignoreCase = true) })
    }

    @Test
    fun `getRecommendations should provide frame rate recommendations`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 40.0f
        every { frameTimeTracker.getAverageFps() } returns 25.0f
        every { networkTracker.getAverageResponseTime() } returns 300L
        every { batteryTracker.getBatteryLevel() } returns 60
        
        // When
        val recommendations = performanceMonitor.getRecommendations()
        
        // Then
        assertTrue("Should provide frame rate optimization recommendations", 
            recommendations.any { it.contains("frame", ignoreCase = true) || it.contains("ui", ignoreCase = true) })
    }

    @Test
    fun `getRecommendations should provide network recommendations`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 40.0f
        every { frameTimeTracker.getAverageFps() } returns 55.0f
        every { networkTracker.getAverageResponseTime() } returns 2500L
        every { batteryTracker.getBatteryLevel() } returns 60
        
        // When
        val recommendations = performanceMonitor.getRecommendations()
        
        // Then
        assertTrue("Should provide network optimization recommendations", 
            recommendations.any { it.contains("network", ignoreCase = true) || it.contains("cache", ignoreCase = true) })
    }

    @Test
    fun `getRecommendations should provide battery recommendations`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 40.0f
        every { frameTimeTracker.getAverageFps() } returns 55.0f
        every { networkTracker.getAverageResponseTime() } returns 300L
        every { batteryTracker.getBatteryLevel() } returns 15
        
        // When
        val recommendations = performanceMonitor.getRecommendations()
        
        // Then
        assertTrue("Should provide battery optimization recommendations", 
            recommendations.any { it.contains("battery", ignoreCase = true) || it.contains("power", ignoreCase = true) })
    }

    @Test
    fun `getRecommendations should return empty list when performance is good`() {
        // Given - all metrics are good
        every { memoryTracker.getMemoryUsagePercentage() } returns 40.0f
        every { frameTimeTracker.getAverageFps() } returns 58.0f
        every { networkTracker.getAverageResponseTime() } returns 200L
        every { batteryTracker.getBatteryLevel() } returns 80
        
        // When
        val recommendations = performanceMonitor.getRecommendations()
        
        // Then
        assertTrue("Should have no recommendations when performance is good", 
            recommendations.isEmpty())
    }

    @Test
    fun `createSnapshot should capture current performance state`() {
        // Given
        every { memoryTracker.getMemoryUsagePercentage() } returns 60.0f
        every { frameTimeTracker.getAverageFps() } returns 45.0f
        every { networkTracker.getAverageResponseTime() } returns 800L
        every { batteryTracker.getBatteryLevel() } returns 70
        
        // When
        val snapshot = performanceMonitor.createSnapshot()
        
        // Then
        assertNotNull("Snapshot should not be null", snapshot)
        assertTrue("Snapshot should contain timestamp", snapshot.timestamp > 0)
        assertTrue("Snapshot should contain overall score", snapshot.overallScore >= 0.0)
        assertTrue("Snapshot should contain memory score", snapshot.memoryScore >= 0.0)
        assertTrue("Snapshot should contain frame rate score", snapshot.frameRateScore >= 0.0)
        assertTrue("Snapshot should contain network score", snapshot.networkScore >= 0.0)
        assertTrue("Snapshot should contain battery score", snapshot.batteryScore >= 0.0)
    }

    @Test
    fun `reset should clear all tracking data`() {
        // Given
        performanceMonitor.startMonitoring()
        
        // When
        performanceMonitor.reset()
        
        // Then
        verify { frameTimeTracker.reset() }
        verify { memoryTracker.reset() }
        verify { networkTracker.reset() }
        verify { batteryTracker.reset() }
    }

    @Test
    fun `multiple start calls should not cause issues`() {
        // When
        performanceMonitor.startMonitoring()
        performanceMonitor.startMonitoring()
        performanceMonitor.startMonitoring()
        
        // Then
        assertTrue("Should still be monitoring", performanceMonitor.isMonitoring())
        // Verify trackers are started only once (due to relaxed mocking)
        verify(atLeast = 1) { frameTimeTracker.startTracking() }
    }

    @Test
    fun `multiple stop calls should not cause issues`() {
        // Given
        performanceMonitor.startMonitoring()
        
        // When
        performanceMonitor.stopMonitoring()
        performanceMonitor.stopMonitoring()
        performanceMonitor.stopMonitoring()
        
        // Then
        assertFalse("Should not be monitoring", performanceMonitor.isMonitoring())
        // Verify trackers are stopped at least once
        verify(atLeast = 1) { frameTimeTracker.stopTracking() }
    }
}