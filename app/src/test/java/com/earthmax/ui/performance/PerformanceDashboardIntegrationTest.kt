package com.earthmax.ui.performance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earthmax.core.performance.*
import com.earthmax.ui.performance.viewmodel.PerformanceDashboardViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PerformanceDashboardIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

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

    @Before
    fun setup() {
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
        every { performanceMonitor.getOverallScore() } returns 85.0f
        every { performanceMonitor.getRecommendations() } returns listOf(
            "Optimize memory usage",
            "Reduce network calls"
        )
        
        every { memoryTracker.getUsagePercentage() } returns 65.0f
        every { memoryTracker.getMemoryInfo() } returns MemoryInfo(
            totalMemory = 8192L,
            availableMemory = 2867L,
            usedMemory = 5325L,
            threshold = 6553L
        )
        
        every { frameTimeTracker.getAverageFps() } returns 58.5f
        every { frameTimeTracker.getDroppedFrames() } returns 12
        
        every { networkTracker.getAverageLatency() } returns 150L
        every { networkTracker.getSuccessRate() } returns 0.95f
        
        every { batteryTracker.getBatteryLevel() } returns 75
        every { batteryTracker.isCharging() } returns false
        every { batteryTracker.getTemperature() } returns 32.5f
        
        every { databaseOptimizer.getPerformanceScore() } returns 78.0f
        every { databaseOptimizer.getSlowQueries() } returns listOf(
            SlowQuery("SELECT * FROM events", 250L, 5)
        )
        
        every { uiPerformanceOptimizer.getPerformanceScore() } returns 82.0f
        every { uiPerformanceOptimizer.getOverdrawAreas() } returns 3
        
        every { memoryLeakDetector.getActiveLeaks() } returns listOf(
            MemoryLeak("MainActivity", "Activity", "Strong reference in static field")
        )

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

    @Test
    fun performanceDashboard_displaysCorrectOverallScore() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify overall performance score is displayed
        composeTestRule
            .onNodeWithText("85")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Overall Performance")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_displaysMemoryMetrics() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify memory metrics are displayed
        composeTestRule
            .onNodeWithText("Memory Usage")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("65%")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("5.2 GB / 8.0 GB")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_displaysFrameRateMetrics() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify frame rate metrics are displayed
        composeTestRule
            .onNodeWithText("Frame Rate")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("58.5 FPS")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("12 dropped frames")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_displaysNetworkMetrics() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify network metrics are displayed
        composeTestRule
            .onNodeWithText("Network")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("150ms avg latency")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("95% success rate")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_displaysBatteryMetrics() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify battery metrics are displayed
        composeTestRule
            .onNodeWithText("Battery")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("75%")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("32.5°C")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_displaysDatabaseMetrics() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify database metrics are displayed
        composeTestRule
            .onNodeWithText("Database")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("78")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("1 slow queries")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_displaysUIPerformanceMetrics() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify UI performance metrics are displayed
        composeTestRule
            .onNodeWithText("UI Performance")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("82")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("3 overdraw areas")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_displaysMemoryLeaks() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify memory leaks section is displayed
        composeTestRule
            .onNodeWithText("Memory Leaks")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("MainActivity")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Activity")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_displaysRecommendations() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify recommendations are displayed
        composeTestRule
            .onNodeWithText("Recommendations")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Optimize memory usage")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Reduce network calls")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_refreshButton_triggersDataUpdate() = runTest {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Find and click refresh button
        composeTestRule
            .onNodeWithContentDescription("Refresh performance data")
            .performClick()

        // Verify that performance modules are called to update data
        verify { performanceMonitor.getOverallScore() }
        verify { memoryTracker.getUsagePercentage() }
        verify { frameTimeTracker.getAverageFps() }
        verify { networkTracker.getAverageLatency() }
        verify { batteryTracker.getBatteryLevel() }
        verify { databaseOptimizer.getPerformanceScore() }
        verify { uiPerformanceOptimizer.getPerformanceScore() }
        verify { memoryLeakDetector.getActiveLeaks() }
    }

    @Test
    fun performanceDashboard_handlesEmptyRecommendations() {
        // Setup empty recommendations
        every { performanceMonitor.getRecommendations() } returns emptyList()

        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify empty state is handled gracefully
        composeTestRule
            .onNodeWithText("No recommendations available")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_handlesNoMemoryLeaks() {
        // Setup no memory leaks
        every { memoryLeakDetector.getActiveLeaks() } returns emptyList()

        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify no leaks state is displayed
        composeTestRule
            .onNodeWithText("No memory leaks detected")
            .assertIsDisplayed()
    }

    @Test
    fun performanceDashboard_showsCorrectPerformanceStatus() {
        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify performance status indicators
        composeTestRule
            .onNodeWithText("Good") // Overall score 85 should be "Good"
            .assertIsDisplayed()
        
        // Memory usage 65% should be "Warning"
        composeTestRule
            .onAllNodesWithText("Warning")
            .assertCountEquals(1)
    }

    @Test
    fun performanceDashboard_integrationWithAllModules() = runTest {
        // Start monitoring on all modules
        viewModel.startMonitoring()

        composeTestRule.setContent {
            PerformanceDashboard(viewModel = viewModel)
        }

        // Verify all modules are started
        verify { performanceMonitor.startMonitoring() }
        verify { frameTimeTracker.startTracking() }
        verify { memoryTracker.startMonitoring() }
        verify { networkTracker.startMonitoring() }
        verify { batteryTracker.startMonitoring() }
        verify { uiPerformanceOptimizer.startMonitoring() }
        verify { memoryLeakDetector.startDetection() }
        verify { databaseOptimizer.startMonitoring() }

        // Stop monitoring
        viewModel.stopMonitoring()

        // Verify all modules are stopped
        verify { performanceMonitor.stopMonitoring() }
        verify { frameTimeTracker.stopTracking() }
        verify { memoryTracker.stopMonitoring() }
        verify { networkTracker.stopMonitoring() }
        verify { batteryTracker.stopMonitoring() }
        verify { uiPerformanceOptimizer.stopMonitoring() }
        verify { memoryLeakDetector.stopDetection() }
        verify { databaseOptimizer.stopMonitoring() }
    }
}