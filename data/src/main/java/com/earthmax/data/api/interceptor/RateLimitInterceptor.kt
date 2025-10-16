package com.earthmax.data.api.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor to implement client-side rate limiting
 */
@Singleton
class RateLimitInterceptor @Inject constructor() : Interceptor {
    
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val windowStartTimes = ConcurrentHashMap<String, AtomicLong>()
    
    // Rate limit: 100 requests per minute per endpoint
    private val maxRequestsPerWindow = 100
    private val windowSizeMs = 60_000L // 1 minute
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val endpoint = getEndpointKey(request.url.encodedPath)
        
        if (isRateLimited(endpoint)) {
            // Return 429 Too Many Requests
            throw RateLimitExceededException("Rate limit exceeded for endpoint: $endpoint")
        }
        
        incrementRequestCount(endpoint)
        return chain.proceed(request)
    }
    
    private fun getEndpointKey(path: String): String {
        // Group similar endpoints (e.g., /users/123 -> /users/{id})
        return path.replace(Regex("/\\d+"), "/{id}")
    }
    
    private fun isRateLimited(endpoint: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val windowStart = windowStartTimes.getOrPut(endpoint) { AtomicLong(currentTime) }
        val requestCount = requestCounts.getOrPut(endpoint) { AtomicInteger(0) }
        
        // Check if we're in a new window
        if (currentTime - windowStart.get() >= windowSizeMs) {
            // Reset window
            windowStart.set(currentTime)
            requestCount.set(0)
            return false
        }
        
        return requestCount.get() >= maxRequestsPerWindow
    }
    
    private fun incrementRequestCount(endpoint: String) {
        requestCounts.getOrPut(endpoint) { AtomicInteger(0) }.incrementAndGet()
    }
}

/**
 * Exception thrown when rate limit is exceeded
 */
class RateLimitExceededException(message: String) : Exception(message)