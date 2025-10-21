package com.earthmax.core.cache

import com.earthmax.core.config.AppConfig
import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.core.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Advanced cache manager with TTL, invalidation policies, and monitoring
 */
@Singleton
class AdvancedCacheManager @Inject constructor(
    private val logger: Logger,
    private val metricsCollector: MetricsCollector
) {
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val policies = ConcurrentHashMap<String, CachePolicy>()
    private val tagIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val currentSize = AtomicLong(0)
    
    private val _invalidationEvents = MutableSharedFlow<InvalidationEvent>()
    val invalidationEvents: Flow<InvalidationEvent> = _invalidationEvents.asSharedFlow()
    
    private val cleanupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        startPeriodicCleanup()
    }
    
    /**
     * Store data in cache with specified policy
     */
    suspend fun <T> put(
        key: String,
        data: T,
        policy: CachePolicy = CachePolicy.TimeToLive(ttl = 5.minutes),
        tags: Set<String> = emptySet()
    ) {
        withContext(Dispatchers.IO) {
            try {
                val size = estimateSize(data)
                val ttl = when (policy) {
                    is CachePolicy.TimeToLive -> policy.ttl
                    is CachePolicy.LeastRecentlyUsed -> policy.ttl
                    is CachePolicy.SizeBased -> policy.ttl
                    else -> null
                }
                
                val entry = CacheEntry(
                    data = data as Any,
                    ttl = ttl,
                    tags = tags,
                    size = size
                )
                
                // Check size constraints
                if (policy is CachePolicy.SizeBased && 
                    currentSize.get() + size > policy.maxSizeBytes) {
                    evictBySize(policy.maxSizeBytes - size)
                }
                
                // Check LRU constraints
                if (policy is CachePolicy.LeastRecentlyUsed && 
                    cache.size >= policy.maxSize) {
                    evictLeastRecentlyUsed(policy.maxSize - 1)
                }
                
                cache[key] = entry as CacheEntry<Any>
                policies[key] = policy
                currentSize.addAndGet(size)
                
                // Update tag index
                tags.forEach { tag ->
                    tagIndex.computeIfAbsent(tag) { mutableSetOf() }.add(key)
                }
                
                kotlinx.coroutines.GlobalScope.launch {
                    metricsCollector.incrementCounter("cache_puts")
                }
                logger.d("AdvancedCacheManager", "Cache put: key=$key, size=$size, tags=$tags")
                
            } catch (e: Exception) {
                logger.e("AdvancedCacheManager", "Failed to put cache entry: ${e.message}")
                kotlinx.coroutines.GlobalScope.launch {
                    metricsCollector.incrementCounter("cache_put_errors")
                }
            }
        }
    }
    
    /**
     * Retrieve data from cache
     */
    suspend fun <T> get(key: String): T? {
        return withContext(Dispatchers.IO) {
            try {
                val entry = cache[key] ?: run {
                    kotlinx.coroutines.GlobalScope.launch {
                        metricsCollector.incrementCounter("cache_misses")
                    }
                    return@withContext null
                }
                
                if (entry.isExpired()) {
                    remove(key)
                    kotlinx.coroutines.GlobalScope.launch {
                        metricsCollector.incrementCounter("cache_expired")
                    }
                    return@withContext null
                }
                
                // Update access information
                cache[key] = entry.accessed()
                kotlinx.coroutines.GlobalScope.launch {
                    metricsCollector.incrementCounter("cache_hits")
                }
                
                @Suppress("UNCHECKED_CAST")
                entry.data as T
                
            } catch (e: Exception) {
                logger.e("AdvancedCacheManager", "Failed to get cache entry: ${e.message}")
                kotlinx.coroutines.GlobalScope.launch {
                    metricsCollector.incrementCounter("cache_get_errors")
                }
                null
            }
        }
    }
    
    /**
     * Get data with fallback function if not in cache
     */
    suspend fun <T> getOrPut(
        key: String,
        policy: CachePolicy = CachePolicy.TimeToLive(ttl = 5.minutes),
        tags: Set<String> = emptySet(),
        producer: suspend () -> T
    ): T {
        return get<T>(key) ?: run {
            val data = producer()
            put(key, data, policy, tags)
            data
        }
    }
    
    /**
     * Remove specific cache entry
     */
    suspend fun remove(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val entry = cache.remove(key)
                policies.remove(key)
                
                entry?.let {
                    currentSize.addAndGet(-it.size)
                    
                    // Remove from tag index
                    it.tags.forEach { tag ->
                        tagIndex[tag]?.remove(key)
                        if (tagIndex[tag]?.isEmpty() == true) {
                            tagIndex.remove(tag)
                        }
                    }
                    
                    kotlinx.coroutines.GlobalScope.launch {
                        metricsCollector.incrementCounter("cache_removals")
                    }
                    logger.d("AdvancedCacheManager", "Cache remove: key=$key")
                    true
                } ?: false
                
            } catch (e: Exception) {
                logger.e("AdvancedCacheManager", "Failed to remove cache entry: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Invalidate cache entries based on strategy
     */
    suspend fun invalidate(strategy: InvalidationStrategy) {
        withContext(Dispatchers.IO) {
            try {
                val keysToRemove = when (strategy) {
                    is InvalidationStrategy.Key -> listOf(strategy.key)
                    is InvalidationStrategy.Pattern -> findKeysByPattern(strategy.pattern)
                    is InvalidationStrategy.Tag -> findKeysByTag(strategy.tag)
                    is InvalidationStrategy.All -> cache.keys.toList()
                    is InvalidationStrategy.Expired -> findExpiredKeys()
                }
                
                keysToRemove.forEach { key ->
                    remove(key)
                }
                
                val event = InvalidationEvent(strategy, keysToRemove.size)
                _invalidationEvents.emit(event)
                
                kotlinx.coroutines.GlobalScope.launch {
                    metricsCollector.incrementCounter("cache_invalidations")
                }
                logger.i("AdvancedCacheManager", "Cache invalidated: strategy=$strategy, count=${keysToRemove.size}")
                
            } catch (e: Exception) {
                logger.e("AdvancedCacheManager", "Failed to invalidate cache: ${e.message}")
            }
        }
    }
    
    /**
     * Check if key exists in cache and is not expired
     */
    suspend fun contains(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            val entry = cache[key]
            entry != null && !entry.isExpired()
        }
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getStats(): CacheStats {
        return withContext(Dispatchers.IO) {
            val totalEntries = cache.size
            val expiredEntries = cache.values.count { it.isExpired() }
            val totalSize = currentSize.get()
            val averageSize = if (totalEntries > 0) totalSize / totalEntries else 0L
            
            CacheStats(
                totalEntries = totalEntries,
                expiredEntries = expiredEntries,
                totalSizeBytes = totalSize,
                averageSizeBytes = averageSize,
                tagCount = tagIndex.size
            )
        }
    }
    
    /**
     * Clear entire cache
     */
    suspend fun clear() {
        withContext(Dispatchers.IO) {
            cache.clear()
            policies.clear()
            tagIndex.clear()
            currentSize.set(0)
            
            kotlinx.coroutines.GlobalScope.launch {
                metricsCollector.incrementCounter("cache_clears")
            }
            logger.i("AdvancedCacheManager", "Cache cleared")
        }
    }
    
    private fun startPeriodicCleanup() {
        cleanupScope.launch {
            while (isActive) {
                try {
                    delay(AppConfig.Cache.CLEANUP_INTERVAL_MS)
                    invalidate(InvalidationStrategy.Expired)
                } catch (e: Exception) {
                    logger.e("AdvancedCacheManager", "Error during cache cleanup: ${e.message}")
                }
            }
        }
    }
    
    private fun findKeysByPattern(pattern: String): List<String> {
        val regex = pattern.replace("*", ".*").toRegex()
        return cache.keys.filter { regex.matches(it) }
    }
    
    private fun findKeysByTag(tag: String): List<String> {
        return tagIndex[tag]?.toList() ?: emptyList()
    }
    
    private fun findExpiredKeys(): List<String> {
        return cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
    }
    
    private fun evictBySize(targetSize: Long) {
        val entries = cache.entries.sortedBy { it.value.lastAccessed }
        var freedSize = 0L
        
        for ((key, entry) in entries) {
            if (freedSize >= targetSize) break
            
            cache.remove(key)
            policies.remove(key)
            freedSize += entry.size
            currentSize.addAndGet(-entry.size)
            
            // Remove from tag index
            entry.tags.forEach { tag ->
                tagIndex[tag]?.remove(key)
                if (tagIndex[tag]?.isEmpty() == true) {
                    tagIndex.remove(tag)
                }
            }
        }
        
        kotlinx.coroutines.GlobalScope.launch {
            metricsCollector.incrementCounter("cache_size_evictions")
        }
        logger.d("AdvancedCacheManager", "Evicted by size: freed=$freedSize bytes")
    }
    
    private fun evictLeastRecentlyUsed(maxEntries: Int) {
        val entries = cache.entries
            .sortedBy { it.value.lastAccessed }
            .take(cache.size - maxEntries)
        
        entries.forEach { (key, entry) ->
            cache.remove(key)
            policies.remove(key)
            currentSize.addAndGet(-entry.size)
            
            // Remove from tag index
            entry.tags.forEach { tag ->
                tagIndex[tag]?.remove(key)
                if (tagIndex[tag]?.isEmpty() == true) {
                    tagIndex.remove(tag)
                }
            }
        }
        
        kotlinx.coroutines.GlobalScope.launch {
            metricsCollector.incrementCounter("cache_lru_evictions")
        }
        logger.d("AdvancedCacheManager", "Evicted LRU: count=${entries.size}")
    }
    
    private fun <T> estimateSize(data: T): Long {
        return when (data) {
            is String -> data.length * 2L // Rough estimate for UTF-16
            is ByteArray -> data.size.toLong()
            is List<*> -> data.size * 50L // Rough estimate
            is Map<*, *> -> data.size * 100L // Rough estimate
            else -> 100L // Default estimate
        }
    }
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val totalEntries: Int,
    val expiredEntries: Int,
    val totalSizeBytes: Long,
    val averageSizeBytes: Long,
    val tagCount: Int
)

/**
 * Cache invalidation event
 */
data class InvalidationEvent(
    val strategy: InvalidationStrategy,
    val affectedCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)