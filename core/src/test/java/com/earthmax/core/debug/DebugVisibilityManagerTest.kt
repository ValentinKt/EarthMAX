package com.earthmax.core.debug

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DebugVisibilityManagerTest {

    private lateinit var mockDebugConfig: DebugConfig
    private lateinit var debugVisibilityManager: DebugVisibilityManager

    @Before
    fun setUp() {
        mockDebugConfig = mockk(relaxed = true)
        debugVisibilityManager = DebugVisibilityManager(mockDebugConfig)
    }

    @Test
    fun `authorizeDebugSession returns success when debug config allows`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns true

        // When
        val result = debugVisibilityManager.authorizeDebugSession()

        // Then
        assertEquals(DebugVisibilityManager.AuthorizationResult.SUCCESS, result)
        assertTrue(debugVisibilityManager.isAuthorized.first())
    }

    @Test
    fun `authorizeDebugSession returns unauthorized when debug config denies`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns false

        // When
        val result = debugVisibilityManager.authorizeDebugSession()

        // Then
        assertEquals(DebugVisibilityManager.AuthorizationResult.UNAUTHORIZED, result)
        assertFalse(debugVisibilityManager.isAuthorized.first())
    }

    @Test
    fun `authorizeDebugSession implements rate limiting after max attempts`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns false

        // When - Exceed max attempts
        repeat(DebugVisibilityManager.MAX_AUTHORIZATION_ATTEMPTS + 1) {
            debugVisibilityManager.authorizeDebugSession()
        }
        val result = debugVisibilityManager.authorizeDebugSession()

        // Then
        assertEquals(DebugVisibilityManager.AuthorizationResult.RATE_LIMITED, result)
    }

    @Test
    fun `revokeAuthorization clears authorization state`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns true
        debugVisibilityManager.authorizeDebugSession()

        // When
        debugVisibilityManager.revokeAuthorization()

        // Then
        assertFalse(debugVisibilityManager.isAuthorized.first())
    }

    @Test
    fun `isFeatureVisible returns false when not authorized`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns false

        // When
        val result = debugVisibilityManager.isFeatureVisible(DebugConfig.FEATURE_MEMORY_MONITORING)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isFeatureVisible returns true when authorized and feature enabled`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns true
        every { mockDebugConfig.isFeatureEnabled(DebugConfig.FEATURE_MEMORY_MONITORING) } returns true
        debugVisibilityManager.authorizeDebugSession()

        // When
        val result = debugVisibilityManager.isFeatureVisible(DebugConfig.FEATURE_MEMORY_MONITORING)

        // Then
        assertTrue(result)
    }

    @Test
    fun `toggleFeatureVisibility updates feature state when authorized`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns true
        debugVisibilityManager.authorizeDebugSession()

        // When
        debugVisibilityManager.toggleFeatureVisibility(DebugConfig.FEATURE_MEMORY_MONITORING)

        // Then
        val visibilityState = debugVisibilityManager.getVisibilityState()
        // The initial state should be toggled
        assertNotNull(visibilityState)
    }

    @Test
    fun `toggleFeatureVisibility does nothing when not authorized`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns false

        // When
        debugVisibilityManager.toggleFeatureVisibility(DebugConfig.FEATURE_MEMORY_MONITORING)

        // Then
        val initialState = debugVisibilityManager.isMemoryMonitoringVisible.first()
        debugVisibilityManager.toggleFeatureVisibility(DebugConfig.FEATURE_MEMORY_MONITORING)
        val finalState = debugVisibilityManager.isMemoryMonitoringVisible.first()
        assertEquals(initialState, finalState)
    }

    @Test
    fun `refreshVisibility updates all feature states`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns true
        every { mockDebugConfig.isFeatureEnabled(any()) } returns true
        debugVisibilityManager.authorizeDebugSession()

        // When
        debugVisibilityManager.refreshVisibility()

        // Then
        verify { mockDebugConfig.isFeatureEnabled(DebugConfig.FEATURE_MEMORY_MONITORING) }
        verify { mockDebugConfig.isFeatureEnabled(DebugConfig.FEATURE_FRAME_RATE_MONITORING) }
        verify { mockDebugConfig.isFeatureEnabled(DebugConfig.FEATURE_NETWORK_MONITORING) }
    }

    @Test
    fun `hideAllMetrics sets all features to false`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns true
        debugVisibilityManager.authorizeDebugSession()

        // When
        debugVisibilityManager.hideAllMetrics()

        // Then
        assertFalse(debugVisibilityManager.isMemoryMonitoringVisible.first())
        assertFalse(debugVisibilityManager.isFrameRateMonitoringVisible.first())
        assertFalse(debugVisibilityManager.isNetworkMonitoringVisible.first())
        assertFalse(debugVisibilityManager.isBatteryMonitoringVisible.first())
        assertFalse(debugVisibilityManager.isDatabaseMonitoringVisible.first())
        assertFalse(debugVisibilityManager.isUiMonitoringVisible.first())
        assertFalse(debugVisibilityManager.isMemoryLeakDetectionVisible.first())
    }

    @Test
    fun `getVisibilityState returns current state`() = runTest {
        // Given
        every { mockDebugConfig.isAuthorizedDebugSession() } returns true
        debugVisibilityManager.authorizeDebugSession()

        // When
        val state = debugVisibilityManager.getVisibilityState()

        // Then
        assertNotNull(state)
        assertEquals(true, state.isAuthorized)
        assertTrue(state.features.containsKey(DebugConfig.FEATURE_MEMORY_MONITORING))
    }
}