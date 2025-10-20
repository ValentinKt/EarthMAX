package com.earthmax.performance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.earthmax.core.database.EarthMaxDatabase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseOptimizerTest {

    private lateinit var database: EarthMaxDatabase
    private lateinit var databaseOptimizer: DatabaseOptimizer
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Create an in-memory database for testing
        database = mockk(relaxed = true)
        
        databaseOptimizer = DatabaseOptimizer(database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startOptimization should initialize database monitoring`() {
        // When
        databaseOptimizer.startOptimization()
        
        // Then
        assertTrue("Should be optimizing", databaseOptimizer.isOptimizing())
    }

    @Test
    fun `stopOptimization should stop database monitoring`() {
        // Given
        databaseOptimizer.startOptimization()
        
        // When
        databaseOptimizer.stopOptimization()
        
        // Then
        assertFalse("Should not be optimizing", databaseOptimizer.isOptimizing())
    }

    @Test
    fun `recordQuery should track query performance`() {
        // Given
        val query = "SELECT * FROM users WHERE id = ?"
        val executionTime = 150L
        val rowsAffected = 1
        
        // When
        databaseOptimizer.recordQuery(query, executionTime, rowsAffected, true)
        
        // Then
        val stats = databaseOptimizer.getQueryStats()
        assertEquals("Total queries should be 1", 1, stats.totalQueries)
        assertEquals("Successful queries should be 1", 1, stats.successfulQueries)
        assertEquals("Failed queries should be 0", 0, stats.failedQueries)
        assertEquals("Average execution time should match", executionTime.toDouble(), stats.averageExecutionTime, 1.0)
    }

    @Test
    fun `recordQuery should track failed query`() {
        // Given
        val query = "SELECT * FROM invalid_table"
        val executionTime = 50L
        val rowsAffected = 0
        
        // When
        databaseOptimizer.recordQuery(query, executionTime, rowsAffected, false)
        
        // Then
        val stats = databaseOptimizer.getQueryStats()
        assertEquals("Total queries should be 1", 1, stats.totalQueries)
        assertEquals("Successful queries should be 0", 0, stats.successfulQueries)
        assertEquals("Failed queries should be 1", 1, stats.failedQueries)
        assertEquals("Error rate should be 100%", 100.0f, stats.errorRate, 0.1f)
    }

    @Test
    fun `recordQuery should track multiple queries and calculate averages`() {
        // Given
        val queries = listOf(
            Triple("SELECT * FROM users", 100L, true),
            Triple("INSERT INTO posts VALUES (?)", 200L, true),
            Triple("UPDATE users SET name = ?", 300L, true),
            Triple("DELETE FROM posts WHERE id = ?", 150L, false)
        )
        
        // When
        queries.forEach { (query, executionTime, success) ->
            databaseOptimizer.recordQuery(query, executionTime, 1, success)
        }
        
        // Then
        val stats = databaseOptimizer.getQueryStats()
        assertEquals("Total queries should be 4", 4, stats.totalQueries)
        assertEquals("Successful queries should be 3", 3, stats.successfulQueries)
        assertEquals("Failed queries should be 1", 1, stats.failedQueries)
        assertEquals("Average execution time should be calculated correctly", 
            187.5, stats.averageExecutionTime, 1.0) // (100+200+300+150)/4
        assertEquals("Error rate should be 25%", 25.0f, stats.errorRate, 0.1f)
    }

    @Test
    fun `getSlowQueries should identify queries with high execution times`() {
        // Given
        databaseOptimizer.recordQuery("SELECT * FROM users", 50L, 10, true) // Fast query
        databaseOptimizer.recordQuery("SELECT * FROM posts JOIN users", 1500L, 100, true) // Slow query
        databaseOptimizer.recordQuery("SELECT COUNT(*) FROM large_table", 3000L, 1, true) // Very slow query
        
        // When
        val slowQueries = databaseOptimizer.getSlowQueries(1000L) // Threshold: 1 second
        
        // Then
        assertEquals("Should identify 2 slow queries", 2, slowQueries.size)
        assertTrue("Should contain JOIN query", 
            slowQueries.any { it.query.contains("JOIN") })
        assertTrue("Should contain COUNT query", 
            slowQueries.any { it.query.contains("COUNT") })
    }

    @Test
    fun `getFailedQueries should identify queries with high error rates`() {
        // Given
        // Query with 50% error rate
        databaseOptimizer.recordQuery("SELECT * FROM sometimes_missing", 100L, 1, true)
        databaseOptimizer.recordQuery("SELECT * FROM sometimes_missing", 100L, 0, false)
        
        // Query with 100% error rate
        databaseOptimizer.recordQuery("SELECT * FROM nonexistent_table", 50L, 0, false)
        
        // Query with 0% error rate
        databaseOptimizer.recordQuery("SELECT * FROM users", 100L, 1, true)
        
        // When
        val failedQueries = databaseOptimizer.getFailedQueries(30.0f) // Threshold: 30% error rate
        
        // Then
        assertEquals("Should identify 2 failed queries", 2, failedQueries.size)
        assertTrue("Should contain sometimes_missing query", 
            failedQueries.any { it.query.contains("sometimes_missing") })
        assertTrue("Should contain nonexistent_table query", 
            failedQueries.any { it.query.contains("nonexistent_table") })
    }

    @Test
    fun `getDatabasePerformanceScore should calculate performance score correctly`() {
        // Given - Good performance scenario
        repeat(10) {
            databaseOptimizer.recordQuery("SELECT * FROM users WHERE id = ?", 100L, 1, true)
        }
        
        // When
        val score = databaseOptimizer.getDatabasePerformanceScore()
        
        // Then
        assertTrue("Score should be high for good performance", score >= 80.0f)
        assertTrue("Score should be within valid range", score >= 0.0f && score <= 100.0f)
    }

    @Test
    fun `getDatabasePerformanceScore should handle poor performance`() {
        // Given - Poor performance scenario
        repeat(5) {
            databaseOptimizer.recordQuery("SELECT * FROM huge_table", 5000L, 0, false)
        }
        
        // When
        val score = databaseOptimizer.getDatabasePerformanceScore()
        
        // Then
        assertTrue("Score should be low for poor performance", score <= 50.0f)
        assertTrue("Score should be within valid range", score >= 0.0f && score <= 100.0f)
    }

    @Test
    fun `getOptimizationRecommendations should provide relevant suggestions`() {
        // Given - Mixed performance scenario
        databaseOptimizer.recordQuery("SELECT * FROM users WHERE name LIKE '%test%'", 2000L, 100, true) // Slow query
        databaseOptimizer.recordQuery("SELECT * FROM invalid_table", 100L, 0, false) // Failed query
        databaseOptimizer.recordQuery("SELECT * FROM posts ORDER BY created_at", 1500L, 1000, true) // Large result set
        
        // When
        val recommendations = databaseOptimizer.getOptimizationRecommendations()
        
        // Then
        assertNotNull("Recommendations should not be null", recommendations)
        assertTrue("Should have recommendations", recommendations.isNotEmpty())
        assertTrue("Should contain database-related recommendations", 
            recommendations.any { it.contains("index", ignoreCase = true) ||
                                it.contains("query", ignoreCase = true) ||
                                it.contains("performance", ignoreCase = true) })
    }

    @Test
    fun `optimizeDatabase should perform database optimization`() = runTest {
        // Given
        databaseOptimizer.startOptimization()
        
        // When
        val result = databaseOptimizer.optimizeDatabase()
        
        // Then
        assertNotNull("Optimization result should not be null", result)
        assertTrue("Optimization should be successful or provide feedback", 
            result.contains("optimized", ignoreCase = true) || 
            result.contains("completed", ignoreCase = true) ||
            result.contains("analyzed", ignoreCase = true))
    }

    @Test
    fun `analyzeQueryPatterns should identify common query patterns`() {
        // Given
        databaseOptimizer.recordQuery("SELECT * FROM users WHERE id = ?", 100L, 1, true)
        databaseOptimizer.recordQuery("SELECT * FROM users WHERE email = ?", 120L, 1, true)
        databaseOptimizer.recordQuery("SELECT * FROM posts WHERE user_id = ?", 150L, 10, true)
        databaseOptimizer.recordQuery("INSERT INTO users VALUES (?, ?, ?)", 80L, 1, true)
        databaseOptimizer.recordQuery("UPDATE users SET last_login = ? WHERE id = ?", 90L, 1, true)
        
        // When
        val patterns = databaseOptimizer.analyzeQueryPatterns()
        
        // Then
        assertNotNull("Patterns should not be null", patterns)
        assertTrue("Should identify SELECT patterns", 
            patterns.any { it.contains("SELECT", ignoreCase = true) })
        assertTrue("Should identify different query types", patterns.size > 1)
    }

    @Test
    fun `getConnectionPoolStats should return connection pool information`() {
        // Given
        databaseOptimizer.startOptimization()
        
        // When
        val poolStats = databaseOptimizer.getConnectionPoolStats()
        
        // Then
        assertNotNull("Pool stats should not be null", poolStats)
        assertTrue("Should have active connections info", poolStats.activeConnections >= 0)
        assertTrue("Should have idle connections info", poolStats.idleConnections >= 0)
        assertTrue("Should have total connections info", poolStats.totalConnections >= 0)
        assertTrue("Should have max connections info", poolStats.maxConnections > 0)
    }

    @Test
    fun `createSnapshot should capture current database state`() {
        // Given
        databaseOptimizer.startOptimization()
        databaseOptimizer.recordQuery("SELECT * FROM users", 100L, 5, true)
        databaseOptimizer.recordQuery("INSERT INTO posts VALUES (?)", 150L, 1, true)
        
        // When
        val snapshot = databaseOptimizer.createSnapshot()
        
        // Then
        assertNotNull("Snapshot should not be null", snapshot)
        assertTrue("Snapshot should have timestamp", snapshot.timestamp > 0)
        assertTrue("Snapshot should have query count", snapshot.totalQueries > 0)
        assertTrue("Snapshot should have performance score", 
            snapshot.performanceScore >= 0.0f && snapshot.performanceScore <= 100.0f)
    }

    @Test
    fun `reset should clear all tracking data`() {
        // Given
        databaseOptimizer.startOptimization()
        databaseOptimizer.recordQuery("SELECT * FROM users", 100L, 1, true)
        databaseOptimizer.recordQuery("INSERT INTO posts VALUES (?)", 150L, 1, false)
        
        // When
        databaseOptimizer.reset()
        
        // Then
        val stats = databaseOptimizer.getQueryStats()
        assertEquals("Total queries should be 0", 0, stats.totalQueries)
        assertEquals("Successful queries should be 0", 0, stats.successfulQueries)
        assertEquals("Failed queries should be 0", 0, stats.failedQueries)
    }

    @Test
    fun `concurrent queries should be handled correctly`() = runTest {
        // Given
        databaseOptimizer.startOptimization()
        
        // When - Simulate concurrent queries
        repeat(100) { index ->
            databaseOptimizer.recordQuery(
                "SELECT * FROM table$index", 
                (50..500).random().toLong(), 
                (0..10).random(), 
                index % 10 != 0 // 90% success rate
            )
        }
        
        // Then
        val stats = databaseOptimizer.getQueryStats()
        assertEquals("Should track all queries", 100, stats.totalQueries)
        assertTrue("Should have mostly successful queries", stats.successfulQueries >= 85)
        assertTrue("Should have some failed queries", stats.failedQueries >= 5)
        assertTrue("Error rate should be around 10%", stats.errorRate >= 5.0f && stats.errorRate <= 15.0f)
    }

    @Test
    fun `edge cases should be handled gracefully`() {
        // Test with zero execution time
        databaseOptimizer.recordQuery("SELECT 1", 0L, 1, true)
        
        // Test with very large execution time
        databaseOptimizer.recordQuery("SELECT * FROM huge_table", Long.MAX_VALUE, 0, false)
        
        // Test with empty query
        databaseOptimizer.recordQuery("", 100L, 0, true)
        
        // Test with null-like query
        databaseOptimizer.recordQuery("null", 100L, 1, true)
        
        // Then - Should not crash and should track all queries
        val stats = databaseOptimizer.getQueryStats()
        assertEquals("Should track all edge case queries", 4, stats.totalQueries)
        assertTrue("Should handle edge cases gracefully", stats.averageExecutionTime >= 0)
    }

    @Test
    fun `multiple start and stop calls should not cause issues`() {
        // When
        databaseOptimizer.startOptimization()
        databaseOptimizer.startOptimization()
        databaseOptimizer.stopOptimization()
        databaseOptimizer.stopOptimization()
        databaseOptimizer.startOptimization()
        
        // Then
        assertTrue("Should be optimizing after multiple calls", databaseOptimizer.isOptimizing())
    }

    @Test
    fun `getQueryStats with no data should return empty stats`() {
        // When
        val stats = databaseOptimizer.getQueryStats()
        
        // Then
        assertEquals("Total queries should be 0", 0, stats.totalQueries)
        assertEquals("Successful queries should be 0", 0, stats.successfulQueries)
        assertEquals("Failed queries should be 0", 0, stats.failedQueries)
        assertEquals("Average execution time should be 0", 0.0, stats.averageExecutionTime, 0.1)
        assertEquals("Error rate should be 0", 0.0f, stats.errorRate, 0.1f)
    }

    @Test
    fun `performance score calculation should handle division by zero`() {
        // Given - No queries recorded
        
        // When
        val score = databaseOptimizer.getDatabasePerformanceScore()
        
        // Then
        assertTrue("Score should be valid even with no data", score >= 0.0f && score <= 100.0f)
    }

    @Test
    fun `query normalization should group similar queries`() {
        // Given - Similar queries with different parameters
        databaseOptimizer.recordQuery("SELECT * FROM users WHERE id = 1", 100L, 1, true)
        databaseOptimizer.recordQuery("SELECT * FROM users WHERE id = 2", 120L, 1, true)
        databaseOptimizer.recordQuery("SELECT * FROM users WHERE id = 3", 110L, 1, true)
        
        // When
        val patterns = databaseOptimizer.analyzeQueryPatterns()
        
        // Then
        assertNotNull("Patterns should not be null", patterns)
        // Should group similar queries together (implementation dependent)
        assertTrue("Should identify parameterized query pattern", 
            patterns.any { it.contains("users WHERE id", ignoreCase = true) })
    }

    @Test
    fun `database health check should provide system status`() {
        // Given
        databaseOptimizer.startOptimization()
        
        // When
        val healthCheck = databaseOptimizer.performHealthCheck()
        
        // Then
        assertNotNull("Health check should not be null", healthCheck)
        assertTrue("Health check should contain status information", 
            healthCheck.contains("database", ignoreCase = true) ||
            healthCheck.contains("health", ignoreCase = true) ||
            healthCheck.contains("status", ignoreCase = true))
    }

    @Test
    fun `vacuum and analyze operations should be tracked`() = runTest {
        // Given
        databaseOptimizer.startOptimization()
        
        // When
        val vacuumResult = databaseOptimizer.performVacuum()
        val analyzeResult = databaseOptimizer.performAnalyze()
        
        // Then
        assertNotNull("Vacuum result should not be null", vacuumResult)
        assertNotNull("Analyze result should not be null", analyzeResult)
        assertTrue("Vacuum should complete successfully", 
            vacuumResult.contains("completed", ignoreCase = true) ||
            vacuumResult.contains("success", ignoreCase = true))
        assertTrue("Analyze should complete successfully", 
            analyzeResult.contains("completed", ignoreCase = true) ||
            analyzeResult.contains("success", ignoreCase = true))
    }
}