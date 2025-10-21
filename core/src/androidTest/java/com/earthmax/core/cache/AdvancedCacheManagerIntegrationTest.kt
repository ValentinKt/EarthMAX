package com.earthmax.core.cache

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.earthmax.core.utils.Logger
import com.earthmax.core.monitoring.MetricsCollector
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvancedCacheManagerIntegrationTest {

    private lateinit var cacheManager: AdvancedCacheManager
    private lateinit var logger: Logger
    private lateinit var metricsCollector: MetricsCollector

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        logger = Logger()
        metricsCollector = MetricsCollector(logger)
        cacheManager = AdvancedCacheManager(logger, metricsCollector)
    }

    @After
    fun tearDown() {
        cacheManager.clear()
    }

    @Test
    fun testCacheManagerWithRealData() = runBlocking {
        // Test with complex data structures
        val testData = mapOf(
            "user_1" to mapOf("name" to "John", "age" to 30),
            "user_2" to mapOf("name" to "Jane", "age" to 25)
        )

        // Store data with TTL
        testData.forEach { (key, value) ->
            cacheManager.put(key, value, CachePolicy.TimeToLive(5000))
        }

        // Verify data is stored
        testData.forEach { (key, expectedValue) ->
            val cachedValue = cacheManager.get<Map<String, Any>>(key)
            assertEquals(expectedValue, cachedValue)
        }

        // Test pattern invalidation
        cacheManager.invalidate(InvalidationStrategy.Pattern("user_.*"))
        
        // Verify all user data is invalidated
        testData.keys.forEach { key ->
            assertNull(cacheManager.get<Map<String, Any>>(key))
        }
    }

    @Test
    fun testCacheManagerWithTags() = runBlocking {
        // Test tag-based caching
        cacheManager.put("profile_1", "Profile Data 1", CachePolicy.Persistent, setOf("profile", "user"))
        cacheManager.put("profile_2", "Profile Data 2", CachePolicy.Persistent, setOf("profile", "user"))
        cacheManager.put("settings_1", "Settings Data", CachePolicy.Persistent, setOf("settings"))

        // Verify data is stored
        assertEquals("Profile Data 1", cacheManager.get<String>("profile_1"))
        assertEquals("Profile Data 2", cacheManager.get<String>("profile_2"))
        assertEquals("Settings Data", cacheManager.get<String>("settings_1"))

        // Invalidate by tag
        cacheManager.invalidate(InvalidationStrategy.Tag("profile"))

        // Verify profile data is invalidated but settings remain
        assertNull(cacheManager.get<String>("profile_1"))
        assertNull(cacheManager.get<String>("profile_2"))
        assertEquals("Settings Data", cacheManager.get<String>("settings_1"))
    }

    @Test
    fun testCacheManagerWithSizeBasedEviction() = runBlocking {
        // Create cache manager with small size limit
        val smallCacheManager = AdvancedCacheManager(logger, metricsCollector)
        
        // Fill cache beyond capacity
        repeat(15) { index ->
            smallCacheManager.put(
                "key_$index", 
                "Large data string that takes up memory $index".repeat(100),
                CachePolicy.SizeBased(maxSize = 10)
            )
        }

        // Verify some entries were evicted
        val stats = smallCacheManager.getStats()
        assertTrue("Cache should have evicted some entries", stats.evictions > 0)
        assertTrue("Cache size should be limited", stats.size <= 10)
    }

    @Test
    fun testCacheManagerWithLRUEviction() = runBlocking {
        // Test LRU eviction policy
        repeat(8) { index ->
            cacheManager.put("lru_$index", "Data $index", CachePolicy.LeastRecentlyUsed(maxSize = 5))
        }

        // Access some entries to update their usage
        cacheManager.get<String>("lru_0")
        cacheManager.get<String>("lru_1")
        
        // Add more entries to trigger eviction
        repeat(3) { index ->
            cacheManager.put("new_$index", "New Data $index", CachePolicy.LeastRecentlyUsed(maxSize = 5))
        }

        // Verify LRU behavior - recently accessed items should still be present
        assertNotNull("Recently accessed item should remain", cacheManager.get<String>("lru_0"))
        assertNotNull("Recently accessed item should remain", cacheManager.get<String>("lru_1"))
        
        val stats = cacheManager.getStats()
        assertTrue("LRU eviction should have occurred", stats.evictions > 0)
    }

    @Test
    fun testCacheManagerMetrics() = runBlocking {
        // Perform various cache operations
        cacheManager.put("metrics_test", "test_data", CachePolicy.TimeToLive(1000))
        cacheManager.get<String>("metrics_test") // Hit
        cacheManager.get<String>("non_existent") // Miss
        
        // Wait for TTL expiration
        delay(1100)
        cacheManager.get<String>("metrics_test") // Miss (expired)

        val stats = cacheManager.getStats()
        
        assertTrue("Should have cache hits", stats.hits > 0)
        assertTrue("Should have cache misses", stats.misses > 0)
        assertTrue("Should have expired entries", stats.expired > 0)
        assertTrue("Hit rate should be calculated", stats.hitRate >= 0.0)
    }

    @Test
    fun testCacheManagerConcurrency() = runBlocking {
        // Test concurrent access to cache
        val jobs = (1..50).map { index ->
            kotlinx.coroutines.async {
                cacheManager.put("concurrent_$index", "Data $index", CachePolicy.Persistent)
                cacheManager.get<String>("concurrent_$index")
            }
        }

        // Wait for all operations to complete
        jobs.forEach { it.await() }

        // Verify all data was stored correctly
        repeat(50) { index ->
            assertEquals("Data $index", cacheManager.get<String>("concurrent_$index"))
        }

        val stats = cacheManager.getStats()
        assertEquals(50, stats.size)
    }
}