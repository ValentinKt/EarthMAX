package com.earthmax.data.api.interceptor

import com.earthmax.core.utils.Logger
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
    
    companion object {
        private const val TAG = "RateLimitInterceptor"
    }
    
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val windowStartTimes = ConcurrentHashMap<String, AtomicLong>()
    
    // Rate limit: 100 requests per minute per endpoint
    private val maxRequestsPerWindow = 100
    private val windowSizeMs = 60_000L // 1 minute
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "intercept")
        
        try {
            val request = chain.request()
            val url = request.url.toString()
            val method = request.method
            val endpoint = getEndpointKey(request.url.encodedPath)
            
            Logger.logNetworkRequest(TAG, method, url)
            Logger.d(TAG, "Checking rate limit for endpoint: $endpoint")
            
            if (isRateLimited(endpoint)) {
                Logger.w(TAG, "Rate limit exceeded for endpoint: $endpoint")
                
                Logger.logBusinessEvent(TAG, "Rate Limit Exceeded", mapOf(
                    "endpoint" to endpoint,
                    "method" to method,
                    "url" to Logger.maskSensitiveData(url),
                    "maxRequests" to maxRequestsPerWindow,
                    "windowSizeMs" to windowSizeMs,
                    "currentCount" to (requestCounts[endpoint]?.get() ?: 0)
                ))
                
                // Return 429 Too Many Requests
                throw RateLimitExceededException("Rate limit exceeded for endpoint: $endpoint")
            }
            
            incrementRequestCount(endpoint)
            val currentCount = requestCounts[endpoint]?.get() ?: 0
            
            Logger.d(TAG, "Request allowed. Current count for $endpoint: $currentCount/$maxRequestsPerWindow")
            
            val response = chain.proceed(request)
            val responseTime = System.currentTimeMillis() - startTime
            
            Logger.logNetworkResponse(TAG, url, response.code, responseTime)
            
            Logger.logBusinessEvent(TAG, "Request Processed", mapOf(
                "endpoint" to endpoint,
                "method" to method,
                "url" to Logger.maskSensitiveData(url),
                "statusCode" to response.code,
                "responseTime" to responseTime,
                "currentCount" to currentCount,
                "maxRequests" to maxRequestsPerWindow,
                "success" to response.isSuccessful
            ))
            
            Logger.logPerformance(TAG, "intercept", responseTime)
            
            return response
            
        } catch (e: RateLimitExceededException) {
            val responseTime = System.currentTimeMillis() - startTime
            Logger.logError(TAG, "Rate limit exceeded", e, mapOf(
                "responseTime" to responseTime,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error"),
                "maxRequests" to maxRequestsPerWindow,
                "windowSizeMs" to windowSizeMs
            ))
            throw e
        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            Logger.logError(TAG, "Failed to intercept request", e, mapOf(
                "responseTime" to responseTime,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            throw e
        } finally {
            Logger.exit(TAG, "intercept")
        }
    }
    
    private fun getEndpointKey(path: String): String {
        Logger.enter(TAG, "getEndpointKey", "path" to path)
        
        try {
            // Group similar endpoints (e.g., /users/123 -> /users/{id})
            val endpointKey = path.replace(Regex("/\\d+"), "/{id}")
            
            Logger.d(TAG, "Mapped path '$path' to endpoint key '$endpointKey'")
            
            Logger.logBusinessEvent(TAG, "Endpoint Key Generated", mapOf(
                "originalPath" to path,
                "endpointKey" to endpointKey
            ))
            
            return endpointKey
            
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to generate endpoint key", e, mapOf(
                "path" to path,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            throw e
        } finally {
            Logger.exit(TAG, "getEndpointKey")
        }
    }
    
    private fun isRateLimited(endpoint: String): Boolean {
        Logger.enter(TAG, "isRateLimited", "endpoint" to endpoint)
        
        try {
            val currentTime = System.currentTimeMillis()
            val windowStart = windowStartTimes.getOrPut(endpoint) { AtomicLong(currentTime) }
            val requestCount = requestCounts.getOrPut(endpoint) { AtomicInteger(0) }
            
            val windowAge = currentTime - windowStart.get()
            val currentCount = requestCount.get()
            
            Logger.d(TAG, "Rate limit check - Endpoint: $endpoint, Count: $currentCount/$maxRequestsPerWindow, Window age: ${windowAge}ms")
            
            // Check if we're in a new window
            if (windowAge >= windowSizeMs) {
                Logger.d(TAG, "Resetting rate limit window for endpoint: $endpoint")
                
                // Reset window
                windowStart.set(currentTime)
                requestCount.set(0)
                
                Logger.logBusinessEvent(TAG, "Rate Limit Window Reset", mapOf(
                    "endpoint" to endpoint,
                    "previousCount" to currentCount,
                    "windowAge" to windowAge,
                    "windowSizeMs" to windowSizeMs
                ))
                
                return false
            }
            
            val isLimited = currentCount >= maxRequestsPerWindow
            
            Logger.logBusinessEvent(TAG, "Rate Limit Check", mapOf(
                "endpoint" to endpoint,
                "currentCount" to currentCount,
                "maxRequests" to maxRequestsPerWindow,
                "windowAge" to windowAge,
                "isLimited" to isLimited
            ))
            
            return isLimited
            
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to check rate limit", e, mapOf(
                "endpoint" to endpoint,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            throw e
        } finally {
            Logger.exit(TAG, "isRateLimited")
        }
    }
    
    private fun incrementRequestCount(endpoint: String) {
        Logger.enter(TAG, "incrementRequestCount", "endpoint" to endpoint)
        
        try {
            val newCount = requestCounts.getOrPut(endpoint) { AtomicInteger(0) }.incrementAndGet()
            
            Logger.d(TAG, "Incremented request count for $endpoint to $newCount")
            
            Logger.logBusinessEvent(TAG, "Request Count Incremented", mapOf(
                "endpoint" to endpoint,
                "newCount" to newCount,
                "maxRequests" to maxRequestsPerWindow
            ))
            
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to increment request count", e, mapOf(
                "endpoint" to endpoint,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            throw e
        } finally {
            Logger.exit(TAG, "incrementRequestCount")
        }
    }
}

/**
 * Exception thrown when rate limit is exceeded
 */
class RateLimitExceededException(message: String) : Exception(message)