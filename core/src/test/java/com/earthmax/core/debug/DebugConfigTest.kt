package com.earthmax.core.debug

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class DebugConfigTest {

    private lateinit var mockContext: Context
    private lateinit var debugConfig: DebugConfig

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        debugConfig = DebugConfig(mockContext)
        
        // Mock static methods
        mockkStatic(Settings.Global::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isDebugBuild returns true when application is debuggable`() {
        // Given
        val mockApplicationInfo = mockk<ApplicationInfo>()
        mockApplicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE
        every { mockContext.applicationInfo } returns mockApplicationInfo

        // When
        val result = debugConfig.isDebugBuild()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isDebugBuild returns false when application is not debuggable`() {
        // Given
        val mockApplicationInfo = mockk<ApplicationInfo>()
        mockApplicationInfo.flags = 0
        every { mockContext.applicationInfo } returns mockApplicationInfo

        // When
        val result = debugConfig.isDebugBuild()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isDeveloperModeEnabled returns true when developer options are enabled`() {
        // Given
        every { 
            Settings.Global.getInt(
                mockContext.contentResolver, 
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 
                0
            ) 
        } returns 1

        // When
        val result = debugConfig.isDeveloperModeEnabled()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isDeveloperModeEnabled returns false when developer options are disabled`() {
        // Given
        every { 
            Settings.Global.getInt(
                mockContext.contentResolver, 
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 
                0
            ) 
        } returns 0

        // When
        val result = debugConfig.isDeveloperModeEnabled()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isUsbDebuggingEnabled returns true when USB debugging is enabled`() {
        // Given
        every { 
            Settings.Global.getInt(
                mockContext.contentResolver, 
                Settings.Global.ADB_ENABLED, 
                0
            ) 
        } returns 1

        // When
        val result = debugConfig.isUsbDebuggingEnabled()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isAuthorizedDebugSession returns true when all conditions are met`() {
        // Given
        val mockApplicationInfo = mockk<ApplicationInfo>()
        mockApplicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE
        every { mockContext.applicationInfo } returns mockApplicationInfo
        
        every { 
            Settings.Global.getInt(
                mockContext.contentResolver, 
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 
                0
            ) 
        } returns 1
        
        every { 
            Settings.Global.getInt(
                mockContext.contentResolver, 
                Settings.Global.ADB_ENABLED, 
                0
            ) 
        } returns 1

        // When
        val result = debugConfig.isAuthorizedDebugSession()

        // Then
        assertTrue(result)
    }

    @Test
    fun `shouldShowPerformanceMetrics returns true when authorized debug session`() {
        // Given
        val mockApplicationInfo = mockk<ApplicationInfo>()
        mockApplicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE
        every { mockContext.applicationInfo } returns mockApplicationInfo
        
        every { 
            Settings.Global.getInt(
                mockContext.contentResolver, 
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 
                0
            ) 
        } returns 1
        
        every { 
            Settings.Global.getInt(
                mockContext.contentResolver, 
                Settings.Global.ADB_ENABLED, 
                0
            ) 
        } returns 1

        // When
        val result = debugConfig.shouldShowPerformanceMetrics()

        // Then
        assertTrue(result)
    }

    @Test
    fun `shouldShowPerformanceMetrics returns false in release build`() {
        // Given
        val mockApplicationInfo = mockk<ApplicationInfo>()
        mockApplicationInfo.flags = 0 // Not debuggable
        every { mockContext.applicationInfo } returns mockApplicationInfo

        // When
        val result = debugConfig.shouldShowPerformanceMetrics()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isFeatureEnabled returns true for enabled features in debug mode`() {
        // Given
        val mockApplicationInfo = mockk<ApplicationInfo>()
        mockApplicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE
        every { mockContext.applicationInfo } returns mockApplicationInfo

        // When
        val result = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_MEMORY_MONITORING)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isFeatureEnabled returns false for features in release mode`() {
        // Given
        val mockApplicationInfo = mockk<ApplicationInfo>()
        mockApplicationInfo.flags = 0 // Not debuggable
        every { mockContext.applicationInfo } returns mockApplicationInfo

        // When
        val result = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_MEMORY_MONITORING)

        // Then
        assertFalse(result)
    }
}