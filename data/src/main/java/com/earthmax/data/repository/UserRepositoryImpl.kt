package com.earthmax.data.repository

import com.earthmax.core.cache.CacheManager
import com.earthmax.core.error.ErrorHandler
import com.earthmax.core.utils.Logger
import com.earthmax.data.local.dao.UserDao
import com.earthmax.data.mappers.toDomainUser
import com.earthmax.data.mappers.toUser
import com.earthmax.data.repository.SupabaseUserRepository
import com.earthmax.domain.model.DomainUser
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.UserRepository as DomainUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val supabaseUserRepository: SupabaseUserRepository,
    private val cacheManager: CacheManager,
    private val errorHandler: ErrorHandler
) : DomainUserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
        private const val CACHE_TTL_MINUTES = 15L
        private const val USER_CACHE_PREFIX = "user_"
        private const val CURRENT_USER_CACHE_KEY = "current_user"
    }

    override suspend fun getCurrentUser(): Result<DomainUser?> {
        return try {
            Logger.d(TAG, "Getting current user")
            
            // Check cache first
            val cachedUser = cacheManager.get<DomainUser>(CURRENT_USER_CACHE_KEY)
            if (cachedUser != null) {
                Logger.d(TAG, "Returning cached current user")
                return Result.Success(cachedUser)
            }

            // Get from remote
            val remoteUser = supabaseUserRepository.getCurrentUser()
            if (remoteUser != null) {
                val domainUser = remoteUser.toDomainUser()
                
                // Cache the result
                cacheManager.put(CURRENT_USER_CACHE_KEY, domainUser, CACHE_TTL_MINUTES)
                
                Logger.d(TAG, "Current user retrieved successfully")
                Result.Success(domainUser)
            } else {
                Logger.d(TAG, "No current user found")
                Result.Success(null)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting current user", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun getUserById(userId: String): Result<DomainUser?> {
        return try {
            Logger.d(TAG, "Getting user by ID: $userId")
            
            // Check cache first
            val cacheKey = "$USER_CACHE_PREFIX$userId"
            val cachedUser = cacheManager.get<DomainUser>(cacheKey)
            if (cachedUser != null) {
                Logger.d(TAG, "Returning cached user for ID: $userId")
                return Result.Success(cachedUser)
            }

            // Try local database first
            val localUser = userDao.getUserById(userId)
            if (localUser != null) {
                val domainUser = localUser.toUser().toDomainUser()
                
                // Cache the result
                cacheManager.put(cacheKey, domainUser, CACHE_TTL_MINUTES)
                
                Logger.d(TAG, "User found in local database: $userId")
                return Result.Success(domainUser)
            }

            // Get from remote
            val remoteUser = supabaseUserRepository.getUserById(userId)
            if (remoteUser != null) {
                val domainUser = remoteUser.toDomainUser()
                
                // Save to local database
                userDao.insertUser(remoteUser.toEntity())
                
                // Cache the result
                cacheManager.put(cacheKey, domainUser, CACHE_TTL_MINUTES)
                
                Logger.d(TAG, "User retrieved from remote: $userId")
                Result.Success(domainUser)
            } else {
                Logger.d(TAG, "User not found: $userId")
                Result.Success(null)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting user by ID: $userId", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun updateUserProfile(
        userId: String,
        firstName: String?,
        lastName: String?,
        bio: String?,
        location: String?
    ): Result<Unit> {
        return try {
            Logger.d(TAG, "Updating user profile: $userId")
            
            // Update in remote first
            val updateResult = supabaseUserRepository.updateUserProfile(
                userId = userId,
                displayName = if (firstName != null && lastName != null) "$firstName $lastName" else null,
                bio = bio,
                location = location
            )
            
            if (updateResult.isSuccess) {
                // Clear cache to force refresh
                cacheManager.remove("$USER_CACHE_PREFIX$userId")
                cacheManager.remove(CURRENT_USER_CACHE_KEY)
                
                Logger.d(TAG, "User profile updated successfully: $userId")
                Result.Success(Unit)
            } else {
                Logger.e(TAG, "Failed to update user profile: $userId")
                Result.Error(errorHandler.handleError(Exception("Failed to update user profile")))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user profile: $userId", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun updateUserPreferences(
        userId: String,
        notificationsEnabled: Boolean,
        locationSharingEnabled: Boolean,
        emailUpdatesEnabled: Boolean,
        preferredCategories: List<com.earthmax.domain.model.EventCategory>
    ): Result<Unit> {
        return try {
            Logger.d(TAG, "Updating user preferences: $userId")
            
            // For now, we'll store preferences in cache
            // In a real implementation, you might want to store this in a separate preferences table
            val preferencesKey = "preferences_$userId"
            val preferences = mapOf(
                "notificationsEnabled" to notificationsEnabled,
                "locationSharingEnabled" to locationSharingEnabled,
                "emailUpdatesEnabled" to emailUpdatesEnabled,
                "preferredCategories" to preferredCategories.map { it.name }
            )
            
            cacheManager.put(preferencesKey, preferences, CACHE_TTL_MINUTES * 4) // Longer TTL for preferences
            
            // Clear user cache to force refresh
            cacheManager.remove("$USER_CACHE_PREFIX$userId")
            cacheManager.remove(CURRENT_USER_CACHE_KEY)
            
            Logger.d(TAG, "User preferences updated successfully: $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user preferences: $userId", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun searchUsers(query: String): Result<List<DomainUser>> {
        return try {
            Logger.d(TAG, "Searching users with query: $query")
            
            // Search in remote
            val remoteUsers = supabaseUserRepository.searchUsers(query)
            val domainUsers = remoteUsers.map { it.toDomainUser() }
            
            Logger.d(TAG, "Found ${domainUsers.size} users for query: $query")
            Result.Success(domainUsers)
        } catch (e: Exception) {
            Logger.e(TAG, "Error searching users with query: $query", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun getUserStatistics(userId: String): Result<DomainUser.UserStatistics> {
        return try {
            Logger.d(TAG, "Getting user statistics: $userId")
            
            // This would typically involve aggregating data from multiple sources
            // For now, we'll return basic statistics
            val user = getUserById(userId)
            when (user) {
                is Result.Success -> {
                    if (user.data != null) {
                        Result.Success(user.data.statistics)
                    } else {
                        Result.Error(errorHandler.handleError(Exception("User not found")))
                    }
                }
                is Result.Error -> user
                is Result.Loading -> Result.Loading()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting user statistics: $userId", e)
            Result.Error(errorHandler.handleError(e))
        }
    }
}