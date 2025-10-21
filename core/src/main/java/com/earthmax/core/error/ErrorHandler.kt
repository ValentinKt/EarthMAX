package com.earthmax.core.error


import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handler for the EarthMAX application.
 * Converts generic exceptions to domain-specific exceptions and handles logging.
 */
@Singleton
class ErrorHandler @Inject constructor() {
    
    /**
     * Handles and converts exceptions to EarthMaxException
     */
    fun handleError(throwable: Throwable): EarthMaxException {
        // Don't handle cancellation exceptions
        if (throwable is CancellationException) {
            throw throwable
        }
        
        val earthMaxException = when (throwable) {
            is EarthMaxException -> throwable
            is UnknownHostException -> EarthMaxException.NetworkException.NoInternetConnection()
            is SocketTimeoutException -> EarthMaxException.NetworkException.Timeout()
            is IOException -> EarthMaxException.NetworkException.UnknownNetworkError(throwable)
            else -> mapGenericException(throwable)
        }
        
        // Log the error
        android.util.Log.e("ErrorHandler", "Error handled: ${earthMaxException.message}", earthMaxException)
        
        return earthMaxException
    }
    
    private fun mapGenericException(throwable: Throwable): EarthMaxException {
        return when {
            throwable.message?.contains("401") == true -> 
                EarthMaxException.AuthException.Unauthorized()
            throwable.message?.contains("404") == true -> 
                EarthMaxException.DataException.NotFound("Resource")
            throwable.message?.contains("timeout", ignoreCase = true) == true -> 
                EarthMaxException.NetworkException.Timeout()
            else -> EarthMaxException.NetworkException.UnknownNetworkError(throwable)
        }
    }
}