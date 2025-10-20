package com.earthmax.core.cache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Defines caching policies for different types of data
 */
sealed class CachePolicy {
    /**
     * No caching - always fetch fresh data
     */
    object NoCache : CachePolicy()
    
    /**
     * Cache with time-to-live (TTL)
     * @param ttl Time after which cache entry expires
     * @param refreshOnExpiry Whether to refresh cache when expired
     */
    data class TimeToLive(
        val ttl: Duration,
        val refreshOnExpiry: Boolean = true
    ) : CachePolicy()
    
    /**
     * Cache until manually invalidated
     */
    object Persistent : CachePolicy()
    
    /**
     * Cache with maximum number of entries (LRU eviction)
     * @param maxSize Maximum number of entries to keep
     * @param ttl Optional TTL for entries
     */
    data class LeastRecentlyUsed(
        val maxSize: Int,
        val ttl: Duration? = null
    ) : CachePolicy()
    
    /**
     * Cache with size-based eviction
     * @param maxSizeBytes Maximum cache size in bytes
     * @param ttl Optional TTL for entries
     */
    data class SizeBased(
        val maxSizeBytes: Long,
        val ttl: Duration? = null
    ) : CachePolicy()
    
    companion object {
        /**
         * Default policies for common data types
         */
        val USER_PROFILE = TimeToLive(ttl = 15.minutes)
        val EVENTS_LIST = TimeToLive(ttl = 5.minutes)
        val EVENT_DETAILS = TimeToLive(ttl = 10.minutes)
        val USER_PREFERENCES = Persistent
        val SEARCH_RESULTS = TimeToLive(ttl = 2.minutes)
        val IMAGES = LeastRecentlyUsed(maxSize = 100, ttl = 60.minutes)
        val TEMPORARY_DATA = TimeToLive(ttl = 1.minutes, refreshOnExpiry = false)
    }
}

/**
 * Cache invalidation strategies
 */
sealed class InvalidationStrategy {
    /**
     * Invalidate specific cache key
     */
    data class Key(val key: String) : InvalidationStrategy()
    
    /**
     * Invalidate all keys matching pattern
     */
    data class Pattern(val pattern: String) : InvalidationStrategy()
    
    /**
     * Invalidate all keys with specific tag
     */
    data class Tag(val tag: String) : InvalidationStrategy()
    
    /**
     * Invalidate entire cache
     */
    object All : InvalidationStrategy()
    
    /**
     * Invalidate expired entries only
     */
    object Expired : InvalidationStrategy()
}

/**
 * Cache entry metadata
 */
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Duration? = null,
    val tags: Set<String> = emptySet(),
    val size: Long = 0L,
    val accessCount: Int = 0,
    val lastAccessed: Long = System.currentTimeMillis()
) {
    /**
     * Check if cache entry is expired
     */
    fun isExpired(): Boolean {
        return ttl?.let { 
            System.currentTimeMillis() - timestamp > it.inWholeMilliseconds 
        } ?: false
    }
    
    /**
     * Create a copy with updated access information
     */
    fun accessed(): CacheEntry<T> {
        return copy(
            accessCount = accessCount + 1,
            lastAccessed = System.currentTimeMillis()
        )
    }
    
    /**
     * Check if entry matches any of the given tags
     */
    fun hasTag(tag: String): Boolean = tags.contains(tag)
    
    /**
     * Check if entry matches tag pattern
     */
    fun matchesTagPattern(pattern: String): Boolean {
        val regex = pattern.replace("*", ".*").toRegex()
        return tags.any { regex.matches(it) }
    }
}