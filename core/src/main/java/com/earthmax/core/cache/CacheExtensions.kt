package com.earthmax.core.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Extension functions for advanced caching operations
 */

/**
 * Cache flow emissions with TTL
 */
fun <T> Flow<T>.cached(
    cacheManager: AdvancedCacheManager,
    keyProvider: (T) -> String,
    ttl: Duration = 5.minutes,
    tags: Set<String> = emptySet()
): Flow<T> = flow {
    collect { value ->
        val key = keyProvider(value)
        cacheManager.put(key, value, CachePolicy.TimeToLive(ttl), tags)
        emit(value)
    }
}

/**
 * Get cached value or compute and cache
 */
suspend fun <T> AdvancedCacheManager.computeIfAbsent(
    key: String,
    ttl: Duration = 5.minutes,
    tags: Set<String> = emptySet(),
    computation: suspend () -> T
): T {
    return getOrPut(key, CachePolicy.TimeToLive(ttl), tags, computation)
}

/**
 * Batch put operations
 */
suspend fun <T> AdvancedCacheManager.putAll(
    data: Map<String, T>,
    policy: CachePolicy = CachePolicy.TimeToLive(ttl = 5.minutes),
    tags: Set<String> = emptySet()
) {
    data.forEach { (key, value) ->
        put(key, value, policy, tags)
    }
}

/**
 * Batch get operations
 */
suspend fun <T> AdvancedCacheManager.getAll(keys: List<String>): Map<String, T?> {
    return keys.associateWith { key ->
        get<T>(key)
    }
}

/**
 * Get all non-null cached values
 */
suspend fun <T> AdvancedCacheManager.getAllPresent(keys: List<String>): Map<String, T> {
    return getAll<T>(keys).filterValues { it != null }.mapValues { it.value!! }
}

/**
 * Cache with automatic refresh
 */
suspend fun <T> AdvancedCacheManager.getWithRefresh(
    key: String,
    refreshThreshold: Duration = 1.minutes,
    policy: CachePolicy = CachePolicy.TimeToLive(ttl = 5.minutes),
    tags: Set<String> = emptySet(),
    producer: suspend () -> T
): T {
    val cached = get<CacheEntry<T>>(key)
    
    return if (cached != null && !cached.shouldRefresh(refreshThreshold)) {
        cached.data
    } else {
        val fresh = producer()
        put(key, fresh, policy, tags)
        fresh
    }
}

/**
 * Conditional cache operations
 */
suspend fun <T> AdvancedCacheManager.putIf(
    key: String,
    data: T,
    condition: suspend (T?) -> Boolean,
    policy: CachePolicy = CachePolicy.TimeToLive(ttl = 5.minutes),
    tags: Set<String> = emptySet()
): Boolean {
    val existing = get<T>(key)
    return if (condition(existing)) {
        put(key, data, policy, tags)
        true
    } else {
        false
    }
}

/**
 * Cache warming operations
 */
suspend fun <T> AdvancedCacheManager.warmUp(
    keys: List<String>,
    producer: suspend (String) -> T,
    policy: CachePolicy = CachePolicy.TimeToLive(ttl = 5.minutes),
    tags: Set<String> = emptySet()
) {
    keys.forEach { key ->
        if (!contains(key)) {
            val data = producer(key)
            put(key, data, policy, tags)
        }
    }
}

/**
 * Check if cache entry should be refreshed based on age
 */
private fun <T> CacheEntry<T>.shouldRefresh(threshold: Duration): Boolean {
    val age = System.currentTimeMillis() - createdAt
    return age > threshold.inWholeMilliseconds
}