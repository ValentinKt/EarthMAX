package com.earthmax.performance

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
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
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects and prevents memory leaks in Android applications
 * Monitors Activities, Fragments, and custom objects for potential leaks
 */
@Singleton
class MemoryLeakDetector @Inject constructor(
    private val application: Application
) : DefaultLifecycleObserver {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    
    // Tracked objects
    private val trackedActivities = ConcurrentHashMap<String, WeakReference<Activity>>()
    private val trackedFragments = ConcurrentHashMap<String, WeakReference<Fragment>>()
    private val trackedObjects = ConcurrentHashMap<String, TrackedObject>()
    
    // Leak detection results
    private val _leakDetectionResults = MutableStateFlow(MemoryLeakResults())
    val leakDetectionResults: StateFlow<MemoryLeakResults> = _leakDetectionResults.asStateFlow()
    
    private var isMonitoring = false
    private val leakCheckInterval = 30000L // 30 seconds
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    /**
     * Start memory leak detection
     */
    fun startDetection() {
        if (isMonitoring) return
        
        isMonitoring = true
        
        // Register activity lifecycle callbacks
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        
        // Start periodic leak checking
        scope.launch {
            while (isMonitoring) {
                delay(leakCheckInterval)
                performLeakCheck()
            }
        }
    }
    
    /**
     * Stop memory leak detection
     */
    fun stopDetection() {
        if (!isMonitoring) return
        
        isMonitoring = false
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        
        // Clear tracked objects
        trackedActivities.clear()
        trackedFragments.clear()
        trackedObjects.clear()
    }
    
    /**
     * Track a custom object for memory leaks
     */
    fun trackObject(obj: Any, tag: String = obj.javaClass.simpleName) {
        val key = "${tag}_${System.identityHashCode(obj)}"
        trackedObjects[key] = TrackedObject(
            weakRef = WeakReference(obj),
            className = obj.javaClass.name,
            tag = tag,
            creationTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Stop tracking a specific object
     */
    fun stopTrackingObject(obj: Any, tag: String = obj.javaClass.simpleName) {
        val key = "${tag}_${System.identityHashCode(obj)}"
        trackedObjects.remove(key)
    }
    
    /**
     * Perform memory leak check
     */
    private fun performLeakCheck() {
        val leaks = mutableListOf<MemoryLeak>()
        
        // Force garbage collection before checking
        System.gc()
        System.runFinalization()
        System.gc()
        
        // Wait a bit for GC to complete
        Thread.sleep(1000)
        
        // Check for activity leaks
        leaks.addAll(checkActivityLeaks())
        
        // Check for fragment leaks
        leaks.addAll(checkFragmentLeaks())
        
        // Check for custom object leaks
        leaks.addAll(checkCustomObjectLeaks())
        
        // Update results
        val results = MemoryLeakResults(
            totalLeaks = leaks.size,
            activityLeaks = leaks.count { it.type == LeakType.ACTIVITY },
            fragmentLeaks = leaks.count { it.type == LeakType.FRAGMENT },
            customObjectLeaks = leaks.count { it.type == LeakType.CUSTOM_OBJECT },
            leaks = leaks,
            lastCheckTime = System.currentTimeMillis(),
            recommendations = generateLeakRecommendations(leaks)
        )
        
        _leakDetectionResults.value = results
    }
    
    /**
     * Check for activity leaks
     */
    private fun checkActivityLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        val currentTime = System.currentTimeMillis()
        
        trackedActivities.entries.removeAll { (key, weakRef) ->
            val activity = weakRef.get()
            if (activity == null) {
                // Activity was garbage collected - good!
                true
            } else if (activity.isDestroyed) {
                // Activity is destroyed but still referenced - potential leak!
                leaks.add(
                    MemoryLeak(
                        type = LeakType.ACTIVITY,
                        className = activity.javaClass.name,
                        description = "Activity ${activity.javaClass.simpleName} is destroyed but still referenced",
                        severity = LeakSeverity.HIGH,
                        detectionTime = currentTime,
                        possibleCauses = listOf(
                            "Static reference to Activity",
                            "Handler with Activity reference",
                            "AsyncTask holding Activity reference",
                            "Listener not unregistered",
                            "Thread holding Activity reference"
                        ),
                        recommendations = listOf(
                            "Use WeakReference for Activity references",
                            "Unregister listeners in onDestroy()",
                            "Cancel AsyncTasks in onDestroy()",
                            "Use Application Context instead of Activity Context where possible"
                        )
                    )
                )
                false // Keep tracking to monitor the leak
            } else {
                false // Keep tracking active activity
            }
        }
        
        return leaks
    }
    
    /**
     * Check for fragment leaks
     */
    private fun checkFragmentLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        val currentTime = System.currentTimeMillis()
        
        trackedFragments.entries.removeAll { (key, weakRef) ->
            val fragment = weakRef.get()
            if (fragment == null) {
                // Fragment was garbage collected - good!
                true
            } else if (fragment.isDetached && fragment.view == null) {
                // Fragment is detached but still referenced - potential leak!
                leaks.add(
                    MemoryLeak(
                        type = LeakType.FRAGMENT,
                        className = fragment.javaClass.name,
                        description = "Fragment ${fragment.javaClass.simpleName} is detached but still referenced",
                        severity = LeakSeverity.MEDIUM,
                        detectionTime = currentTime,
                        possibleCauses = listOf(
                            "Fragment retained by FragmentManager",
                            "Static reference to Fragment",
                            "Handler with Fragment reference",
                            "Callback not cleared in onDestroyView()"
                        ),
                        recommendations = listOf(
                            "Clear references in onDestroyView()",
                            "Use WeakReference for Fragment references",
                            "Remove Fragment from FragmentManager properly",
                            "Clear callbacks and listeners"
                        )
                    )
                )
                false // Keep tracking to monitor the leak
            } else {
                false // Keep tracking active fragment
            }
        }
        
        return leaks
    }
    
    /**
     * Check for custom object leaks
     */
    private fun checkCustomObjectLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        val currentTime = System.currentTimeMillis()
        val leakThreshold = 5 * 60 * 1000L // 5 minutes
        
        trackedObjects.entries.removeAll { (key, trackedObj) ->
            val obj = trackedObj.weakRef.get()
            if (obj == null) {
                // Object was garbage collected - good!
                true
            } else if (currentTime - trackedObj.creationTime > leakThreshold) {
                // Object has been alive for too long - potential leak!
                leaks.add(
                    MemoryLeak(
                        type = LeakType.CUSTOM_OBJECT,
                        className = trackedObj.className,
                        description = "Object ${trackedObj.tag} has been alive for ${(currentTime - trackedObj.creationTime) / 1000}s",
                        severity = LeakSeverity.LOW,
                        detectionTime = currentTime,
                        possibleCauses = listOf(
                            "Object held by static reference",
                            "Object in collection not cleared",
                            "Circular reference preventing GC",
                            "Event listener not unregistered"
                        ),
                        recommendations = listOf(
                            "Check for static references",
                            "Clear collections when done",
                            "Break circular references",
                            "Unregister event listeners"
                        )
                    )
                )
                false // Keep tracking to monitor the leak
            } else {
                false // Keep tracking active object
            }
        }
        
        return leaks
    }
    
    /**
     * Generate leak prevention recommendations
     */
    private fun generateLeakRecommendations(leaks: List<MemoryLeak>): List<String> {
        val recommendations = mutableSetOf<String>()
        
        if (leaks.any { it.type == LeakType.ACTIVITY }) {
            recommendations.add("Use Application Context instead of Activity Context where possible")
            recommendations.add("Always unregister listeners and callbacks in onDestroy()")
            recommendations.add("Cancel AsyncTasks and background operations in onDestroy()")
        }
        
        if (leaks.any { it.type == LeakType.FRAGMENT }) {
            recommendations.add("Clear view references in onDestroyView()")
            recommendations.add("Remove fragments properly from FragmentManager")
            recommendations.add("Use ViewBinding with proper cleanup")
        }
        
        if (leaks.any { it.severity == LeakSeverity.HIGH }) {
            recommendations.add("High severity leaks detected - immediate attention required")
            recommendations.add("Consider using LeakCanary for detailed leak analysis")
        }
        
        if (leaks.size > 5) {
            recommendations.add("Multiple leaks detected - review object lifecycle management")
            recommendations.add("Implement proper cleanup patterns throughout the app")
        }
        
        return recommendations.toList()
    }
    
    /**
     * Get memory leak prevention tips
     */
    fun getLeakPreventionTips(): List<LeakPreventionTip> {
        return listOf(
            LeakPreventionTip(
                category = "Activities",
                tip = "Use WeakReference for Activity references in static contexts",
                example = "private static WeakReference<Activity> activityRef;"
            ),
            LeakPreventionTip(
                category = "Handlers",
                tip = "Use static Handler classes with WeakReference",
                example = "private static class MyHandler extends Handler { private final WeakReference<Activity> ref; }"
            ),
            LeakPreventionTip(
                category = "AsyncTasks",
                tip = "Cancel AsyncTasks in onDestroy() and use WeakReference",
                example = "asyncTask.cancel(true); // in onDestroy()"
            ),
            LeakPreventionTip(
                category = "Listeners",
                tip = "Always unregister listeners and callbacks",
                example = "sensorManager.unregisterListener(this); // in onDestroy()"
            ),
            LeakPreventionTip(
                category = "Threads",
                tip = "Interrupt and join background threads properly",
                example = "backgroundThread.interrupt(); backgroundThread.join();"
            ),
            LeakPreventionTip(
                category = "Collections",
                tip = "Clear collections and maps when done",
                example = "myList.clear(); myMap.clear();"
            ),
            LeakPreventionTip(
                category = "Context",
                tip = "Use Application Context for long-lived operations",
                example = "context.getApplicationContext()"
            )
        )
    }
    
    /**
     * Activity lifecycle callbacks for tracking
     */
    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            val key = "${activity.javaClass.simpleName}_${System.identityHashCode(activity)}"
            trackedActivities[key] = WeakReference(activity)
            
            // Track fragments if it's a FragmentActivity
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                    fragmentLifecycleCallbacks, true
                )
            }
        }
        
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        
        override fun onActivityDestroyed(activity: Activity) {
            // Don't remove from tracking immediately - we want to detect if it's still referenced
            // The leak check will handle cleanup
        }
    }
    
    /**
     * Fragment lifecycle callbacks for tracking
     */
    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
            val key = "${f.javaClass.simpleName}_${System.identityHashCode(f)}"
            trackedFragments[key] = WeakReference(f)
        }
        
        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
            // Don't remove from tracking immediately - we want to detect if it's still referenced
            // The leak check will handle cleanup
        }
    }
    
    override fun onStart(owner: LifecycleOwner) {
        startDetection()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        stopDetection()
    }
}

/**
 * Tracked object information
 */
data class TrackedObject(
    val weakRef: WeakReference<Any>,
    val className: String,
    val tag: String,
    val creationTime: Long
)

/**
 * Memory leak information
 */
data class MemoryLeak(
    val type: LeakType,
    val className: String,
    val description: String,
    val severity: LeakSeverity,
    val detectionTime: Long,
    val possibleCauses: List<String>,
    val recommendations: List<String>
)

/**
 * Memory leak detection results
 */
data class MemoryLeakResults(
    val totalLeaks: Int = 0,
    val activityLeaks: Int = 0,
    val fragmentLeaks: Int = 0,
    val customObjectLeaks: Int = 0,
    val leaks: List<MemoryLeak> = emptyList(),
    val lastCheckTime: Long = 0,
    val recommendations: List<String> = emptyList()
)

/**
 * Leak prevention tip
 */
data class LeakPreventionTip(
    val category: String,
    val tip: String,
    val example: String
)

/**
 * Types of memory leaks
 */
enum class LeakType {
    ACTIVITY,
    FRAGMENT,
    CUSTOM_OBJECT
}

/**
 * Leak severity levels
 */
enum class LeakSeverity {
    LOW,
    MEDIUM,
    HIGH
}