package com.earthmax.data.repository

import com.earthmax.core.utils.Logger
import com.earthmax.data.graphql.GetEcoTipsQuery
import com.earthmax.data.remote.ApolloService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EcoTipsRepository @Inject constructor(
    private val apolloService: ApolloService
) {
    
    companion object {
        private const val TAG = "EcoTipsRepository"
    }
    
    suspend fun getEcoTips(
        category: String? = null,
        limit: Int? = null
    ): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        Logger.enter(TAG, "getEcoTips", 
            "category" to (category ?: "all"),
            "limit" to (limit?.toString() ?: "unlimited")
        )
        val startTime = System.currentTimeMillis()
        
        return apolloService.getEcoTips(category, limit)
            .onStart {
                Logger.d(TAG, "Starting eco tips retrieval from GraphQL")
            }
            .map { result ->
                if (result.isSuccess) {
                    val ecoTips = result.getOrNull() ?: emptyList()
                    Logger.i(TAG, "Successfully retrieved ${ecoTips.size} eco tips")
                    Logger.logBusinessEvent(TAG, "Eco Tips Retrieved", mapOf(
                        "category" to (category ?: "all"),
                        "limit" to (limit?.toString() ?: "unlimited"),
                        "count" to ecoTips.size.toString(),
                        "source" to "graphql"
                    ))
                    result
                } else {
                    val error = result.exceptionOrNull() ?: Exception("Unknown error")
                    Logger.w(TAG, "Failed to retrieve eco tips", error)
                    Logger.logBusinessEvent(TAG, "Eco Tips Retrieval Failed", mapOf(
                        "category" to (category ?: "all"),
                        "limit" to (limit?.toString() ?: "unlimited"),
                        "errorType" to error.javaClass.simpleName,
                        "errorMessage" to (error.message ?: "Unknown error")
                    ))
                    result
                }
            }
            .catch { e ->
                Logger.e(TAG, "Error retrieving eco tips", e)
                Logger.logBusinessEvent(TAG, "Eco Tips Retrieval Error", mapOf(
                    "category" to (category ?: "all"),
                    "limit" to (limit?.toString() ?: "unlimited"),
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Unknown error")
                ))
                emit(Result.failure(e))
            }
            .onCompletion {
                Logger.logPerformance(TAG, "getEcoTips", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "getEcoTips")
            }
    }
    
    suspend fun getEcoTipsByCategory(category: String): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        Logger.enter(TAG, "getEcoTipsByCategory", "category" to category)
        val startTime = System.currentTimeMillis()
        
        return apolloService.getEcoTipsByCategory(category)
            .onStart {
                Logger.d(TAG, "Starting eco tips retrieval by category from GraphQL")
            }
            .map { result ->
                if (result.isSuccess) {
                    val ecoTips = result.getOrNull() ?: emptyList()
                    Logger.i(TAG, "Successfully retrieved ${ecoTips.size} eco tips for category: $category")
                    Logger.logBusinessEvent(TAG, "Eco Tips By Category Retrieved", mapOf(
                        "category" to category,
                        "count" to ecoTips.size.toString(),
                        "source" to "graphql"
                    ))
                    result
                } else {
                    val error = result.exceptionOrNull() ?: Exception("Unknown error")
                    Logger.w(TAG, "Failed to retrieve eco tips by category", error)
                    Logger.logBusinessEvent(TAG, "Eco Tips By Category Retrieval Failed", mapOf(
                        "category" to category,
                        "errorType" to error.javaClass.simpleName,
                        "errorMessage" to (error.message ?: "Unknown error")
                    ))
                    result
                }
            }
            .catch { e ->
                Logger.e(TAG, "Error retrieving eco tips by category", e)
                Logger.logBusinessEvent(TAG, "Eco Tips By Category Retrieval Error", mapOf(
                    "category" to category,
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Unknown error")
                ))
                emit(Result.failure(e))
            }
            .onCompletion {
                Logger.logPerformance(TAG, "getEcoTipsByCategory", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "getEcoTipsByCategory")
            }
    }
    
    suspend fun getRandomEcoTips(limit: Int = 10): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        Logger.enter(TAG, "getRandomEcoTips", "limit" to limit.toString())
        val startTime = System.currentTimeMillis()
        
        return apolloService.getRandomEcoTips(limit)
            .onStart {
                Logger.d(TAG, "Starting random eco tips retrieval from GraphQL")
            }
            .map { result ->
                if (result.isSuccess) {
                    val ecoTips = result.getOrNull() ?: emptyList()
                    Logger.i(TAG, "Successfully retrieved ${ecoTips.size} random eco tips")
                    Logger.logBusinessEvent(TAG, "Random Eco Tips Retrieved", mapOf(
                        "limit" to limit.toString(),
                        "count" to ecoTips.size.toString(),
                        "source" to "graphql"
                    ))
                    result
                } else {
                    val error = result.exceptionOrNull() ?: Exception("Unknown error")
                    Logger.w(TAG, "Failed to retrieve random eco tips", error)
                    Logger.logBusinessEvent(TAG, "Random Eco Tips Retrieval Failed", mapOf(
                        "limit" to limit.toString(),
                        "errorType" to error.javaClass.simpleName,
                        "errorMessage" to (error.message ?: "Unknown error")
                    ))
                    result
                }
            }
            .catch { e ->
                Logger.e(TAG, "Error retrieving random eco tips", e)
                Logger.logBusinessEvent(TAG, "Random Eco Tips Retrieval Error", mapOf(
                    "limit" to limit.toString(),
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Unknown error")
                ))
                emit(Result.failure(e))
            }
            .onCompletion {
                Logger.logPerformance(TAG, "getRandomEcoTips", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "getRandomEcoTips")
            }
    }
    
    suspend fun getDailyEcoTip(): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        Logger.enter(TAG, "getDailyEcoTip")
        val startTime = System.currentTimeMillis()
        
        return apolloService.getRandomEcoTips(1)
            .onStart {
                Logger.d(TAG, "Starting daily eco tip retrieval from GraphQL")
            }
            .map { result ->
                if (result.isSuccess) {
                    val ecoTips = result.getOrNull() ?: emptyList()
                    Logger.i(TAG, "Successfully retrieved daily eco tip")
                    Logger.logBusinessEvent(TAG, "Daily Eco Tip Retrieved", mapOf(
                        "count" to ecoTips.size.toString(),
                        "source" to "graphql"
                    ))
                    result
                } else {
                    val error = result.exceptionOrNull() ?: Exception("Unknown error")
                    Logger.w(TAG, "Failed to retrieve daily eco tip", error)
                    Logger.logBusinessEvent(TAG, "Daily Eco Tip Retrieval Failed", mapOf(
                        "errorType" to error.javaClass.simpleName,
                        "errorMessage" to (error.message ?: "Unknown error")
                    ))
                    result
                }
            }
            .catch { e ->
                Logger.e(TAG, "Error retrieving daily eco tip", e)
                Logger.logBusinessEvent(TAG, "Daily Eco Tip Retrieval Error", mapOf(
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Unknown error")
                ))
                emit(Result.failure(e))
            }
            .onCompletion {
                Logger.logPerformance(TAG, "getDailyEcoTip", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "getDailyEcoTip")
            }
    }
}