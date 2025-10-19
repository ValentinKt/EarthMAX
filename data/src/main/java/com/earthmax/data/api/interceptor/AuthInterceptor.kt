package com.earthmax.data.api.interceptor

import com.earthmax.core.utils.Logger
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor to add authentication headers to API requests
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
    }
    
    private var authToken: String? = null
    
    fun setAuthToken(token: String?) {
        Logger.enter(TAG, "setAuthToken", "hasToken" to (token != null))
        
        try {
            this.authToken = token
            
            Logger.logBusinessEvent(TAG, "Auth Token Set", mapOf(
                "hasToken" to (token != null),
                "tokenLength" to (token?.length ?: 0)
            ))
            
            Logger.d(TAG, "Auth token ${if (token != null) "set" else "cleared"}")
            
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to set auth token", e, mapOf(
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            throw e
        } finally {
            Logger.exit(TAG, "setAuthToken")
        }
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "intercept")
        
        try {
            val originalRequest = chain.request()
            val url = originalRequest.url.toString()
            val method = originalRequest.method
            
            Logger.logNetworkRequest(TAG, method, url)
            
            // Add authentication header if token is available
            val requestBuilder = originalRequest.newBuilder()
            
            val hasToken = authToken != null
            authToken?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer ${Logger.maskSensitiveData(token)}")
                Logger.d(TAG, "Added Authorization header to request")
            }
            
            // Add common headers
            requestBuilder
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
            
            Logger.d(TAG, "Added common headers (Content-Type, Accept)")
            
            val response = chain.proceed(requestBuilder.build())
            val responseTime = System.currentTimeMillis() - startTime
            
            Logger.logNetworkResponse(TAG, url, response.code, responseTime)
            
            Logger.logBusinessEvent(TAG, "Request Intercepted", mapOf(
                "method" to method,
                "url" to Logger.maskSensitiveData(url),
                "statusCode" to response.code,
                "responseTime" to responseTime,
                "hasAuthToken" to hasToken,
                "success" to response.isSuccessful
            ))
            
            Logger.logPerformance(TAG, "intercept", responseTime)
            
            return response
            
        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            Logger.logError(TAG, "Failed to intercept request", e, mapOf(
                "responseTime" to responseTime,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error"),
                "hasAuthToken" to (authToken != null)
            ))
            throw e
        } finally {
            Logger.exit(TAG, "intercept")
        }
    }
}