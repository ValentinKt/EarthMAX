package com.earthmax.core.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Generic cache manager for the EarthMAX application.
 * Provides in-memory caching with TTL (Time To Live) support.
 */
@Singleton
class CacheManager @Inject constructor() {
    
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Instant,
        val ttl: Duration
    ) {
        fun isExpired(): Boolean {
            return Clock.System.now() > timestamp + ttl
        }
    }
    
    private val cache = mutableMapOf<String, CacheEntry<*>>()
    private val mutex = Mutex()
    
    /**
     * Store data in cache with specified TTL
     */
    suspend fun <T> put(key: String, data: T, ttl: Duration = 5.minutes) {
        mutex.withLock {
            cache[key] = CacheEntry(data, Clock.System.now(), ttl)
        }
    }
    
    /**
     * Retrieve data from cache if not expired
     */
    suspend fun <T> get(key: String): T? {
        mutex.withLock {
            val entry = cache[key] as? CacheEntry<T> ?: return null
            
            return if (entry.isExpired()) {
                cache.remove(key)
                null
            } else {
                entry.data
            }
        }
    }
    
    /**
     * Remove specific key from cache
     */
    suspend fun remove(key: String) {
        mutex.withLock {
            cache.remove(key)
        }
    }
    
    /**
     * Clear all cache entries
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
    
    /**
     * Remove expired entries from cache
     */
    suspend fun cleanupExpired() {
        mutex.withLock {
            val expiredKeys = cache.filter { (_, entry) -> entry.isExpired() }.keys
            expiredKeys.forEach { cache.remove(it) }
        }
    }
}