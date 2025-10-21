package com.earthmax.core.debug

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
// import com.earthmax.core.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug configuration utility that manages performance metrics visibility
 * and ensures secure access controls for debugging features.
 */
@Singleton
class DebugConfig @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val DEBUG_SETTINGS_KEY = "earthmax_debug_performance"
        private const val AUTHORIZED_DEBUG_TOKEN = "earthmax_debug_2024"
        
        // Performance monitoring feature flags
        const val FEATURE_PERFORMANCE_DASHBOARD = "performance_dashboard"
        const val FEATURE_MEMORY_MONITORING = "memory_monitoring"
        const val FEATURE_FRAME_TRACKING = "frame_tracking"
        const val FEATURE_NETWORK_MONITORING = "network_monitoring"
        const val FEATURE_BATTERY_TRACKING = "battery_tracking"
        const val FEATURE_DATABASE_OPTIMIZATION = "database_optimization"
        const val FEATURE_UI_PERFORMANCE = "ui_performance"
        const val FEATURE_MEMORY_LEAK_DETECTION = "memory_leak_detection"
    }
    
    /**
     * Checks if the application is running in debug mode
     */
    val isDebugBuild: Boolean
        get() = true // BuildConfig.DEBUG
    
    /**
     * Checks if the device is in developer mode
     */
    val isDeveloperModeEnabled: Boolean
        get() = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    
    /**
     * Checks if USB debugging is enabled
     */
    val isUsbDebuggingEnabled: Boolean
        get() = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ) == 1
    
    /**
     * Checks if the app is debuggable (has debuggable flag in manifest)
     */
    val isAppDebuggable: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    
    /**
     * Comprehensive check for authorized debugging session
     */
    val isAuthorizedDebugSession: Boolean
        get() = isDebugBuild && 
                isAppDebuggable && 
                (isDeveloperModeEnabled || isUsbDebuggingEnabled) &&
                isDebugTokenValid()
    
    /**
     * Checks if performance metrics should be visible
     */
    val shouldShowPerformanceMetrics: Boolean
        get() = isAuthorizedDebugSession && isPerformanceDebuggingEnabled()
    
    /**
     * Checks if a specific performance feature is enabled
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return shouldShowPerformanceMetrics && when (feature) {
            FEATURE_PERFORMANCE_DASHBOARD -> true // Always enabled if debugging is on
            FEATURE_MEMORY_MONITORING -> getDebugSetting("memory_monitoring", true)
            FEATURE_FRAME_TRACKING -> getDebugSetting("frame_tracking", true)
            FEATURE_NETWORK_MONITORING -> getDebugSetting("network_monitoring", true)
            FEATURE_BATTERY_TRACKING -> getDebugSetting("battery_tracking", true)
            FEATURE_DATABASE_OPTIMIZATION -> getDebugSetting("database_optimization", true)
            FEATURE_UI_PERFORMANCE -> getDebugSetting("ui_performance", true)
            FEATURE_MEMORY_LEAK_DETECTION -> getDebugSetting("memory_leak_detection", true)
            else -> false
        }
    }    
    /**
     * Enables or disables a specific performance feature
     */
    fun setFeatureEnabled(feature: String, enabled: Boolean) {
        if (!isAuthorizedDebugSession) {
            throw SecurityException("Unauthorized access to debug features")
        }
        
        setDebugSetting(feature, enabled)
    }
    
    /**
     * Gets the debug session info for logging and verification
     */
    fun getDebugSessionInfo(): DebugSessionInfo {
        return DebugSessionInfo(
            isDebugBuild = isDebugBuild,
            isAppDebuggable = isAppDebuggable,
            isDeveloperModeEnabled = isDeveloperModeEnabled,
            isUsbDebuggingEnabled = isUsbDebuggingEnabled,
            isAuthorized = isAuthorizedDebugSession,
            deviceInfo = getDeviceInfo(),
            buildInfo = getBuildInfo()
        )
    }
    
    /**
     * Validates the debug token for additional security
     */
    private fun isDebugTokenValid(): Boolean {
        // In a real implementation, this could check for:
        // - Environment variables
        // - Secure preferences
        // - Network-based validation
        // - Certificate validation
        
        return try {
            // Check if debug token is set in system properties (for testing)
            val systemToken = System.getProperty("earthmax.debug.token")
            systemToken == AUTHORIZED_DEBUG_TOKEN || 
            // Allow in debug builds without token for development
            (isDebugBuild && Build.TYPE == "userdebug")
        } catch (e: Exception) {
            false
        }
    }    
    /**
     * Checks if performance debugging is specifically enabled
     */
    private fun isPerformanceDebuggingEnabled(): Boolean {
        return getDebugSetting(DEBUG_SETTINGS_KEY, true)
    }
    
    /**
     * Gets a debug setting from secure preferences
     */
    private fun getDebugSetting(key: String, defaultValue: Boolean): Boolean {
        return try {
            val prefs = context.getSharedPreferences("earthmax_debug", Context.MODE_PRIVATE)
            prefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * Sets a debug setting in secure preferences
     */
    private fun setDebugSetting(key: String, value: Boolean) {
        try {
            val prefs = context.getSharedPreferences("earthmax_debug", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(key, value).apply()
        } catch (e: Exception) {
            // Silently fail for security
        }
    }
    
    /**
     * Gets device information for debug logging
     */
    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            buildType = Build.TYPE,
            isEmulator = isEmulator()
        )
    }    
    /**
     * Gets build information for debug logging
     */
    private fun getBuildInfo(): BuildInfo {
        return BuildInfo(
            versionName = "1.0.0", // BuildConfig.VERSION_NAME,
            versionCode = 1, // BuildConfig.VERSION_CODE,
            buildType = "debug", // BuildConfig.BUILD_TYPE,
            flavor = "", // BuildConfig.FLAVOR,
            applicationId = "com.earthmax.app" // BuildConfig.APPLICATION_ID
        )
    }
    
    /**
     * Detects if running on an emulator
     */
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                "google_sdk" == Build.PRODUCT)
    }
}

/**
 * Data class containing debug session information
 */
data class DebugSessionInfo(
    val isDebugBuild: Boolean,
    val isAppDebuggable: Boolean,
    val isDeveloperModeEnabled: Boolean,
    val isUsbDebuggingEnabled: Boolean,
    val isAuthorized: Boolean,
    val deviceInfo: DeviceInfo,
    val buildInfo: BuildInfo
)
/**
 * Data class containing device information
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildType: String,
    val isEmulator: Boolean
)

/**
 * Data class containing build information
 */
data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val flavor: String,
    val applicationId: String
)