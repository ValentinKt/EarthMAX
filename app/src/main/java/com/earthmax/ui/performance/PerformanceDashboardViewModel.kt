package com.earthmax.ui.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.performance.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerformanceDashboardViewModel @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
    private val frameTimeTracker: FrameTimeTracker,
    private val memoryTracker: MemoryTracker,
    private val networkTracker: NetworkTracker,
    private val batteryTracker: BatteryTracker,
    private val uiPerformanceOptimizer: UIPerformanceOptimizer,
    private val memoryLeakDetector: MemoryLeakDetector,
    private val databaseOptimizer: DatabaseOptimizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerformanceDashboardUiState())
    val uiState: StateFlow<PerformanceDashboardUiState> = _uiState.asStateFlow()

    private var isMonitoring = false

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        viewModelScope.launch {
            // Start all monitoring services
            performanceMonitor.startMonitoring()
            frameTimeTracker.startTracking()
            memoryTracker.startTracking()
            batteryTracker.startTracking()
            memoryLeakDetector.startDetection()

            // Collect performance data periodically
            while (isMonitoring) {
                updatePerformanceMetrics()
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        performanceMonitor.stopMonitoring()
        frameTimeTracker.stopTracking()
        memoryTracker.stopTracking()
        batteryTracker.stopTracking()
        memoryLeakDetector.stopDetection()
    }

    private suspend fun updatePerformanceMetrics() {
        try {
            val performanceReport = performanceMonitor.generateReport()
            val frameStats = frameTimeTracker.getFrameStatistics()
            val memorySnapshot = memoryTracker.createSnapshot()
            val networkStats = networkTracker.getNetworkStatistics()
            val batterySnapshot = batteryTracker.createSnapshot()
            val uiOptimization = uiPerformanceOptimizer.analyzeCurrentView()
            val memoryLeaks = memoryLeakDetector.getLeakReport().leaks
            val dbStats = databaseOptimizer.getPerformanceStatistics()

            _uiState.value = _uiState.value.copy(
                // Overall performance
                overallScore = performanceReport.overallScore,

                // Memory metrics
                memoryUsage = ((memorySnapshot.usedMemory.toFloat() / memorySnapshot.totalMemory) * 100).toInt(),
                memoryUsed = (memorySnapshot.usedMemory / (1024 * 1024)).toInt(), // Convert to MB
                memoryAvailable = ((memorySnapshot.totalMemory - memorySnapshot.usedMemory) / (1024 * 1024)).toInt(),
                memoryMax = (memorySnapshot.maxMemory / (1024 * 1024)).toInt(),
                memoryRecommendations = memoryTracker.getOptimizationRecommendations(),

                // Frame rate metrics
                averageFps = frameStats.averageFps,
                droppedFrames = frameStats.droppedFrames,
                jankFrames = frameStats.jankFrames,
                frameConsistency = ((1.0f - (frameStats.jankFrames.toFloat() / maxOf(frameStats.totalFrames, 1))) * 100).toInt(),
                frameRateRecommendations = getFrameRateRecommendations(frameStats),

                // Network metrics
                averageResponseTime = networkStats.averageResponseTime,
                networkSuccessRate = ((networkStats.successfulRequests.toFloat() / maxOf(networkStats.totalRequests, 1)) * 100).toInt(),
                dataUsage = (networkStats.totalDataTransferred / (1024 * 1024)).toInt(), // Convert to MB
                activeRequests = networkStats.activeRequests,
                networkRecommendations = networkTracker.getRecommendations(),

                // Battery metrics
                batteryLevel = batterySnapshot.batteryLevel,
                batteryTemperature = (batterySnapshot.temperature / 10.0f).toInt(), // Convert from tenths of degree
                powerUsage = batterySnapshot.powerUsage.toInt(),
                timeRemaining = formatTimeRemaining(batterySnapshot.estimatedTimeRemaining),
                batteryRecommendations = batteryTracker.getRecommendations(),

                // Database metrics
                dbPerformanceScore = dbStats.performanceScore,
                avgQueryTime = dbStats.averageQueryTime,
                slowQueries = dbStats.slowQueries.size,
                cacheHitRate = ((dbStats.cacheHits.toFloat() / maxOf(dbStats.totalQueries, 1)) * 100).toInt(),
                databaseRecommendations = databaseOptimizer.getOptimizationRecommendations(),

                // UI Performance metrics
                uiPerformanceScore = calculateUIPerformanceScore(uiOptimization),
                overdrawLevel = uiOptimization.overdrawLevel.name,
                layoutDepth = uiOptimization.maxLayoutDepth,
                viewCount = uiOptimization.totalViews,
                uiRecommendations = uiOptimization.recommendations,

                // Memory leaks
                memoryLeaks = memoryLeaks.map { leak ->
                    MemoryLeak(
                        objectName = leak.objectName,
                        size = leak.size,
                        possibleCause = leak.possibleCause,
                        recommendation = leak.recommendation
                    )
                }
            )
        } catch (e: Exception) {
            // Handle errors gracefully
            e.printStackTrace()
        }
    }

    fun fixMemoryLeak(leak: MemoryLeak) {
        viewModelScope.launch {
            try {
                // Apply the recommended fix
                // This would typically involve calling specific cleanup methods
                // based on the leak type and recommendation
                
                // For now, we'll just remove it from the UI
                val currentLeaks = _uiState.value.memoryLeaks.toMutableList()
                currentLeaks.remove(leak)
                _uiState.value = _uiState.value.copy(memoryLeaks = currentLeaks)
                
                // In a real implementation, you would:
                // 1. Identify the specific object causing the leak
                // 2. Apply the appropriate cleanup (e.g., unregister listeners, close resources)
                // 3. Force garbage collection if necessary
                // 4. Verify the leak is resolved
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFrameRateRecommendations(frameStats: FrameTimeTracker.FrameStatistics): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (frameStats.averageFps < 30f) {
            recommendations.add("Consider reducing animation complexity")
            recommendations.add("Optimize heavy computations on main thread")
        }
        
        if (frameStats.jankFrames > frameStats.totalFrames * 0.1) {
            recommendations.add("Reduce overdraw in layouts")
            recommendations.add("Use RecyclerView for large lists")
        }
        
        if (frameStats.maxFrameTime > 32f) {
            recommendations.add("Profile and optimize slow operations")
            recommendations.add("Consider using background threads for heavy work")
        }
        
        return recommendations
    }

    private fun calculateUIPerformanceScore(optimization: UIPerformanceOptimizer.OptimizationResult): Int {
        var score = 100
        
        // Deduct points based on issues
        when (optimization.overdrawLevel) {
            UIPerformanceOptimizer.OverdrawLevel.HIGH -> score -= 30
            UIPerformanceOptimizer.OverdrawLevel.MEDIUM -> score -= 15
            UIPerformanceOptimizer.OverdrawLevel.LOW -> score -= 5
            UIPerformanceOptimizer.OverdrawLevel.NONE -> { /* No deduction */ }
        }
        
        if (optimization.maxLayoutDepth > 10) score -= 20
        if (optimization.totalViews > 100) score -= 15
        if (optimization.recommendations.isNotEmpty()) score -= (optimization.recommendations.size * 5)
        
        return maxOf(score, 0)
    }

    private fun formatTimeRemaining(minutes: Long): String {
        return when {
            minutes < 60 -> "${minutes}m"
            minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
            else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}

data class PerformanceDashboardUiState(
    // Overall
    val overallScore: Int = 0,
    
    // Memory
    val memoryUsage: Int = 0,
    val memoryUsed: Int = 0,
    val memoryAvailable: Int = 0,
    val memoryMax: Int = 0,
    val memoryRecommendations: List<String> = emptyList(),
    
    // Frame Rate
    val averageFps: Float = 0f,
    val droppedFrames: Int = 0,
    val jankFrames: Int = 0,
    val frameConsistency: Int = 0,
    val frameRateRecommendations: List<String> = emptyList(),
    
    // Network
    val averageResponseTime: Long = 0,
    val networkSuccessRate: Int = 0,
    val dataUsage: Int = 0,
    val activeRequests: Int = 0,
    val networkRecommendations: List<String> = emptyList(),
    
    // Battery
    val batteryLevel: Int = 0,
    val batteryTemperature: Int = 0,
    val powerUsage: Int = 0,
    val timeRemaining: String = "",
    val batteryRecommendations: List<String> = emptyList(),
    
    // Database
    val dbPerformanceScore: Int = 0,
    val avgQueryTime: Long = 0,
    val slowQueries: Int = 0,
    val cacheHitRate: Int = 0,
    val databaseRecommendations: List<String> = emptyList(),
    
    // UI Performance
    val uiPerformanceScore: Int = 0,
    val overdrawLevel: String = "",
    val layoutDepth: Int = 0,
    val viewCount: Int = 0,
    val uiRecommendations: List<String> = emptyList(),
    
    // Memory Leaks
    val memoryLeaks: List<MemoryLeak> = emptyList()
)