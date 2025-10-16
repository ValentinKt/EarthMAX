package com.earthmax.data.remote

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.earthmax.data.graphql.GetEcoTipsQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApolloService @Inject constructor(
    private val apolloClient: ApolloClient
) {
    
    suspend fun getEcoTips(
        category: String? = null,
        limit: Int? = null
    ): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> = flow {
        try {
            val response: ApolloResponse<GetEcoTipsQuery.Data> = apolloClient
                .query(GetEcoTipsQuery(
                    category = Optional.presentIfNotNull(category), 
                    limit = Optional.presentIfNotNull(limit)
                ))
                .execute()
            
            if (response.hasErrors()) {
                emit(Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown GraphQL error")))
            } else {
                val ecoTips = response.data?.getEcoTips ?: emptyList()
                emit(Result.success(ecoTips))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getEcoTipsByCategory(category: String): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return getEcoTips(category = category)
    }
    
    suspend fun getRandomEcoTips(limit: Int = 10): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return getEcoTips(limit = limit)
    }
}