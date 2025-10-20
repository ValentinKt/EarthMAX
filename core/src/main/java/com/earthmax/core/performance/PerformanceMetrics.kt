package com.earthmax.core.performance

/**
 * Data classes for performance metrics
 */

data class MemoryMetrics(
    val usedMemory: Long,
    val totalMemory: Long,
    val availableMemory: Long,
    val gcCount: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val usagePercentage: Float
        get() = if (totalMemory > 0) (usedMemory.toFloat() / totalMemory) * 100f else 0f
}

data class FrameRateMetrics(
    val currentFps: Float,
    val averageFps: Float,
    val droppedFrames: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class NetworkMetrics(
    val latency: Long,
    val downloadSpeed: Float,
    val uploadSpeed: Float,
    val bytesReceived: Long,
    val bytesSent: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class BatteryMetrics(
    val level: Int,
    val temperature: Float,
    val voltage: Float,
    val isCharging: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class DatabaseMetrics(
    val queryCount: Int,
    val averageQueryTime: Long,
    val slowQueries: Int,
    val cacheHitRate: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class UiMetrics(
    val renderTime: Long,
    val layoutTime: Long,
    val drawTime: Long,
    val inputLatency: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class PerformanceSummary(
    val memoryUsage: Float,
    val frameRate: Float,
    val networkLatency: Long,
    val batteryLevel: Int,
    val databasePerformance: Float,
    val uiPerformance: Float,
    val timestamp: Long
) {
    companion object {
        fun empty(): PerformanceSummary = PerformanceSummary(
            memoryUsage = 0f,
            frameRate = 0f,
            networkLatency = 0L,
            batteryLevel = 0,
            databasePerformance = 0f,
            uiPerformance = 0f,
            timestamp = System.currentTimeMillis()
        )
    }
}