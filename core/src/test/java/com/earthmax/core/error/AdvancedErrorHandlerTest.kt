package com.earthmax.core.error

import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.core.utils.Logger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedErrorHandlerTest {

    private lateinit var logger: Logger
    private lateinit var metricsCollector: MetricsCollector
    private lateinit var errorHandler: AdvancedErrorHandler
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        logger = mockk(relaxed = true)
        metricsCollector = mockk(relaxed = true)
        errorHandler = AdvancedErrorHandler(logger, metricsCollector)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `handleWithRetry should succeed on first attempt`() = runTest {
        // Given
        val operation = "test_operation"
        val expectedResult = "success"

        // When
        val result = errorHandler.handleWithRetry(operation) {
            expectedResult
        }

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
        verify { metricsCollector.incrementCounter("error_handler_success", mapOf("operation" to operation)) }
    }

    @Test
    fun `handleWithRetry should retry on failure and eventually succeed`() = runTest {
        // Given
        val operation = "test_operation"
        val expectedResult = "success"
        var attemptCount = 0
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 3, delay = 10.milliseconds)

        // When
        val result = errorHandler.handleWithRetry(operation, retryPolicy) {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Temporary failure")
            }
            expectedResult
        }

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
        assertEquals(3, attemptCount)
        verify(exactly = 2) { metricsCollector.incrementCounter("error_handler_retries", mapOf("operation" to operation)) }
    }

    @Test
    fun `handleWithRetry should fail after max attempts`() = runTest {
        // Given
        val operation = "test_operation"
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 2, delay = 10.milliseconds)
        var attemptCount = 0

        // When
        val result = errorHandler.handleWithRetry(operation, retryPolicy) {
            attemptCount++
            throw RuntimeException("Persistent failure")
        }

        // Then
        assertTrue(result.isFailure)
        assertEquals(2, attemptCount)
        verify(exactly = 1) { metricsCollector.incrementCounter("error_handler_retries", mapOf("operation" to operation)) }
        verify { metricsCollector.incrementCounter(match { it.startsWith("error_handler_errors") }, any()) }
    }

    @Test
    fun `handleWithRetry should not retry non-retryable exceptions`() = runTest {
        // Given
        val operation = "test_operation"
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 3, delay = 10.milliseconds)
        var attemptCount = 0

        // When
        val result = errorHandler.handleWithRetry(operation, retryPolicy) {
            attemptCount++
            throw IllegalArgumentException("Invalid argument")
        }

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, attemptCount) // Should not retry
        verify(exactly = 0) { metricsCollector.incrementCounter("error_handler_retries", any()) }
    }

    @Test
    fun `circuit breaker should open after failure threshold`() = runTest {
        // Given
        val operation = "test_operation"
        val circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 2, timeout = 1.seconds)
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 1, delay = 10.milliseconds)

        // When - Trigger failures to open circuit breaker
        repeat(3) {
            errorHandler.handleWithRetry(operation, retryPolicy, circuitBreakerConfig) {
                throw RuntimeException("Failure")
            }
        }

        // Then - Circuit breaker should be open
        val status = errorHandler.getCircuitBreakerStatus(operation)
        assertEquals(CircuitBreakerState.OPEN, status?.state)
    }

    @Test
    fun `circuit breaker should prevent execution when open`() = runTest {
        // Given
        val operation = "test_operation"
        val circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 1, timeout = 1.seconds)
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 1, delay = 10.milliseconds)

        // Open the circuit breaker
        errorHandler.handleWithRetry(operation, retryPolicy, circuitBreakerConfig) {
            throw RuntimeException("Failure")
        }

        // When - Try to execute with open circuit breaker
        val result = errorHandler.handleWithRetry(operation, retryPolicy, circuitBreakerConfig) {
            "should not execute"
        }

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CircuitBreakerOpenException)
    }

    @Test
    fun `circuit breaker should reset after successful execution`() = runTest {
        // Given
        val operation = "test_operation"
        val circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 1, timeout = 100.milliseconds)
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 1, delay = 10.milliseconds)

        // Open the circuit breaker
        errorHandler.handleWithRetry(operation, retryPolicy, circuitBreakerConfig) {
            throw RuntimeException("Failure")
        }

        // Wait for timeout
        delay(150)

        // When - Execute successfully
        val result = errorHandler.handleWithRetry(operation, retryPolicy, circuitBreakerConfig) {
            "success"
        }

        // Then
        assertTrue(result.isSuccess)
        val status = errorHandler.getCircuitBreakerStatus(operation)
        assertEquals(CircuitBreakerState.CLOSED, status?.state)
    }

    @Test
    fun `Flow handleErrors should retry and recover`() = runTest {
        // Given
        val operation = "flow_operation"
        var attemptCount = 0
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 3, delay = 10.milliseconds)

        val sourceFlow = flow {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Temporary failure")
            }
            emit("success")
        }

        // When
        val results = sourceFlow
            .handleErrors(operation, retryPolicy)
            .toList()

        // Then
        assertEquals(listOf("success"), results)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `Flow handleErrors should emit fallback value on failure`() = runTest {
        // Given
        val operation = "flow_operation"
        val fallbackValue = "fallback"
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 2, delay = 10.milliseconds)

        val sourceFlow = flow<String> {
            throw RuntimeException("Persistent failure")
        }

        // When
        val results = sourceFlow
            .handleErrors(operation, retryPolicy, fallbackValue)
            .toList()

        // Then
        assertEquals(listOf(fallbackValue), results)
    }

    @Test
    fun `exponential backoff should increase delay`() = runTest {
        // Given
        val retryPolicy = RetryPolicy.exponentialBackoff(
            maxAttempts = 4,
            baseDelay = 100.milliseconds,
            maxDelay = 1.seconds
        )
        val operation = "test_operation"
        val startTime = System.currentTimeMillis()
        var attemptCount = 0

        // When
        errorHandler.handleWithRetry(operation, retryPolicy) {
            attemptCount++
            if (attemptCount < 4) {
                throw RuntimeException("Failure")
            }
            "success"
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Then - Should have taken at least the sum of exponential delays
        // 100ms + 200ms + 400ms = 700ms minimum
        assertTrue(totalTime >= 700, "Total time was ${totalTime}ms, expected at least 700ms")
        assertEquals(4, attemptCount)
    }

    @Test
    fun `resetCircuitBreaker should reset circuit breaker state`() = runTest {
        // Given
        val operation = "test_operation"
        val circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 1, timeout = 1.seconds)
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 1, delay = 10.milliseconds)

        // Open the circuit breaker
        errorHandler.handleWithRetry(operation, retryPolicy, circuitBreakerConfig) {
            throw RuntimeException("Failure")
        }

        // When
        errorHandler.resetCircuitBreaker(operation)

        // Then
        val status = errorHandler.getCircuitBreakerStatus(operation)
        assertEquals(CircuitBreakerState.CLOSED, status?.state)
        assertEquals(0, status?.failureCount)
    }

    @Test
    fun `getErrorStats should return correct statistics`() = runTest {
        // Given
        val operation1 = "operation1"
        val operation2 = "operation2"
        val circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 1, timeout = 1.seconds)
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 1, delay = 10.milliseconds)

        // Open one circuit breaker
        errorHandler.handleWithRetry(operation1, retryPolicy, circuitBreakerConfig) {
            throw RuntimeException("Failure")
        }

        // Keep another closed
        errorHandler.handleWithRetry(operation2, retryPolicy, circuitBreakerConfig) {
            "success"
        }

        // When
        val stats = errorHandler.getErrorStats()

        // Then
        assertEquals(2, stats.totalCircuitBreakers)
        assertEquals(1, stats.openCircuitBreakers)
        assertTrue(stats.circuitBreakerDetails.containsKey(operation1))
        assertTrue(stats.circuitBreakerDetails.containsKey(operation2))
    }

    @Test
    fun `error severity should be determined correctly`() = runTest {
        // Given
        val operation = "test_operation"
        val retryPolicy = RetryPolicy.fixedDelay(maxAttempts = 1, delay = 10.milliseconds)

        // When & Then - Test different error types
        val securityResult = errorHandler.handleWithRetry(operation, retryPolicy) {
            throw SecurityException("Security error")
        }
        assertTrue(securityResult.isFailure)

        val validationResult = errorHandler.handleWithRetry(operation, retryPolicy) {
            throw ValidationException("Validation error")
        }
        assertTrue(validationResult.isFailure)

        val networkResult = errorHandler.handleWithRetry(operation, retryPolicy) {
            throw NetworkException("Network error")
        }
        assertTrue(networkResult.isFailure)

        // Verify error events were emitted with correct severity
        verify(atLeast = 3) { 
            metricsCollector.incrementCounter(
                match { it.startsWith("error_handler_errors") }, 
                any()
            ) 
        }
    }
}