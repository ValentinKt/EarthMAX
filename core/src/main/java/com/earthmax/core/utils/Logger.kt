package com.earthmax.core.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

/**
 * Centralized logging utility for EarthMAX application.
 * Provides consistent log formatting, different log levels, sensitive data masking,
 * performance tracking, and log filtering capabilities.
 */
object Logger {
    
    // Log levels
    enum class Level(val priority: Int) {
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARNING(Log.WARN),
        ERROR(Log.ERROR)
    }
    
    // Performance metrics data classes
    data class PerformanceMetric(
        val operation: String,
        val tag: String,
        val duration: Long,
        val timestamp: Long,
        val additionalMetrics: Map<String, Any> = emptyMap()
    )
    
    data class LogEntry(
        val level: Level,
        val tag: String,
        val message: String,
        val timestamp: Long,
        val threadName: String,
        val throwable: Throwable? = null
    )
    
    // Log filtering
    data class LogFilter(
        val minLevel: Level? = null,
        val tags: Set<String>? = null,
        val excludeTags: Set<String>? = null,
        val contentFilter: String? = null,
        val timeRange: Pair<Long, Long>? = null
    )
    
    // Default log level - can be changed based on build variant
    private var currentLogLevel = Level.DEBUG
    
    // Performance tracking
    private val performanceMetrics = mutableListOf<PerformanceMetric>()
    private val performanceCounters = ConcurrentHashMap<String, AtomicLong>()
    private val performanceTimers = ConcurrentHashMap<String, Long>()
    
    // Log storage for filtering and monitoring
    private val logEntries = mutableListOf<LogEntry>()
    private val maxLogEntries = 1000 // Keep last 1000 log entries
    
    // Filtering
    private var currentFilter: LogFilter? = null
    
    // Date formatter for timestamps
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    // Patterns for sensitive data masking
    private val emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})")
    private val passwordPattern = Pattern.compile("(?i)(password|pwd|pass)\\s*[:=]\\s*[\"']?([^\\s\"',}]+)")
    private val tokenPattern = Pattern.compile("(?i)(token|key|secret|auth)\\s*[:=]\\s*[\"']?([a-zA-Z0-9+/=]{20,})")
    
    /**
     * Set the minimum log level to be displayed
     */
    fun setLogLevel(level: Level) {
        currentLogLevel = level
    }
    
    /**
     * Set log filter for monitoring dashboard
     */
    fun setLogFilter(filter: LogFilter?) {
        currentFilter = filter
    }
    
    /**
     * Get all log entries
     */
    fun getLogEntries(): List<LogEntry> {
        return logEntries.toList()
    }
    
    /**
     * Get filtered log entries
     */
    fun getFilteredLogs(): List<LogEntry> {
        val filter = currentFilter ?: return logEntries.toList()
        
        return logEntries.filter { entry ->
            // Level filter
            if (filter.minLevel != null && entry.level.priority < filter.minLevel.priority) {
                return@filter false
            }
            
            // Tag filters
            if (filter.tags != null && !filter.tags.contains(entry.tag)) {
                return@filter false
            }
            
            if (filter.excludeTags != null && filter.excludeTags.contains(entry.tag)) {
                return@filter false
            }
            
            // Content filter
            if (filter.contentFilter != null && !entry.message.contains(filter.contentFilter, ignoreCase = true)) {
                return@filter false
            }
            
            // Time range filter
            if (filter.timeRange != null) {
                val (startTime, endTime) = filter.timeRange
                if (entry.timestamp < startTime || entry.timestamp > endTime) {
                    return@filter false
                }
            }
            
            true
        }
    }
    
    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): List<PerformanceMetric> {
        return performanceMetrics.toList()
    }
    
    /**
     * Get performance counters
     */
    fun getPerformanceCounters(): Map<String, Long> {
        return performanceCounters.mapValues { it.value.get() }
    }
    
    /**
     * Clear performance metrics
     */
    fun clearPerformanceMetrics() {
        performanceMetrics.clear()
        performanceCounters.clear()
    }
    
    /**
     * Clear log entries
     */
    fun clearLogEntries() {
        logEntries.clear()
    }
    
    /**
     * Start performance timer
     */
    fun startTimer(operation: String): String {
        val timerId = "${operation}_${System.currentTimeMillis()}"
        performanceTimers[timerId] = System.currentTimeMillis()
        return timerId
    }
    
    /**
     * Stop performance timer and log result
     */
    fun stopTimer(timerId: String, tag: String, additionalMetrics: Map<String, Any> = emptyMap()) {
        val startTime = performanceTimers.remove(timerId) ?: return
        val duration = System.currentTimeMillis() - startTime
        val operation = timerId.substringBeforeLast("_")
        
        logPerformance(tag, operation, duration, additionalMetrics)
    }
    
    /**
     * Increment performance counter
     */
    fun incrementCounter(counterName: String, increment: Long = 1) {
        performanceCounters.computeIfAbsent(counterName) { AtomicLong(0) }.addAndGet(increment)
    }
    
    /**
     * Log a debug message
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.DEBUG, tag, message, throwable)
    }
    
    /**
     * Log an info message
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.INFO, tag, message, throwable)
    }
    
    /**
     * Log a warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARNING, tag, message, throwable)
    }
    
    /**
     * Log an error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, tag, message, throwable)
    }
    
    /**
     * Log method entry with parameters
     */
    fun enter(tag: String, methodName: String, vararg params: Pair<String, Any?>) {
        if (shouldLog(Level.DEBUG)) {
            val paramString = params.joinToString(", ") { "${it.first}=${maskSensitiveData(it.second.toString())}" }
            d(tag, "→ $methodName($paramString)")
        }
        incrementCounter("method_entries")
    }
    
    /**
     * Log method exit with result
     */
    fun exit(tag: String, methodName: String, result: Any? = null) {
        if (shouldLog(Level.DEBUG)) {
            val resultString = result?.let { " → ${maskSensitiveData(it.toString())}" } ?: ""
            d(tag, "← $methodName$resultString")
        }
        incrementCounter("method_exits")
    }
    
    /**
     * Log network request
     */
    fun logNetworkRequest(tag: String, method: String, url: String, headers: Map<String, String>? = null) {
        if (shouldLog(Level.DEBUG)) {
            d(tag, "🌐 $method $url")
            headers?.let { h ->
                val maskedHeaders = h.mapValues { maskSensitiveData(it.value) }
                d(tag, "Headers: $maskedHeaders")
            }
        }
        incrementCounter("network_requests")
        incrementCounter("network_requests_$method")
    }
    
    /**
     * Log network response
     */
    fun logNetworkResponse(tag: String, url: String, statusCode: Int, responseTime: Long? = null) {
        if (shouldLog(Level.DEBUG)) {
            val timeString = responseTime?.let { " (${it}ms)" } ?: ""
            d(tag, "📡 $statusCode $url$timeString")
        }
        incrementCounter("network_responses")
        incrementCounter("network_responses_${statusCode / 100}xx")
        
        responseTime?.let {
            logPerformance(tag, "network_response", it, mapOf(
                "statusCode" to statusCode,
                "url" to maskSensitiveData(url)
            ))
        }
    }
    
    /**
     * Log user action
     */
    fun logUserAction(tag: String, action: String, context: Map<String, Any>? = null) {
        if (shouldLog(Level.INFO)) {
            val contextString = context?.let { 
                " | " + it.entries.joinToString(", ") { "${it.key}=${maskSensitiveData(it.value.toString())}" }
            } ?: ""
            i(tag, "👤 User Action: $action$contextString")
        }
        incrementCounter("user_actions")
        incrementCounter("user_action_$action")
    }
    
    /**
     * Log business logic event
     */
    fun logBusinessEvent(tag: String, event: String, details: Map<String, Any>? = null) {
        if (shouldLog(Level.INFO)) {
            val detailsString = details?.let {
                " | " + it.entries.joinToString(", ") { "${it.key}=${maskSensitiveData(it.value.toString())}" }
            } ?: ""
            i(tag, "💼 Business Event: $event$detailsString")
        }
        incrementCounter("business_events")
        incrementCounter("business_event_$event")
    }
    
    /**
     * Log performance metrics
     */
    fun logPerformance(tag: String, operation: String, duration: Long, additionalMetrics: Map<String, Any>? = null) {
        if (shouldLog(Level.INFO)) {
            val metricsString = additionalMetrics?.let {
                " | " + it.entries.joinToString(", ") { "${it.key}=${it.value}" }
            } ?: ""
            i(tag, "⚡ Performance: $operation took ${duration}ms$metricsString")
        }
        
        // Store performance metric
        val metric = PerformanceMetric(
            operation = operation,
            tag = tag,
            duration = duration,
            timestamp = System.currentTimeMillis(),
            additionalMetrics = additionalMetrics ?: emptyMap()
        )
        
        synchronized(performanceMetrics) {
            performanceMetrics.add(metric)
            // Keep only last 500 performance metrics
            if (performanceMetrics.size > 500) {
                performanceMetrics.removeAt(0)
            }
        }
        
        incrementCounter("performance_logs")
        incrementCounter("performance_${operation}")
    }
    
    /**
     * Log error with additional context
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) {
        val contextString = context?.let {
            " | " + it.entries.joinToString(", ") { "${it.key}=${maskSensitiveData(it.value.toString())}" }
        } ?: ""
        e(tag, "❌ Error: $message$contextString", throwable)
        incrementCounter("errors")
        incrementCounter("error_${throwable?.javaClass?.simpleName ?: "unknown"}")
    }
    
    /**
     * Core logging method with consistent formatting
     */
    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        if (!shouldLog(level)) return
        
        val timestamp = System.currentTimeMillis()
        val threadName = Thread.currentThread().name
        val maskedMessage = maskSensitiveData(message)
        val formattedMessage = "[${dateFormatter.format(Date(timestamp))}] [$threadName] $maskedMessage"
        
        // Store log entry for monitoring
        val logEntry = LogEntry(level, tag, message, timestamp, threadName, throwable)
        synchronized(logEntries) {
            logEntries.add(logEntry)
            // Keep only last maxLogEntries
            if (logEntries.size > maxLogEntries) {
                logEntries.removeAt(0)
            }
        }
        
        // Log to Android Log
        when (level) {
            Level.DEBUG -> {
                if (throwable != null) {
                    Log.d(tag, formattedMessage, throwable)
                } else {
                    Log.d(tag, formattedMessage)
                }
            }
            Level.INFO -> {
                if (throwable != null) {
                    Log.i(tag, formattedMessage, throwable)
                } else {
                    Log.i(tag, formattedMessage)
                }
            }
            Level.WARNING -> {
                if (throwable != null) {
                    Log.w(tag, formattedMessage, throwable)
                } else {
                    Log.w(tag, formattedMessage)
                }
            }
            Level.ERROR -> {
                if (throwable != null) {
                    Log.e(tag, formattedMessage, throwable)
                } else {
                    Log.e(tag, formattedMessage)
                }
            }
        }
        
        // Update counters
        incrementCounter("total_logs")
        incrementCounter("logs_${level.name.lowercase()}")
        incrementCounter("logs_tag_$tag")
    }
    
    /**
     * Check if a log level should be displayed
     */
    private fun shouldLog(level: Level): Boolean {
        return level.priority >= currentLogLevel.priority
    }
    
    /**
     * Mask sensitive data in log messages
     */
    fun maskSensitiveData(input: String): String {
        var masked = input
        
        // Mask email addresses (keep first 2 chars and domain)
        val emailMatcher = emailPattern.matcher(masked)
        val emailBuffer = StringBuffer()
        while (emailMatcher.find()) {
            val localPart = emailMatcher.group(1)
            val domain = emailMatcher.group(2)
            val maskedLocal = if (localPart.length > 2) {
                localPart.substring(0, 2) + "*".repeat(localPart.length - 2)
            } else {
                "*".repeat(localPart.length)
            }
            emailMatcher.appendReplacement(emailBuffer, "$maskedLocal@$domain")
        }
        emailMatcher.appendTail(emailBuffer)
        masked = emailBuffer.toString()
        
        // Mask passwords completely
        val passwordMatcher = passwordPattern.matcher(masked)
        val passwordBuffer = StringBuffer()
        while (passwordMatcher.find()) {
            passwordMatcher.appendReplacement(passwordBuffer, "${passwordMatcher.group(1)}: ****")
        }
        passwordMatcher.appendTail(passwordBuffer)
        masked = passwordBuffer.toString()
        
        // Mask tokens/keys (show first 4 and last 4 characters)
        val tokenMatcher = tokenPattern.matcher(masked)
        val tokenBuffer = StringBuffer()
        while (tokenMatcher.find()) {
            val key = tokenMatcher.group(1)
            val value = tokenMatcher.group(2)
            val maskedValue = if (value.length > 8) {
                "${value.substring(0, 4)}****${value.substring(value.length - 4)}"
            } else {
                "****"
            }
            tokenMatcher.appendReplacement(tokenBuffer, "$key: $maskedValue")
        }
        tokenMatcher.appendTail(tokenBuffer)
        masked = tokenBuffer.toString()
        
        return masked
    }
}