package com.earthmax.performance.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.earthmax.core.performance.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerformanceMonitorBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var frameTimeTracker: FrameTimeTracker
    private lateinit var memoryTracker: MemoryTracker
    private lateinit var networkTracker: NetworkTracker
    private lateinit var batteryTracker: BatteryTracker

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        frameTimeTracker = FrameTimeTracker()
        memoryTracker = MemoryTracker(context)
        networkTracker = NetworkTracker()
        batteryTracker = BatteryTracker(context)
        
        performanceMonitor = PerformanceMonitor(
            frameTimeTracker,
            memoryTracker,
            networkTracker,
            batteryTracker
        )
    }

    @Test
    fun benchmark_performanceMonitor_initialization() {
        benchmarkRule.measureRepeated {
            val monitor = PerformanceMonitor(
                frameTimeTracker,
                memoryTracker,
                networkTracker,
                batteryTracker
            )
            // Ensure the object is used to prevent optimization
            monitor.hashCode()
        }
    }

    @Test
    fun benchmark_performanceMonitor_startStopMonitoring() {
        benchmarkRule.measureRepeated {
            runBlocking {
                performanceMonitor.startMonitoring()
                performanceMonitor.stopMonitoring()
            }
        }
    }

    @Test
    fun benchmark_performanceMonitor_getOverallScore() {
        runBlocking {
            performanceMonitor.startMonitoring()
        }

        benchmarkRule.measureRepeated {
            val score = performanceMonitor.getOverallScore()
            // Ensure the result is used
            require(score >= 0.0f)
        }
    }

    @Test
    fun benchmark_performanceMonitor_getRecommendations() {
        runBlocking {
            performanceMonitor.startMonitoring()
        }

        benchmarkRule.measureRepeated {
            val recommendations = performanceMonitor.getRecommendations()
            // Ensure the result is used
            recommendations.size
        }
    }

    @Test
    fun benchmark_performanceMonitor_createSnapshot() {
        runBlocking {
            performanceMonitor.startMonitoring()
        }

        benchmarkRule.measureRepeated {
            val snapshot = performanceMonitor.createSnapshot()
            // Ensure the result is used
            snapshot.timestamp
        }
    }

    @Test
    fun benchmark_frameTimeTracker_onFrame() {
        frameTimeTracker.startTracking()

        benchmarkRule.measureRepeated {
            frameTimeTracker.onFrame(System.nanoTime())
        }
    }

    @Test
    fun benchmark_frameTimeTracker_getAverageFps() {
        frameTimeTracker.startTracking()
        // Add some frame data
        repeat(60) {
            frameTimeTracker.onFrame(System.nanoTime())
        }

        benchmarkRule.measureRepeated {
            val fps = frameTimeTracker.getAverageFps()
            // Ensure the result is used
            require(fps >= 0.0f)
        }
    }

    @Test
    fun benchmark_memoryTracker_getUsagePercentage() {
        runBlocking {
            memoryTracker.startMonitoring()
        }

        benchmarkRule.measureRepeated {
            val usage = memoryTracker.getUsagePercentage()
            // Ensure the result is used
            require(usage >= 0.0f)
        }
    }

    @Test
    fun benchmark_memoryTracker_getMemoryInfo() {
        runBlocking {
            memoryTracker.startMonitoring()
        }

        benchmarkRule.measureRepeated {
            val memoryInfo = memoryTracker.getMemoryInfo()
            // Ensure the result is used
            memoryInfo.totalMemory
        }
    }

    @Test
    fun benchmark_networkTracker_recordRequest() {
        runBlocking {
            networkTracker.startMonitoring()
        }

        benchmarkRule.measureRepeated {
            networkTracker.recordRequest("https://api.example.com/test", 150L, true)
        }
    }

    @Test
    fun benchmark_networkTracker_getAverageLatency() {
        runBlocking {
            networkTracker.startMonitoring()
        }
        
        // Add some request data
        repeat(100) {
            networkTracker.recordRequest("https://api.example.com/test", 100L + it, true)
        }

        benchmarkRule.measureRepeated {
            val latency = networkTracker.getAverageLatency()
            // Ensure the result is used
            require(latency >= 0L)
        }
    }

    @Test
    fun benchmark_batteryTracker_getBatteryLevel() {
        runBlocking {
            batteryTracker.startMonitoring()
        }

        benchmarkRule.measureRepeated {
            val level = batteryTracker.getBatteryLevel()
            // Ensure the result is used
            require(level >= 0)
        }
    }

    @Test
    fun benchmark_performanceMonitor_fullCycle() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Full monitoring cycle
                performanceMonitor.startMonitoring()
                
                // Simulate some activity
                frameTimeTracker.onFrame(System.nanoTime())
                networkTracker.recordRequest("https://api.example.com/test", 120L, true)
                
                // Get metrics
                val score = performanceMonitor.getOverallScore()
                val recommendations = performanceMonitor.getRecommendations()
                val snapshot = performanceMonitor.createSnapshot()
                
                performanceMonitor.stopMonitoring()
                
                // Ensure results are used
                require(score >= 0.0f)
                require(recommendations.isNotEmpty() || recommendations.isEmpty())
                require(snapshot.timestamp > 0)
            }
        }
    }

    @Test
    fun benchmark_performanceMonitor_concurrentAccess() {
        runBlocking {
            performanceMonitor.startMonitoring()
        }

        benchmarkRule.measureRepeated {
            // Simulate concurrent access to performance metrics
            val score = performanceMonitor.getOverallScore()
            val memoryUsage = memoryTracker.getUsagePercentage()
            val fps = frameTimeTracker.getAverageFps()
            val latency = networkTracker.getAverageLatency()
            val batteryLevel = batteryTracker.getBatteryLevel()
            
            // Ensure all results are used
            require(score >= 0.0f)
            require(memoryUsage >= 0.0f)
            require(fps >= 0.0f)
            require(latency >= 0L)
            require(batteryLevel >= 0)
        }
    }

    @Test
    fun benchmark_performanceMonitor_memoryOverhead() {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                val monitor = PerformanceMonitor(
                    frameTimeTracker,
                    memoryTracker,
                    networkTracker,
                    batteryTracker
                )
                monitor.startMonitoring()
                
                // Simulate monitoring activity
                repeat(100) {
                    frameTimeTracker.onFrame(System.nanoTime())
                    networkTracker.recordRequest("https://api.example.com/test$it", 100L + it, true)
                }
                
                val score = monitor.getOverallScore()
                monitor.stopMonitoring()
                
                // Ensure the result is used
                require(score >= 0.0f)
            }
        }
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Log memory overhead for analysis
        println("Memory overhead: ${memoryIncrease / 1024}KB")
    }
}