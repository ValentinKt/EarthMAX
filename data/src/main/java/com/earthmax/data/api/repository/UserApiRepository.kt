package com.earthmax.data.api.repository

import com.earthmax.data.api.UserApiService
import com.earthmax.data.api.dto.CreateUserRequest
import com.earthmax.data.api.dto.UpdateUserRequest
import com.earthmax.data.api.dto.UserResponse
import com.earthmax.data.api.validation.ApiValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user-related API calls
 */
@Singleton
class UserApiRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val apiValidator: ApiValidator
) {
    
    suspend fun createUser(request: CreateUserRequest): Flow<Result<UserResponse>> = flow {
        try {
            // Validate request
            val validation = apiValidator.validateCreateUserRequest(request)
            if (!validation.isValid) {
                emit(Result.failure(IllegalArgumentException(validation.getErrorMessage())))
                return@flow
            }
            
            val response = userApiService.createUser(request)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    emit(Result.success(userResponse))
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserById(userId: String): Flow<Result<UserResponse>> = flow {
        try {
            val response = userApiService.getUserById(userId)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    emit(Result.success(userResponse))
                } ?: emit(Result.failure(Exception("User not found")))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateUser(userId: String, request: UpdateUserRequest): Flow<Result<UserResponse>> = flow {
        try {
            // Validate request
            val validation = apiValidator.validateUpdateUserRequest(request)
            if (!validation.isValid) {
                emit(Result.failure(IllegalArgumentException(validation.getErrorMessage())))
                return@flow
            }
            
            val response = userApiService.updateUser(userId, request)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    emit(Result.success(userResponse))
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun deleteUser(userId: String): Flow<Result<Unit>> = flow {
        try {
            val response = userApiService.deleteUser(userId)
            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getAllUsers(page: Int = 1, limit: Int = 20): Flow<Result<List<UserResponse>>> = flow {
        try {
            val response = userApiService.getUsers(page, limit)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    emit(Result.success(users))
                } ?: emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUsersByLocation(
        latitude: Double,
        longitude: Double,
        radius: Double = 10.0,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<UserResponse>>> = flow {
        try {
            val response = userApiService.getUsersByLocation(latitude, longitude, radius, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    emit(Result.success(users))
                } ?: emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun searchUsers(query: String, page: Int = 1, limit: Int = 20): Flow<Result<List<UserResponse>>> = flow {
        try {
            // Validate search query
            val queryValidation = apiValidator.validateSearchQuery(query)
            if (!queryValidation.isValid) {
                emit(Result.failure(IllegalArgumentException(queryValidation.getErrorMessage())))
                return@flow
            }
            
            // Validate pagination
            val paginationValidation = apiValidator.validatePaginationParams(page, limit)
            if (!paginationValidation.isValid) {
                emit(Result.failure(IllegalArgumentException(paginationValidation.getErrorMessage())))
                return@flow
            }
            
            val response = userApiService.searchUsers(query, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    emit(Result.success(users))
                } ?: emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateUserProfile(userId: String, request: UpdateUserRequest): Flow<Result<UserResponse>> = flow {
        try {
            val response = userApiService.updateUserProfile(userId, request)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    emit(Result.success(userResponse))
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}