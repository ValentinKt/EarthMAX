package com.earthmax.core.monitoring

import com.earthmax.core.utils.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metrics collection utility for application monitoring
 */
@Singleton
class MetricsCollector @Inject constructor() {

    companion object {
        private const val TAG = "MetricsCollector"
    }

    private val counters = ConcurrentHashMap<String, AtomicLong>()
    private val gauges = ConcurrentHashMap<String, AtomicLong>()
    private val timers = ConcurrentHashMap<String, MutableList<Long>>()
    private val timerMutex = Mutex()

    /**
     * Increment a counter metric
     */
    suspend fun incrementCounter(name: String, value: Long = 1) {
        counters.computeIfAbsent(name) { AtomicLong(0) }.addAndGet(value)
        Logger.d(TAG, "Counter incremented: $name = ${counters[name]?.get()}")
    }

    /**
     * Set a gauge metric value
     */
    suspend fun setGauge(name: String, value: Long) {
        gauges.computeIfAbsent(name) { AtomicLong(0) }.set(value)
        Logger.d(TAG, "Gauge set: $name = $value")
    }

    /**
     * Record a timer metric
     */
    suspend fun recordTimer(name: String, durationMs: Long) {
        timerMutex.withLock {
            timers.computeIfAbsent(name) { mutableListOf() }.add(durationMs)
        }
        Logger.d(TAG, "Timer recorded: $name = ${durationMs}ms")
    }

    /**
     * Get counter value
     */
    fun getCounter(name: String): Long {
        return counters[name]?.get() ?: 0L
    }

    /**
     * Get gauge value
     */
    fun getGauge(name: String): Long {
        return gauges[name]?.get() ?: 0L
    }

    /**
     * Get timer statistics
     */
    suspend fun getTimerStats(name: String): TimerStats? {
        return timerMutex.withLock {
            val values = timers[name] ?: return null
            if (values.isEmpty()) return null

            val sorted = values.sorted()
            val count = sorted.size
            val sum = sorted.sum()
            val avg = sum / count
            val min = sorted.first()
            val max = sorted.last()
            val median = if (count % 2 == 0) {
                (sorted[count / 2 - 1] + sorted[count / 2]) / 2
            } else {
                sorted[count / 2]
            }
            val p95 = sorted[(count * 0.95).toInt().coerceAtMost(count - 1)]
            val p99 = sorted[(count * 0.99).toInt().coerceAtMost(count - 1)]

            TimerStats(
                name = name,
                count = count,
                sum = sum,
                average = avg,
                min = min,
                max = max,
                median = median,
                p95 = p95,
                p99 = p99
            )
        }
    }

    /**
     * Get all counter metrics
     */
    fun getAllCounters(): Map<String, Long> {
        return counters.mapValues { it.value.get() }
    }

    /**
     * Get all gauge metrics
     */
    fun getAllGauges(): Map<String, Long> {
        return gauges.mapValues { it.value.get() }
    }

    /**
     * Get all timer names
     */
    fun getAllTimerNames(): Set<String> {
        return timers.keys.toSet()
    }

    /**
     * Clear all metrics
     */
    suspend fun clearAll() {
        counters.clear()
        gauges.clear()
        timerMutex.withLock {
            timers.clear()
        }
        Logger.i(TAG, "All metrics cleared")
    }

    /**
     * Clear specific metric type
     */
    suspend fun clearCounters() {
        counters.clear()
        Logger.i(TAG, "Counters cleared")
    }

    suspend fun clearGauges() {
        gauges.clear()
        Logger.i(TAG, "Gauges cleared")
    }

    suspend fun clearTimers() {
        timerMutex.withLock {
            timers.clear()
        }
        Logger.i(TAG, "Timers cleared")
    }

    /**
     * Log all metrics summary
     */
    suspend fun logSummary() {
        Logger.i(TAG, "=== Metrics Summary ===")
        
        // Log counters
        if (counters.isNotEmpty()) {
            Logger.i(TAG, "Counters:")
            counters.forEach { (name, value) ->
                Logger.i(TAG, "  $name: ${value.get()}")
            }
        }

        // Log gauges
        if (gauges.isNotEmpty()) {
            Logger.i(TAG, "Gauges:")
            gauges.forEach { (name, value) ->
                Logger.i(TAG, "  $name: ${value.get()}")
            }
        }

        // Log timer stats
        if (timers.isNotEmpty()) {
            Logger.i(TAG, "Timers:")
            timers.keys.forEach { name ->
                val stats = getTimerStats(name)
                if (stats != null) {
                    Logger.i(TAG, "  $name: count=${stats.count}, avg=${stats.average}ms, " +
                            "min=${stats.min}ms, max=${stats.max}ms, p95=${stats.p95}ms")
                }
            }
        }
        
        Logger.i(TAG, "=== End Summary ===")
    }

    /**
     * Timer statistics data class
     */
    data class TimerStats(
        val name: String,
        val count: Int,
        val sum: Long,
        val average: Long,
        val min: Long,
        val max: Long,
        val median: Long,
        val p95: Long,
        val p99: Long
    )
}