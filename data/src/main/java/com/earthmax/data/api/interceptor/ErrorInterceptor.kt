package com.earthmax.data.api.interceptor

import com.earthmax.core.utils.Logger
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
    
    companion object {
        private const val TAG = "ErrorInterceptor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "intercept")
        
        try {
            val request = chain.request()
            val url = request.url.toString()
            val method = request.method
            
            Logger.logNetworkRequest(TAG, method, url)
            
            val response = chain.proceed(request)
            val responseTime = System.currentTimeMillis() - startTime
            
            Logger.logNetworkResponse(TAG, url, response.code, responseTime)
            
            // Handle error responses
            if (!response.isSuccessful) {
                Logger.w(TAG, "Received error response: ${response.code} for $method $url")
                
                val errorBody = response.body?.string()
                Logger.d(TAG, "Error response body length: ${errorBody?.length ?: 0}")
                
                // Try to parse error response
                val apiError = try {
                    if (!errorBody.isNullOrEmpty()) {
                        val parsedError = gson.fromJson(errorBody, ApiError::class.java)
                        Logger.d(TAG, "Successfully parsed API error response")
                        parsedError
                    } else {
                        Logger.d(TAG, "Empty error body, creating default error")
                        createDefaultError(response.code, response.message)
                    }
                } catch (e: Exception) {
                    Logger.logError(TAG, "Failed to parse error response", e, mapOf(
                        "statusCode" to response.code,
                        "errorBodyLength" to (errorBody?.length ?: 0),
                        "errorType" to e.javaClass.simpleName,
                        "errorMessage" to (e.message ?: "Unknown error")
                    ))
                    createDefaultError(response.code, response.message)
                }
                
                // Create new response with parsed error
                val newErrorBody = gson.toJson(apiError).toResponseBody(response.body?.contentType())
                
                Logger.logBusinessEvent(TAG, "Error Response Processed", mapOf(
                    "method" to method,
                    "url" to Logger.maskSensitiveData(url),
                    "statusCode" to response.code,
                    "errorType" to apiError.error,
                    "errorMessage" to apiError.message,
                    "responseTime" to responseTime
                ))
                
                Logger.logPerformance(TAG, "intercept", responseTime)
                
                return response.newBuilder()
                    .body(newErrorBody)
                    .build()
            }
            
            Logger.logBusinessEvent(TAG, "Successful Response", mapOf(
                "method" to method,
                "url" to Logger.maskSensitiveData(url),
                "statusCode" to response.code,
                "responseTime" to responseTime
            ))
            
            Logger.logPerformance(TAG, "intercept", responseTime)
            
            return response
            
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
    
    private fun createDefaultError(code: Int, message: String): ApiError {
        Logger.enter(TAG, "createDefaultError", "code" to code, "message" to message)
        
        try {
            val errorType = getErrorTypeFromCode(code)
            val defaultMessage = message.ifEmpty { getDefaultMessageForCode(code) }
            
            val apiError = ApiError(
                error = errorType,
                message = defaultMessage,
                code = code
            )
            
            Logger.logBusinessEvent(TAG, "Default Error Created", mapOf(
                "code" to code,
                "errorType" to errorType,
                "message" to defaultMessage
            ))
            
            return apiError
            
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to create default error", e, mapOf(
                "code" to code,
                "message" to message,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            throw e
        } finally {
            Logger.exit(TAG, "createDefaultError")
        }
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