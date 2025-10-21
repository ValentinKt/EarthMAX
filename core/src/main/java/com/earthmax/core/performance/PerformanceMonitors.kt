package com.earthmax.core.performance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub interfaces for performance monitors to avoid circular dependencies
 */

interface MemoryMonitor {
    fun collectMetrics(): Flow<MemoryMetrics>
    fun startMonitoring()
    fun stopMonitoring()
    fun getCurrentUsage(): Float
}

interface FrameRateMonitor {
    fun collectMetrics(): Flow<FrameRateMetrics>
    fun startMonitoring()
    fun stopMonitoring()
}

interface NetworkMonitor {
    fun collectMetrics(): Flow<NetworkMetrics>
    fun startMonitoring()
    fun stopMonitoring()
}

interface BatteryMonitor {
    fun collectMetrics(): Flow<BatteryMetrics>
    fun startMonitoring()
    fun stopMonitoring()
}

interface DatabaseMonitor {
    fun collectMetrics(): Flow<DatabaseMetrics>
    fun startMonitoring()
    fun stopMonitoring()
}

interface UiMonitor {
    fun collectMetrics(): Flow<UiMetrics>
    fun startMonitoring()
    fun stopMonitoring()
}

/**
 * No-op implementations for stub monitors
 */

@Singleton
class NoOpMemoryMonitor @Inject constructor() : MemoryMonitor {
    override fun collectMetrics(): Flow<MemoryMetrics> = emptyFlow()
    override fun startMonitoring() {}
    override fun stopMonitoring() {}
    override fun getCurrentUsage(): Float = 0f
}

@Singleton
class NoOpFrameRateMonitor @Inject constructor() : FrameRateMonitor {
    override fun collectMetrics(): Flow<FrameRateMetrics> = emptyFlow()
    override fun startMonitoring() {}
    override fun stopMonitoring() {}
}

@Singleton
class NoOpNetworkMonitor @Inject constructor() : NetworkMonitor {
    override fun collectMetrics(): Flow<NetworkMetrics> = emptyFlow()
    override fun startMonitoring() {}
    override fun stopMonitoring() {}
}

@Singleton
class NoOpBatteryMonitor @Inject constructor() : BatteryMonitor {
    override fun collectMetrics(): Flow<BatteryMetrics> = emptyFlow()
    override fun startMonitoring() {}
    override fun stopMonitoring() {}
}

@Singleton
class NoOpDatabaseMonitor @Inject constructor() : DatabaseMonitor {
    override fun collectMetrics(): Flow<DatabaseMetrics> = emptyFlow()
    override fun startMonitoring() {}
    override fun stopMonitoring() {}
}

@Singleton
class NoOpUiMonitor @Inject constructor() : UiMonitor {
    override fun collectMetrics(): Flow<UiMetrics> = emptyFlow()
    override fun startMonitoring() {}
    override fun stopMonitoring() {}
}