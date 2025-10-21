package com.earthmax.performance

import android.content.Context
import javax.inject.Inject

/**
 * Lightweight performance monitor used by unit tests.
 * Coordinates trackers and computes simple scores, issues, recommendations, and snapshots.
 */
class NewPerformanceMonitor @Inject constructor(
    private val context: Context,
    private val frameTimeTracker: FrameTimeTracker,
    private val memoryTracker: MemoryTracker,
    private val networkTracker: NetworkTracker,
    private val batteryTracker: BatteryTracker
) {
    private var monitoring = false

    fun startMonitoring() {
        if (monitoring) return
        monitoring = true
        frameTimeTracker.startTracking()
        memoryTracker.startTracking()
        networkTracker.startTracking()
        batteryTracker.startTracking()
    }

    fun stopMonitoring() {
        if (!monitoring) return
        monitoring = false
        frameTimeTracker.stopTracking()
        memoryTracker.stopTracking()
        networkTracker.stopTracking()
        batteryTracker.stopTracking()
    }

    fun isMonitoring(): Boolean = monitoring

    // Scores
    fun getMemoryScore(): Double {
        val usage = memoryTracker.getMemoryUsagePercentage()
        return when {
            usage <= 30f -> 85.0
            usage <= 40f -> 80.0
            usage <= 60f -> 60.0
            usage <= 80f -> 40.0
            else -> 20.0
        }
    }

    fun getFrameRateScore(): Double {
        val fps = frameTimeTracker.getAverageFps()
        return when {
            fps >= 55f -> 95.0
            fps >= 45f -> 75.0
            fps >= 30f -> 50.0
            fps >= 20f -> 35.0
            else -> 20.0
        }
    }

    fun getNetworkScore(): Double {
        val rt = networkTracker.getAverageResponseTime()
        return when {
            rt < 250L -> 95.0
            rt < 800L -> 75.0
            rt < 1500L -> 60.0
            rt < 2500L -> 40.0
            else -> 25.0
        }
    }

    fun getBatteryScore(): Double {
        val level = batteryTracker.getBatteryLevel()
        return when {
            level >= 80 -> 90.0
            level >= 60 -> 75.0
            level >= 40 -> 60.0
            level >= 20 -> 40.0
            else -> 20.0
        }
    }

    fun getOverallScore(): Double {
        val memory = getMemoryScore()
        val frame = getFrameRateScore()
        val network = getNetworkScore()
        val battery = getBatteryScore()
        return (memory + frame + network + battery) / 4.0
    }

    fun getPerformanceIssues(): List<String> {
        val issues = mutableListOf<String>()
        val memoryUsage = memoryTracker.getMemoryUsagePercentage()
        val fps = frameTimeTracker.getAverageFps()
        val responseTime = networkTracker.getAverageResponseTime()
        val batteryLevel = batteryTracker.getBatteryLevel()

        if (memoryUsage > 80f) issues.add("High memory usage detected")
        if (fps < 30f) issues.add("Low frame rate detected")
        if (responseTime > 2000L) issues.add("Slow network response detected")
        if (batteryLevel < 20) issues.add("Low battery level detected")

        return issues
    }

    fun getRecommendations(): List<String> {
        val recs = mutableListOf<String>()
        val memoryUsage = memoryTracker.getMemoryUsagePercentage()
        val fps = frameTimeTracker.getAverageFps()
        val responseTime = networkTracker.getAverageResponseTime()
        val batteryLevel = batteryTracker.getBatteryLevel()

        if (memoryUsage > 80f) {
            recs.add("Memory optimization: reduce allocations, use caching, check for leaks")
        }
        if (fps < 30f) {
            recs.add("Frame rate optimization: simplify UI layouts, offload heavy work from main thread")
        }
        if (responseTime > 2000L) {
            recs.add("Network optimization: enable caching, batch requests, compress payloads")
        }
        if (batteryLevel < 20) {
            recs.add("Battery optimization: enable power saving and reduce background activity")
        }

        // If all metrics are good, return empty list
        if (recs.isEmpty()) return emptyList()
        return recs
    }

    fun createSnapshot(): PerformanceSnapshot {
        val snapshot = PerformanceSnapshot(
            timestamp = System.currentTimeMillis(),
            overallScore = getOverallScore(),
            memoryScore = getMemoryScore(),
            frameRateScore = getFrameRateScore(),
            networkScore = getNetworkScore(),
            batteryScore = getBatteryScore()
        )
        return snapshot
    }

    fun reset() {
        frameTimeTracker.reset()
        memoryTracker.reset()
        networkTracker.reset()
        batteryTracker.reset()
        monitoring = false
    }
}

/**
 * Snapshot of performance scores captured at a point in time.
 */
data class PerformanceSnapshot(
    val timestamp: Long,
    val overallScore: Double,
    val memoryScore: Double,
    val frameRateScore: Double,
    val networkScore: Double,
    val batteryScore: Double
)