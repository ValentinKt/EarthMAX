package com.earthmax.domain.repository

import com.earthmax.domain.model.DomainUser
import com.earthmax.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user operations.
 * This defines the contract for user data access in the domain layer.
 */
interface UserRepository {
    
    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): Flow<Result<DomainUser?>>
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<DomainUser>
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(user: DomainUser): Result<DomainUser>
    
    /**
     * Update user preferences
     */
    suspend fun updateUserPreferences(userId: String, preferences: com.earthmax.domain.model.UserPreferences): Result<Unit>
    
    /**
     * Search users by query
     */
    suspend fun searchUsers(query: String): Result<List<DomainUser>>
    
    /**
     * Get user statistics
     */
    suspend fun getUserStatistics(userId: String): Result<com.earthmax.domain.model.UserStatistics>
}