package com.earthmax.core.performance

import com.earthmax.core.debug.DebugConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PerformanceGuardTest {

    private lateinit var mockDebugConfig: DebugConfig
    private lateinit var performanceGuard: PerformanceGuard

    @Before
    fun setUp() {
        mockDebugConfig = mockk(relaxed = true)
        performanceGuard = PerformanceGuard(mockDebugConfig)
    }

    @Test
    fun `shouldEnableMonitoring returns true when debug config allows`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true

        // When
        val result = performanceGuard.shouldEnableMonitoring()

        // Then
        assertTrue(result)
        verify { mockDebugConfig.shouldShowPerformanceMetrics() }
    }

    @Test
    fun `shouldEnableMonitoring returns false when debug config denies`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns false

        // When
        val result = performanceGuard.shouldEnableMonitoring()

        // Then
        assertFalse(result)
    }

    @Test
    fun `guardOperation executes operation when monitoring enabled`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true
        val expectedValue = "test_result"

        // When
        val result = performanceGuard.guardOperation("default") { expectedValue }

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `guardOperation returns default when monitoring disabled`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns false
        val defaultValue = "default_value"

        // When
        val result = performanceGuard.guardOperation(defaultValue) { "should_not_execute" }

        // Then
        assertEquals(defaultValue, result)
    }

    @Test
    fun `guardOperation returns default when operation throws exception`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true
        every { mockDebugConfig.isDebugBuild() } returns true
        val defaultValue = "default_value"

        // When
        val result = performanceGuard.guardOperation(defaultValue) { 
            throw RuntimeException("Test exception") 
        }

        // Then
        assertEquals(defaultValue, result)
    }

    @Test
    fun `guardFlow executes flow when monitoring enabled`() = runTest {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true
        val expectedValues = listOf(1, 2, 3)

        // When
        val result = performanceGuard.guardFlow { flowOf(*expectedValues.toTypedArray()) }

        // Then
        assertEquals(expectedValues, result.toList())
    }

    @Test
    fun `guardFlow returns empty flow when monitoring disabled`() = runTest {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns false

        // When
        val result = performanceGuard.guardFlow { flowOf(1, 2, 3) }

        // Then
        assertEquals(emptyList<Int>(), result.toList())
    }

    @Test
    fun `guardFlow returns empty flow when operation throws exception`() = runTest {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true
        every { mockDebugConfig.isDebugBuild() } returns true

        // When
        val result = performanceGuard.guardFlow<Int> { 
            throw RuntimeException("Test exception") 
        }

        // Then
        assertEquals(emptyList<Int>(), result.toList())
    }

    @Test
    fun `guardSuspendOperation executes when monitoring enabled`() = runTest {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true
        val expectedValue = "suspend_result"

        // When
        val result = performanceGuard.guardSuspendOperation("default") { expectedValue }

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `guardSuspendOperation returns default when monitoring disabled`() = runTest {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns false
        val defaultValue = "default_value"

        // When
        val result = performanceGuard.guardSuspendOperation(defaultValue) { "should_not_execute" }

        // Then
        assertEquals(defaultValue, result)
    }

    @Test
    fun `executeIfEnabled executes when monitoring enabled`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true
        var executed = false

        // When
        performanceGuard.executeIfEnabled { executed = true }

        // Then
        assertTrue(executed)
    }

    @Test
    fun `executeIfEnabled does not execute when monitoring disabled`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns false
        var executed = false

        // When
        performanceGuard.executeIfEnabled { executed = true }

        // Then
        assertFalse(executed)
    }

    @Test
    fun `isFeatureEnabled checks both monitoring and feature state`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true
        every { mockDebugConfig.isFeatureEnabled("test_feature") } returns true

        // When
        val result = performanceGuard.isFeatureEnabled("test_feature")

        // Then
        assertTrue(result)
        verify { mockDebugConfig.shouldShowPerformanceMetrics() }
        verify { mockDebugConfig.isFeatureEnabled("test_feature") }
    }

    @Test
    fun `shouldCollectNow respects rate limiting`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true

        // When
        val firstCall = performanceGuard.shouldCollectNow()
        val secondCall = performanceGuard.shouldCollectNow() // Immediate second call

        // Then
        assertTrue(firstCall)
        assertFalse(secondCall) // Should be rate limited
    }

    @Test
    fun `getMonitoringState returns correct state`() {
        // Given
        every { mockDebugConfig.shouldShowPerformanceMetrics() } returns true
        every { mockDebugConfig.isAuthorizedDebugSession() } returns true
        every { mockDebugConfig.isDebugBuild() } returns true
        every { mockDebugConfig.isFeatureEnabled(any()) } returns true

        // When
        val state = performanceGuard.getMonitoringState()

        // Then
        assertTrue(state.isEnabled)
        assertTrue(state.authorizedSession)
        assertTrue(state.debugBuild)
        assertTrue(state.features.isNotEmpty())
    }
}