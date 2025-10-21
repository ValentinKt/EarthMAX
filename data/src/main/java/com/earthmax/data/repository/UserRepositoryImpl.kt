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
import com.earthmax.domain.model.UserStatistics
import com.earthmax.domain.repository.UserRepository as DomainUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import com.earthmax.data.local.entities.toEntity
import com.earthmax.data.local.entities.toUser
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val supabaseUserRepository: SupabaseUserRepository,
    private val cacheManager: CacheManager,
    private val errorHandler: ErrorHandler
) : DomainUserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
        private val CACHE_TTL: Duration = 15.minutes
        private const val USER_CACHE_PREFIX = "user_"
        private const val CURRENT_USER_CACHE_KEY = "current_user"
    }

    override fun getCurrentUser(): Flow<Result<DomainUser?>> {
        return flow {
            emit(Result.Loading)
            try {
                val cachedUser = cacheManager.get<DomainUser>(CURRENT_USER_CACHE_KEY)
                if (cachedUser != null) {
                    emit(Result.Success(cachedUser))
                }
                supabaseUserRepository.getCurrentUser().collect { coreUser ->
                    val domainUser = coreUser?.toDomainUser()
                    if (domainUser != null) {
                        cacheManager.put(CURRENT_USER_CACHE_KEY, domainUser, CACHE_TTL)
                        emit(Result.Success(domainUser))
                    } else {
                        emit(Result.Success(null))
                    }
                }
            } catch (e: Exception) {
                emit(Result.Error(errorHandler.handleError(e)))
            }
        }
    }

    override suspend fun getUserById(userId: String): Result<DomainUser> {
        return try {
            Logger.d(TAG, "Getting user by ID: $userId")
            val cacheKey = "$USER_CACHE_PREFIX$userId"
            val cachedUser = cacheManager.get<DomainUser>(cacheKey)
            if (cachedUser != null) {
                Logger.d(TAG, "Returning cached user for ID: $userId")
                return Result.Success(cachedUser)
            }
            val localUser = userDao.getUserById(userId)
            if (localUser != null) {
                val domainUser = localUser.toUser().toDomainUser()
                cacheManager.put(cacheKey, domainUser, CACHE_TTL)
                Logger.d(TAG, "User found in local database: $userId")
                return Result.Success(domainUser)
            }
            val remoteUser = supabaseUserRepository.getUserById(userId)
            if (remoteUser != null) {
                val domainUser = remoteUser.toDomainUser()
                userDao.insertUser(remoteUser.toEntity())
                cacheManager.put(cacheKey, domainUser, CACHE_TTL)
                Logger.d(TAG, "User retrieved from remote: $userId")
                Result.Success(domainUser)
            } else {
                Logger.d(TAG, "User not found: $userId")
                Result.Error(errorHandler.handleError(Exception("User not found")))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting user by ID: $userId", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun updateUserProfile(user: DomainUser): Result<DomainUser> {
        return try {
            Logger.d(TAG, "Updating user profile: ${user.id}")
            val coreUser = user.toUser()
            val updateResult = supabaseUserRepository.updateUser(coreUser)
            if (updateResult.isSuccess) {
                userDao.updateUser(coreUser.toEntity())
                cacheManager.remove("$USER_CACHE_PREFIX${user.id}")
                cacheManager.remove(CURRENT_USER_CACHE_KEY)
                Logger.d(TAG, "User profile updated successfully: ${user.id}")
                Result.Success(user)
            } else {
                Logger.e(TAG, "Failed to update user profile: ${user.id}")
                Result.Error(errorHandler.handleError(Exception("Failed to update user profile")))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user profile: ${user.id}", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun updateUserPreferences(
        userId: String,
        preferences: com.earthmax.domain.model.UserPreferences
    ): Result<Unit> {
        return try {
            Logger.d(TAG, "Updating user preferences: $userId")
            val existing = userDao.getUserById(userId)?.toUser()
            if (existing != null) {
                val updatedCore = existing.copy(
                    preferences = existing.preferences.copy(
                        notificationsEnabled = preferences.notificationsEnabled,
                        locationSharingEnabled = preferences.locationSharing
                    )
                )
                userDao.updateUser(updatedCore.toEntity())
                cacheManager.put("preferences_$userId", preferences, CACHE_TTL * 4)
                cacheManager.remove("$USER_CACHE_PREFIX$userId")
                cacheManager.remove(CURRENT_USER_CACHE_KEY)
                Logger.d(TAG, "User preferences updated successfully: $userId")
                Result.Success(Unit)
            } else {
                Logger.w(TAG, "User not found when updating preferences: $userId")
                Result.Error(errorHandler.handleError(Exception("User not found")))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user preferences: $userId", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun searchUsers(query: String): Result<List<DomainUser>> {
        return try {
            Logger.d(TAG, "Searching users with query: $query")
            val coreUsers = supabaseUserRepository.searchUsers(query).first()
            val domainUsers = coreUsers.map { it.toDomainUser() }
            Logger.d(TAG, "Found ${domainUsers.size} users for query: $query")
            Result.Success(domainUsers)
        } catch (e: Exception) {
            Logger.e(TAG, "Error searching users with query: $query", e)
            Result.Error(errorHandler.handleError(e))
        }
    }

    override suspend fun getUserStatistics(userId: String): Result<UserStatistics> {
        return try {
            Logger.d(TAG, "Getting user statistics: $userId")
            val user = getUserById(userId)
            when (user) {
                is Result.Success -> {
                    val u = user.data
                    Result.Success(u.statistics)
                }
                is Result.Error -> user
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting user statistics: $userId", e)
            Result.Error(errorHandler.handleError(e))
        }
    }
}