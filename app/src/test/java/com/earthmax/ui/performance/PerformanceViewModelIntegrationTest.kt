package com.earthmax.ui.performance

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earthmax.core.performance.*
import com.earthmax.ui.performance.viewmodel.PerformanceDashboardViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PerformanceViewModelIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var performanceMonitor: PerformanceMonitor

    @Inject
    lateinit var frameTimeTracker: FrameTimeTracker

    @Inject
    lateinit var memoryTracker: MemoryTracker

    @Inject
    lateinit var networkTracker: NetworkTracker

    @Inject
    lateinit var batteryTracker: BatteryTracker

    @Inject
    lateinit var uiPerformanceOptimizer: UIPerformanceOptimizer

    @Inject
    lateinit var memoryLeakDetector: MemoryLeakDetector

    @Inject
    lateinit var databaseOptimizer: DatabaseOptimizer

    private lateinit var viewModel: PerformanceDashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        hiltRule.inject()
        
        // Mock all performance modules
        mockkObject(performanceMonitor)
        mockkObject(frameTimeTracker)
        mockkObject(memoryTracker)
        mockkObject(networkTracker)
        mockkObject(batteryTracker)
        mockkObject(uiPerformanceOptimizer)
        mockkObject(memoryLeakDetector)
        mockkObject(databaseOptimizer)

        // Setup default mock behaviors
        setupDefaultMocks()

        viewModel = PerformanceDashboardViewModel(
            performanceMonitor,
            frameTimeTracker,
            memoryTracker,
            networkTracker,
            batteryTracker,
            uiPerformanceOptimizer,
            memoryLeakDetector,
            databaseOptimizer
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun setupDefaultMocks() {
        every { performanceMonitor.startMonitoring() } just Runs
        every { performanceMonitor.stopMonitoring() } just Runs
        every { performanceMonitor.getOverallScore() } returns 85.0f
        every { performanceMonitor.getRecommendations() } returns listOf(
            "Optimize memory usage",
            "Reduce network calls"
        )
        
        every { frameTimeTracker.startTracking() } just Runs
        every { frameTimeTracker.stopTracking() } just Runs
        every { frameTimeTracker.getAverageFps() } returns 58.5f
        every { frameTimeTracker.getDroppedFrames() } returns 12
        
        every { memoryTracker.startMonitoring() } just Runs
        every { memoryTracker.stopMonitoring() } just Runs
        every { memoryTracker.getUsagePercentage() } returns 65.0f
        every { memoryTracker.getMemoryInfo() } returns MemoryInfo(
            totalMemory = 8192L,
            availableMemory = 2867L,
            usedMemory = 5325L,
            threshold = 6553L
        )
        
        every { networkTracker.startMonitoring() } just Runs
        every { networkTracker.stopMonitoring() } just Runs
        every { networkTracker.getAverageLatency() } returns 150L
        every { networkTracker.getSuccessRate() } returns 0.95f
        
        every { batteryTracker.startMonitoring() } just Runs
        every { batteryTracker.stopMonitoring() } just Runs
        every { batteryTracker.getBatteryLevel() } returns 75
        every { batteryTracker.isCharging() } returns false
        every { batteryTracker.getTemperature() } returns 32.5f
        
        every { uiPerformanceOptimizer.startMonitoring() } just Runs
        every { uiPerformanceOptimizer.stopMonitoring() } just Runs
        every { uiPerformanceOptimizer.getPerformanceScore() } returns 82.0f
        every { uiPerformanceOptimizer.getOverdrawAreas() } returns 3
        
        every { memoryLeakDetector.startDetection() } just Runs
        every { memoryLeakDetector.stopDetection() } just Runs
        every { memoryLeakDetector.getActiveLeaks() } returns listOf(
            MemoryLeak("MainActivity", "Activity", "Strong reference in static field")
        )
        
        every { databaseOptimizer.startMonitoring() } just Runs
        every { databaseOptimizer.stopMonitoring() } just Runs
        every { databaseOptimizer.getPerformanceScore() } returns 78.0f
        every { databaseOptimizer.getSlowQueries() } returns listOf(
            SlowQuery("SELECT * FROM events", 250L, 5)
        )
    }

    @Test
    fun viewModel_initialState_isCorrect() = runTest {
        // Verify initial state
        assertFalse(viewModel.uiState.value.isMonitoring)
        assertEquals(0.0f, viewModel.uiState.value.overallScore)
        assertTrue(viewModel.uiState.value.recommendations.isEmpty())
        assertTrue(viewModel.uiState.value.memoryLeaks.isEmpty())
    }

    @Test
    fun viewModel_startMonitoring_startsAllModules() = runTest {
        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify all modules are started
        verify { performanceMonitor.startMonitoring() }
        verify { frameTimeTracker.startTracking() }
        verify { memoryTracker.startMonitoring() }
        verify { networkTracker.startMonitoring() }
        verify { batteryTracker.startMonitoring() }
        verify { uiPerformanceOptimizer.startMonitoring() }
        verify { memoryLeakDetector.startDetection() }
        verify { databaseOptimizer.startMonitoring() }

        // Verify monitoring state is updated
        assertTrue(viewModel.uiState.value.isMonitoring)
    }

    @Test
    fun viewModel_stopMonitoring_stopsAllModules() = runTest {
        // Start then stop monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.stopMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify all modules are stopped
        verify { performanceMonitor.stopMonitoring() }
        verify { frameTimeTracker.stopTracking() }
        verify { memoryTracker.stopMonitoring() }
        verify { networkTracker.stopMonitoring() }
        verify { batteryTracker.stopMonitoring() }
        verify { uiPerformanceOptimizer.stopMonitoring() }
        verify { memoryLeakDetector.stopDetection() }
        verify { databaseOptimizer.stopMonitoring() }

        // Verify monitoring state is updated
        assertFalse(viewModel.uiState.value.isMonitoring)
    }

    @Test
    fun viewModel_updateMetrics_updatesAllPerformanceData() = runTest {
        // Start monitoring and update metrics
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify all metrics are updated in UI state
        val uiState = viewModel.uiState.value
        
        assertEquals(85.0f, uiState.overallScore)
        assertEquals(65.0f, uiState.memoryUsage)
        assertEquals(58.5f, uiState.frameRate)
        assertEquals(150L, uiState.networkLatency)
        assertEquals(0.95f, uiState.networkSuccessRate)
        assertEquals(75, uiState.batteryLevel)
        assertEquals(false, uiState.isCharging)
        assertEquals(32.5f, uiState.batteryTemperature)
        assertEquals(78.0f, uiState.databaseScore)
        assertEquals(82.0f, uiState.uiPerformanceScore)
        assertEquals(1, uiState.memoryLeaks.size)
        assertEquals(2, uiState.recommendations.size)
    }

    @Test
    fun viewModel_handlesPerformanceModuleErrors() = runTest {
        // Setup error conditions
        every { performanceMonitor.getOverallScore() } throws RuntimeException("Performance error")
        every { memoryTracker.getUsagePercentage() } throws RuntimeException("Memory error")

        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error handling - should not crash and provide default values
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isMonitoring) // Should still be monitoring
        // Error values should be handled gracefully
    }

    @Test
    fun viewModel_periodicUpdates_refreshMetrics() = runTest {
        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Clear previous calls
        clearMocks(performanceMonitor, memoryTracker, frameTimeTracker)

        // Advance time to trigger periodic update
        testDispatcher.scheduler.advanceTimeBy(5000L) // 5 seconds
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify metrics are refreshed
        verify(atLeast = 1) { performanceMonitor.getOverallScore() }
        verify(atLeast = 1) { memoryTracker.getUsagePercentage() }
        verify(atLeast = 1) { frameTimeTracker.getAverageFps() }
    }

    @Test
    fun viewModel_memoryLeakDetection_updatesLeaksList() = runTest {
        // Setup multiple memory leaks
        every { memoryLeakDetector.getActiveLeaks() } returns listOf(
            MemoryLeak("MainActivity", "Activity", "Static reference"),
            MemoryLeak("TestFragment", "Fragment", "Handler reference"),
            MemoryLeak("CustomView", "View", "Listener reference")
        )

        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify memory leaks are updated
        val uiState = viewModel.uiState.value
        assertEquals(3, uiState.memoryLeaks.size)
        assertEquals("MainActivity", uiState.memoryLeaks[0].objectName)
        assertEquals("TestFragment", uiState.memoryLeaks[1].objectName)
        assertEquals("CustomView", uiState.memoryLeaks[2].objectName)
    }

    @Test
    fun viewModel_recommendationsUpdate_basedOnPerformance() = runTest {
        // Setup different recommendations
        every { performanceMonitor.getRecommendations() } returns listOf(
            "Enable hardware acceleration",
            "Optimize image loading",
            "Reduce view hierarchy depth"
        )

        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify recommendations are updated
        val uiState = viewModel.uiState.value
        assertEquals(3, uiState.recommendations.size)
        assertTrue(uiState.recommendations.contains("Enable hardware acceleration"))
        assertTrue(uiState.recommendations.contains("Optimize image loading"))
        assertTrue(uiState.recommendations.contains("Reduce view hierarchy depth"))
    }

    @Test
    fun viewModel_databasePerformance_tracksSlowQueries() = runTest {
        // Setup multiple slow queries
        every { databaseOptimizer.getSlowQueries() } returns listOf(
            SlowQuery("SELECT * FROM events WHERE date > ?", 300L, 10),
            SlowQuery("SELECT COUNT(*) FROM users", 150L, 25),
            SlowQuery("UPDATE events SET status = ?", 200L, 5)
        )

        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify slow queries are tracked
        val uiState = viewModel.uiState.value
        assertEquals(3, uiState.slowQueries.size)
        assertEquals(300L, uiState.slowQueries[0].executionTime)
        assertEquals(10, uiState.slowQueries[0].count)
    }

    @Test
    fun viewModel_networkPerformance_tracksLatencyAndSuccess() = runTest {
        // Setup network performance data
        every { networkTracker.getAverageLatency() } returns 250L
        every { networkTracker.getSuccessRate() } returns 0.88f

        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify network metrics are tracked
        val uiState = viewModel.uiState.value
        assertEquals(250L, uiState.networkLatency)
        assertEquals(0.88f, uiState.networkSuccessRate)
    }

    @Test
    fun viewModel_batteryOptimization_tracksUsageAndTemperature() = runTest {
        // Setup battery data
        every { batteryTracker.getBatteryLevel() } returns 45
        every { batteryTracker.isCharging() } returns true
        every { batteryTracker.getTemperature() } returns 38.2f

        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify battery metrics are tracked
        val uiState = viewModel.uiState.value
        assertEquals(45, uiState.batteryLevel)
        assertEquals(true, uiState.isCharging)
        assertEquals(38.2f, uiState.batteryTemperature)
    }

    @Test
    fun viewModel_uiPerformanceOptimization_tracksOverdrawAndScore() = runTest {
        // Setup UI performance data
        every { uiPerformanceOptimizer.getPerformanceScore() } returns 72.0f
        every { uiPerformanceOptimizer.getOverdrawAreas() } returns 7

        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify UI performance metrics are tracked
        val uiState = viewModel.uiState.value
        assertEquals(72.0f, uiState.uiPerformanceScore)
        assertEquals(7, uiState.overdrawAreas)
    }

    @Test
    fun viewModel_integrationWithAllModules_coordinatesCorrectly() = runTest {
        // Start monitoring
        viewModel.startMonitoring()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify all modules are integrated and working together
        verify { performanceMonitor.startMonitoring() }
        verify { frameTimeTracker.startTracking() }
        verify { memoryTracker.startMonitoring() }
        verify { networkTracker.startMonitoring() }
        verify { batteryTracker.startMonitoring() }
        verify { uiPerformanceOptimizer.startMonitoring() }
        verify { memoryLeakDetector.startDetection() }
        verify { databaseOptimizer.startMonitoring() }

        // Verify data is collected from all modules
        verify { performanceMonitor.getOverallScore() }
        verify { performanceMonitor.getRecommendations() }
        verify { frameTimeTracker.getAverageFps() }
        verify { frameTimeTracker.getDroppedFrames() }
        verify { memoryTracker.getUsagePercentage() }
        verify { memoryTracker.getMemoryInfo() }
        verify { networkTracker.getAverageLatency() }
        verify { networkTracker.getSuccessRate() }
        verify { batteryTracker.getBatteryLevel() }
        verify { batteryTracker.isCharging() }
        verify { batteryTracker.getTemperature() }
        verify { uiPerformanceOptimizer.getPerformanceScore() }
        verify { uiPerformanceOptimizer.getOverdrawAreas() }
        verify { memoryLeakDetector.getActiveLeaks() }
        verify { databaseOptimizer.getPerformanceScore() }
        verify { databaseOptimizer.getSlowQueries() }

        // Verify UI state is properly updated with all data
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isMonitoring)
        assertTrue(uiState.overallScore > 0)
        assertTrue(uiState.recommendations.isNotEmpty())
        assertTrue(uiState.memoryLeaks.isNotEmpty())
    }
}