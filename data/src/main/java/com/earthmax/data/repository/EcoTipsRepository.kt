package com.earthmax.data.repository

import com.earthmax.data.graphql.GetEcoTipsQuery
import com.earthmax.data.remote.ApolloService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EcoTipsRepository @Inject constructor(
    private val apolloService: ApolloService
) {
    
    suspend fun getEcoTips(
        category: String? = null,
        limit: Int? = null
    ): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return apolloService.getEcoTips(category, limit)
    }
    
    suspend fun getEcoTipsByCategory(category: String): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return apolloService.getEcoTipsByCategory(category)
    }
    
    suspend fun getRandomEcoTips(limit: Int = 10): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return apolloService.getRandomEcoTips(limit)
    }
    
    suspend fun getDailyEcoTip(): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return apolloService.getRandomEcoTips(1)
    }
}