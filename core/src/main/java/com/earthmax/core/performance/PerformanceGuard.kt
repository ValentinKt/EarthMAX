package com.earthmax.core.performance

import com.earthmax.core.debug.DebugConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance guard that prevents monitoring overhead in release builds
 * and ensures performance metrics are only collected in authorized debug sessions.
 */
@Singleton
class PerformanceGuard @Inject constructor(
    private val debugConfig: DebugConfig
) {
    
    /**
     * Checks if performance monitoring should be enabled
     */
    fun shouldEnableMonitoring(): Boolean {
        return debugConfig.shouldShowPerformanceMetrics
    }
    
    /**
     * Guards a performance monitoring operation
     * Returns the result only if monitoring is enabled, otherwise returns default value
     */
    fun <T> guardOperation(
        defaultValue: T,
        operation: () -> T
    ): T {
        return if (shouldEnableMonitoring()) {
            try {
                operation()
            } catch (e: Exception) {
                // Log error in debug mode only
                if (debugConfig.isDebugBuild) {
                    android.util.Log.w("PerformanceGuard", "Performance operation failed", e)
                }
                defaultValue
            }
        } else {
            defaultValue
        }
    }
    
    /**
     * Guards a Flow-based performance monitoring operation
     * Returns the flow only if monitoring is enabled, otherwise returns empty flow
     */
    fun <T> guardFlow(
        operation: () -> Flow<T>
    ): Flow<T> {
        return if (shouldEnableMonitoring()) {
            try {
                operation()
            } catch (e: Exception) {
                if (debugConfig.isDebugBuild) {
                    android.util.Log.w("PerformanceGuard", "Performance flow operation failed", e)
                }
                emptyFlow()
            }
        } else {
            emptyFlow()
        }
    }    
    /**
     * Guards a suspend function performance monitoring operation
     */
    suspend fun <T> guardSuspendOperation(
        defaultValue: T,
        operation: suspend () -> T
    ): T {
        return if (shouldEnableMonitoring()) {
            try {
                operation()
            } catch (e: Exception) {
                if (debugConfig.isDebugBuild) {
                    android.util.Log.w("PerformanceGuard", "Suspend performance operation failed", e)
                }
                defaultValue
            }
        } else {
            defaultValue
        }
    }
    
    /**
     * Executes a performance monitoring operation only if enabled
     * Does nothing if monitoring is disabled
     */
    fun executeIfEnabled(operation: () -> Unit) {
        if (shouldEnableMonitoring()) {
            try {
                operation()
            } catch (e: Exception) {
                if (debugConfig.isDebugBuild) {
                    android.util.Log.w("PerformanceGuard", "Performance execution failed", e)
                }
            }
        }
    }    
    /**
     * Creates a guarded performance metric collector
     */
    fun <T> createGuardedCollector(
        defaultValue: T,
        collector: () -> T
    ): () -> T = {
        guardOperation(defaultValue, collector)
    }
    
    /**
     * Checks if specific performance feature should be enabled
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return shouldEnableMonitoring() && debugConfig.isFeatureEnabled(feature)
    }
    
    /**
     * Guards performance data collection with rate limiting
     */
    private var lastCollectionTime = 0L
    private val minCollectionInterval = 100L // 100ms minimum interval
    
    fun shouldCollectNow(): Boolean {
        if (!shouldEnableMonitoring()) return false
        
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastCollectionTime >= minCollectionInterval) {
            lastCollectionTime = currentTime
            true
        } else {
            false
        }
    }    
    /**
     * Performance monitoring state
     */
    data class MonitoringState(
        val isEnabled: Boolean,
        val authorizedSession: Boolean,
        val debugBuild: Boolean,
        val features: Map<String, Boolean>
    )
    
    /**
     * Gets current monitoring state
     */
    fun getMonitoringState(): MonitoringState {
        return MonitoringState(
            isEnabled = shouldEnableMonitoring(),
            authorizedSession = debugConfig.isAuthorizedDebugSession,
            debugBuild = debugConfig.isDebugBuild,
            features = mapOf(
                "FEATURE_MEMORY_MONITORING" to false,
                "FEATURE_FRAME_RATE_MONITORING" to false,
                "FEATURE_NETWORK_MONITORING" to false,
                "FEATURE_BATTERY_MONITORING" to false,
                "FEATURE_DATABASE_MONITORING" to false,
                "FEATURE_UI_MONITORING" to false,
                "FEATURE_MEMORY_LEAK_DETECTION" to false
            )
        )
    }
}