package com.earthmax.core.performance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarded performance collector that wraps existing performance monitoring
 * components to prevent overhead in release builds.
 */
@Singleton
class GuardedPerformanceCollector @Inject constructor(
    private val performanceGuard: PerformanceGuard,
    private val memoryMonitor: MemoryMonitor,
    private val frameRateMonitor: FrameRateMonitor,
    private val networkMonitor: NetworkMonitor,
    private val batteryMonitor: BatteryMonitor,
    private val databaseMonitor: DatabaseMonitor,
    private val uiMonitor: UiMonitor
) {
    
    /**
     * Collects memory metrics with guard protection
     */
    fun collectMemoryMetrics(): Flow<MemoryMetrics> {
        return performanceGuard.guardFlow {
            memoryMonitor.collectMetrics()
        }
    }
    
    /**
     * Collects frame rate metrics with guard protection
     */
    fun collectFrameRateMetrics(): Flow<FrameRateMetrics> {
        return performanceGuard.guardFlow {
            frameRateMonitor.collectMetrics()
        }
    }
    
    /**
     * Collects network metrics with guard protection
     */
    fun collectNetworkMetrics(): Flow<NetworkMetrics> {
        return performanceGuard.guardFlow {
            networkMonitor.collectMetrics()
        }
    }
    
    /**
     * Collects battery metrics with guard protection
     */
    fun collectBatteryMetrics(): Flow<BatteryMetrics> {
        return performanceGuard.guardFlow {
            batteryMonitor.collectMetrics()
        }
    }
    
    /**
     * Collects database metrics with guard protection
     */
    fun collectDatabaseMetrics(): Flow<DatabaseMetrics> {
        return performanceGuard.guardFlow {
            databaseMonitor.collectMetrics()
        }
    }
    
    /**
     * Collects UI metrics with guard protection
     */
    fun collectUiMetrics(): Flow<UiMetrics> {
        return performanceGuard.guardFlow {
            uiMonitor.collectMetrics()
        }
    }
    
    /**
     * Starts all performance monitoring with guard protection
     */
    fun startMonitoring() {
        performanceGuard.executeIfEnabled {
            memoryMonitor.startMonitoring()
            frameRateMonitor.startMonitoring()
            networkMonitor.startMonitoring()
            batteryMonitor.startMonitoring()
            databaseMonitor.startMonitoring()
            uiMonitor.startMonitoring()
        }
    }
    
    /**
     * Stops all performance monitoring
     */
    fun stopMonitoring() {
        // Always allow stopping monitoring regardless of guard state
        memoryMonitor.stopMonitoring()
        frameRateMonitor.stopMonitoring()
        networkMonitor.stopMonitoring()
        batteryMonitor.stopMonitoring()
        databaseMonitor.stopMonitoring()
        uiMonitor.stopMonitoring()
    }
    
    /**
     * Gets current performance summary with guard protection
     */
    fun getPerformanceSummary(): PerformanceSummary {
        return performanceGuard.guardOperation(
            defaultValue = PerformanceSummary.empty()
        ) {
            PerformanceSummary(
                memoryUsage = memoryMonitor.getCurrentUsage(),
                frameRate = 60.0f,
                networkLatency = 0,
                batteryLevel = 100,
                databasePerformance = 0.0f,
                uiPerformance = 0.0f,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}