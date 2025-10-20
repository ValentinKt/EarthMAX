package com.earthmax.feature.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.monitoring.LogFilterManager
import com.earthmax.core.monitoring.PerformanceMetricsCollector
import com.earthmax.core.utils.Logger
import com.earthmax.data.repository.PerformanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing monitoring dashboard state and data
 */
@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val performanceRepository: PerformanceRepository,
    private val metricsCollector: PerformanceMetricsCollector,
    private val logFilterManager: LogFilterManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MonitoringUiState())
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(MonitoringTab.OVERVIEW)
    val selectedTab: StateFlow<MonitoringTab> = _selectedTab.asStateFlow()
    
    private val _timeRange = MutableStateFlow(TimeRange.LAST_HOUR)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        Logger.enter("MonitoringViewModel", "init")
        
        // Start collecting real-time data
        collectRealtimeData()
        
        // Load initial data
        refreshData()
        
        Logger.exit("MonitoringViewModel", "init")
    }
    
    /**
     * Collect real-time performance metrics and system health
     */
    private fun collectRealtimeData() {
        Logger.enter("MonitoringViewModel", "collectRealtimeData")
        
        viewModelScope.launch {
            try {
                // Combine real-time metrics, system health, filtered logs, and filter stats
                combine(
                    performanceRepository.getRealtimeMetrics(metricsCollector),
                    performanceRepository.getRealtimeSystemHealth(metricsCollector),
                    performanceRepository.getFilteredLogs(logFilterManager),
                    performanceRepository.getFilterStats(logFilterManager)
                ) { metrics, health, logs, filterStats ->
                    RealtimeData(
                        metrics = metrics,
                        systemHealth = health,
                        filteredLogs = logs,
                        filterStats = filterStats
                    )
                }.catch { e ->
                    Logger.logError("MonitoringViewModel", "Failed to collect realtime data", e)
                    _errorMessage.value = "Failed to load real-time data: ${e.message}"
                }.collect { realtimeData ->
                    _uiState.value = _uiState.value.copy(
                        realtimeMetrics = realtimeData.metrics,
                        systemHealth = realtimeData.systemHealth,
                        filteredLogs = realtimeData.filteredLogs,
                        filterStats = realtimeData.filterStats
                    )
                }
            } catch (e: Exception) {
                Logger.logError("MonitoringViewModel", "Failed to setup realtime data collection", e)
                _errorMessage.value = "Failed to setup real-time monitoring: ${e.message}"
            }
        }
        
        Logger.exit("MonitoringViewModel", "collectRealtimeData")
    }
    
    /**
     * Refresh all monitoring data
     */
    fun refreshData() {
        Logger.enter("MonitoringViewModel", "refreshData")
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val currentTimeRange = _timeRange.value
                val (startTime, endTime) = getTimeRangeMillis(currentTimeRange)
                
                // Load historical data
                val metrics = performanceRepository.getMetricsForTimeRange(startTime, endTime)
                val logs = performanceRepository.getLogsForTimeRange(startTime, endTime)
                val statistics = performanceRepository.getPerformanceStatistics(startTime, endTime)
                
                _uiState.value = _uiState.value.copy(
                    historicalMetrics = metrics,
                    historicalLogs = logs,
                    performanceStatistics = statistics,
                    lastRefresh = System.currentTimeMillis()
                )
                
                Logger.logBusinessEvent(
                    "MonitoringViewModel",
                    "monitoring_data_refreshed",
                    mapOf(
                        "metrics_count" to metrics.size,
                        "logs_count" to logs.size,
                        "time_range" to currentTimeRange.name
                    )
                )
                
            } catch (e: Exception) {
                Logger.logError("MonitoringViewModel", "Failed to refresh monitoring data", e, mapOf(
                    "time_range" to _timeRange.value.name
                ))
                _errorMessage.value = "Failed to refresh data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        
        Logger.exit("MonitoringViewModel", "refreshData")
    }
    
    /**
     * Change selected tab
     */
    fun selectTab(tab: MonitoringTab) {
        Logger.enter("MonitoringViewModel", "selectTab", 
            "tab" to tab.name
        )
        
        _selectedTab.value = tab
        
        Logger.logBusinessEvent(
            "MonitoringViewModel",
            "monitoring_tab_selected",
            mapOf("tab" to tab.name)
        )
        
        Logger.exit("MonitoringViewModel", "selectTab")
    }
    
    /**
     * Change time range for historical data
     */
    fun changeTimeRange(newTimeRange: TimeRange) {
        Logger.enter("MonitoringViewModel", "changeTimeRange", 
            "old_range" to _timeRange.value.name,
            "new_range" to newTimeRange.name
        )
        
        _timeRange.value = newTimeRange
        refreshData()
        
        Logger.logBusinessEvent(
            "MonitoringViewModel",
            "monitoring_time_range_changed",
            mapOf(
                "old_range" to _timeRange.value.name,
                "new_range" to newTimeRange.name
            )
        )
        
        Logger.exit("MonitoringViewModel", "changeTimeRange")
    }
    
    /**
     * Update log filter
     */
    fun updateLogFilter(filter: LogFilterManager.FilterCriteria) {
        Logger.enter("MonitoringViewModel", "updateLogFilter", 
            "levels" to filter.levels.joinToString(",") { it.name },
            "tags_count" to filter.tags.size,
            "has_search" to filter.searchQuery.isNotEmpty()
        )
        
        logFilterManager.updateFilter(filter)
        
        Logger.logBusinessEvent(
            "MonitoringViewModel",
            "log_filter_updated",
            mapOf(
                "levels_count" to filter.levels.size,
                "tags_count" to filter.tags.size,
                "has_search" to filter.searchQuery.isNotEmpty(),
                "has_time_range" to (filter.timeRange != null)
            )
        )
        
        Logger.exit("MonitoringViewModel", "updateLogFilter")
    }
    
    /**
     * Apply log filter preset
     */
    fun applyLogFilterPreset(preset: LogFilterManager.FilterPreset) {
        Logger.enter("MonitoringViewModel", "applyLogFilterPreset", 
            "preset" to preset.name
        )
        
        logFilterManager.applyPreset(preset.name)
        
        Logger.logBusinessEvent(
            "MonitoringViewModel",
            "log_filter_preset_applied",
            mapOf("preset" to preset.name)
        )
        
        Logger.exit("MonitoringViewModel", "applyLogFilterPreset")
    }
    
    /**
     * Clear log filters
     */
    fun clearLogFilters() {
        Logger.enter("MonitoringViewModel", "clearLogFilters")
        
        logFilterManager.clearFilters()
        
        Logger.logBusinessEvent("MonitoringViewModel", "log_filters_cleared", emptyMap())
        
        Logger.exit("MonitoringViewModel", "clearLogFilters")
    }
    
    /**
     * Filter logs by level
     */
    fun filterLogsByLevel(level: Logger.Level) {
        Logger.enter("MonitoringViewModel", "filterLogsByLevel", "level" to level.name)
        
        val currentFilter = logFilterManager.currentFilter.value
        val newFilter = currentFilter.copy(
            levels = setOf(level)
        )
        logFilterManager.updateFilter(newFilter)
        
        Logger.exit("MonitoringViewModel", "filterLogsByLevel")
    }
    
    /**
     * Export performance data
     */
    fun exportPerformanceData() {
        Logger.enter("MonitoringViewModel", "exportPerformanceData")
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val currentTimeRange = _timeRange.value
                val (startTime, endTime) = getTimeRangeMillis(currentTimeRange)
                
                val exportData = performanceRepository.exportPerformanceData(startTime, endTime)
                
                _uiState.value = _uiState.value.copy(
                    exportData = exportData
                )
                
                Logger.logBusinessEvent(
                    "MonitoringViewModel",
                    "performance_data_exported",
                    mapOf(
                        "time_range" to currentTimeRange.name,
                        "data_size" to exportData.length
                    )
                )
                
            } catch (e: Exception) {
                Logger.logError("MonitoringViewModel", "Failed to export performance data", e, mapOf(
                    "time_range" to _timeRange.value.name
                ))
                _errorMessage.value = "Failed to export data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        
        Logger.exit("MonitoringViewModel", "exportPerformanceData")
    }
    
    /**
     * Store current metrics to database
     */
    fun storeCurrentMetrics() {
        Logger.enter("MonitoringViewModel", "storeCurrentMetrics")
        
        viewModelScope.launch {
            try {
                performanceRepository.storeCurrentMetrics()
                
                Logger.logBusinessEvent("MonitoringViewModel", "current_metrics_stored", emptyMap())
                
            } catch (e: Exception) {
                Logger.logError("MonitoringViewModel", "Failed to store current metrics", e)
                _errorMessage.value = "Failed to store metrics: ${e.message}"
            }
        }
        
        Logger.exit("MonitoringViewModel", "storeCurrentMetrics")
    }
    
    /**
     * Clean up old performance data
     */
    fun cleanupOldData(olderThanDays: Int = 7) {
        Logger.enter("MonitoringViewModel", "cleanupOldData", "olderThanDays" to olderThanDays)
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                performanceRepository.cleanupOldData(olderThanDays)
                
                // Refresh data after cleanup
                refreshData()
                
                Logger.logBusinessEvent(
                    "MonitoringViewModel",
                    "old_data_cleaned",
                    mapOf("olderThanDays" to olderThanDays)
                )
                
            } catch (e: Exception) {
                Logger.logError("MonitoringViewModel", "Failed to cleanup old data", e, mapOf(
                    "olderThanDays" to olderThanDays
                ))
                _errorMessage.value = "Failed to cleanup data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        
        Logger.exit("MonitoringViewModel", "cleanupOldData")
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Get time range in milliseconds
     */
    private fun getTimeRangeMillis(timeRange: TimeRange): Pair<Long, Long> {
        val endTime = System.currentTimeMillis()
        val startTime = when (timeRange) {
            TimeRange.LAST_HOUR -> endTime - (60 * 60 * 1000L)
            TimeRange.LAST_6_HOURS -> endTime - (6 * 60 * 60 * 1000L)
            TimeRange.LAST_24_HOURS -> endTime - (24 * 60 * 60 * 1000L)
            TimeRange.LAST_7_DAYS -> endTime - (7 * 24 * 60 * 60 * 1000L)
            TimeRange.LAST_30_DAYS -> endTime - (30 * 24 * 60 * 60 * 1000L)
        }
        return startTime to endTime
    }
    
    override fun onCleared() {
        Logger.enter("MonitoringViewModel", "onCleared")
        super.onCleared()
        Logger.exit("MonitoringViewModel", "onCleared")
    }
    
    /**
     * Data classes for UI state management
     */
    data class MonitoringUiState(
        val realtimeMetrics: PerformanceMetricsCollector.AggregatedMetrics? = null,
        val realtimeData: RealtimeData? = null,
        val systemHealth: PerformanceMetricsCollector.SystemHealth? = null,
        val historicalMetrics: List<Logger.PerformanceMetric> = emptyList(),
        val historicalLogs: List<Logger.LogEntry> = emptyList(),
        val filteredLogs: List<Logger.LogEntry> = emptyList(),
        val performanceStatistics: PerformanceRepository.PerformanceStatistics? = null,
        val performanceMetrics: PerformanceMetricsCollector.AggregatedMetrics? = null,
        val filterStats: LogFilterManager.FilterStats? = null,
        val exportData: String? = null,
        val lastRefresh: Long = 0L,
        val healthAlerts: List<String> = emptyList(),
        val selectedLogLevel: Logger.Level? = null,
        val slowOperations: List<SlowOperation> = emptyList(),
        val recentLogs: List<Logger.LogEntry> = emptyList()
    )
    
    data class RealtimeData(
        val metrics: PerformanceMetricsCollector.AggregatedMetrics,
        val systemHealth: PerformanceMetricsCollector.SystemHealth,
        val filteredLogs: List<Logger.LogEntry>,
        val filterStats: LogFilterManager.FilterStats
    )
    
    enum class MonitoringTab {
        OVERVIEW,
        PERFORMANCE,
        LOGS,
        HEALTH
    }
    
    enum class TimeRange(val displayName: String) {
        LAST_HOUR("Last Hour"),
        LAST_6_HOURS("Last 6 Hours"),
        LAST_24_HOURS("Last 24 Hours"),
        LAST_7_DAYS("Last 7 Days"),
        LAST_30_DAYS("Last 30 Days")
    }
    
    /**
     * Data class representing a slow operation
     */
    data class SlowOperation(
        val name: String,
        val duration: Long,
        val details: String
    )
    
    /**
     * Data class representing performance data
     */
    data class PerformanceData(
        val cpuUsage: Double = 0.0,
        val memoryUsage: Double = 0.0,
        val networkIO: Double = 0.0,
        val diskIO: Double = 0.0,
        val responseTime: Double = 0.0,
        val throughput: Double = 0.0
    )
    
    /**
     * Data class representing a log entry for UI display
     */
    data class LogEntry(
        val level: String,
        val message: String,
        val timestamp: Long,
        val tag: String = ""
    )
}