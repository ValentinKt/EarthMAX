package com.earthmax.data.api.repository

import com.earthmax.core.utils.Logger
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
    
    companion object {
        private const val TAG = "UserApiRepository"
    }
    
    suspend fun createUser(request: CreateUserRequest): Flow<Result<UserResponse>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "createUser", mapOf(
            "email" to request.email,
            "name" to request.name
        ))
        
        try {
            // Validate request
            val validation = apiValidator.validateCreateUserRequest(request)
            if (!validation.isValid) {
                val validationError = validation.getErrorMessage()
                Logger.error(TAG, "User creation validation failed", mapOf(
                    "email" to request.email,
                    "validationError" to validationError,
                    "errorType" to "VALIDATION_ERROR"
                ))
                Logger.business(TAG, "user_creation_failed", mapOf(
                    "email" to request.email,
                    "reason" to "validation_error",
                    "validationError" to validationError
                ))
                emit(Result.failure(IllegalArgumentException(validationError)))
                return@flow
            }
            
            val response = userApiService.createUser(request)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "createUser", duration, mapOf(
                        "userId" to userResponse.id,
                        "email" to request.email,
                        "success" to true
                    ))
                    Logger.business(TAG, "user_created", mapOf(
                        "userId" to userResponse.id,
                        "email" to request.email,
                        "name" to request.name
                    ))
                    Logger.exit(TAG, "createUser", mapOf(
                        "userId" to userResponse.id,
                        "success" to true
                    ))
                    emit(Result.success(userResponse))
                } ?: run {
                    Logger.error(TAG, "User creation failed - empty response body", mapOf(
                        "email" to request.email,
                        "httpCode" to response.code(),
                        "httpMessage" to response.message(),
                        "errorType" to "EMPTY_RESPONSE"
                    ))
                    Logger.business(TAG, "user_creation_failed", mapOf(
                        "email" to request.email,
                        "reason" to "empty_response"
                    ))
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                Logger.error(TAG, "User creation failed - HTTP error", mapOf(
                    "email" to request.email,
                    "httpCode" to response.code(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.business(TAG, "user_creation_failed", mapOf(
                    "email" to request.email,
                    "reason" to "http_error",
                    "httpCode" to response.code()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.performance(TAG, "createUser", duration, mapOf(
                "email" to request.email,
                "success" to false
            ))
            Logger.error(TAG, "User creation failed - exception", mapOf(
                "email" to request.email,
                "errorMessage" to e.message,
                "errorType" to "EXCEPTION"
            ), e)
            Logger.business(TAG, "user_creation_failed", mapOf(
                "email" to request.email,
                "reason" to "exception",
                "errorMessage" to e.message
            ))
            Logger.exit(TAG, "createUser", mapOf(
                "email" to request.email,
                "success" to false,
                "error" to e.message
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserById(userId: String): Flow<Result<UserResponse>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "getUserById", mapOf("userId" to userId))
        
        try {
            val response = userApiService.getUserById(userId)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "getUserById", duration, mapOf(
                        "userId" to userId,
                        "email" to userResponse.email,
                        "success" to true
                    ))
                    Logger.business(TAG, "user_retrieved", mapOf(
                        "userId" to userId,
                        "email" to userResponse.email,
                        "source" to "api"
                    ))
                    Logger.exit(TAG, "getUserById", mapOf(
                        "userId" to userId,
                        "success" to true
                    ))
                    emit(Result.success(userResponse))
                } ?: run {
                    Logger.error(TAG, "User retrieval failed - user not found", mapOf(
                        "userId" to userId,
                        "httpCode" to response.code(),
                        "httpMessage" to response.message(),
                        "errorType" to "USER_NOT_FOUND"
                    ))
                    Logger.business(TAG, "user_retrieval_failed", mapOf(
                        "userId" to userId,
                        "reason" to "user_not_found"
                    ))
                    emit(Result.failure(Exception("User not found")))
                }
            } else {
                Logger.error(TAG, "User retrieval failed - HTTP error", mapOf(
                    "userId" to userId,
                    "httpCode" to response.code(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.business(TAG, "user_retrieval_failed", mapOf(
                    "userId" to userId,
                    "reason" to "http_error",
                    "httpCode" to response.code()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.performance(TAG, "getUserById", duration, mapOf(
                "userId" to userId,
                "success" to false
            ))
            Logger.error(TAG, "User retrieval failed - exception", mapOf(
                "userId" to userId,
                "errorMessage" to e.message,
                "errorType" to "EXCEPTION"
            ), e)
            Logger.business(TAG, "user_retrieval_failed", mapOf(
                "userId" to userId,
                "reason" to "exception",
                "errorMessage" to e.message
            ))
            Logger.exit(TAG, "getUserById", mapOf(
                "userId" to userId,
                "success" to false,
                "error" to e.message
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateUser(userId: String, request: UpdateUserRequest): Flow<Result<UserResponse>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "updateUser", mapOf(
            "userId" to userId,
            "email" to request.email,
            "name" to request.name
        ))
        
        try {
            // Validate request
            val validation = apiValidator.validateUpdateUserRequest(request)
            if (!validation.isValid) {
                val validationError = validation.getErrorMessage()
                Logger.error(TAG, "User update validation failed", mapOf(
                    "userId" to userId,
                    "validationError" to validationError,
                    "errorType" to "VALIDATION_ERROR"
                ))
                Logger.business(TAG, "user_update_failed", mapOf(
                    "userId" to userId,
                    "reason" to "validation_error",
                    "validationError" to validationError
                ))
                emit(Result.failure(IllegalArgumentException(validationError)))
                return@flow
            }
            
            val response = userApiService.updateUser(userId, request)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "updateUser", duration, mapOf(
                        "userId" to userId,
                        "email" to userResponse.email,
                        "success" to true
                    ))
                    Logger.business(TAG, "user_updated", mapOf(
                        "userId" to userId,
                        "email" to userResponse.email,
                        "name" to userResponse.name
                    ))
                    Logger.exit(TAG, "updateUser", mapOf(
                        "userId" to userId,
                        "success" to true
                    ))
                    emit(Result.success(userResponse))
                } ?: run {
                    Logger.error(TAG, "User update failed - empty response body", mapOf(
                        "userId" to userId,
                        "httpCode" to response.code(),
                        "httpMessage" to response.message(),
                        "errorType" to "EMPTY_RESPONSE"
                    ))
                    Logger.business(TAG, "user_update_failed", mapOf(
                        "userId" to userId,
                        "reason" to "empty_response"
                    ))
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                Logger.error(TAG, "User update failed - HTTP error", mapOf(
                    "userId" to userId,
                    "httpCode" to response.code(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.business(TAG, "user_update_failed", mapOf(
                    "userId" to userId,
                    "reason" to "http_error",
                    "httpCode" to response.code()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.performance(TAG, "updateUser", duration, mapOf(
                "userId" to userId,
                "success" to false
            ))
            Logger.error(TAG, "User update failed - exception", mapOf(
                "userId" to userId,
                "errorMessage" to e.message,
                "errorType" to "EXCEPTION"
            ), e)
            Logger.business(TAG, "user_update_failed", mapOf(
                "userId" to userId,
                "reason" to "exception",
                "errorMessage" to e.message
            ))
            Logger.exit(TAG, "updateUser", mapOf(
                "userId" to userId,
                "success" to false,
                "error" to e.message
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun deleteUser(userId: String): Flow<Result<Unit>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "deleteUser", mapOf("userId" to userId))
        
        try {
            val response = userApiService.deleteUser(userId)
            if (response.isSuccessful) {
                val duration = System.currentTimeMillis() - startTime
                Logger.performance(TAG, "deleteUser", duration, mapOf(
                    "userId" to userId,
                    "success" to true
                ))
                Logger.business(TAG, "user_deleted", mapOf(
                    "userId" to userId
                ))
                Logger.exit(TAG, "deleteUser", mapOf(
                    "userId" to userId,
                    "success" to true
                ))
                emit(Result.success(Unit))
            } else {
                Logger.error(TAG, "User deletion failed - HTTP error", mapOf(
                    "userId" to userId,
                    "httpCode" to response.code(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.business(TAG, "user_deletion_failed", mapOf(
                    "userId" to userId,
                    "reason" to "http_error",
                    "httpCode" to response.code()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.performance(TAG, "deleteUser", duration, mapOf(
                "userId" to userId,
                "success" to false
            ))
            Logger.error(TAG, "User deletion failed - exception", mapOf(
                "userId" to userId,
                "errorMessage" to e.message,
                "errorType" to "EXCEPTION"
            ), e)
            Logger.business(TAG, "user_deletion_failed", mapOf(
                "userId" to userId,
                "reason" to "exception",
                "errorMessage" to e.message
            ))
            Logger.exit(TAG, "deleteUser", mapOf(
                "userId" to userId,
                "success" to false,
                "error" to e.message
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun getAllUsers(page: Int = 1, limit: Int = 20): Flow<Result<List<UserResponse>>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "getAllUsers", mapOf(
            "page" to page,
            "limit" to limit
        ))
        
        try {
            val response = userApiService.getUsers(page, limit)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "getAllUsers", duration, mapOf(
                        "page" to page,
                        "limit" to limit,
                        "userCount" to users.size,
                        "success" to true
                    ))
                    Logger.business(TAG, "users_retrieved", mapOf(
                        "page" to page,
                        "limit" to limit,
                        "userCount" to users.size,
                        "source" to "api"
                    ))
                    Logger.exit(TAG, "getAllUsers", mapOf(
                        "page" to page,
                        "userCount" to users.size,
                        "success" to true
                    ))
                    emit(Result.success(users))
                } ?: run {
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "getAllUsers", duration, mapOf(
                        "page" to page,
                        "limit" to limit,
                        "userCount" to 0,
                        "success" to true
                    ))
                    Logger.business(TAG, "users_retrieved", mapOf(
                        "page" to page,
                        "limit" to limit,
                        "userCount" to 0,
                        "source" to "api"
                    ))
                    Logger.exit(TAG, "getAllUsers", mapOf(
                        "page" to page,
                        "userCount" to 0,
                        "success" to true
                    ))
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.error(TAG, "User retrieval failed - HTTP error", mapOf(
                    "page" to page,
                    "limit" to limit,
                    "httpCode" to response.code(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.business(TAG, "users_retrieval_failed", mapOf(
                    "page" to page,
                    "limit" to limit,
                    "reason" to "http_error",
                    "httpCode" to response.code()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.performance(TAG, "getAllUsers", duration, mapOf(
                "page" to page,
                "limit" to limit,
                "success" to false
            ))
            Logger.error(TAG, "User retrieval failed - exception", mapOf(
                "page" to page,
                "limit" to limit,
                "errorMessage" to e.message,
                "errorType" to "EXCEPTION"
            ), e)
            Logger.business(TAG, "users_retrieval_failed", mapOf(
                "page" to page,
                "limit" to limit,
                "reason" to "exception",
                "errorMessage" to e.message
            ))
            Logger.exit(TAG, "getAllUsers", mapOf(
                "page" to page,
                "success" to false,
                "error" to e.message
            ))
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
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "getUsersByLocation", mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "radius" to radius,
            "page" to page,
            "limit" to limit
        ))
        
        try {
            val response = userApiService.getUsersByLocation(latitude, longitude, radius, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "getUsersByLocation", duration, mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "radius" to radius,
                        "page" to page,
                        "limit" to limit,
                        "userCount" to users.size,
                        "success" to true
                    ))
                    Logger.business(TAG, "users_retrieved_by_location", mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "radius" to radius,
                        "page" to page,
                        "limit" to limit,
                        "userCount" to users.size
                    ))
                    Logger.exit(TAG, "getUsersByLocation", mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "userCount" to users.size,
                        "success" to true
                    ))
                    emit(Result.success(users))
                } ?: run {
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "getUsersByLocation", duration, mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "radius" to radius,
                        "page" to page,
                        "limit" to limit,
                        "userCount" to 0,
                        "success" to true
                    ))
                    Logger.business(TAG, "users_retrieved_by_location", mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "radius" to radius,
                        "page" to page,
                        "limit" to limit,
                        "userCount" to 0
                    ))
                    Logger.exit(TAG, "getUsersByLocation", mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "userCount" to 0,
                        "success" to true
                    ))
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.error(TAG, "User location retrieval failed - HTTP error", mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "radius" to radius,
                    "page" to page,
                    "limit" to limit,
                    "httpCode" to response.code(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.business(TAG, "users_location_retrieval_failed", mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "radius" to radius,
                    "reason" to "http_error",
                    "httpCode" to response.code()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.performance(TAG, "getUsersByLocation", duration, mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "radius" to radius,
                "page" to page,
                "limit" to limit,
                "success" to false
            ))
            Logger.error(TAG, "User location retrieval failed - exception", mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "radius" to radius,
                "page" to page,
                "limit" to limit,
                "errorMessage" to e.message,
                "errorType" to "EXCEPTION"
            ), e)
            Logger.business(TAG, "users_location_retrieval_failed", mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "radius" to radius,
                "reason" to "exception",
                "errorMessage" to e.message
            ))
            Logger.exit(TAG, "getUsersByLocation", mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "success" to false,
                "error" to e.message
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun searchUsers(query: String, page: Int = 1, limit: Int = 20): Flow<Result<List<UserResponse>>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "searchUsers", mapOf(
            "query" to query,
            "page" to page,
            "limit" to limit
        ))
        
        try {
            // Validate search query
            val queryValidation = apiValidator.validateSearchQuery(query)
            if (!queryValidation.isValid) {
                val validationError = queryValidation.getErrorMessage()
                Logger.error(TAG, "User search validation failed - invalid query", mapOf(
                    "query" to query,
                    "validationError" to validationError,
                    "errorType" to "VALIDATION_ERROR"
                ))
                Logger.business(TAG, "user_search_failed", mapOf(
                    "query" to query,
                    "reason" to "query_validation_error",
                    "validationError" to validationError
                ))
                emit(Result.failure(IllegalArgumentException(validationError)))
                return@flow
            }
            
            // Validate pagination
            val paginationValidation = apiValidator.validatePaginationParams(page, limit)
            if (!paginationValidation.isValid) {
                val validationError = paginationValidation.getErrorMessage()
                Logger.error(TAG, "User search validation failed - invalid pagination", mapOf(
                    "query" to query,
                    "page" to page,
                    "limit" to limit,
                    "validationError" to validationError,
                    "errorType" to "VALIDATION_ERROR"
                ))
                Logger.business(TAG, "user_search_failed", mapOf(
                    "query" to query,
                    "reason" to "pagination_validation_error",
                    "validationError" to validationError
                ))
                emit(Result.failure(IllegalArgumentException(validationError)))
                return@flow
            }
            
            val response = userApiService.searchUsers(query, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "searchUsers", duration, mapOf(
                        "query" to query,
                        "page" to page,
                        "limit" to limit,
                        "userCount" to users.size,
                        "success" to true
                    ))
                    Logger.business(TAG, "user_search_completed", mapOf(
                        "query" to query,
                        "page" to page,
                        "limit" to limit,
                        "userCount" to users.size
                    ))
                    Logger.exit(TAG, "searchUsers", mapOf(
                        "query" to query,
                        "userCount" to users.size,
                        "success" to true
                    ))
                    emit(Result.success(users))
                } ?: run {
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "searchUsers", duration, mapOf(
                        "query" to query,
                        "page" to page,
                        "limit" to limit,
                        "userCount" to 0,
                        "success" to true
                    ))
                    Logger.business(TAG, "user_search_completed", mapOf(
                        "query" to query,
                        "page" to page,
                        "limit" to limit,
                        "userCount" to 0
                    ))
                    Logger.exit(TAG, "searchUsers", mapOf(
                        "query" to query,
                        "userCount" to 0,
                        "success" to true
                    ))
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.error(TAG, "User search failed - HTTP error", mapOf(
                    "query" to query,
                    "page" to page,
                    "limit" to limit,
                    "httpCode" to response.code(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.business(TAG, "user_search_failed", mapOf(
                    "query" to query,
                    "reason" to "http_error",
                    "httpCode" to response.code()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.performance(TAG, "searchUsers", duration, mapOf(
                "query" to query,
                "page" to page,
                "limit" to limit,
                "success" to false
            ))
            Logger.error(TAG, "User search failed - exception", mapOf(
                "query" to query,
                "page" to page,
                "limit" to limit,
                "errorMessage" to e.message,
                "errorType" to "EXCEPTION"
            ), e)
            Logger.business(TAG, "user_search_failed", mapOf(
                "query" to query,
                "reason" to "exception",
                "errorMessage" to e.message
            ))
            Logger.exit(TAG, "searchUsers", mapOf(
                "query" to query,
                "success" to false,
                "error" to e.message
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateUserProfile(userId: String, request: UpdateUserRequest): Flow<Result<UserResponse>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "updateUserProfile", mapOf(
            "userId" to userId,
            "email" to request.email,
            "name" to request.name
        ))
        
        try {
            val response = userApiService.updateUserProfile(userId, request)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.performance(TAG, "updateUserProfile", duration, mapOf(
                        "userId" to userId,
                        "email" to userResponse.email,
                        "name" to userResponse.name,
                        "success" to true
                    ))
                    Logger.business(TAG, "user_profile_updated", mapOf(
                        "userId" to userId,
                        "email" to userResponse.email,
                        "name" to userResponse.name
                    ))
                    Logger.exit(TAG, "updateUserProfile", mapOf(
                        "userId" to userId,
                        "email" to userResponse.email,
                        "success" to true
                    ))
                    emit(Result.success(userResponse))
                } ?: run {
                    Logger.error(TAG, "User profile update failed - empty response body", mapOf(
                        "userId" to userId,
                        "email" to request.email,
                        "name" to request.name,
                        "errorType" to "EMPTY_RESPONSE"
                    ))
                    Logger.business(TAG, "user_profile_update_failed", mapOf(
                        "userId" to userId,
                        "reason" to "empty_response_body"
                    ))
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                Logger.error(TAG, "User profile update failed - HTTP error", mapOf(
                    "userId" to userId,
                    "email" to request.email,
                    "name" to request.name,
                    "httpCode" to response.code(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.business(TAG, "user_profile_update_failed", mapOf(
                    "userId" to userId,
                    "reason" to "http_error",
                    "httpCode" to response.code()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.performance(TAG, "updateUserProfile", duration, mapOf(
                "userId" to userId,
                "email" to request.email,
                "name" to request.name,
                "success" to false
            ))
            Logger.error(TAG, "User profile update failed - exception", mapOf(
                "userId" to userId,
                "email" to request.email,
                "name" to request.name,
                "errorMessage" to e.message,
                "errorType" to "EXCEPTION"
            ), e)
            Logger.business(TAG, "user_profile_update_failed", mapOf(
                "userId" to userId,
                "reason" to "exception",
                "errorMessage" to e.message
            ))
            Logger.exit(TAG, "updateUserProfile", mapOf(
                "userId" to userId,
                "success" to false,
                "error" to e.message
            ))
            emit(Result.failure(e))
        }
    }
}