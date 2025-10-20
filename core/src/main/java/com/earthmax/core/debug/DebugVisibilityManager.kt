package com.earthmax.core.debug

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure visibility manager for performance metrics in debug mode.
 * Provides centralized control over debug feature visibility with authorization checks.
 */
@Singleton
class DebugVisibilityManager @Inject constructor(
    private val debugConfig: DebugConfig
) {
    
    companion object {
        private const val TAG = "DebugVisibilityManager"
        private const val MAX_AUTHORIZATION_ATTEMPTS = 3
        private const val AUTHORIZATION_COOLDOWN_MS = 30000L // 30 seconds
    }
    
    private val _isPerformanceDashboardVisible = MutableStateFlow(false)
    val isPerformanceDashboardVisible: StateFlow<Boolean> = _isPerformanceDashboardVisible.asStateFlow()
    
    private val _isMemoryMonitoringVisible = MutableStateFlow(false)
    val isMemoryMonitoringVisible: StateFlow<Boolean> = _isMemoryMonitoringVisible.asStateFlow()
    
    private val _isFrameTrackingVisible = MutableStateFlow(false)
    val isFrameTrackingVisible: StateFlow<Boolean> = _isFrameTrackingVisible.asStateFlow()
    
    private val _isNetworkMonitoringVisible = MutableStateFlow(false)
    val isNetworkMonitoringVisible: StateFlow<Boolean> = _isNetworkMonitoringVisible.asStateFlow()
    
    private val _isBatteryTrackingVisible = MutableStateFlow(false)
    val isBatteryTrackingVisible: StateFlow<Boolean> = _isBatteryTrackingVisible.asStateFlow()
    
    private val _isDatabaseOptimizationVisible = MutableStateFlow(false)
    val isDatabaseOptimizationVisible: StateFlow<Boolean> = _isDatabaseOptimizationVisible.asStateFlow()
    
    private val _isUIPerformanceVisible = MutableStateFlow(false)
    val isUIPerformanceVisible: StateFlow<Boolean> = _isUIPerformanceVisible.asStateFlow()
    
    private val _isMemoryLeakDetectionVisible = MutableStateFlow(false)
    val isMemoryLeakDetectionVisible: StateFlow<Boolean> = _isMemoryLeakDetectionVisible.asStateFlow()
    
    // Security tracking
    private var authorizationAttempts = 0
    private var lastAuthorizationAttempt = 0L
    private var isAuthorized = false
    
    init {
        // Initialize visibility based on debug configuration
        refreshVisibility()
    }
    
    /**
     * Attempts to authorize debug session with security checks
     */
    fun authorizeDebugSession(): AuthorizationResult {
        val currentTime = System.currentTimeMillis()
        
        // Check cooldown period
        if (authorizationAttempts >= MAX_AUTHORIZATION_ATTEMPTS && 
            currentTime - lastAuthorizationAttempt < AUTHORIZATION_COOLDOWN_MS) {
            Log.w(TAG, "Authorization blocked due to cooldown period")
            return AuthorizationResult.BLOCKED_COOLDOWN
        }
        
        // Reset attempts after cooldown
        if (currentTime - lastAuthorizationAttempt >= AUTHORIZATION_COOLDOWN_MS) {
            authorizationAttempts = 0
        }
        
        lastAuthorizationAttempt = currentTime
        authorizationAttempts++
        
        // Check authorization
        if (debugConfig.isAuthorizedDebugSession) {
            isAuthorized = true
            authorizationAttempts = 0 // Reset on success
            refreshVisibility()
            
            Log.d(TAG, "Debug session authorized successfully")
            logDebugSessionInfo()
            
            return AuthorizationResult.SUCCESS
        } else {
            isAuthorized = false
            hideAllMetrics()
            
            Log.w(TAG, "Debug session authorization failed (attempt $authorizationAttempts)")
            return AuthorizationResult.FAILED
        }
    }
    
    /**
     * Revokes debug session authorization
     */
    fun revokeAuthorization() {
        isAuthorized = false
        hideAllMetrics()
        Log.d(TAG, "Debug session authorization revoked")
    }
    
    /**
     * Checks if a specific feature should be visible
     */
    fun isFeatureVisible(feature: String): Boolean {
        return isAuthorized && debugConfig.isFeatureEnabled(feature)
    }
    
    /**
     * Toggles visibility of a specific feature (with authorization check)
     */
    fun toggleFeatureVisibility(feature: String): Boolean {
        if (!isAuthorized) {
            Log.w(TAG, "Unauthorized attempt to toggle feature: $feature")
            return false
        }
        
        return try {
            val currentState = debugConfig.isFeatureEnabled(feature)
            debugConfig.setFeatureEnabled(feature, !currentState)
            refreshVisibility()
            
            Log.d(TAG, "Feature $feature visibility toggled to ${!currentState}")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while toggling feature: $feature", e)
            false
        }
    }
    
    /**
     * Gets current visibility state for all features
     */
    fun getVisibilityState(): DebugVisibilityState {
        return DebugVisibilityState(
            isAuthorized = isAuthorized,
            performanceDashboard = _isPerformanceDashboardVisible.value,
            memoryMonitoring = _isMemoryMonitoringVisible.value,
            frameTracking = _isFrameTrackingVisible.value,
            networkMonitoring = _isNetworkMonitoringVisible.value,
            batteryTracking = _isBatteryTrackingVisible.value,
            databaseOptimization = _isDatabaseOptimizationVisible.value,
            uiPerformance = _isUIPerformanceVisible.value,
            memoryLeakDetection = _isMemoryLeakDetectionVisible.value
        )
    }    
    /**
     * Refreshes visibility state based on current debug configuration
     */
    private fun refreshVisibility() {
        if (!isAuthorized) {
            hideAllMetrics()
            return
        }
        
        _isPerformanceDashboardVisible.value = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_PERFORMANCE_DASHBOARD)
        _isMemoryMonitoringVisible.value = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_MEMORY_MONITORING)
        _isFrameTrackingVisible.value = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_FRAME_TRACKING)
        _isNetworkMonitoringVisible.value = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_NETWORK_MONITORING)
        _isBatteryTrackingVisible.value = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_BATTERY_TRACKING)
        _isDatabaseOptimizationVisible.value = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_DATABASE_OPTIMIZATION)
        _isUIPerformanceVisible.value = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_UI_PERFORMANCE)
        _isMemoryLeakDetectionVisible.value = debugConfig.isFeatureEnabled(DebugConfig.FEATURE_MEMORY_LEAK_DETECTION)
    }
    
    /**
     * Hides all performance metrics
     */
    private fun hideAllMetrics() {
        _isPerformanceDashboardVisible.value = false
        _isMemoryMonitoringVisible.value = false
        _isFrameTrackingVisible.value = false
        _isNetworkMonitoringVisible.value = false
        _isBatteryTrackingVisible.value = false
        _isDatabaseOptimizationVisible.value = false
        _isUIPerformanceVisible.value = false
        _isMemoryLeakDetectionVisible.value = false
    }
    
    /**
     * Logs debug session information for audit purposes
     */
    private fun logDebugSessionInfo() {
        val sessionInfo = debugConfig.getDebugSessionInfo()
        Log.d(TAG, "Debug Session Info: $sessionInfo")
    }
}

/**
 * Result of authorization attempt
 */
enum class AuthorizationResult {
    SUCCESS,
    FAILED,
    BLOCKED_COOLDOWN
}

/**
 * Current visibility state of all debug features
 */
data class DebugVisibilityState(
    val isAuthorized: Boolean,
    val performanceDashboard: Boolean,
    val memoryMonitoring: Boolean,
    val frameTracking: Boolean,
    val networkMonitoring: Boolean,
    val batteryTracking: Boolean,
    val databaseOptimization: Boolean,
    val uiPerformance: Boolean,
    val memoryLeakDetection: Boolean
)