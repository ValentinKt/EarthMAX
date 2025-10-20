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
        Logger.enter(TAG, "createUser", 
            "displayName" to request.displayName,
            "bio" to (request.bio ?: "null")
        )
        
        try {
            // Validate request
            val validation = apiValidator.validateCreateUserRequest(request)
            if (!validation.isValid) {
                val validationError = validation.getErrorMessage()
                Logger.logError(TAG, "User creation validation failed", null, mapOf(
                    "displayName" to request.displayName,
                    "validationError" to validationError,
                    "errorType" to "VALIDATION_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_creation_failed", mapOf(
                    "displayName" to request.displayName,
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
                    Logger.logPerformance(TAG, "createUser", duration, mapOf(
                    "userId" to userResponse.id,
                    "displayName" to request.displayName,
                    "success" to "true"
                ))
                Logger.logBusinessEvent(TAG, "user_created", mapOf(
                    "userId" to userResponse.id,
                    "displayName" to request.displayName,
                    "bio" to (request.bio ?: "null")
                ))
                    Logger.exit(TAG, "createUser", mapOf(
                        "userId" to userResponse.id,
                        "success" to "true"
                    ))
                    emit(Result.success(userResponse))
                } ?: run {
                    Logger.logError(TAG, "User creation failed - empty response body", null, mapOf(
                        "displayName" to request.displayName,
                        "httpCode" to response.code().toString(),
                        "httpMessage" to response.message(),
                        "errorType" to "EMPTY_RESPONSE"
                    ))
                    Logger.logBusinessEvent(TAG, "user_creation_failed", mapOf(
                        "displayName" to request.displayName,
                        "reason" to "empty_response"
                    ))
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                Logger.logError(TAG, "User creation failed - HTTP error", null, mapOf(
                    "displayName" to request.displayName,
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_creation_failed", mapOf(
                    "displayName" to request.displayName,
                    "reason" to "http_error",
                    "httpCode" to response.code().toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.logPerformance(TAG, "createUser", duration, mapOf(
                "displayName" to request.displayName,
                "success" to "false"
            ))
            Logger.logError(TAG, "User creation failed - exception", e, mapOf(
                "displayName" to request.displayName,
                "errorMessage" to (e.message ?: "null"),
                "errorType" to "EXCEPTION"
            ))
            Logger.logBusinessEvent(TAG, "user_creation_failed", mapOf(
                "displayName" to request.displayName,
                "reason" to "exception",
                "errorMessage" to (e.message ?: "null")
            ))
            Logger.exit(TAG, "createUser", mapOf(
                "displayName" to request.displayName,
                "success" to "false",
                "error" to (e.message ?: "null")
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserById(userId: String): Flow<Result<UserResponse>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "getUserById", "userId" to userId)
        
        try {
            val response = userApiService.getUserById(userId)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.logPerformance(TAG, "getUserById", duration, mapOf(
                        "userId" to userId,
                        "displayName" to userResponse.displayName,
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "user_retrieved", mapOf(
                        "userId" to userId,
                        "displayName" to userResponse.displayName,
                        "source" to "api"
                    ))
                    Logger.exit(TAG, "getUserById", mapOf(
                        "userId" to userId,
                        "success" to "true"
                    ))
                    emit(Result.success(userResponse))
                } ?: run {
                    Logger.logError(TAG, "User retrieval failed - user not found", null, mapOf(
                        "userId" to userId,
                        "httpCode" to response.code().toString(),
                        "httpMessage" to response.message(),
                        "errorType" to "USER_NOT_FOUND"
                    ))
                    Logger.logBusinessEvent(TAG, "user_retrieval_failed", mapOf(
                        "userId" to userId,
                        "reason" to "user_not_found"
                    ))
                    emit(Result.failure(Exception("User not found")))
                }
            } else {
                Logger.logError(TAG, "User retrieval failed - HTTP error", null, mapOf(
                    "userId" to userId,
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_retrieval_failed", mapOf(
                    "userId" to userId,
                    "reason" to "http_error",
                    "httpCode" to response.code().toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.logPerformance(TAG, "getUserById", duration, mapOf(
                "userId" to userId,
                "success" to "false"
            ))
            Logger.logError(TAG, "User retrieval failed - exception", e, mapOf(
                "userId" to userId,
                "errorMessage" to (e.message ?: "Unknown error"),
                "errorType" to "EXCEPTION"
            ))
            Logger.logBusinessEvent(TAG, "user_retrieval_failed", mapOf(
                "userId" to userId,
                "reason" to "exception",
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.exit(TAG, "getUserById", mapOf(
                "userId" to userId,
                "success" to "false",
                "error" to (e.message ?: "Unknown error")
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateUser(userId: String, request: UpdateUserRequest): Flow<Result<UserResponse>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "updateUser", 
            "userId" to userId,
            "displayName" to request.displayName,
            "bio" to (request.bio ?: "null")
        )
        
        try {
            // Validate request
            val validation = apiValidator.validateUpdateUserRequest(request)
            if (!validation.isValid) {
                val validationError = validation.getErrorMessage()
                Logger.logError(TAG, "User update validation failed", null, mapOf(
                    "userId" to userId,
                    "validationError" to validationError,
                    "errorType" to "VALIDATION_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_update_failed", mapOf(
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
                    Logger.logPerformance(TAG, "updateUser", duration, mapOf(
                        "userId" to userId,
                        "displayName" to userResponse.displayName,
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "user_updated", mapOf(
                        "userId" to userId,
                        "displayName" to userResponse.displayName,
                        "displayName" to userResponse.displayName
                    ))
                    Logger.exit(TAG, "updateUser", mapOf(
                        "userId" to userId,
                        "success" to "true"
                    ))
                    emit(Result.success(userResponse))
                } ?: run {
                    Logger.logError(TAG, "User update failed - empty response body", null, mapOf(
                        "userId" to userId,
                        "httpCode" to response.code().toString(),
                        "httpMessage" to response.message(),
                        "errorType" to "EMPTY_RESPONSE"
                    ))
                    Logger.logBusinessEvent(TAG, "user_update_failed", mapOf(
                        "userId" to userId,
                        "reason" to "empty_response"
                    ))
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                Logger.logError(TAG, "User update failed - HTTP error", null, mapOf(
                    "userId" to userId,
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_update_failed", mapOf(
                    "userId" to userId,
                    "reason" to "http_error",
                    "httpCode" to response.code().toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.logPerformance(TAG, "updateUser", duration, mapOf(
                "userId" to userId,
                "success" to "false"
            ))
            Logger.logError(TAG, "User update failed - exception", e, mapOf(
                "userId" to userId,
                "errorMessage" to (e.message ?: "Unknown error"),
                "errorType" to "EXCEPTION"
            ))
            Logger.logBusinessEvent(TAG, "user_update_failed", mapOf(
                "userId" to userId,
                "reason" to "exception",
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.exit(TAG, "updateUser", mapOf(
                "userId" to userId,
                "success" to "false",
                "error" to (e.message ?: "Unknown error")
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun deleteUser(userId: String): Flow<Result<Unit>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "deleteUser", "userId" to userId)
        
        try {
            val response = userApiService.deleteUser(userId)
            if (response.isSuccessful) {
                val duration = System.currentTimeMillis() - startTime
                Logger.logPerformance(TAG, "deleteUser", duration, mapOf(
                    "userId" to userId,
                    "success" to "true"
                ))
                Logger.logBusinessEvent(TAG, "user_deleted", mapOf(
                    "userId" to userId
                ))
                Logger.exit(TAG, "deleteUser", mapOf(
                    "userId" to userId,
                    "success" to "true"
                ))
                emit(Result.success(Unit))
            } else {
                Logger.logError(TAG, "User deletion failed - HTTP error", null, mapOf(
                    "userId" to userId,
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_deletion_failed", mapOf(
                    "userId" to userId,
                    "reason" to "http_error",
                    "httpCode" to response.code().toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.logPerformance(TAG, "deleteUser", duration, mapOf(
                "userId" to userId,
                "success" to "false"
            ))
            Logger.logError(TAG, "User deletion failed - exception", e, mapOf(
                "userId" to userId,
                "errorMessage" to (e.message ?: "Unknown error"),
                "errorType" to "EXCEPTION"
            ))
            Logger.logBusinessEvent(TAG, "user_deletion_failed", mapOf(
                "userId" to userId,
                "reason" to "exception",
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.exit(TAG, "deleteUser", mapOf(
                "userId" to userId,
                "success" to "false",
                "error" to (e.message ?: "Unknown error")
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun getAllUsers(page: Int = 1, limit: Int = 20): Flow<Result<List<UserResponse>>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "getAllUsers", 
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        
        try {
            val response = userApiService.getUsers(page, limit)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.logPerformance(TAG, "getAllUsers", duration, mapOf(
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to users.size.toString(),
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "users_retrieved", mapOf(
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to users.size.toString(),
                        "source" to "api"
                    ))
                    Logger.exit(TAG, "getAllUsers", mapOf(
                        "page" to page.toString(),
                        "userCount" to users.size.toString(),
                        "success" to "true"
                    ))
                    emit(Result.success(users))
                } ?: run {
                    val duration = System.currentTimeMillis() - startTime
                    Logger.logPerformance(TAG, "getAllUsers", duration, mapOf(
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to "0",
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "users_retrieved", mapOf(
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to "0",
                        "source" to "api"
                    ))
                    Logger.exit(TAG, "getAllUsers", mapOf(
                        "page" to page.toString(),
                        "userCount" to "0",
                        "success" to "true"
                    ))
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.logError(TAG, "User retrieval failed - HTTP error", null, mapOf(
                    "page" to page.toString(),
                    "limit" to limit.toString(),
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "users_retrieval_failed", mapOf(
                    "page" to page.toString(),
                    "limit" to limit.toString(),
                    "reason" to "http_error",
                    "httpCode" to response.code().toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.logPerformance(TAG, "getAllUsers", duration, mapOf(
                "page" to page.toString(),
                "limit" to limit.toString(),
                "success" to "false"
            ))
            Logger.logError(TAG, "User retrieval failed - exception", e, mapOf(
                "page" to page.toString(),
                "limit" to limit.toString(),
                "errorMessage" to (e.message ?: "Unknown error"),
                "errorType" to "EXCEPTION"
            ))
            Logger.logBusinessEvent(TAG, "users_retrieval_failed", mapOf(
                "page" to page.toString(),
                "limit" to limit.toString(),
                "reason" to "exception",
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.exit(TAG, "getAllUsers", mapOf(
                "page" to page.toString(),
                "success" to "false",
                "error" to (e.message ?: "Unknown error")
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
        Logger.enter(TAG, "getUsersByLocation", 
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString(),
            "radius" to radius.toString(),
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        
        try {
            val response = userApiService.getUsersByLocation(latitude, longitude, radius, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.logPerformance(TAG, "getUsersByLocation", duration, mapOf(
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString(),
                        "radius" to radius.toString(),
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to users.size.toString(),
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "users_retrieved_by_location", mapOf(
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString(),
                        "radius" to radius.toString(),
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to users.size.toString()
                    ))
                    Logger.exit(TAG, "getUsersByLocation", mapOf(
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString(),
                        "userCount" to users.size.toString(),
                        "success" to "true"
                    ))
                    emit(Result.success(users))
                } ?: run {
                    val duration = System.currentTimeMillis() - startTime
                    Logger.logPerformance(TAG, "getUsersByLocation", duration, mapOf(
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString(),
                        "radius" to radius.toString(),
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to "0",
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "users_retrieved_by_location", mapOf(
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString(),
                        "radius" to radius.toString(),
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to "0"
                    ))
                    Logger.exit(TAG, "getUsersByLocation", mapOf(
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString(),
                        "userCount" to "0",
                        "success" to "true"
                    ))
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.logError(TAG, "User location retrieval failed - HTTP error", null, mapOf(
                    "latitude" to latitude.toString(),
                    "longitude" to longitude.toString(),
                    "radius" to radius.toString(),
                    "page" to page.toString(),
                    "limit" to limit.toString(),
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "users_location_retrieval_failed", mapOf(
                    "latitude" to latitude.toString(),
                    "longitude" to longitude.toString(),
                    "radius" to radius.toString(),
                    "reason" to "http_error",
                    "httpCode" to response.code().toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.logPerformance(TAG, "getUsersByLocation", duration, mapOf(
                "latitude" to latitude.toString(),
                "longitude" to longitude.toString(),
                "radius" to radius.toString(),
                "page" to page.toString(),
                "limit" to limit.toString(),
                "success" to "false"
            ))
            Logger.logError(TAG, "User location retrieval failed - exception", e, mapOf(
                "latitude" to latitude.toString(),
                "longitude" to longitude.toString(),
                "radius" to radius.toString(),
                "page" to page.toString(),
                "limit" to limit.toString(),
                "errorMessage" to (e.message ?: "Unknown error"),
                "errorType" to "EXCEPTION"
            ))
            Logger.logBusinessEvent(TAG, "users_location_retrieval_failed", mapOf(
                "latitude" to latitude.toString(),
                "longitude" to longitude.toString(),
                "radius" to radius.toString(),
                "reason" to "exception",
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.exit(TAG, "getUsersByLocation", mapOf(
                "latitude" to latitude.toString(),
                "longitude" to longitude.toString(),
                "success" to "false",
                "error" to (e.message ?: "Unknown error")
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun searchUsers(query: String, page: Int = 1, limit: Int = 20): Flow<Result<List<UserResponse>>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "searchUsers", 
            "query" to query,
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        
        try {
            // Validate search query
            val queryValidation = apiValidator.validateSearchQuery(query)
            if (!queryValidation.isValid) {
                val validationError = queryValidation.getErrorMessage()
                Logger.logError(TAG, "User search validation failed - invalid query", null, mapOf(
                    "query" to query,
                    "validationError" to validationError,
                    "errorType" to "VALIDATION_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_search_failed", mapOf(
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
                Logger.logError(TAG, "User search validation failed - invalid pagination", null, mapOf(
                    "query" to query,
                    "page" to page.toString(),
                    "limit" to limit.toString(),
                    "validationError" to validationError,
                    "errorType" to "VALIDATION_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_search_failed", mapOf(
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
                    Logger.logPerformance(TAG, "searchUsers", duration, mapOf(
                        "query" to query,
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to users.size.toString(),
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "user_search_completed", mapOf(
                        "query" to query,
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to users.size.toString()
                    ))
                    Logger.exit(TAG, "searchUsers", mapOf(
                        "query" to query,
                        "userCount" to users.size.toString(),
                        "success" to "true"
                    ))
                    emit(Result.success(users))
                } ?: run {
                    val duration = System.currentTimeMillis() - startTime
                    Logger.logPerformance(TAG, "searchUsers", duration, mapOf(
                        "query" to query,
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to "0",
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "user_search_completed", mapOf(
                        "query" to query,
                        "page" to page.toString(),
                        "limit" to limit.toString(),
                        "userCount" to "0"
                    ))
                    Logger.exit(TAG, "searchUsers", mapOf(
                        "query" to query,
                        "userCount" to "0",
                        "success" to "true"
                    ))
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.logError(TAG, "User search failed - HTTP error", null, mapOf(
                    "query" to query,
                    "page" to page.toString(),
                    "limit" to limit.toString(),
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_search_failed", mapOf(
                    "query" to query,
                    "reason" to "http_error",
                    "httpCode" to response.code().toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.logPerformance(TAG, "searchUsers", duration, mapOf(
                "query" to query,
                "page" to page.toString(),
                "limit" to limit.toString(),
                "success" to "false"
            ))
            Logger.logError(TAG, "User search failed - exception", e, mapOf(
                "query" to query,
                "page" to page.toString(),
                "limit" to limit.toString(),
                "errorMessage" to (e.message ?: "null"),
                "errorType" to "EXCEPTION"
            ))
            Logger.logBusinessEvent(TAG, "user_search_failed", mapOf(
                "query" to query,
                "reason" to "exception",
                "errorMessage" to (e.message ?: "null")
            ))
            Logger.exit(TAG, "searchUsers", mapOf(
                "query" to query,
                "success" to "false",
                "error" to (e.message ?: "null")
            ))
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateUserProfile(userId: String, request: UpdateUserRequest): Flow<Result<UserResponse>> = flow {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "updateUserProfile", 
            "userId" to userId,
            "displayName" to (request.displayName ?: "null"),
            "bio" to (request.bio ?: "null")
        )
        
        try {
            val response = userApiService.updateUserProfile(userId, request)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    val duration = System.currentTimeMillis() - startTime
                    Logger.logPerformance(TAG, "updateUserProfile", duration, mapOf(
                        "userId" to userId,
                        "displayName" to userResponse.displayName,
                        "success" to "true"
                    ))
                    Logger.logBusinessEvent(TAG, "user_profile_updated", mapOf(
                        "userId" to userId,
                        "displayName" to userResponse.displayName
                    ))
                    Logger.exit(TAG, "updateUserProfile", mapOf(
                        "userId" to userId,
                        "displayName" to userResponse.displayName,
                        "success" to "true"
                    ))
                    emit(Result.success(userResponse))
                } ?: run {
                    Logger.logError(TAG, "User profile update failed - empty response body", null, mapOf(
                        "userId" to userId,
                        "displayName" to (request.displayName ?: "null"),
                        "bio" to (request.bio ?: "null"),
                        "errorType" to "EMPTY_RESPONSE"
                    ))
                    Logger.logBusinessEvent(TAG, "user_profile_update_failed", mapOf(
                        "userId" to userId,
                        "reason" to "empty_response_body"
                    ))
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                Logger.logError(TAG, "User profile update failed - HTTP error", null, mapOf(
                    "userId" to userId,
                    "displayName" to (request.displayName ?: "null"),
                    "bio" to (request.bio ?: "null"),
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "errorType" to "HTTP_ERROR"
                ))
                Logger.logBusinessEvent(TAG, "user_profile_update_failed", mapOf(
                    "userId" to userId,
                    "reason" to "http_error",
                    "httpCode" to response.code().toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.logPerformance(TAG, "updateUserProfile", duration, mapOf(
                "userId" to userId,
                "displayName" to (request.displayName ?: "null"),
                "bio" to (request.bio ?: "null"),
                "success" to "false"
            ))
            Logger.logError(TAG, "User profile update failed - exception", e, mapOf(
                "userId" to userId,
                "displayName" to (request.displayName ?: "null"),
                "bio" to (request.bio ?: "null"),
                "errorMessage" to (e.message ?: "null"),
                "errorType" to "EXCEPTION"
            ))
            Logger.logBusinessEvent(TAG, "user_profile_update_failed", mapOf(
                "userId" to userId,
                "reason" to "exception",
                "errorMessage" to (e.message ?: "null")
            ))
            Logger.exit(TAG, "updateUserProfile", mapOf(
                "userId" to userId,
                "success" to "false",
                "error" to (e.message ?: "null")
            ))
            emit(Result.failure(e))
        }
    }
}