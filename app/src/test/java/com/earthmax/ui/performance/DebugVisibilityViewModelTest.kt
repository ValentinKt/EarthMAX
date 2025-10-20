package com.earthmax.ui.performance

import com.earthmax.core.debug.DebugVisibilityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DebugVisibilityViewModelTest {

    private lateinit var mockDebugVisibilityManager: DebugVisibilityManager
    private lateinit var viewModel: DebugVisibilityViewModel

    @Before
    fun setUp() {
        mockDebugVisibilityManager = mockk(relaxed = true)
        
        // Mock the StateFlow properties
        every { mockDebugVisibilityManager.isAuthorized } returns MutableStateFlow(false)
        every { mockDebugVisibilityManager.isPerformanceDashboardVisible } returns MutableStateFlow(false)
        every { mockDebugVisibilityManager.isMemoryMonitoringVisible } returns MutableStateFlow(false)
        every { mockDebugVisibilityManager.isFrameRateMonitoringVisible } returns MutableStateFlow(false)
        every { mockDebugVisibilityManager.isNetworkMonitoringVisible } returns MutableStateFlow(false)
        every { mockDebugVisibilityManager.isBatteryMonitoringVisible } returns MutableStateFlow(false)
        every { mockDebugVisibilityManager.isDatabaseMonitoringVisible } returns MutableStateFlow(false)
        every { mockDebugVisibilityManager.isUiMonitoringVisible } returns MutableStateFlow(false)
        every { mockDebugVisibilityManager.isMemoryLeakDetectionVisible } returns MutableStateFlow(false)
        
        viewModel = DebugVisibilityViewModel(mockDebugVisibilityManager)
    }

    @Test
    fun `viewModel exposes debugVisibilityManager`() {
        // When
        val manager = viewModel.debugVisibilityManager

        // Then
        assertEquals(mockDebugVisibilityManager, manager)
    }

    @Test
    fun `viewModel properly injects DebugVisibilityManager`() {
        // Given & When
        val viewModel = DebugVisibilityViewModel(mockDebugVisibilityManager)

        // Then
        assertNotNull(viewModel.debugVisibilityManager)
        assertEquals(mockDebugVisibilityManager, viewModel.debugVisibilityManager)
    }

    @Test
    fun `viewModel can access all visibility states`() {
        // When accessing visibility states through the manager
        val isAuthorized = viewModel.debugVisibilityManager.isAuthorized
        val isDashboardVisible = viewModel.debugVisibilityManager.isPerformanceDashboardVisible
        val isMemoryVisible = viewModel.debugVisibilityManager.isMemoryMonitoringVisible

        // Then
        assertNotNull(isAuthorized)
        assertNotNull(isDashboardVisible)
        assertNotNull(isMemoryVisible)
    }

    @Test
    fun `viewModel delegates authorization to manager`() = runTest {
        // Given
        every { mockDebugVisibilityManager.authorizeDebugSession() } returns DebugVisibilityManager.AuthorizationResult.SUCCESS

        // When
        val result = viewModel.debugVisibilityManager.authorizeDebugSession()

        // Then
        assertEquals(DebugVisibilityManager.AuthorizationResult.SUCCESS, result)
        verify { mockDebugVisibilityManager.authorizeDebugSession() }
    }

    @Test
    fun `viewModel delegates feature visibility checks to manager`() {
        // Given
        val featureName = "test_feature"
        every { mockDebugVisibilityManager.isFeatureVisible(featureName) } returns true

        // When
        val result = viewModel.debugVisibilityManager.isFeatureVisible(featureName)

        // Then
        assertTrue(result)
        verify { mockDebugVisibilityManager.isFeatureVisible(featureName) }
    }

    @Test
    fun `viewModel delegates feature toggle to manager`() {
        // Given
        val featureName = "test_feature"

        // When
        viewModel.debugVisibilityManager.toggleFeatureVisibility(featureName)

        // Then
        verify { mockDebugVisibilityManager.toggleFeatureVisibility(featureName) }
    }

    @Test
    fun `viewModel delegates authorization revocation to manager`() {
        // When
        viewModel.debugVisibilityManager.revokeAuthorization()

        // Then
        verify { mockDebugVisibilityManager.revokeAuthorization() }
    }

    @Test
    fun `viewModel delegates visibility refresh to manager`() {
        // When
        viewModel.debugVisibilityManager.refreshVisibility()

        // Then
        verify { mockDebugVisibilityManager.refreshVisibility() }
    }

    @Test
    fun `viewModel delegates hide all metrics to manager`() {
        // When
        viewModel.debugVisibilityManager.hideAllMetrics()

        // Then
        verify { mockDebugVisibilityManager.hideAllMetrics() }
    }

    @Test
    fun `viewModel delegates visibility state retrieval to manager`() {
        // Given
        val expectedState = DebugVisibilityManager.DebugVisibilityState(
            isAuthorized = true,
            features = mapOf("test" to true)
        )
        every { mockDebugVisibilityManager.getVisibilityState() } returns expectedState

        // When
        val result = viewModel.debugVisibilityManager.getVisibilityState()

        // Then
        assertEquals(expectedState, result)
        verify { mockDebugVisibilityManager.getVisibilityState() }
    }
}