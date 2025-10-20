package com.earthmax.performance

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

/**
 * Tracks frame rendering performance using Choreographer
 */
class FrameTimeTracker : Choreographer.FrameCallback {
    
    private val choreographer = Choreographer.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    
    private val _frameMetrics = MutableStateFlow(FrameMetrics())
    val frameMetrics: StateFlow<FrameMetrics> = _frameMetrics.asStateFlow()
    
    private val frameTimes = ConcurrentLinkedQueue<Long>()
    private var lastFrameTime = 0L
    private var totalFrames = 0L
    private var droppedFrames = 0
    private var jankFrames = 0
    
    private var isTracking = false
    
    // Frame time thresholds
    private val targetFrameTime = 16_666_667L // 60 FPS in nanoseconds
    private val jankThreshold = targetFrameTime * 2 // 2 frames = jank
    private val maxStoredFrames = 300 // Store last 5 seconds at 60 FPS
    
    /**
     * Start tracking frame times
     */
    fun start() {
        if (isTracking) return
        
        isTracking = true
        lastFrameTime = System.nanoTime()
        
        handler.post {
            choreographer.postFrameCallback(this)
        }
    }
    
    /**
     * Stop tracking frame times
     */
    fun stop() {
        if (!isTracking) return
        
        isTracking = false
        
        handler.post {
            choreographer.removeFrameCallback(this)
        }
    }
    
    override fun doFrame(frameTimeNanos: Long) {
        if (!isTracking) return
        
        if (lastFrameTime != 0L) {
            val frameTime = frameTimeNanos - lastFrameTime
            processFrameTime(frameTime)
        }
        
        lastFrameTime = frameTimeNanos
        totalFrames++
        
        // Schedule next frame callback
        choreographer.postFrameCallback(this)
    }
    
    /**
     * Process individual frame time
     */
    private fun processFrameTime(frameTimeNanos: Long) {
        // Add to frame times queue
        frameTimes.offer(frameTimeNanos)
        
        // Remove old frames to maintain window size
        while (frameTimes.size > maxStoredFrames) {
            frameTimes.poll()
        }
        
        // Check for dropped frames
        val droppedFrameCount = (frameTimeNanos / targetFrameTime).toInt() - 1
        if (droppedFrameCount > 0) {
            droppedFrames += droppedFrameCount
        }
        
        // Check for jank
        if (frameTimeNanos > jankThreshold) {
            jankFrames++
        }
        
        // Update metrics
        updateMetrics()
    }
    
    /**
     * Update frame metrics
     */
    private fun updateMetrics() {
        if (frameTimes.isEmpty()) return
        
        val frameTimesList = frameTimes.toList()
        val averageFrameTimeNanos = frameTimesList.average()
        val averageFrameTimeMs = averageFrameTimeNanos / 1_000_000.0
        
        val metrics = FrameMetrics(
            averageFrameTime = averageFrameTimeMs,
            frameDrops = droppedFrames,
            totalFrames = totalFrames,
            jankFrames = jankFrames
        )
        
        _frameMetrics.value = metrics
    }
    
    /**
     * Get current frame information
     */
    fun getFrameInfo(): FrameMetrics {
        return _frameMetrics.value
    }
    
    /**
     * Get detailed frame statistics
     */
    fun getDetailedStats(): FrameStats {
        if (frameTimes.isEmpty()) {
            return FrameStats()
        }
        
        val frameTimesList = frameTimes.toList()
        val frameTimesMs = frameTimesList.map { it / 1_000_000.0 }
        
        val min = frameTimesMs.minOrNull() ?: 0.0
        val max = frameTimesMs.maxOrNull() ?: 0.0
        val average = frameTimesMs.average()
        val median = frameTimesMs.sorted().let { sorted ->
            val size = sorted.size
            if (size % 2 == 0) {
                (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
            } else {
                sorted[size / 2]
            }
        }
        
        // Calculate percentiles
        val sorted = frameTimesMs.sorted()
        val p95 = getPercentile(sorted, 0.95)
        val p99 = getPercentile(sorted, 0.99)
        
        // Calculate FPS
        val averageFps = if (average > 0) 1000.0 / average else 0.0
        
        // Calculate frame consistency (lower is better)
        val variance = frameTimesMs.map { (it - average) * (it - average) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        return FrameStats(
            minFrameTime = min,
            maxFrameTime = max,
            averageFrameTime = average,
            medianFrameTime = median,
            p95FrameTime = p95,
            p99FrameTime = p99,
            averageFps = averageFps,
            frameConsistency = standardDeviation,
            totalFrames = totalFrames,
            droppedFrames = droppedFrames,
            jankFrames = jankFrames,
            jankPercentage = if (totalFrames > 0) (jankFrames.toDouble() / totalFrames) * 100 else 0.0
        )
    }
    
    /**
     * Calculate percentile from sorted list
     */
    private fun getPercentile(sortedList: List<Double>, percentile: Double): Double {
        if (sortedList.isEmpty()) return 0.0
        
        val index = (percentile * (sortedList.size - 1)).toInt()
        return sortedList[max(0, index)]
    }
    
    /**
     * Reset all tracking data
     */
    fun reset() {
        frameTimes.clear()
        totalFrames = 0L
        droppedFrames = 0
        jankFrames = 0
        lastFrameTime = 0L
        
        _frameMetrics.value = FrameMetrics()
    }
    
    /**
     * Check if currently tracking
     */
    fun isTracking(): Boolean = isTracking
}

/**
 * Detailed frame statistics
 */
data class FrameStats(
    val minFrameTime: Double = 0.0,
    val maxFrameTime: Double = 0.0,
    val averageFrameTime: Double = 0.0,
    val medianFrameTime: Double = 0.0,
    val p95FrameTime: Double = 0.0,
    val p99FrameTime: Double = 0.0,
    val averageFps: Double = 0.0,
    val frameConsistency: Double = 0.0,
    val totalFrames: Long = 0L,
    val droppedFrames: Int = 0,
    val jankFrames: Int = 0,
    val jankPercentage: Double = 0.0
)