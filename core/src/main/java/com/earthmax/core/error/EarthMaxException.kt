package com.earthmax.core.error

/**
 * Base exception class for all EarthMAX application errors.
 * Provides a consistent error handling structure across the application.
 */
sealed class EarthMaxException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Network-related exceptions
     */
    sealed class NetworkException(message: String, cause: Throwable? = null) : EarthMaxException(message, cause) {
        class NoInternetConnection : NetworkException("No internet connection available")
        class Timeout : NetworkException("Network request timed out")
        class ServerError(code: Int, message: String) : NetworkException("Server error: $code - $message")
        class UnknownNetworkError(cause: Throwable) : NetworkException("Unknown network error", cause)
    }
    
    /**
     * Authentication-related exceptions
     */
    sealed class AuthException(message: String, cause: Throwable? = null) : EarthMaxException(message, cause) {
        class InvalidCredentials : AuthException("Invalid email or password")
        class UserNotFound : AuthException("User not found")
        class EmailAlreadyExists : AuthException("Email already registered")
        class TokenExpired : AuthException("Authentication token expired")
        class Unauthorized : AuthException("Unauthorized access")
    }
    
    /**
     * Data-related exceptions
     */
    sealed class DataException(message: String, cause: Throwable? = null) : EarthMaxException(message, cause) {
        class NotFound(resource: String) : DataException("$resource not found")
        class InvalidData(field: String) : DataException("Invalid data for field: $field")
        class DatabaseError(cause: Throwable) : DataException("Database operation failed", cause)
        class CacheError(cause: Throwable) : DataException("Cache operation failed", cause)
    }
    
    /**
     * Business logic exceptions
     */
    sealed class BusinessException(message: String, cause: Throwable? = null) : EarthMaxException(message, cause) {
        class EventCapacityReached : BusinessException("Event has reached maximum capacity")
        class EventAlreadyStarted : BusinessException("Cannot modify event that has already started")
        class InsufficientPermissions : BusinessException("Insufficient permissions for this operation")
        class InvalidOperation(operation: String) : BusinessException("Invalid operation: $operation")
    }
}