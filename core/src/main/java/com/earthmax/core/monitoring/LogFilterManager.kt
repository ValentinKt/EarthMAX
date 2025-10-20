package com.earthmax.core.monitoring

import com.earthmax.core.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages log filtering with advanced search and filter capabilities.
 * Provides real-time filtered log streams for monitoring dashboard.
 */
@Singleton
class LogFilterManager @Inject constructor() {
    
    data class FilterCriteria(
        val levels: Set<Logger.Level> = emptySet(),
        val tags: Set<String> = emptySet(),
        val searchQuery: String = "",
        val timeRange: TimeRange? = null,
        val excludePatterns: Set<String> = emptySet(),
        val includePatterns: Set<String> = emptySet(),
        val maxResults: Int = 1000,
        val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
    )
    
    data class TimeRange(
        val startTime: Long,
        val endTime: Long
    )
    
    enum class SortOrder {
        NEWEST_FIRST, OLDEST_FIRST, LEVEL_PRIORITY
    }
    
    data class FilterPreset(
        val name: String,
        val criteria: FilterCriteria
    )
    
    data class FilterStats(
        val totalLogs: Int,
        val filteredLogs: Int,
        val levelCounts: Map<Logger.Level, Int>,
        val tagCounts: Map<String, Int>,
        val timeSpan: Long
    )
    
    private val _currentFilter = MutableStateFlow(FilterCriteria())
    val currentFilter: StateFlow<FilterCriteria> = _currentFilter.asStateFlow()
    
    private val _filterStats = MutableStateFlow(FilterStats(0, 0, emptyMap(), emptyMap(), 0))
    val filterStats: StateFlow<FilterStats> = _filterStats.asStateFlow()
    
    // Predefined filter presets
    private val filterPresets = mapOf(
        "errors_only" to FilterPreset(
            name = "errors_only",
            criteria = FilterCriteria(
                levels = setOf(Logger.Level.ERROR),
                sortOrder = SortOrder.NEWEST_FIRST
            )
        ),
        "warnings_and_errors" to FilterPreset(
            name = "warnings_and_errors",
            criteria = FilterCriteria(
                levels = setOf(Logger.Level.WARNING, Logger.Level.ERROR),
                sortOrder = SortOrder.LEVEL_PRIORITY
            )
        ),
        "network_logs" to FilterPreset(
            name = "network_logs",
            criteria = FilterCriteria(
                tags = setOf("NETWORK", "API", "HTTP"),
                sortOrder = SortOrder.NEWEST_FIRST
            )
        ),
        "user_actions" to FilterPreset(
            name = "user_actions",
            criteria = FilterCriteria(
                tags = setOf("USER_ACTION", "AUTH", "PROFILE"),
                sortOrder = SortOrder.NEWEST_FIRST
            )
        ),
        "performance_issues" to FilterPreset(
            name = "performance_issues",
            criteria = FilterCriteria(
                includePatterns = setOf("slow", "timeout", "performance", "latency"),
                levels = setOf(Logger.Level.WARNING, Logger.Level.ERROR),
                sortOrder = SortOrder.NEWEST_FIRST
            )
        ),
        "last_hour" to FilterPreset(
            name = "last_hour",
            criteria = FilterCriteria(
                timeRange = TimeRange(
                    startTime = System.currentTimeMillis() - (60 * 60 * 1000),
                    endTime = System.currentTimeMillis()
                ),
                sortOrder = SortOrder.NEWEST_FIRST
            )
        )
    )
    
    /**
     * Apply filter criteria and get filtered logs
     */
    fun getFilteredLogs(): Flow<List<Logger.LogEntry>> {
        return currentFilter.map { criteria ->
            val allLogs = Logger.getLogEntries()
            val filtered = applyFilter(allLogs, criteria)
            updateFilterStats(allLogs, filtered)
            filtered
        }
    }
    
    /**
     * Update current filter criteria
     */
    fun updateFilter(criteria: FilterCriteria) {
        _currentFilter.value = criteria
    }
    
    /**
     * Add log level to current filter
     */
    fun addLogLevel(level: Logger.Level) {
        val current = _currentFilter.value
        _currentFilter.value = current.copy(
            levels = current.levels + level
        )
    }
    
    /**
     * Remove log level from current filter
     */
    fun removeLogLevel(level: Logger.Level) {
        val current = _currentFilter.value
        _currentFilter.value = current.copy(
            levels = current.levels - level
        )
    }
    
    /**
     * Add tag to current filter
     */
    fun addTag(tag: String) {
        val current = _currentFilter.value
        _currentFilter.value = current.copy(
            tags = current.tags + tag
        )
    }
    
    /**
     * Remove tag from current filter
     */
    fun removeTag(tag: String) {
        val current = _currentFilter.value
        _currentFilter.value = current.copy(
            tags = current.tags - tag
        )
    }
    
    /**
     * Set search query
     */
    fun setSearchQuery(query: String) {
        val current = _currentFilter.value
        _currentFilter.value = current.copy(searchQuery = query)
    }
    
    /**
     * Set time range filter
     */
    fun setTimeRange(startTime: Long, endTime: Long) {
        val current = _currentFilter.value
        _currentFilter.value = current.copy(
            timeRange = TimeRange(startTime, endTime)
        )
    }
    
    /**
     * Clear time range filter
     */
    fun clearTimeRange() {
        val current = _currentFilter.value
        _currentFilter.value = current.copy(timeRange = null)
    }
    
    /**
     * Apply predefined filter preset
     */
    fun applyPreset(presetName: String) {
        filterPresets[presetName]?.let { preset ->
            _currentFilter.value = preset.criteria
        }
    }
    
    /**
     * Get available filter presets
     */
    fun getAvailablePresets(): Map<String, FilterPreset> = filterPresets
    
    /**
     * Clear all filters
     */
    fun clearAllFilters() {
        _currentFilter.value = FilterCriteria()
    }
    
    /**
     * Clear filters (alias for clearAllFilters)
     */
    fun clearFilters() {
        clearAllFilters()
    }
    
    /**
     * Get unique tags from all logs
     */
    fun getAvailableTags(): Set<String> {
        return Logger.getLogEntries().map { it.tag }.toSet()
    }
    
    /**
     * Get logs matching specific error pattern
     */
    fun getLogsWithError(errorPattern: String): List<Logger.LogEntry> {
        val allLogs = Logger.getLogEntries()
        val pattern = Pattern.compile(errorPattern, Pattern.CASE_INSENSITIVE)
        
        return allLogs.filter { log ->
            log.level == Logger.Level.ERROR && 
            (pattern.matcher(log.message).find() || 
             log.throwable?.message?.let { pattern.matcher(it).find() } == true)
        }
    }
    
    /**
     * Get logs for specific time period
     */
    fun getLogsForTimePeriod(hours: Int): List<Logger.LogEntry> {
        val startTime = System.currentTimeMillis() - (hours * 60 * 60 * 1000)
        val endTime = System.currentTimeMillis()
        
        return Logger.getLogEntries().filter { log ->
            log.timestamp in startTime..endTime
        }
    }
    
    /**
     * Export filtered logs as text
     */
    fun exportFilteredLogs(criteria: FilterCriteria): String {
        val logs = applyFilter(Logger.getLogEntries(), criteria)
        val sb = StringBuilder()
        
        sb.appendLine("# Filtered Logs Export")
        sb.appendLine("# Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
        sb.appendLine("# Total logs: ${logs.size}")
        sb.appendLine()
        
        logs.forEach { log ->
            sb.appendLine("${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(java.util.Date(log.timestamp))} [${log.level}] ${log.tag}: ${log.message}")
            log.throwable?.let { throwable ->
                sb.appendLine("  Exception: ${throwable.message}")
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    private fun applyFilter(logs: List<Logger.LogEntry>, criteria: FilterCriteria): List<Logger.LogEntry> {
        var filtered = logs
        
        // Filter by log levels
        if (criteria.levels.isNotEmpty()) {
            filtered = filtered.filter { it.level in criteria.levels }
        }
        
        // Filter by tags
        if (criteria.tags.isNotEmpty()) {
            filtered = filtered.filter { it.tag in criteria.tags }
        }
        
        // Filter by search query
        if (criteria.searchQuery.isNotEmpty()) {
            val query = criteria.searchQuery.lowercase()
            filtered = filtered.filter { log ->
                log.message.lowercase().contains(query) ||
                log.tag.lowercase().contains(query) ||
                log.throwable?.message?.lowercase()?.contains(query) == true
            }
        }
        
        // Filter by time range
        criteria.timeRange?.let { timeRange ->
            filtered = filtered.filter { log ->
                log.timestamp in timeRange.startTime..timeRange.endTime
            }
        }
        
        // Apply exclude patterns
        if (criteria.excludePatterns.isNotEmpty()) {
            val excludeRegexes = criteria.excludePatterns.map { 
                Pattern.compile(it, Pattern.CASE_INSENSITIVE) 
            }
            filtered = filtered.filter { log ->
                excludeRegexes.none { regex ->
                    regex.matcher(log.message).find() ||
                    regex.matcher(log.tag).find()
                }
            }
        }
        
        // Apply include patterns
        if (criteria.includePatterns.isNotEmpty()) {
            val includeRegexes = criteria.includePatterns.map { 
                Pattern.compile(it, Pattern.CASE_INSENSITIVE) 
            }
            filtered = filtered.filter { log ->
                includeRegexes.any { regex ->
                    regex.matcher(log.message).find() ||
                    regex.matcher(log.tag).find()
                }
            }
        }
        
        // Sort logs
        filtered = when (criteria.sortOrder) {
            SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.timestamp }
            SortOrder.LEVEL_PRIORITY -> filtered.sortedWith(
                compareByDescending<Logger.LogEntry> { 
                    when (it.level) {
                        Logger.Level.ERROR -> 4
                        Logger.Level.WARNING -> 3
                        Logger.Level.INFO -> 2
                        Logger.Level.DEBUG -> 1
                    }
                }.thenByDescending { it.timestamp }
            )
        }
        
        // Limit results
        return filtered.take(criteria.maxResults)
    }
    
    private fun updateFilterStats(allLogs: List<Logger.LogEntry>, filteredLogs: List<Logger.LogEntry>) {
        val levelCounts = filteredLogs.groupingBy { it.level }.eachCount()
        val tagCounts = filteredLogs.groupingBy { it.tag }.eachCount()
        
        val timeSpan = if (filteredLogs.isNotEmpty()) {
            filteredLogs.maxOf { it.timestamp } - filteredLogs.minOf { it.timestamp }
        } else {
            0L
        }
        _filterStats.value = FilterStats(
            totalLogs = allLogs.size,
            filteredLogs = filteredLogs.size,
            levelCounts = levelCounts,
            tagCounts = tagCounts,
            timeSpan = timeSpan
        )
    }
}