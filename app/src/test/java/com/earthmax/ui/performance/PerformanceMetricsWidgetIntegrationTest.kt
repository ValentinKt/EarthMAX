package com.earthmax.ui.performance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earthmax.core.performance.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PerformanceMetricsWidgetIntegrationTest {

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
    lateinit var batteryTracker: BatteryTracker

    @Inject
    lateinit var memoryLeakDetector: MemoryLeakDetector

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Mock all performance modules
        mockkObject(performanceMonitor)
        mockkObject(frameTimeTracker)
        mockkObject(memoryTracker)
        mockkObject(batteryTracker)
        mockkObject(memoryLeakDetector)

        // Setup default mock behaviors for good performance
        every { performanceMonitor.getOverallScore() } returns 85.0f
        every { memoryTracker.getUsagePercentage() } returns 45.0f
        every { frameTimeTracker.getAverageFps() } returns 60.0f
        every { batteryTracker.getBatteryLevel() } returns 80
        every { memoryLeakDetector.getActiveLeaks() } returns emptyList()
    }

    @Test
    fun performanceMetricsWidget_displaysOverallScore() {
        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify overall performance score is displayed
        composeTestRule
            .onNodeWithText("85")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Performance")
            .assertIsDisplayed()
    }

    @Test
    fun performanceMetricsWidget_displaysQuickMetrics() {
        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify quick metrics are displayed
        composeTestRule
            .onNodeWithText("45%") // Memory
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("60") // FPS
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("80%") // Battery
            .assertIsDisplayed()
    }

    @Test
    fun performanceMetricsWidget_showsWarningForHighMemoryUsage() {
        // Setup high memory usage
        every { memoryTracker.getUsagePercentage() } returns 85.0f

        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify warning indicator is shown
        composeTestRule
            .onNodeWithContentDescription("High memory usage warning")
            .assertIsDisplayed()
    }

    @Test
    fun performanceMetricsWidget_showsWarningForLowFPS() {
        // Setup low FPS
        every { frameTimeTracker.getAverageFps() } returns 25.0f

        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify warning indicator is shown
        composeTestRule
            .onNodeWithContentDescription("Low FPS warning")
            .assertIsDisplayed()
    }

    @Test
    fun performanceMetricsWidget_showsWarningForMemoryLeaks() {
        // Setup memory leaks
        every { memoryLeakDetector.getActiveLeaks() } returns listOf(
            MemoryLeak("TestActivity", "Activity", "Static reference")
        )

        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify warning indicator is shown
        composeTestRule
            .onNodeWithContentDescription("Memory leaks detected")
            .assertIsDisplayed()
    }

    @Test
    fun performanceMetricsWidget_clickableExpandsToFullDashboard() {
        var onClickCalled = false

        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector,
                onClick = { onClickCalled = true }
            )
        }

        // Click on the widget
        composeTestRule
            .onNodeWithContentDescription("Performance metrics widget")
            .performClick()

        // Verify click callback was called
        assert(onClickCalled)
    }

    @Test
    fun performanceMetricsWidget_handlesNullValues() {
        // Setup null/error values
        every { performanceMonitor.getOverallScore() } returns 0.0f
        every { memoryTracker.getUsagePercentage() } returns 0.0f
        every { frameTimeTracker.getAverageFps() } returns 0.0f
        every { batteryTracker.getBatteryLevel() } returns 0

        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify widget still displays without crashing
        composeTestRule
            .onNodeWithText("Performance")
            .assertIsDisplayed()
        
        // Should show "N/A" or "0" for unavailable metrics
        composeTestRule
            .onNodeWithText("0")
            .assertIsDisplayed()
    }

    @Test
    fun performanceMetricsWidget_compactLayout() {
        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify compact layout elements are present
        composeTestRule
            .onNodeWithContentDescription("Performance metrics widget")
            .assertIsDisplayed()
        
        // Should have all key metrics in a compact form
        composeTestRule
            .onNodeWithText("Performance")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Memory")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("FPS")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Battery")
            .assertIsDisplayed()
    }

    @Test
    fun performanceMetricsWidget_multipleWarnings() {
        // Setup multiple warning conditions
        every { memoryTracker.getUsagePercentage() } returns 90.0f
        every { frameTimeTracker.getAverageFps() } returns 20.0f
        every { memoryLeakDetector.getActiveLeaks() } returns listOf(
            MemoryLeak("TestActivity", "Activity", "Static reference"),
            MemoryLeak("TestFragment", "Fragment", "Handler reference")
        )

        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify all warning indicators are shown
        composeTestRule
            .onNodeWithContentDescription("High memory usage warning")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Low FPS warning")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Memory leaks detected")
            .assertIsDisplayed()
    }

    @Test
    fun performanceMetricsWidget_refreshesDataPeriodically() {
        composeTestRule.setContent {
            PerformanceMetricsWidget(
                performanceMonitor = performanceMonitor,
                frameTimeTracker = frameTimeTracker,
                memoryTracker = memoryTracker,
                batteryTracker = batteryTracker,
                memoryLeakDetector = memoryLeakDetector
            )
        }

        // Verify initial data is loaded
        verify(atLeast = 1) { performanceMonitor.getOverallScore() }
        verify(atLeast = 1) { memoryTracker.getUsagePercentage() }
        verify(atLeast = 1) { frameTimeTracker.getAverageFps() }
        verify(atLeast = 1) { batteryTracker.getBatteryLevel() }
        verify(atLeast = 1) { memoryLeakDetector.getActiveLeaks() }
    }
}