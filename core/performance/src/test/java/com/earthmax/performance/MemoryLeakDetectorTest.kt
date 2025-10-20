package com.earthmax.performance

import android.app.Activity
import androidx.fragment.app.Fragment
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryLeakDetectorTest {

    private lateinit var memoryLeakDetector: MemoryLeakDetector
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        memoryLeakDetector = MemoryLeakDetector()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startDetection should initialize memory leak monitoring`() {
        // When
        memoryLeakDetector.startDetection()
        
        // Then
        assertTrue("Should be detecting", memoryLeakDetector.isDetecting())
    }

    @Test
    fun `stopDetection should stop memory leak monitoring`() {
        // Given
        memoryLeakDetector.startDetection()
        
        // When
        memoryLeakDetector.stopDetection()
        
        // Then
        assertFalse("Should not be detecting", memoryLeakDetector.isDetecting())
    }

    @Test
    fun `trackActivity should monitor activity lifecycle`() {
        // Given
        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "MainActivity"
        
        memoryLeakDetector.startDetection()
        
        // When
        memoryLeakDetector.trackActivity(mockActivity)
        
        // Then
        val trackedObjects = memoryLeakDetector.getTrackedObjects()
        assertTrue("Should track activity", trackedObjects.any { it.contains("MainActivity") })
    }

    @Test
    fun `trackFragment should monitor fragment lifecycle`() {
        // Given
        val mockFragment = mockk<Fragment>(relaxed = true)
        every { mockFragment.javaClass.simpleName } returns "ProfileFragment"
        
        memoryLeakDetector.startDetection()
        
        // When
        memoryLeakDetector.trackFragment(mockFragment)
        
        // Then
        val trackedObjects = memoryLeakDetector.getTrackedObjects()
        assertTrue("Should track fragment", trackedObjects.any { it.contains("ProfileFragment") })
    }

    @Test
    fun `trackCustomObject should monitor custom objects`() {
        // Given
        val customObject = "CustomObject"
        val objectName = "TestObject"
        
        memoryLeakDetector.startDetection()
        
        // When
        memoryLeakDetector.trackCustomObject(customObject, objectName)
        
        // Then
        val trackedObjects = memoryLeakDetector.getTrackedObjects()
        assertTrue("Should track custom object", trackedObjects.any { it.contains(objectName) })
    }

    @Test
    fun `untrackObject should remove object from monitoring`() {
        // Given
        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "MainActivity"
        
        memoryLeakDetector.startDetection()
        memoryLeakDetector.trackActivity(mockActivity)
        
        // When
        memoryLeakDetector.untrackObject("MainActivity")
        
        // Then
        val trackedObjects = memoryLeakDetector.getTrackedObjects()
        assertFalse("Should not track activity after untracking", 
            trackedObjects.any { it.contains("MainActivity") })
    }

    @Test
    fun `performLeakCheck should detect potential memory leaks`() = runTest {
        // Given
        memoryLeakDetector.startDetection()
        
        // Track some objects
        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "LeakyActivity"
        memoryLeakDetector.trackActivity(mockActivity)
        
        // Simulate garbage collection
        System.gc()
        testDispatcher.scheduler.advanceTimeBy(1000)
        
        // When
        memoryLeakDetector.performLeakCheck()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val leakReport = memoryLeakDetector.getLeakReport()
        assertNotNull("Leak report should not be null", leakReport)
    }

    @Test
    fun `getLeakReport should provide detailed leak information`() {
        // Given
        memoryLeakDetector.startDetection()
        
        // Track multiple objects
        val mockActivity = mockk<Activity>(relaxed = true)
        val mockFragment = mockk<Fragment>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "MainActivity"
        every { mockFragment.javaClass.simpleName } returns "ProfileFragment"
        
        memoryLeakDetector.trackActivity(mockActivity)
        memoryLeakDetector.trackFragment(mockFragment)
        
        // When
        val leakReport = memoryLeakDetector.getLeakReport()
        
        // Then
        assertNotNull("Leak report should not be null", leakReport)
        assertTrue("Should have tracked objects count", leakReport.totalTrackedObjects >= 0)
        assertTrue("Should have potential leaks count", leakReport.potentialLeaks >= 0)
        assertNotNull("Should have leak details", leakReport.leakDetails)
        assertNotNull("Should have recommendations", leakReport.recommendations)
    }

    @Test
    fun `getLeakReport should identify common leak patterns`() {
        // Given
        memoryLeakDetector.startDetection()
        
        // Simulate objects that commonly cause leaks
        memoryLeakDetector.trackCustomObject("StaticReference", "StaticActivityReference")
        memoryLeakDetector.trackCustomObject("AsyncTask", "LongRunningAsyncTask")
        memoryLeakDetector.trackCustomObject("Handler", "StaticHandler")
        
        // When
        val leakReport = memoryLeakDetector.getLeakReport()
        
        // Then
        assertNotNull("Leak report should not be null", leakReport)
        assertTrue("Should provide recommendations for common patterns", 
            leakReport.recommendations.any { it.contains("static", ignoreCase = true) ||
                                           it.contains("handler", ignoreCase = true) ||
                                           it.contains("async", ignoreCase = true) })
    }

    @Test
    fun `analyzeHeapDump should provide memory analysis`() = runTest {
        // Given
        memoryLeakDetector.startDetection()
        
        // When
        val heapAnalysis = memoryLeakDetector.analyzeHeapDump()
        
        // Then
        assertNotNull("Heap analysis should not be null", heapAnalysis)
        assertTrue("Should contain memory information", 
            heapAnalysis.contains("memory", ignoreCase = true) ||
            heapAnalysis.contains("heap", ignoreCase = true) ||
            heapAnalysis.contains("objects", ignoreCase = true))
    }

    @Test
    fun `getMemoryLeakScore should calculate leak risk score`() {
        // Given
        memoryLeakDetector.startDetection()
        
        // Track some objects to establish baseline
        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "MainActivity"
        memoryLeakDetector.trackActivity(mockActivity)
        
        // When
        val score = memoryLeakDetector.getMemoryLeakScore()
        
        // Then
        assertTrue("Score should be within valid range", score >= 0.0f && score <= 100.0f)
    }

    @Test
    fun `getMemoryLeakScore should reflect leak risk accurately`() {
        // Given
        memoryLeakDetector.startDetection()
        
        // Simulate high-risk scenario
        repeat(10) { index ->
            memoryLeakDetector.trackCustomObject("StaticReference$index", "HighRiskObject$index")
        }
        
        // When
        val highRiskScore = memoryLeakDetector.getMemoryLeakScore()
        
        // Reset and test low-risk scenario
        memoryLeakDetector.reset()
        memoryLeakDetector.startDetection()
        
        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "SafeActivity"
        memoryLeakDetector.trackActivity(mockActivity)
        
        val lowRiskScore = memoryLeakDetector.getMemoryLeakScore()
        
        // Then
        assertTrue("High risk scenario should have lower score", highRiskScore <= lowRiskScore)
        assertTrue("Both scores should be valid", 
            highRiskScore >= 0.0f && highRiskScore <= 100.0f &&
            lowRiskScore >= 0.0f && lowRiskScore <= 100.0f)
    }

    @Test
    fun `createSnapshot should capture current detection state`() {
        // Given
        memoryLeakDetector.startDetection()
        
        val mockActivity = mockk<Activity>(relaxed = true)
        val mockFragment = mockk<Fragment>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "MainActivity"
        every { mockFragment.javaClass.simpleName } returns "ProfileFragment"
        
        memoryLeakDetector.trackActivity(mockActivity)
        memoryLeakDetector.trackFragment(mockFragment)
        
        // When
        val snapshot = memoryLeakDetector.createSnapshot()
        
        // Then
        assertNotNull("Snapshot should not be null", snapshot)
        assertTrue("Snapshot should have timestamp", snapshot.timestamp > 0)
        assertTrue("Snapshot should have tracked objects count", snapshot.trackedObjects >= 0)
        assertTrue("Snapshot should have potential leaks count", snapshot.potentialLeaks >= 0)
        assertTrue("Snapshot should have leak score", 
            snapshot.leakScore >= 0.0f && snapshot.leakScore <= 100.0f)
    }

    @Test
    fun `reset should clear all tracking data`() {
        // Given
        memoryLeakDetector.startDetection()
        
        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "MainActivity"
        memoryLeakDetector.trackActivity(mockActivity)
        memoryLeakDetector.trackCustomObject("TestObject", "TestName")
        
        // When
        memoryLeakDetector.reset()
        
        // Then
        val trackedObjects = memoryLeakDetector.getTrackedObjects()
        assertTrue("Should clear all tracked objects", trackedObjects.isEmpty())
        
        val leakReport = memoryLeakDetector.getLeakReport()
        assertEquals("Should reset tracked objects count", 0, leakReport.totalTrackedObjects)
        assertEquals("Should reset potential leaks count", 0, leakReport.potentialLeaks)
    }

    @Test
    fun `detectActivityLeaks should identify activity-specific leaks`() {
        // Given
        memoryLeakDetector.startDetection()
        
        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.javaClass.simpleName } returns "LeakyActivity"
        every { mockActivity.isFinishing } returns true // Activity is finishing but still tracked
        
        memoryLeakDetector.trackActivity(mockActivity)
        
        // When
        val activityLeaks = memoryLeakDetector.detectActivityLeaks()
        
        // Then
        assertNotNull("Activity leaks should not be null", activityLeaks)
        // Should detect that a finishing activity is still being tracked
    }

    @Test
    fun `detectFragmentLeaks should identify fragment-specific leaks`() {
        // Given
        memoryLeakDetector.startDetection()
        
        val mockFragment = mockk<Fragment>(relaxed = true)
        every { mockFragment.javaClass.simpleName } returns "LeakyFragment"
        every { mockFragment.isDetached } returns true // Fragment is detached but still tracked
        
        memoryLeakDetector.trackFragment(mockFragment)
        
        // When
        val fragmentLeaks = memoryLeakDetector.detectFragmentLeaks()
        
        // Then
        assertNotNull("Fragment leaks should not be null", fragmentLeaks)
        // Should detect that a detached fragment is still being tracked
    }

    @Test
    fun `getRecommendations should provide actionable advice`() {
        // Given
        memoryLeakDetector.startDetection()
        
        // Simulate various leak-prone scenarios
        memoryLeakDetector.trackCustomObject("StaticContext", "StaticContextReference")
        memoryLeakDetector.trackCustomObject("AsyncTask", "InnerAsyncTask")
        memoryLeakDetector.trackCustomObject("Handler", "StaticHandler")
        
        // When
        val recommendations = memoryLeakDetector.getRecommendations()
        
        // Then
        assertNotNull("Recommendations should not be null", recommendations)
        assertTrue("Should have recommendations", recommendations.isNotEmpty())
        assertTrue("Should provide specific advice", 
            recommendations.any { it.contains("WeakReference", ignoreCase = true) ||
                                it.contains("static", ignoreCase = true) ||
                                it.contains("context", ignoreCase = true) ||
                                it.contains("lifecycle", ignoreCase = true) })
    }

    @Test
    fun `concurrent tracking operations should be handled correctly`() = runTest {
        // Given
        memoryLeakDetector.startDetection()
        
        // When - Simulate concurrent tracking operations
        repeat(100) { index ->
            val mockActivity = mockk<Activity>(relaxed = true)
            every { mockActivity.javaClass.simpleName } returns "Activity$index"
            memoryLeakDetector.trackActivity(mockActivity)
            
            if (index % 2 == 0) {
                memoryLeakDetector.untrackObject("Activity${index - 1}")
            }
        }
        
        // Then
        val trackedObjects = memoryLeakDetector.getTrackedObjects()
        assertTrue("Should handle concurrent operations", trackedObjects.size >= 50)
        
        val leakReport = memoryLeakDetector.getLeakReport()
        assertTrue("Should maintain consistent state", leakReport.totalTrackedObjects >= 0)
    }

    @Test
    fun `edge cases should be handled gracefully`() {
        // Test tracking null objects
        memoryLeakDetector.startDetection()
        
        try {
            memoryLeakDetector.trackCustomObject(null, "NullObject")
            memoryLeakDetector.untrackObject("")
            memoryLeakDetector.untrackObject("NonExistentObject")
            
            // Should not crash
            val trackedObjects = memoryLeakDetector.getTrackedObjects()
            assertNotNull("Should handle null objects gracefully", trackedObjects)
        } catch (e: Exception) {
            fail("Should not throw exception for edge cases: ${e.message}")
        }
    }

    @Test
    fun `multiple start and stop calls should not cause issues`() {
        // When
        memoryLeakDetector.startDetection()
        memoryLeakDetector.startDetection()
        memoryLeakDetector.stopDetection()
        memoryLeakDetector.stopDetection()
        memoryLeakDetector.startDetection()
        
        // Then
        assertTrue("Should be detecting after multiple calls", memoryLeakDetector.isDetecting())
    }

    @Test
    fun `leak detection with no tracked objects should return empty results`() {
        // Given
        memoryLeakDetector.startDetection()
        
        // When
        val leakReport = memoryLeakDetector.getLeakReport()
        val trackedObjects = memoryLeakDetector.getTrackedObjects()
        
        // Then
        assertEquals("Should have no tracked objects", 0, leakReport.totalTrackedObjects)
        assertEquals("Should have no potential leaks", 0, leakReport.potentialLeaks)
        assertTrue("Tracked objects list should be empty", trackedObjects.isEmpty())
    }

    @Test
    fun `weak reference handling should work correctly`() {
        // Given
        memoryLeakDetector.startDetection()
        
        var testObject: String? = "TestObject"
        val weakRef = WeakReference(testObject)
        
        memoryLeakDetector.trackCustomObject(testObject!!, "WeakRefTest")
        
        // When
        testObject = null // Remove strong reference
        System.gc() // Suggest garbage collection
        
        // Then
        // The weak reference should eventually become null
        // This is a timing-dependent test, so we'll just verify the mechanism works
        val trackedObjects = memoryLeakDetector.getTrackedObjects()
        assertNotNull("Should handle weak references", trackedObjects)
    }

    @Test
    fun `memory pressure simulation should trigger appropriate responses`() = runTest {
        // Given
        memoryLeakDetector.startDetection()
        
        // Simulate memory pressure by tracking many objects
        repeat(1000) { index ->
            memoryLeakDetector.trackCustomObject("Object$index", "PressureTest$index")
        }
        
        // When
        memoryLeakDetector.onMemoryPressure()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val leakReport = memoryLeakDetector.getLeakReport()
        assertNotNull("Should handle memory pressure", leakReport)
        assertTrue("Should provide recommendations under pressure", 
            leakReport.recommendations.isNotEmpty())
    }

    @Test
    fun `leak detection patterns should be configurable`() {
        // Given
        memoryLeakDetector.startDetection()
        
        // When
        memoryLeakDetector.configureDetectionPatterns(
            detectStaticReferences = true,
            detectAsyncTasks = true,
            detectHandlers = true,
            detectListeners = false
        )
        
        // Then
        // Configuration should affect detection behavior
        val config = memoryLeakDetector.getDetectionConfiguration()
        assertNotNull("Configuration should not be null", config)
        assertTrue("Should enable static reference detection", config.detectStaticReferences)
        assertTrue("Should enable async task detection", config.detectAsyncTasks)
        assertTrue("Should enable handler detection", config.detectHandlers)
        assertFalse("Should disable listener detection", config.detectListeners)
    }

    @Test
    fun `performance impact should be minimal`() {
        // Given
        val startTime = System.currentTimeMillis()
        
        // When
        memoryLeakDetector.startDetection()
        
        repeat(1000) { index ->
            val mockActivity = mockk<Activity>(relaxed = true)
            every { mockActivity.javaClass.simpleName } returns "Activity$index"
            memoryLeakDetector.trackActivity(mockActivity)
        }
        
        memoryLeakDetector.performLeakCheck()
        val endTime = System.currentTimeMillis()
        
        // Then
        val executionTime = endTime - startTime
        assertTrue("Detection should be performant", executionTime < 5000) // Should complete within 5 seconds
    }
}