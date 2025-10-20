package com.earthmax.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring system for EarthMAX Android app.
 * Tracks memory usage, frame rates, network performance, and other metrics.
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val frameTimeTracker = FrameTimeTracker()
    private val memoryTracker = MemoryTracker()
    private val networkTracker = NetworkTracker()
    private val batteryTracker = BatteryTracker(context)
    
    private var isMonitoring = false
    private var monitoringStartTime = 0L
    
    // Performance thresholds
    private val frameDropThreshold = 16.67 // 60 FPS threshold in ms
    private val memoryWarningThreshold = 0.8 // 80% of available memory
    private val networkTimeoutThreshold = 5000L // 5 seconds
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        startMonitoring()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        stopMonitoring()
    }
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringStartTime = System.currentTimeMillis()
        
        Log.d(TAG, "Starting performance monitoring")
        
        // Start frame time monitoring
        frameTimeTracker.start()
        
        // Start periodic monitoring tasks
        scope.launch {
            while (isMonitoring) {
                collectMetrics()
                delay(MONITORING_INTERVAL)
            }
        }
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        frameTimeTracker.stop()
        
        Log.d(TAG, "Stopping performance monitoring")
    }
    
    /**
     * Collect all performance metrics
     */
    private suspend fun collectMetrics() {
        val currentMetrics = _performanceMetrics.value
        
        val memoryInfo = memoryTracker.getMemoryInfo()
        val frameInfo = frameTimeTracker.getFrameInfo()
        val networkInfo = networkTracker.getNetworkInfo()
        val batteryInfo = batteryTracker.getBatteryInfo()
        
        val updatedMetrics = currentMetrics.copy(
            timestamp = System.currentTimeMillis(),
            memoryUsage = memoryInfo,
            frameMetrics = frameInfo,
            networkMetrics = networkInfo,
            batteryMetrics = batteryInfo,
            uptime = System.currentTimeMillis() - monitoringStartTime
        )
        
        _performanceMetrics.value = updatedMetrics
        
        // Check for performance issues
        checkPerformanceThresholds(updatedMetrics)
    }
    
    /**
     * Check performance thresholds and log warnings
     */
    private fun checkPerformanceThresholds(metrics: PerformanceMetrics) {
        // Check frame drops
        if (metrics.frameMetrics.averageFrameTime > frameDropThreshold) {
            Log.w(TAG, "Frame drops detected: ${metrics.frameMetrics.averageFrameTime}ms average frame time")
            reportPerformanceIssue(
                PerformanceIssue.FRAME_DROPS,
                "Average frame time: ${metrics.frameMetrics.averageFrameTime}ms"
            )
        }
        
        // Check memory usage
        val memoryUsageRatio = metrics.memoryUsage.usedMemory.toDouble() / metrics.memoryUsage.totalMemory
        if (memoryUsageRatio > memoryWarningThreshold) {
            Log.w(TAG, "High memory usage: ${(memoryUsageRatio * 100).toInt()}%")
            reportPerformanceIssue(
                PerformanceIssue.HIGH_MEMORY_USAGE,
                "Memory usage: ${(memoryUsageRatio * 100).toInt()}%"
            )
        }
        
        // Check network performance
        if (metrics.networkMetrics.averageResponseTime > networkTimeoutThreshold) {
            Log.w(TAG, "Slow network response: ${metrics.networkMetrics.averageResponseTime}ms")
            reportPerformanceIssue(
                PerformanceIssue.SLOW_NETWORK,
                "Average response time: ${metrics.networkMetrics.averageResponseTime}ms"
            )
        }
    }
    
    /**
     * Report performance issue
     */
    private fun reportPerformanceIssue(issue: PerformanceIssue, details: String) {
        // In a real app, this would send to analytics/crash reporting
        Log.w(TAG, "Performance issue detected: $issue - $details")
        
        // Could integrate with Firebase Crashlytics, Bugsnag, etc.
        // crashlytics.recordException(PerformanceException(issue, details))
    }
    
    /**
     * Get current performance summary
     */
    fun getPerformanceSummary(): PerformanceSummary {
        val metrics = _performanceMetrics.value
        
        return PerformanceSummary(
            overallScore = calculateOverallScore(metrics),
            memoryScore = calculateMemoryScore(metrics.memoryUsage),
            frameScore = calculateFrameScore(metrics.frameMetrics),
            networkScore = calculateNetworkScore(metrics.networkMetrics),
            batteryScore = calculateBatteryScore(metrics.batteryMetrics),
            recommendations = generateRecommendations(metrics)
        )
    }
    
    /**
     * Calculate overall performance score (0-100)
     */
    private fun calculateOverallScore(metrics: PerformanceMetrics): Int {
        val memoryScore = calculateMemoryScore(metrics.memoryUsage)
        val frameScore = calculateFrameScore(metrics.frameMetrics)
        val networkScore = calculateNetworkScore(metrics.networkMetrics)
        val batteryScore = calculateBatteryScore(metrics.batteryMetrics)
        
        return (memoryScore + frameScore + networkScore + batteryScore) / 4
    }
    
    private fun calculateMemoryScore(memoryUsage: MemoryUsage): Int {
        val usageRatio = memoryUsage.usedMemory.toDouble() / memoryUsage.totalMemory
        return when {
            usageRatio < 0.5 -> 100
            usageRatio < 0.7 -> 80
            usageRatio < 0.8 -> 60
            usageRatio < 0.9 -> 40
            else -> 20
        }
    }
    
    private fun calculateFrameScore(frameMetrics: FrameMetrics): Int {
        return when {
            frameMetrics.averageFrameTime < 16.67 -> 100 // 60+ FPS
            frameMetrics.averageFrameTime < 33.33 -> 80  // 30+ FPS
            frameMetrics.averageFrameTime < 50.0 -> 60   // 20+ FPS
            frameMetrics.averageFrameTime < 100.0 -> 40  // 10+ FPS
            else -> 20
        }
    }
    
    private fun calculateNetworkScore(networkMetrics: NetworkMetrics): Int {
        return when {
            networkMetrics.averageResponseTime < 500 -> 100
            networkMetrics.averageResponseTime < 1000 -> 80
            networkMetrics.averageResponseTime < 2000 -> 60
            networkMetrics.averageResponseTime < 5000 -> 40
            else -> 20
        }
    }
    
    private fun calculateBatteryScore(batteryMetrics: BatteryMetrics): Int {
        return when {
            batteryMetrics.batteryLevel > 80 -> 100
            batteryMetrics.batteryLevel > 60 -> 80
            batteryMetrics.batteryLevel > 40 -> 60
            batteryMetrics.batteryLevel > 20 -> 40
            else -> 20
        }
    }
    
    /**
     * Generate performance recommendations
     */
    private fun generateRecommendations(metrics: PerformanceMetrics): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Memory recommendations
        val memoryUsageRatio = metrics.memoryUsage.usedMemory.toDouble() / metrics.memoryUsage.totalMemory
        if (memoryUsageRatio > 0.8) {
            recommendations.add("Consider reducing memory usage by optimizing images and caching")
        }
        
        // Frame rate recommendations
        if (metrics.frameMetrics.averageFrameTime > 16.67) {
            recommendations.add("Optimize UI rendering to improve frame rate")
        }
        
        // Network recommendations
        if (metrics.networkMetrics.averageResponseTime > 2000) {
            recommendations.add("Implement request caching and optimize network calls")
        }
        
        // Battery recommendations
        if (metrics.batteryMetrics.batteryLevel < 20) {
            recommendations.add("Enable battery optimization mode")
        }
        
        return recommendations
    }
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MONITORING_INTERVAL = 5000L // 5 seconds
    }
}

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    val timestamp: Long = 0L,
    val memoryUsage: MemoryUsage = MemoryUsage(),
    val frameMetrics: FrameMetrics = FrameMetrics(),
    val networkMetrics: NetworkMetrics = NetworkMetrics(),
    val batteryMetrics: BatteryMetrics = BatteryMetrics(),
    val uptime: Long = 0L
)

/**
 * Memory usage information
 */
data class MemoryUsage(
    val usedMemory: Long = 0L,
    val totalMemory: Long = 0L,
    val availableMemory: Long = 0L,
    val heapSize: Long = 0L,
    val heapUsed: Long = 0L
)

/**
 * Frame rendering metrics
 */
data class FrameMetrics(
    val averageFrameTime: Double = 0.0,
    val frameDrops: Int = 0,
    val totalFrames: Long = 0L,
    val jankFrames: Int = 0
)

/**
 * Network performance metrics
 */
data class NetworkMetrics(
    val averageResponseTime: Long = 0L,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L
)

/**
 * Battery usage metrics
 */
data class BatteryMetrics(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val batteryTemperature: Float = 0f,
    val powerUsage: Double = 0.0
)

/**
 * Performance summary
 */
data class PerformanceSummary(
    val overallScore: Int,
    val memoryScore: Int,
    val frameScore: Int,
    val networkScore: Int,
    val batteryScore: Int,
    val recommendations: List<String>
)

/**
 * Performance issues enum
 */
enum class PerformanceIssue {
    FRAME_DROPS,
    HIGH_MEMORY_USAGE,
    SLOW_NETWORK,
    LOW_BATTERY,
    HIGH_CPU_USAGE
}