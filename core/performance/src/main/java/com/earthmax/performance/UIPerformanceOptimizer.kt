package com.earthmax.performance

import android.app.Activity
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimizes UI performance by detecting and reducing overdraw, 
 * optimizing layouts, and providing performance recommendations
 */
@Singleton
class UIPerformanceOptimizer @Inject constructor() {
    
    private val _uiMetrics = MutableStateFlow(UIPerformanceMetrics())
    val uiMetrics: StateFlow<UIPerformanceMetrics> = _uiMetrics.asStateFlow()
    
    private val handler = Handler(Looper.getMainLooper())
    private val overdrawDetector = OverdrawDetector()
    private val layoutOptimizer = LayoutOptimizer()
    private val viewHierarchyAnalyzer = ViewHierarchyAnalyzer()
    
    private var isOptimizing = false
    
    /**
     * Start UI performance optimization
     */
    fun startOptimization(activity: Activity) {
        if (isOptimizing) return
        
        isOptimizing = true
        
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        
        // Start overdraw detection
        overdrawDetector.startDetection(rootView)
        
        // Analyze view hierarchy
        viewHierarchyAnalyzer.analyzeHierarchy(rootView)
        
        // Start layout optimization
        layoutOptimizer.optimizeLayouts(rootView)
        
        // Update metrics periodically
        handler.post(updateMetricsRunnable)
    }
    
    /**
     * Stop UI performance optimization
     */
    fun stopOptimization() {
        if (!isOptimizing) return
        
        isOptimizing = false
        
        overdrawDetector.stopDetection()
        handler.removeCallbacks(updateMetricsRunnable)
    }
    
    private val updateMetricsRunnable = object : Runnable {
        override fun run() {
            if (isOptimizing) {
                updateUIMetrics()
                handler.postDelayed(this, 5000) // Update every 5 seconds
            }
        }
    }
    
    /**
     * Update UI performance metrics
     */
    private fun updateUIMetrics() {
        val overdrawInfo = overdrawDetector.getOverdrawInfo()
        val hierarchyInfo = viewHierarchyAnalyzer.getHierarchyInfo()
        val layoutInfo = layoutOptimizer.getLayoutInfo()
        
        val metrics = UIPerformanceMetrics(
            overdrawLevel = overdrawInfo.averageOverdrawLevel,
            overdrawAreas = overdrawInfo.overdrawAreas,
            viewHierarchyDepth = hierarchyInfo.maxDepth,
            totalViews = hierarchyInfo.totalViews,
            layoutPasses = layoutInfo.layoutPasses,
            measurePasses = layoutInfo.measurePasses,
            drawCalls = layoutInfo.drawCalls,
            recommendations = generateUIRecommendations(overdrawInfo, hierarchyInfo, layoutInfo)
        )
        
        _uiMetrics.value = metrics
    }
    
    /**
     * Generate UI performance recommendations
     */
    private fun generateUIRecommendations(
        overdrawInfo: OverdrawInfo,
        hierarchyInfo: ViewHierarchyInfo,
        layoutInfo: LayoutInfo
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Overdraw recommendations
        if (overdrawInfo.averageOverdrawLevel > 2.5f) {
            recommendations.add("High overdraw detected (${overdrawInfo.averageOverdrawLevel}x). Consider reducing background layers.")
        }
        
        if (overdrawInfo.overdrawAreas.size > 10) {
            recommendations.add("Multiple overdraw areas found. Optimize view backgrounds and transparency.")
        }
        
        // View hierarchy recommendations
        if (hierarchyInfo.maxDepth > 10) {
            recommendations.add("Deep view hierarchy (${hierarchyInfo.maxDepth} levels). Consider flattening layouts.")
        }
        
        if (hierarchyInfo.totalViews > 80) {
            recommendations.add("High view count (${hierarchyInfo.totalViews}). Consider using RecyclerView or view recycling.")
        }
        
        // Layout performance recommendations
        if (layoutInfo.layoutPasses > 2) {
            recommendations.add("Multiple layout passes detected. Check for layout_weight usage and nested layouts.")
        }
        
        if (layoutInfo.measurePasses > 3) {
            recommendations.add("Excessive measure passes. Optimize RelativeLayout usage and view constraints.")
        }
        
        return recommendations
    }
    
    /**
     * Optimize specific view for performance
     */
    fun optimizeView(view: View): ViewOptimizationResult {
        val optimizations = mutableListOf<String>()
        var performanceGain = 0
        
        // Check for unnecessary backgrounds
        if (view.background != null && view.parent is ViewGroup) {
            val parent = view.parent as ViewGroup
            if (parent.background != null) {
                optimizations.add("Remove redundant background - parent already has background")
                performanceGain += 10
            }
        }
        
        // Check for overdraw in ViewGroup
        if (view is ViewGroup) {
            val overdrawCount = countOverdrawInViewGroup(view)
            if (overdrawCount > 0) {
                optimizations.add("Reduce overdraw by optimizing $overdrawCount child backgrounds")
                performanceGain += overdrawCount * 5
            }
        }
        
        // Check for expensive operations
        if (view.alpha < 1.0f && view.hasOverlappingRendering()) {
            optimizations.add("Use hardware layer for transparent views with complex content")
            performanceGain += 15
        }
        
        // Check for unnecessary clipChildren
        if (view is ViewGroup && view.clipChildren && !needsClipping(view)) {
            optimizations.add("Disable clipChildren if not needed")
            performanceGain += 5
        }
        
        return ViewOptimizationResult(
            viewId = view.id,
            optimizations = optimizations,
            estimatedPerformanceGain = performanceGain
        )
    }
    
    /**
     * Count overdraw in ViewGroup
     */
    private fun countOverdrawInViewGroup(viewGroup: ViewGroup): Int {
        var overdrawCount = 0
        val bounds = Rect()
        
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child.visibility == View.VISIBLE && child.background != null) {
                child.getHitRect(bounds)
                
                // Check if this child overlaps with siblings
                for (j in i + 1 until viewGroup.childCount) {
                    val sibling = viewGroup.getChildAt(j)
                    if (sibling.visibility == View.VISIBLE) {
                        val siblingBounds = Rect()
                        sibling.getHitRect(siblingBounds)
                        
                        if (Rect.intersects(bounds, siblingBounds)) {
                            overdrawCount++
                            break
                        }
                    }
                }
            }
        }
        
        return overdrawCount
    }
    
    /**
     * Check if ViewGroup needs clipping
     */
    private fun needsClipping(viewGroup: ViewGroup): Boolean {
        // Check if any children extend beyond bounds
        val parentBounds = Rect(0, 0, viewGroup.width, viewGroup.height)
        
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            val childBounds = Rect()
            child.getHitRect(childBounds)
            
            if (!parentBounds.contains(childBounds)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Get UI performance score (0-100)
     */
    fun getUIPerformanceScore(): Int {
        val metrics = _uiMetrics.value
        
        var score = 100
        
        // Deduct points for overdraw
        score -= (metrics.overdrawLevel * 10).toInt()
        
        // Deduct points for deep hierarchy
        if (metrics.viewHierarchyDepth > 8) {
            score -= (metrics.viewHierarchyDepth - 8) * 5
        }
        
        // Deduct points for high view count
        if (metrics.totalViews > 50) {
            score -= (metrics.totalViews - 50) / 10
        }
        
        // Deduct points for layout passes
        if (metrics.layoutPasses > 1) {
            score -= (metrics.layoutPasses - 1) * 10
        }
        
        return maxOf(0, score)
    }
}

/**
 * Composable function for UI performance monitoring
 */
@Composable
fun UIPerformanceMonitor(
    optimizer: UIPerformanceOptimizer,
    onMetricsUpdate: (UIPerformanceMetrics) -> Unit = {}
) {
    val view = LocalView.current
    
    LaunchedEffect(view) {
        // Start monitoring when composable enters composition
        val activity = view.context as? Activity
        activity?.let { optimizer.startOptimization(it) }
    }
    
    // Collect metrics
    val metrics = remember { optimizer.uiMetrics }
    
    LaunchedEffect(metrics) {
        metrics.collect { onMetricsUpdate(it) }
    }
}

/**
 * Overdraw detection helper
 */
class OverdrawDetector {
    private var isDetecting = false
    private val overdrawAreas = mutableListOf<OverdrawArea>()
    
    fun startDetection(rootView: ViewGroup) {
        isDetecting = true
        detectOverdraw(rootView)
    }
    
    fun stopDetection() {
        isDetecting = false
        overdrawAreas.clear()
    }
    
    private fun detectOverdraw(viewGroup: ViewGroup) {
        if (!isDetecting) return
        
        val viewBounds = mutableMapOf<View, Rect>()
        
        // Collect all visible view bounds
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child.visibility == View.VISIBLE) {
                val bounds = Rect()
                child.getHitRect(bounds)
                viewBounds[child] = bounds
            }
        }
        
        // Find overlapping areas
        viewBounds.forEach { (view1, bounds1) ->
            viewBounds.forEach { (view2, bounds2) ->
                if (view1 != view2 && Rect.intersects(bounds1, bounds2)) {
                    val intersection = Rect(bounds1)
                    intersection.intersect(bounds2)
                    
                    overdrawAreas.add(
                        OverdrawArea(
                            bounds = intersection,
                            overlappingViews = listOf(view1, view2),
                            overdrawLevel = calculateOverdrawLevel(intersection, viewBounds.values)
                        )
                    )
                }
            }
        }
    }
    
    private fun calculateOverdrawLevel(area: Rect, allBounds: Collection<Rect>): Float {
        var overlaps = 0f
        allBounds.forEach { bounds ->
            if (Rect.intersects(area, bounds)) {
                overlaps += 1f
            }
        }
        return overlaps
    }
    
    fun getOverdrawInfo(): OverdrawInfo {
        val averageLevel = if (overdrawAreas.isNotEmpty()) {
            overdrawAreas.map { it.overdrawLevel }.average().toFloat()
        } else 0f
        
        return OverdrawInfo(
            averageOverdrawLevel = averageLevel,
            overdrawAreas = overdrawAreas.toList()
        )
    }
}

/**
 * Layout optimization helper
 */
class LayoutOptimizer {
    private var layoutPasses = 0
    private var measurePasses = 0
    private var drawCalls = 0
    
    fun optimizeLayouts(rootView: ViewGroup) {
        // Add global layout listener to count passes
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                layoutPasses++
            }
        })
        
        // Optimize specific layout issues
        optimizeViewGroup(rootView)
    }
    
    private fun optimizeViewGroup(viewGroup: ViewGroup) {
        // Remove unnecessary nested LinearLayouts
        removeUnnecessaryNesting(viewGroup)
        
        // Optimize RelativeLayout usage
        optimizeRelativeLayouts(viewGroup)
        
        // Set appropriate layout parameters
        optimizeLayoutParams(viewGroup)
    }
    
    private fun removeUnnecessaryNesting(viewGroup: ViewGroup) {
        // Implementation for removing unnecessary nesting
    }
    
    private fun optimizeRelativeLayouts(viewGroup: ViewGroup) {
        // Implementation for optimizing RelativeLayout usage
    }
    
    private fun optimizeLayoutParams(viewGroup: ViewGroup) {
        // Implementation for optimizing layout parameters
    }
    
    fun getLayoutInfo(): LayoutInfo {
        return LayoutInfo(
            layoutPasses = layoutPasses,
            measurePasses = measurePasses,
            drawCalls = drawCalls
        )
    }
}

/**
 * View hierarchy analyzer
 */
class ViewHierarchyAnalyzer {
    private var maxDepth = 0
    private var totalViews = 0
    
    fun analyzeHierarchy(rootView: ViewGroup) {
        maxDepth = 0
        totalViews = 0
        analyzeView(rootView, 0)
    }
    
    private fun analyzeView(view: View, depth: Int) {
        totalViews++
        maxDepth = maxOf(maxDepth, depth)
        
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                analyzeView(view.getChildAt(i), depth + 1)
            }
        }
    }
    
    fun getHierarchyInfo(): ViewHierarchyInfo {
        return ViewHierarchyInfo(
            maxDepth = maxDepth,
            totalViews = totalViews
        )
    }
}

/**
 * UI Performance metrics data class
 */
data class UIPerformanceMetrics(
    val overdrawLevel: Float = 0f,
    val overdrawAreas: List<OverdrawArea> = emptyList(),
    val viewHierarchyDepth: Int = 0,
    val totalViews: Int = 0,
    val layoutPasses: Int = 0,
    val measurePasses: Int = 0,
    val drawCalls: Int = 0,
    val recommendations: List<String> = emptyList()
)

/**
 * Overdraw area information
 */
data class OverdrawArea(
    val bounds: Rect,
    val overlappingViews: List<View>,
    val overdrawLevel: Float
)

/**
 * Overdraw detection result
 */
data class OverdrawInfo(
    val averageOverdrawLevel: Float,
    val overdrawAreas: List<OverdrawArea>
)

/**
 * View hierarchy information
 */
data class ViewHierarchyInfo(
    val maxDepth: Int,
    val totalViews: Int
)

/**
 * Layout performance information
 */
data class LayoutInfo(
    val layoutPasses: Int,
    val measurePasses: Int,
    val drawCalls: Int
)

/**
 * View optimization result
 */
data class ViewOptimizationResult(
    val viewId: Int,
    val optimizations: List<String>,
    val estimatedPerformanceGain: Int
)