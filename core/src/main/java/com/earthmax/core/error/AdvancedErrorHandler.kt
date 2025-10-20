package com.earthmax.core.error

import com.earthmax.core.config.AppConfig
import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.core.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Advanced error handler with retry mechanisms, circuit breakers, and monitoring
 */
@Singleton
class AdvancedErrorHandler @Inject constructor(
    private val logger: Logger,
    private val metricsCollector: MetricsCollector
) {
    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreaker>()
    private val retryPolicies = ConcurrentHashMap<String, RetryPolicy>()
    
    private val _errorEvents = MutableSharedFlow<ErrorEvent>()
    val errorEvents: Flow<ErrorEvent> = _errorEvents.asSharedFlow()
    
    /**
     * Handle error with retry and circuit breaker logic
     */
    suspend fun <T> handleWithRetry(
        operation: String,
        retryPolicy: RetryPolicy = RetryPolicy.exponentialBackoff(),
        circuitBreakerConfig: CircuitBreakerConfig? = null,
        action: suspend () -> T
    ): Result<T> {
        val circuitBreaker = circuitBreakerConfig?.let { config ->
            getOrCreateCircuitBreaker(operation, config)
        }
        
        // Check circuit breaker state
        if (circuitBreaker?.isOpen() == true) {
            val error = CircuitBreakerOpenException("Circuit breaker is open for operation: $operation")
            handleError(operation, error)
            return Result.failure(error)
        }
        
        return try {
            val result = executeWithRetry(operation, retryPolicy, action)
            circuitBreaker?.recordSuccess()
            metricsCollector.incrementCounter("error_handler_success", mapOf("operation" to operation))
            result
        } catch (e: Exception) {
            circuitBreaker?.recordFailure()
            handleError(operation, e)
            Result.failure(e)
        }
    }
    
    /**
     * Handle Flow with retry and error recovery
     */
    fun <T> Flow<T>.handleErrors(
        operation: String,
        retryPolicy: RetryPolicy = RetryPolicy.exponentialBackoff(),
        fallbackValue: T? = null
    ): Flow<T> {
        return this
            .retryWhen { cause, attempt ->
                val shouldRetry = shouldRetry(cause, attempt.toInt(), retryPolicy)
                if (shouldRetry) {
                    val delay = calculateDelay(attempt.toInt(), retryPolicy)
                    logger.warn("Retrying $operation (attempt ${attempt + 1}) after ${delay}ms", cause)
                    delay(delay.milliseconds)
                    metricsCollector.incrementCounter("error_handler_retries", mapOf("operation" to operation))
                }
                shouldRetry
            }
            .catch { cause ->
                handleError(operation, cause)
                fallbackValue?.let { emit(it) }
            }
    }
    
    /**
     * Execute operation with exponential backoff retry
     */
    private suspend fun <T> executeWithRetry(
        operation: String,
        retryPolicy: RetryPolicy,
        action: suspend () -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(retryPolicy.maxAttempts) { attempt ->
            try {
                return action()
            } catch (e: Exception) {
                lastException = e
                
                if (attempt < retryPolicy.maxAttempts - 1 && shouldRetry(e, attempt, retryPolicy)) {
                    val delay = calculateDelay(attempt, retryPolicy)
                    logger.warn("Retrying $operation (attempt ${attempt + 1}) after ${delay}ms", e)
                    delay(delay.milliseconds)
                    metricsCollector.incrementCounter("error_handler_retries", mapOf("operation" to operation))
                } else {
                    throw e
                }
            }
        }
        
        throw lastException ?: RuntimeException("Unknown error in retry logic")
    }
    
    /**
     * Handle and log error
     */
    private suspend fun handleError(operation: String, error: Throwable) {
        logger.error("Error in operation: $operation", error)
        
        val errorEvent = ErrorEvent(
            operation = operation,
            error = error,
            timestamp = System.currentTimeMillis(),
            severity = determineSeverity(error)
        )
        
        _errorEvents.emit(errorEvent)
        metricsCollector.incrementCounter("error_handler_errors", mapOf(
            "operation" to operation,
            "error_type" to error::class.simpleName.orEmpty(),
            "severity" to errorEvent.severity.name
        ))
    }
    
    /**
     * Get or create circuit breaker for operation
     */
    private fun getOrCreateCircuitBreaker(
        operation: String,
        config: CircuitBreakerConfig
    ): CircuitBreaker {
        return circuitBreakers.computeIfAbsent(operation) {
            CircuitBreaker(operation, config, logger, metricsCollector)
        }
    }
    
    /**
     * Determine if should retry based on error type and attempt
     */
    private fun shouldRetry(error: Throwable, attempt: Int, retryPolicy: RetryPolicy): Boolean {
        if (attempt >= retryPolicy.maxAttempts - 1) return false
        
        return when (error) {
            is CircuitBreakerOpenException -> false
            is SecurityException -> false
            is IllegalArgumentException -> false
            else -> retryPolicy.retryableExceptions.any { it.isInstance(error) }
        }
    }
    
    /**
     * Calculate delay for retry attempt
     */
    private fun calculateDelay(attempt: Int, retryPolicy: RetryPolicy): Long {
        return when (retryPolicy.strategy) {
            RetryStrategy.FIXED -> retryPolicy.baseDelay.inWholeMilliseconds
            RetryStrategy.LINEAR -> retryPolicy.baseDelay.inWholeMilliseconds * (attempt + 1)
            RetryStrategy.EXPONENTIAL -> {
                val exponentialDelay = retryPolicy.baseDelay.inWholeMilliseconds * 
                    kotlin.math.pow(2.0, attempt.toDouble()).toLong()
                kotlin.math.min(exponentialDelay, retryPolicy.maxDelay.inWholeMilliseconds)
            }
        }
    }
    
    /**
     * Determine error severity
     */
    private fun determineSeverity(error: Throwable): ErrorSeverity {
        return when (error) {
            is OutOfMemoryError -> ErrorSeverity.CRITICAL
            is SecurityException -> ErrorSeverity.HIGH
            is IllegalStateException -> ErrorSeverity.HIGH
            is NetworkException -> ErrorSeverity.MEDIUM
            is ValidationException -> ErrorSeverity.LOW
            else -> ErrorSeverity.MEDIUM
        }
    }
    
    /**
     * Get circuit breaker status
     */
    fun getCircuitBreakerStatus(operation: String): CircuitBreakerStatus? {
        return circuitBreakers[operation]?.getStatus()
    }
    
    /**
     * Reset circuit breaker
     */
    fun resetCircuitBreaker(operation: String) {
        circuitBreakers[operation]?.reset()
        logger.info("Circuit breaker reset for operation: $operation")
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStats(): ErrorStats {
        val totalCircuitBreakers = circuitBreakers.size
        val openCircuitBreakers = circuitBreakers.values.count { it.isOpen() }
        
        return ErrorStats(
            totalCircuitBreakers = totalCircuitBreakers,
            openCircuitBreakers = openCircuitBreakers,
            circuitBreakerDetails = circuitBreakers.mapValues { it.value.getStatus() }
        )
    }
}

/**
 * Circuit breaker implementation
 */
class CircuitBreaker(
    private val operation: String,
    private val config: CircuitBreakerConfig,
    private val logger: Logger,
    private val metricsCollector: MetricsCollector
) {
    private var state = CircuitBreakerState.CLOSED
    private val failureCount = AtomicInteger(0)
    private var lastFailureTime = 0L
    private var nextAttemptTime = 0L
    
    fun isOpen(): Boolean = state == CircuitBreakerState.OPEN
    
    fun recordSuccess() {
        failureCount.set(0)
        state = CircuitBreakerState.CLOSED
        metricsCollector.incrementCounter("circuit_breaker_success", mapOf("operation" to operation))
    }
    
    fun recordFailure() {
        val failures = failureCount.incrementAndGet()
        lastFailureTime = System.currentTimeMillis()
        
        if (failures >= config.failureThreshold) {
            state = CircuitBreakerState.OPEN
            nextAttemptTime = lastFailureTime + config.timeout.inWholeMilliseconds
            logger.warn("Circuit breaker opened for operation: $operation (failures: $failures)")
            metricsCollector.incrementCounter("circuit_breaker_opened", mapOf("operation" to operation))
        }
    }
    
    fun reset() {
        failureCount.set(0)
        state = CircuitBreakerState.CLOSED
        nextAttemptTime = 0L
    }
    
    fun getStatus(): CircuitBreakerStatus {
        // Check if we should transition from OPEN to HALF_OPEN
        if (state == CircuitBreakerState.OPEN && System.currentTimeMillis() >= nextAttemptTime) {
            state = CircuitBreakerState.HALF_OPEN
        }
        
        return CircuitBreakerStatus(
            operation = operation,
            state = state,
            failureCount = failureCount.get(),
            lastFailureTime = lastFailureTime,
            nextAttemptTime = nextAttemptTime
        )
    }
}

/**
 * Retry policy configuration
 */
data class RetryPolicy(
    val maxAttempts: Int,
    val baseDelay: Duration,
    val maxDelay: Duration,
    val strategy: RetryStrategy,
    val retryableExceptions: List<Class<out Throwable>>
) {
    companion object {
        fun exponentialBackoff(
            maxAttempts: Int = 3,
            baseDelay: Duration = 1.seconds,
            maxDelay: Duration = 30.seconds
        ) = RetryPolicy(
            maxAttempts = maxAttempts,
            baseDelay = baseDelay,
            maxDelay = maxDelay,
            strategy = RetryStrategy.EXPONENTIAL,
            retryableExceptions = listOf(
                RuntimeException::class.java,
                NetworkException::class.java,
                TimeoutException::class.java
            )
        )
        
        fun fixedDelay(
            maxAttempts: Int = 3,
            delay: Duration = 1.seconds
        ) = RetryPolicy(
            maxAttempts = maxAttempts,
            baseDelay = delay,
            maxDelay = delay,
            strategy = RetryStrategy.FIXED,
            retryableExceptions = listOf(
                RuntimeException::class.java,
                NetworkException::class.java
            )
        )
    }
}

/**
 * Circuit breaker configuration
 */
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,
    val timeout: Duration = 60.seconds
)

/**
 * Retry strategy enum
 */
enum class RetryStrategy {
    FIXED, LINEAR, EXPONENTIAL
}

/**
 * Circuit breaker state enum
 */
enum class CircuitBreakerState {
    CLOSED, OPEN, HALF_OPEN
}

/**
 * Error severity enum
 */
enum class ErrorSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Error event data class
 */
data class ErrorEvent(
    val operation: String,
    val error: Throwable,
    val timestamp: Long,
    val severity: ErrorSeverity
)

/**
 * Circuit breaker status data class
 */
data class CircuitBreakerStatus(
    val operation: String,
    val state: CircuitBreakerState,
    val failureCount: Int,
    val lastFailureTime: Long,
    val nextAttemptTime: Long
)

/**
 * Error statistics data class
 */
data class ErrorStats(
    val totalCircuitBreakers: Int,
    val openCircuitBreakers: Int,
    val circuitBreakerDetails: Map<String, CircuitBreakerStatus>
)

/**
 * Custom exceptions
 */
class CircuitBreakerOpenException(message: String) : Exception(message)
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ValidationException(message: String) : Exception(message)
class TimeoutException(message: String) : Exception(message)