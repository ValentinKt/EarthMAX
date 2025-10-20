package com.earthmax.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks memory usage and detects potential memory leaks
 */
@Singleton
class MemoryTracker @Inject constructor(
    private val context: Context
) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val _memoryMetrics = MutableStateFlow(MemoryUsage())
    val memoryMetrics: StateFlow<MemoryUsage> = _memoryMetrics.asStateFlow()
    
    // Track object references for leak detection
    private val trackedObjects = ConcurrentHashMap<String, MutableList<WeakReference<Any>>>()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    
    // Memory thresholds
    private val lowMemoryThreshold = 0.8 // 80% of available memory
    private val criticalMemoryThreshold = 0.9 // 90% of available memory
    private val maxSnapshots = 50 // Keep last 50 snapshots
    
    /**
     * Get current memory information
     */
    fun getMemoryInfo(): MemoryUsage {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val heapSize = runtime.totalMemory()
        val heapUsed = heapSize - runtime.freeMemory()
        val heapMax = runtime.maxMemory()
        
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapUsed = Debug.getNativeHeapAllocatedSize()
        
        val memoryUsage = MemoryUsage(
            usedMemory = memoryInfo.totalMem - memoryInfo.availMem,
            totalMemory = memoryInfo.totalMem,
            availableMemory = memoryInfo.availMem,
            heapSize = heapSize,
            heapUsed = heapUsed,
            heapMax = heapMax,
            nativeHeapSize = nativeHeapSize,
            nativeHeapUsed = nativeHeapUsed,
            isLowMemory = memoryInfo.lowMemory,
            threshold = memoryInfo.threshold
        )
        
        _memoryMetrics.value = memoryUsage
        
        // Create memory snapshot
        createMemorySnapshot(memoryUsage)
        
        return memoryUsage
    }
    
    /**
     * Create a memory snapshot for trend analysis
     */
    private fun createMemorySnapshot(memoryUsage: MemoryUsage) {
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            heapUsed = memoryUsage.heapUsed,
            nativeHeapUsed = memoryUsage.nativeHeapUsed,
            totalUsed = memoryUsage.usedMemory,
            availableMemory = memoryUsage.availableMemory
        )
        
        memorySnapshots.add(snapshot)
        
        // Keep only recent snapshots
        while (memorySnapshots.size > maxSnapshots) {
            memorySnapshots.removeAt(0)
        }
    }
    
    /**
     * Track object for potential memory leaks
     */
    fun trackObject(tag: String, obj: Any) {
        val references = trackedObjects.getOrPut(tag) { mutableListOf() }
        references.add(WeakReference(obj))
        
        // Clean up null references
        cleanupReferences(tag)
    }
    
    /**
     * Clean up null weak references
     */
    private fun cleanupReferences(tag: String) {
        trackedObjects[tag]?.removeAll { it.get() == null }
    }
    
    /**
     * Get memory leak report
     */
    fun getMemoryLeakReport(): MemoryLeakReport {
        val leaks = mutableMapOf<String, Int>()
        val totalObjects = mutableMapOf<String, Int>()
        
        trackedObjects.forEach { (tag, references) ->
            cleanupReferences(tag)
            val activeReferences = references.count { it.get() != null }
            totalObjects[tag] = activeReferences
            
            // Consider it a potential leak if there are too many instances
            if (activeReferences > getExpectedInstanceCount(tag)) {
                leaks[tag] = activeReferences
            }
        }
        
        return MemoryLeakReport(
            potentialLeaks = leaks,
            totalTrackedObjects = totalObjects,
            recommendations = generateMemoryRecommendations()
        )
    }
    
    /**
     * Get expected instance count for different object types
     */
    private fun getExpectedInstanceCount(tag: String): Int {
        return when {
            tag.contains("Activity") -> 5 // Max 5 activities in memory
            tag.contains("Fragment") -> 10 // Max 10 fragments
            tag.contains("ViewModel") -> 10 // Max 10 view models
            tag.contains("Repository") -> 5 // Max 5 repositories
            else -> 20 // Default threshold
        }
    }
    
    /**
     * Generate memory optimization recommendations
     */
    private fun generateMemoryRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val currentUsage = _memoryMetrics.value
        
        // Check heap usage
        val heapUsageRatio = currentUsage.heapUsed.toDouble() / currentUsage.heapMax
        if (heapUsageRatio > lowMemoryThreshold) {
            recommendations.add("High heap usage detected. Consider reducing object allocations.")
        }
        
        // Check native heap usage
        if (currentUsage.nativeHeapUsed > currentUsage.nativeHeapSize * 0.8) {
            recommendations.add("High native heap usage. Check for native memory leaks.")
        }
        
        // Check memory growth trend
        if (memorySnapshots.size >= 10) {
            val recentSnapshots = memorySnapshots.takeLast(10)
            val memoryGrowth = recentSnapshots.last().totalUsed - recentSnapshots.first().totalUsed
            val timeSpan = recentSnapshots.last().timestamp - recentSnapshots.first().timestamp
            
            if (memoryGrowth > 0 && timeSpan > 0) {
                val growthRate = memoryGrowth.toDouble() / timeSpan * 1000 * 60 // MB per minute
                if (growthRate > 1.0) { // More than 1MB per minute
                    recommendations.add("Memory usage is growing rapidly. Check for memory leaks.")
                }
            }
        }
        
        // Check for low memory condition
        if (currentUsage.isLowMemory) {
            recommendations.add("Device is in low memory condition. Consider freeing unused resources.")
        }
        
        return recommendations
    }
    
    /**
     * Force garbage collection and get memory info
     */
    fun forceGCAndGetMemoryInfo(): MemoryUsage {
        System.gc()
        Thread.sleep(100) // Give GC time to run
        return getMemoryInfo()
    }
    
    /**
     * Get memory trend analysis
     */
    fun getMemoryTrend(): MemoryTrend {
        if (memorySnapshots.size < 2) {
            return MemoryTrend()
        }
        
        val recent = memorySnapshots.takeLast(10)
        val older = memorySnapshots.dropLast(10).takeLast(10)
        
        val recentAverage = recent.map { it.totalUsed }.average()
        val olderAverage = if (older.isNotEmpty()) older.map { it.totalUsed }.average() else recentAverage
        
        val trend = when {
            recentAverage > olderAverage * 1.1 -> MemoryTrendDirection.INCREASING
            recentAverage < olderAverage * 0.9 -> MemoryTrendDirection.DECREASING
            else -> MemoryTrendDirection.STABLE
        }
        
        return MemoryTrend(
            direction = trend,
            changePercentage = ((recentAverage - olderAverage) / olderAverage * 100).toInt(),
            snapshots = memorySnapshots.toList()
        )
    }
    
    /**
     * Clear all tracking data
     */
    fun clearTrackingData() {
        trackedObjects.clear()
        memorySnapshots.clear()
    }
}

/**
 * Enhanced memory usage data class
 */
data class MemoryUsage(
    val usedMemory: Long = 0L,
    val totalMemory: Long = 0L,
    val availableMemory: Long = 0L,
    val heapSize: Long = 0L,
    val heapUsed: Long = 0L,
    val heapMax: Long = 0L,
    val nativeHeapSize: Long = 0L,
    val nativeHeapUsed: Long = 0L,
    val isLowMemory: Boolean = false,
    val threshold: Long = 0L
)

/**
 * Memory snapshot for trend analysis
 */
data class MemorySnapshot(
    val timestamp: Long,
    val heapUsed: Long,
    val nativeHeapUsed: Long,
    val totalUsed: Long,
    val availableMemory: Long
)

/**
 * Memory leak detection report
 */
data class MemoryLeakReport(
    val potentialLeaks: Map<String, Int>,
    val totalTrackedObjects: Map<String, Int>,
    val recommendations: List<String>
)

/**
 * Memory trend analysis
 */
data class MemoryTrend(
    val direction: MemoryTrendDirection = MemoryTrendDirection.STABLE,
    val changePercentage: Int = 0,
    val snapshots: List<MemorySnapshot> = emptyList()
)

/**
 * Memory trend direction
 */
enum class MemoryTrendDirection {
    INCREASING,
    DECREASING,
    STABLE
}