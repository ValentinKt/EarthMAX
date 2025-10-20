package com.earthmax.domain.usecase

import com.earthmax.domain.model.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base class for use cases that provides common functionality and error handling.
 */
abstract class BaseUseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    /**
     * Executes the use case with proper error handling and context switching.
     */
    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            withContext(coroutineDispatcher) {
                execute(parameters).let { result ->
                    Result.Success(result)
                }
            }
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    /**
     * Abstract method to be implemented by concrete use cases.
     */
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}

/**
 * Base class for use cases that don't require parameters.
 */
abstract class BaseUseCaseNoParams<R>(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    suspend operator fun invoke(): Result<R> {
        return try {
            withContext(coroutineDispatcher) {
                execute().let { result ->
                    Result.Success(result)
                }
            }
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(): R
}