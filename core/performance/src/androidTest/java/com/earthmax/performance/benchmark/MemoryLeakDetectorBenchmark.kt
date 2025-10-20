package com.earthmax.performance.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.earthmax.core.performance.MemoryLeakDetector
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference

@RunWith(AndroidJUnit4::class)
class MemoryLeakDetectorBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var memoryLeakDetector: MemoryLeakDetector

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        memoryLeakDetector = MemoryLeakDetector(context)
    }

    @Test
    fun benchmark_memoryLeakDetector_initialization() {
        benchmarkRule.measureRepeated {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val detector = MemoryLeakDetector(context)
            // Ensure the object is used to prevent optimization
            detector.hashCode()
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_startStopDetection() {
        benchmarkRule.measureRepeated {
            runBlocking {
                memoryLeakDetector.startDetection()
                memoryLeakDetector.stopDetection()
            }
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_trackActivity() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }

        benchmarkRule.measureRepeated {
            val mockActivity = MockActivity()
            memoryLeakDetector.trackActivity(mockActivity)
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_untrackActivity() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }
        
        val mockActivity = MockActivity()
        memoryLeakDetector.trackActivity(mockActivity)

        benchmarkRule.measureRepeated {
            memoryLeakDetector.untrackActivity(mockActivity)
            // Re-track for next iteration
            memoryLeakDetector.trackActivity(mockActivity)
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_trackFragment() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }

        benchmarkRule.measureRepeated {
            val mockFragment = MockFragment()
            memoryLeakDetector.trackFragment(mockFragment)
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_trackCustomObject() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }

        benchmarkRule.measureRepeated {
            val customObject = CustomTestObject()
            memoryLeakDetector.trackObject(customObject, "CustomTestObject")
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_checkForLeaks() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }
        
        // Add some objects to track
        repeat(50) {
            val mockActivity = MockActivity()
            memoryLeakDetector.trackActivity(mockActivity)
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                memoryLeakDetector.checkForLeaks()
            }
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_getActiveLeaks() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }
        
        // Add some objects that might leak
        repeat(20) {
            val mockActivity = MockActivity()
            memoryLeakDetector.trackActivity(mockActivity)
        }

        benchmarkRule.measureRepeated {
            val leaks = memoryLeakDetector.getActiveLeaks()
            // Ensure the result is used
            leaks.size
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_generateLeakReport() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }
        
        // Add some objects to track
        repeat(30) {
            val mockActivity = MockActivity()
            memoryLeakDetector.trackActivity(mockActivity)
        }

        benchmarkRule.measureRepeated {
            val report = memoryLeakDetector.generateLeakReport()
            // Ensure the result is used
            report.totalLeaks
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_massiveObjectTracking() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }

        benchmarkRule.measureRepeated {
            // Track a large number of objects
            repeat(1000) {
                val customObject = CustomTestObject()
                memoryLeakDetector.trackObject(customObject, "CustomTestObject$it")
            }
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_concurrentTracking() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }

        benchmarkRule.measureRepeated {
            // Simulate concurrent tracking operations
            val activity1 = MockActivity()
            val activity2 = MockActivity()
            val fragment1 = MockFragment()
            val fragment2 = MockFragment()
            val custom1 = CustomTestObject()
            val custom2 = CustomTestObject()
            
            memoryLeakDetector.trackActivity(activity1)
            memoryLeakDetector.trackActivity(activity2)
            memoryLeakDetector.trackFragment(fragment1)
            memoryLeakDetector.trackFragment(fragment2)
            memoryLeakDetector.trackObject(custom1, "Custom1")
            memoryLeakDetector.trackObject(custom2, "Custom2")
            
            // Check for leaks
            runBlocking {
                memoryLeakDetector.checkForLeaks()
            }
            
            // Get active leaks
            val leaks = memoryLeakDetector.getActiveLeaks()
            leaks.size
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_weakReferenceHandling() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }

        benchmarkRule.measureRepeated {
            // Create objects and let them be garbage collected
            repeat(100) {
                val obj = CustomTestObject()
                memoryLeakDetector.trackObject(obj, "TestObject$it")
                // Create weak reference to simulate GC
                WeakReference(obj)
            }
            
            // Force garbage collection
            System.gc()
            
            // Check for leaks after GC
            runBlocking {
                memoryLeakDetector.checkForLeaks()
            }
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_memoryPressureSimulation() {
        runBlocking {
            memoryLeakDetector.startDetection()
        }

        benchmarkRule.measureRepeated {
            // Simulate memory pressure by creating and tracking many objects
            val objects = mutableListOf<Any>()
            
            repeat(500) {
                val obj = when (it % 3) {
                    0 -> MockActivity()
                    1 -> MockFragment()
                    else -> CustomTestObject()
                }
                objects.add(obj)
                
                when (obj) {
                    is MockActivity -> memoryLeakDetector.trackActivity(obj)
                    is MockFragment -> memoryLeakDetector.trackFragment(obj)
                    else -> memoryLeakDetector.trackObject(obj, "Object$it")
                }
            }
            
            // Check for leaks under memory pressure
            runBlocking {
                memoryLeakDetector.checkForLeaks()
            }
            
            val leaks = memoryLeakDetector.getActiveLeaks()
            val report = memoryLeakDetector.generateLeakReport()
            
            // Ensure results are used
            require(leaks.size >= 0)
            require(report.totalLeaks >= 0)
            
            // Clear objects to prevent actual memory issues
            objects.clear()
        }
    }

    @Test
    fun benchmark_memoryLeakDetector_fullCycle() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Full detection cycle
                memoryLeakDetector.startDetection()
                
                // Track various objects
                val activity = MockActivity()
                val fragment = MockFragment()
                val custom = CustomTestObject()
                
                memoryLeakDetector.trackActivity(activity)
                memoryLeakDetector.trackFragment(fragment)
                memoryLeakDetector.trackObject(custom, "CustomObject")
                
                // Perform leak detection
                memoryLeakDetector.checkForLeaks()
                
                // Get results
                val leaks = memoryLeakDetector.getActiveLeaks()
                val report = memoryLeakDetector.generateLeakReport()
                
                // Cleanup
                memoryLeakDetector.untrackActivity(activity)
                memoryLeakDetector.untrackFragment(fragment)
                memoryLeakDetector.untrackObject(custom)
                
                memoryLeakDetector.stopDetection()
                
                // Ensure results are used
                require(leaks.size >= 0)
                require(report.totalLeaks >= 0)
            }
        }
    }

    // Mock classes for testing
    private class MockActivity {
        private val data = ByteArray(1024) // 1KB of data
        override fun toString(): String = "MockActivity@${hashCode()}"
    }

    private class MockFragment {
        private val data = ByteArray(512) // 512B of data
        override fun toString(): String = "MockFragment@${hashCode()}"
    }

    private class CustomTestObject {
        private val data = ByteArray(256) // 256B of data
        override fun toString(): String = "CustomTestObject@${hashCode()}"
    }
}