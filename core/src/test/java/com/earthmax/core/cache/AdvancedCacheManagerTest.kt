package com.earthmax.core.cache

import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.core.utils.Logger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedCacheManagerTest {

    private lateinit var logger: Logger
    private lateinit var metricsCollector: MetricsCollector
    private lateinit var cacheManager: AdvancedCacheManager
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        logger = mockk(relaxed = true)
        metricsCollector = mockk(relaxed = true)
        cacheManager = AdvancedCacheManager(logger, metricsCollector)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `put and get should work correctly`() = runTest {
        // Given
        val key = "test_key"
        val value = "test_value"
        val policy = CachePolicy.TimeToLive(ttl = 1.seconds)

        // When
        cacheManager.put(key, value, policy)
        val result = cacheManager.get<String>(key)

        // Then
        assertEquals(value, result)
        verify { metricsCollector.incrementCounter("cache_puts") }
        verify { metricsCollector.incrementCounter("cache_hits") }
    }

    @Test
    fun `get should return null for non-existent key`() = runTest {
        // When
        val result = cacheManager.get<String>("non_existent_key")

        // Then
        assertNull(result)
        verify { metricsCollector.incrementCounter("cache_misses") }
    }

    @Test
    fun `expired entries should be removed automatically`() = runTest {
        // Given
        val key = "test_key"
        val value = "test_value"
        val policy = CachePolicy.TimeToLive(ttl = 100.milliseconds)

        // When
        cacheManager.put(key, value, policy)
        delay(150) // Wait for expiration
        val result = cacheManager.get<String>(key)

        // Then
        assertNull(result)
        verify { metricsCollector.incrementCounter("cache_expired") }
    }

    @Test
    fun `getOrPut should return cached value if exists`() = runTest {
        // Given
        val key = "test_key"
        val cachedValue = "cached_value"
        val policy = CachePolicy.TimeToLive(ttl = 1.seconds)
        cacheManager.put(key, cachedValue, policy)

        // When
        val result = cacheManager.getOrPut(key, policy) { "new_value" }

        // Then
        assertEquals(cachedValue, result)
    }

    @Test
    fun `getOrPut should compute and cache value if not exists`() = runTest {
        // Given
        val key = "test_key"
        val computedValue = "computed_value"
        val policy = CachePolicy.TimeToLive(ttl = 1.seconds)

        // When
        val result = cacheManager.getOrPut(key, policy) { computedValue }

        // Then
        assertEquals(computedValue, result)
        assertEquals(computedValue, cacheManager.get<String>(key))
    }

    @Test
    fun `remove should delete entry and return true`() = runTest {
        // Given
        val key = "test_key"
        val value = "test_value"
        cacheManager.put(key, value)

        // When
        val removed = cacheManager.remove(key)
        val result = cacheManager.get<String>(key)

        // Then
        assertTrue(removed)
        assertNull(result)
        verify { metricsCollector.incrementCounter("cache_removals") }
    }

    @Test
    fun `remove should return false for non-existent key`() = runTest {
        // When
        val removed = cacheManager.remove("non_existent_key")

        // Then
        assertFalse(removed)
    }

    @Test
    fun `invalidate by key should remove specific entry`() = runTest {
        // Given
        val key1 = "key1"
        val key2 = "key2"
        cacheManager.put(key1, "value1")
        cacheManager.put(key2, "value2")

        // When
        cacheManager.invalidate(InvalidationStrategy.Key(key1))

        // Then
        assertNull(cacheManager.get<String>(key1))
        assertNotNull(cacheManager.get<String>(key2))
        verify { metricsCollector.incrementCounter("cache_invalidations") }
    }

    @Test
    fun `invalidate by pattern should remove matching entries`() = runTest {
        // Given
        cacheManager.put("user_1", "value1")
        cacheManager.put("user_2", "value2")
        cacheManager.put("product_1", "value3")

        // When
        cacheManager.invalidate(InvalidationStrategy.Pattern("user_*"))

        // Then
        assertNull(cacheManager.get<String>("user_1"))
        assertNull(cacheManager.get<String>("user_2"))
        assertNotNull(cacheManager.get<String>("product_1"))
    }

    @Test
    fun `invalidate by tag should remove tagged entries`() = runTest {
        // Given
        val tag = "user_data"
        cacheManager.put("key1", "value1", tags = setOf(tag))
        cacheManager.put("key2", "value2", tags = setOf(tag))
        cacheManager.put("key3", "value3", tags = setOf("other_tag"))

        // When
        cacheManager.invalidate(InvalidationStrategy.Tag(tag))

        // Then
        assertNull(cacheManager.get<String>("key1"))
        assertNull(cacheManager.get<String>("key2"))
        assertNotNull(cacheManager.get<String>("key3"))
    }

    @Test
    fun `invalidate all should clear entire cache`() = runTest {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key3", "value3")

        // When
        cacheManager.invalidate(InvalidationStrategy.All)

        // Then
        assertNull(cacheManager.get<String>("key1"))
        assertNull(cacheManager.get<String>("key2"))
        assertNull(cacheManager.get<String>("key3"))
    }

    @Test
    fun `contains should return true for existing non-expired entry`() = runTest {
        // Given
        val key = "test_key"
        cacheManager.put(key, "value")

        // When
        val contains = cacheManager.contains(key)

        // Then
        assertTrue(contains)
    }

    @Test
    fun `contains should return false for expired entry`() = runTest {
        // Given
        val key = "test_key"
        val policy = CachePolicy.TimeToLive(ttl = 100.milliseconds)
        cacheManager.put(key, "value", policy)
        delay(150) // Wait for expiration

        // When
        val contains = cacheManager.contains(key)

        // Then
        assertFalse(contains)
    }

    @Test
    fun `getStats should return correct statistics`() = runTest {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2", tags = setOf("tag1"))
        cacheManager.put("key3", "value3", policy = CachePolicy.TimeToLive(ttl = 100.milliseconds))
        delay(150) // Let one entry expire

        // When
        val stats = cacheManager.getStats()

        // Then
        assertEquals(3, stats.totalEntries)
        assertEquals(1, stats.expiredEntries)
        assertTrue(stats.totalSizeBytes > 0)
        assertEquals(1, stats.tagCount)
    }

    @Test
    fun `clear should remove all entries`() = runTest {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key3", "value3")

        // When
        cacheManager.clear()
        val stats = cacheManager.getStats()

        // Then
        assertEquals(0, stats.totalEntries)
        assertEquals(0, stats.totalSizeBytes)
        assertEquals(0, stats.tagCount)
        verify { metricsCollector.incrementCounter("cache_clears") }
    }

    @Test
    fun `LRU policy should evict least recently used entries`() = runTest {
        // Given
        val policy = CachePolicy.LeastRecentlyUsed(maxSize = 2, ttl = 1.seconds)
        
        // When
        cacheManager.put("key1", "value1", policy)
        cacheManager.put("key2", "value2", policy)
        cacheManager.get<String>("key1") // Access key1 to make it more recently used
        cacheManager.put("key3", "value3", policy) // This should evict key2

        // Then
        assertNotNull(cacheManager.get<String>("key1"))
        assertNull(cacheManager.get<String>("key2"))
        assertNotNull(cacheManager.get<String>("key3"))
    }

    @Test
    fun `size-based policy should evict entries when size limit exceeded`() = runTest {
        // Given
        val policy = CachePolicy.SizeBased(maxSizeBytes = 200, ttl = 1.seconds)
        val largeValue = "x".repeat(150) // Large value to trigger size eviction

        // When
        cacheManager.put("key1", largeValue, policy)
        cacheManager.put("key2", largeValue, policy) // This should trigger eviction

        // Then
        val stats = cacheManager.getStats()
        assertTrue(stats.totalSizeBytes <= 200)
    }
}