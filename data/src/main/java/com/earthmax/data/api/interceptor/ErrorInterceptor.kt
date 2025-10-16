package com.earthmax.data.api.interceptor

import com.earthmax.data.api.dto.ApiError
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor to handle API errors and provide consistent error responses
 */
@Singleton
class ErrorInterceptor @Inject constructor(
    private val gson: Gson
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Handle error responses
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            
            // Try to parse error response
            val apiError = try {
                if (!errorBody.isNullOrEmpty()) {
                    gson.fromJson(errorBody, ApiError::class.java)
                } else {
                    createDefaultError(response.code, response.message)
                }
            } catch (e: Exception) {
                createDefaultError(response.code, response.message)
            }
            
            // Create new response with parsed error
            val newErrorBody = gson.toJson(apiError).toResponseBody(response.body?.contentType())
            
            return response.newBuilder()
                .body(newErrorBody)
                .build()
        }
        
        return response
    }
    
    private fun createDefaultError(code: Int, message: String): ApiError {
        return ApiError(
            error = getErrorTypeFromCode(code),
            message = message.ifEmpty { getDefaultMessageForCode(code) },
            code = code
        )
    }
    
    private fun getErrorTypeFromCode(code: Int): String {
        return when (code) {
            400 -> "BAD_REQUEST"
            401 -> "UNAUTHORIZED"
            403 -> "FORBIDDEN"
            404 -> "NOT_FOUND"
            409 -> "CONFLICT"
            422 -> "VALIDATION_ERROR"
            429 -> "RATE_LIMIT_EXCEEDED"
            500 -> "INTERNAL_SERVER_ERROR"
            502 -> "BAD_GATEWAY"
            503 -> "SERVICE_UNAVAILABLE"
            504 -> "GATEWAY_TIMEOUT"
            else -> "UNKNOWN_ERROR"
        }
    }
    
    private fun getDefaultMessageForCode(code: Int): String {
        return when (code) {
            400 -> "Bad request"
            401 -> "Unauthorized access"
            403 -> "Access forbidden"
            404 -> "Resource not found"
            409 -> "Resource conflict"
            422 -> "Validation failed"
            429 -> "Too many requests"
            500 -> "Internal server error"
            502 -> "Bad gateway"
            503 -> "Service unavailable"
            504 -> "Gateway timeout"
            else -> "Unknown error occurred"
        }
    }
}