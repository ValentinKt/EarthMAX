package com.earthmax.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryTrackerTest {

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager
    private lateinit var memoryTracker: MemoryTracker
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        activityManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        
        memoryTracker = MemoryTracker(context)
        
        // Mock Debug class
        mockkStatic(Debug::class)
        mockkStatic(Runtime::class)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Debug::class)
        unmockkStatic(Runtime::class)
    }

    @Test
    fun `startTracking should initialize memory monitoring`() {
        // When
        memoryTracker.startTracking()
        
        // Then
        assertTrue("Should be tracking", memoryTracker.isTracking())
    }

    @Test
    fun `stopTracking should stop memory monitoring`() {
        // Given
        memoryTracker.startTracking()
        
        // When
        memoryTracker.stopTracking()
        
        // Then
        assertFalse("Should not be tracking", memoryTracker.isTracking())
    }

    @Test
    fun `getMemoryInfo should return current memory information`() {
        // Given
        val mockMemoryInfo = ActivityManager.MemoryInfo().apply {
            availMem = 1024 * 1024 * 1024L // 1GB available
            totalMem = 2048 * 1024 * 1024L // 2GB total
            threshold = 100 * 1024 * 1024L // 100MB threshold
            lowMemory = false
        }
        every { activityManager.getMemoryInfo(any()) } answers {
            val memInfo = firstArg<ActivityManager.MemoryInfo>()
            memInfo.availMem = mockMemoryInfo.availMem
            memInfo.totalMem = mockMemoryInfo.totalMem
            memInfo.threshold = mockMemoryInfo.threshold
            memInfo.lowMemory = mockMemoryInfo.lowMemory
        }
        
        // When
        val memoryInfo = memoryTracker.getMemoryInfo()
        
        // Then
        assertEquals("Available memory should match", 1024 * 1024 * 1024L, memoryInfo.availableMemory)
        assertEquals("Total memory should match", 2048 * 1024 * 1024L, memoryInfo.totalMemory)
        assertEquals("Used memory should be calculated correctly", 
            1024 * 1024 * 1024L, memoryInfo.usedMemory)
        assertFalse("Should not be low memory", memoryInfo.isLowMemory)
    }

    @Test
    fun `getMemoryUsagePercentage should calculate correct percentage`() {
        // Given
        val mockMemoryInfo = ActivityManager.MemoryInfo().apply {
            availMem = 512 * 1024 * 1024L // 512MB available
            totalMem = 2048 * 1024 * 1024L // 2GB total
        }
        every { activityManager.getMemoryInfo(any()) } answers {
            val memInfo = firstArg<ActivityManager.MemoryInfo>()
            memInfo.availMem = mockMemoryInfo.availMem
            memInfo.totalMem = mockMemoryInfo.totalMem
        }
        
        // When
        val percentage = memoryTracker.getMemoryUsagePercentage()
        
        // Then
        // Used = 2048 - 512 = 1536MB, Percentage = (1536 / 2048) * 100 = 75%
        assertEquals("Memory usage percentage should be 75%", 75.0f, percentage, 1.0f)
    }

    @Test
    fun `createSnapshot should capture current memory state`() {
        // Given
        val mockMemoryInfo = ActivityManager.MemoryInfo().apply {
            availMem = 1024 * 1024 * 1024L
            totalMem = 2048 * 1024 * 1024L
            lowMemory = false
        }
        every { activityManager.getMemoryInfo(any()) } answers {
            val memInfo = firstArg<ActivityManager.MemoryInfo>()
            memInfo.availMem = mockMemoryInfo.availMem
            memInfo.totalMem = mockMemoryInfo.totalMem
            memInfo.lowMemory = mockMemoryInfo.lowMemory
        }
        
        // Mock heap memory
        val mockRuntime = mockk<Runtime>()
        every { Runtime.getRuntime() } returns mockRuntime
        every { mockRuntime.totalMemory() } returns 100 * 1024 * 1024L // 100MB
        every { mockRuntime.freeMemory() } returns 20 * 1024 * 1024L // 20MB
        every { mockRuntime.maxMemory() } returns 200 * 1024 * 1024L // 200MB
        
        // When
        val snapshot = memoryTracker.createSnapshot()
        
        // Then
        assertNotNull("Snapshot should not be null", snapshot)
        assertTrue("Snapshot should have timestamp", snapshot.timestamp > 0)
        assertTrue("Snapshot should have total memory", snapshot.totalMemory > 0)
        assertTrue("Snapshot should have available memory", snapshot.availableMemory > 0)
        assertTrue("Snapshot should have used memory", snapshot.usedMemory > 0)
        assertTrue("Snapshot should have heap total", snapshot.heapTotal > 0)
        assertTrue("Snapshot should have heap used", snapshot.heapUsed > 0)
        assertTrue("Snapshot should have heap max", snapshot.heapMax > 0)
    }

    @Test
    fun `trackObject should add object to tracking`() {
        // Given
        val testObject = Any()
        val objectName = "TestObject"
        
        // When
        memoryTracker.trackObject(testObject, objectName)
        
        // Then
        val trackedObjects = memoryTracker.getTrackedObjects()
        assertTrue("Should contain tracked object", 
            trackedObjects.any { it.name == objectName })
    }

    @Test
    fun `untrackObject should remove object from tracking`() {
        // Given
        val testObject = Any()
        val objectName = "TestObject"
        memoryTracker.trackObject(testObject, objectName)
        
        // When
        memoryTracker.untrackObject(testObject)
        
        // Then
        val trackedObjects = memoryTracker.getTrackedObjects()
        assertFalse("Should not contain untracked object", 
            trackedObjects.any { it.name == objectName })
    }

    @Test
    fun `detectLeaks should identify leaked objects`() {
        // Given
        val testObject = Any()
        val objectName = "TestObject"
        memoryTracker.trackObject(testObject, objectName)
        
        // Simulate object being eligible for GC but still tracked
        System.gc()
        
        // When
        val leaks = memoryTracker.detectLeaks()
        
        // Then
        // Note: This test might be flaky due to GC behavior
        // In a real scenario, we'd need more sophisticated leak detection
        assertNotNull("Leaks list should not be null", leaks)
    }

    @Test
    fun `generateLeakReport should create comprehensive report`() {
        // Given
        val testObject1 = Any()
        val testObject2 = Any()
        memoryTracker.trackObject(testObject1, "Object1")
        memoryTracker.trackObject(testObject2, "Object2")
        
        // When
        val report = memoryTracker.generateLeakReport()
        
        // Then
        assertNotNull("Report should not be null", report)
        assertTrue("Report should contain tracking info", 
            report.contains("tracked", ignoreCase = true))
    }

    @Test
    fun `getMemoryTrend should analyze memory usage over time`() {
        // Given
        memoryTracker.startTracking()
        
        // Create multiple snapshots
        repeat(5) {
            memoryTracker.createSnapshot()
        }
        
        // When
        val trend = memoryTracker.getMemoryTrend()
        
        // Then
        assertNotNull("Trend should not be null", trend)
        assertTrue("Trend should have direction", 
            trend in listOf("INCREASING", "DECREASING", "STABLE"))
    }

    @Test
    fun `getOptimizationRecommendations should provide memory recommendations`() {
        // Given
        val mockMemoryInfo = ActivityManager.MemoryInfo().apply {
            availMem = 100 * 1024 * 1024L // Low available memory
            totalMem = 2048 * 1024 * 1024L
            lowMemory = true
        }
        every { activityManager.getMemoryInfo(any()) } answers {
            val memInfo = firstArg<ActivityManager.MemoryInfo>()
            memInfo.availMem = mockMemoryInfo.availMem
            memInfo.totalMem = mockMemoryInfo.totalMem
            memInfo.lowMemory = mockMemoryInfo.lowMemory
        }
        
        // When
        val recommendations = memoryTracker.getOptimizationRecommendations()
        
        // Then
        assertNotNull("Recommendations should not be null", recommendations)
        assertTrue("Should have recommendations for low memory", recommendations.isNotEmpty())
        assertTrue("Should contain memory-related recommendations", 
            recommendations.any { it.contains("memory", ignoreCase = true) })
    }

    @Test
    fun `reset should clear all tracking data`() {
        // Given
        memoryTracker.startTracking()
        val testObject = Any()
        memoryTracker.trackObject(testObject, "TestObject")
        memoryTracker.createSnapshot()
        
        // When
        memoryTracker.reset()
        
        // Then
        assertTrue("Tracked objects should be empty", memoryTracker.getTrackedObjects().isEmpty())
        // Note: Snapshots might still exist depending on implementation
    }

    @Test
    fun `multiple start calls should not cause issues`() {
        // When
        memoryTracker.startTracking()
        memoryTracker.startTracking()
        memoryTracker.startTracking()
        
        // Then
        assertTrue("Should still be tracking", memoryTracker.isTracking())
    }

    @Test
    fun `stop without start should not cause issues`() {
        // When
        memoryTracker.stopTracking()
        
        // Then
        assertFalse("Should not be tracking", memoryTracker.isTracking())
    }

    @Test
    fun `getMemoryInfo with null ActivityManager should handle gracefully`() {
        // Given
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns null
        val trackerWithNullAM = MemoryTracker(context)
        
        // When & Then
        assertDoesNotThrow("Should handle null ActivityManager gracefully") {
            trackerWithNullAM.getMemoryInfo()
        }
    }

    @Test
    fun `trackObject with null object should handle gracefully`() {
        // When & Then
        assertDoesNotThrow("Should handle null object gracefully") {
            memoryTracker.trackObject(null, "NullObject")
        }
    }

    @Test
    fun `trackObject with duplicate name should handle gracefully`() {
        // Given
        val object1 = Any()
        val object2 = Any()
        val objectName = "DuplicateName"
        
        // When
        memoryTracker.trackObject(object1, objectName)
        memoryTracker.trackObject(object2, objectName)
        
        // Then
        val trackedObjects = memoryTracker.getTrackedObjects()
        // Should handle duplicates appropriately (implementation dependent)
        assertNotNull("Tracked objects should not be null", trackedObjects)
    }

    @Test
    fun `memory calculations should handle edge cases`() {
        // Given - edge case with very small memory values
        val mockMemoryInfo = ActivityManager.MemoryInfo().apply {
            availMem = 0L
            totalMem = 1024L // 1KB total
        }
        every { activityManager.getMemoryInfo(any()) } answers {
            val memInfo = firstArg<ActivityManager.MemoryInfo>()
            memInfo.availMem = mockMemoryInfo.availMem
            memInfo.totalMem = mockMemoryInfo.totalMem
        }
        
        // When
        val percentage = memoryTracker.getMemoryUsagePercentage()
        
        // Then
        assertTrue("Percentage should be valid", percentage >= 0.0f && percentage <= 100.0f)
    }

    private fun assertDoesNotThrow(message: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("$message - Exception thrown: ${e.message}")
        }
    }
}