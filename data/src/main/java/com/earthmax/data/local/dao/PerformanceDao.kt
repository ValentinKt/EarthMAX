package com.earthmax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.earthmax.data.local.entity.LogEntryEntity
import com.earthmax.data.local.entity.PerformanceMetricEntity

/**
 * DAO for performance metrics and log entries database operations
 */
@Dao
interface PerformanceDao {
    
    // Performance Metrics Operations
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<PerformanceMetricEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: PerformanceMetricEntity)
    
    @Query("SELECT * FROM performance_metrics WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getMetricsInTimeRange(startTime: Long, endTime: Long): List<PerformanceMetricEntity>
    
    @Query("SELECT * FROM performance_metrics WHERE operation = :operation ORDER BY timestamp DESC")
    suspend fun getMetricsByOperation(operation: String): List<PerformanceMetricEntity>
    
    @Query("SELECT * FROM performance_metrics WHERE tag = :tag ORDER BY timestamp DESC")
    suspend fun getMetricsByTag(tag: String): List<PerformanceMetricEntity>
    
    @Query("SELECT * FROM performance_metrics ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMetrics(limit: Int = 100): List<PerformanceMetricEntity>
    
    @Query("DELETE FROM performance_metrics WHERE timestamp < :cutoffTime")
    suspend fun deleteMetricsOlderThan(cutoffTime: Long): Int
    
    @Query("DELETE FROM performance_metrics")
    suspend fun deleteAllMetrics()
    
    @Query("SELECT COUNT(*) FROM performance_metrics")
    suspend fun getMetricsCount(): Int
    
    @Query("SELECT AVG(duration) FROM performance_metrics WHERE operation = :operation")
    suspend fun getAverageDurationForOperation(operation: String): Double?
    
    @Query("SELECT MAX(duration) FROM performance_metrics WHERE operation = :operation")
    suspend fun getMaxDurationForOperation(operation: String): Double?
    
    @Query("SELECT MIN(duration) FROM performance_metrics WHERE operation = :operation")
    suspend fun getMinDurationForOperation(operation: String): Double?
    
    // Log Entries Operations
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntryEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntryEntity)
    
    @Query("SELECT * FROM log_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getLogsInTimeRange(startTime: Long, endTime: Long): List<LogEntryEntity>
    
    @Query("SELECT * FROM log_entries WHERE level = :level ORDER BY timestamp DESC")
    suspend fun getLogsByLevel(level: String): List<LogEntryEntity>
    
    @Query("SELECT * FROM log_entries WHERE tag = :tag ORDER BY timestamp DESC")
    suspend fun getLogsByTag(tag: String): List<LogEntryEntity>
    
    @Query("SELECT * FROM log_entries WHERE message LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    suspend fun searchLogs(searchQuery: String): List<LogEntryEntity>
    
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<LogEntryEntity>
    
    @Query("DELETE FROM log_entries WHERE timestamp < :cutoffTime")
    suspend fun deleteLogsOlderThan(cutoffTime: Long): Int
    
    @Query("DELETE FROM log_entries")
    suspend fun deleteAllLogs()
    
    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun getLogsCount(): Int
    
    @Query("SELECT COUNT(*) FROM log_entries WHERE level = :level")
    suspend fun getLogsCountByLevel(level: String): Int
    
    @Query("SELECT DISTINCT tag FROM log_entries ORDER BY tag")
    suspend fun getAllLogTags(): List<String>
    
    @Query("SELECT DISTINCT operation FROM performance_metrics ORDER BY operation")
    suspend fun getAllOperations(): List<String>
    
    // Combined Operations
    
    @Query("SELECT COUNT(*) FROM log_entries WHERE level = 'ERROR' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getErrorCountInTimeRange(startTime: Long, endTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM log_entries WHERE level = 'WARNING' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getWarningCountInTimeRange(startTime: Long, endTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM performance_metrics WHERE duration > :threshold AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getSlowOperationsCount(threshold: Double, startTime: Long, endTime: Long): Int
}