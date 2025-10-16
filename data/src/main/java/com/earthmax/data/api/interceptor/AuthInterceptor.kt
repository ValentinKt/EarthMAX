package com.earthmax.data.api.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor to add authentication headers to API requests
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {
    
    private var authToken: String? = null
    
    fun setAuthToken(token: String?) {
        this.authToken = token
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Add authentication header if token is available
        val requestBuilder = originalRequest.newBuilder()
        
        authToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        // Add common headers
        requestBuilder
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
        
        return chain.proceed(requestBuilder.build())
    }
}