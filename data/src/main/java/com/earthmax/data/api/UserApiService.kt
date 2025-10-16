package com.earthmax.data.api

import com.earthmax.core.models.User
import com.earthmax.data.api.dto.CreateUserRequest
import com.earthmax.data.api.dto.UpdateUserRequest
import com.earthmax.data.api.dto.UserResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * RESTful API service for user management operations
 */
interface UserApiService {
    
    /**
     * Get all users with optional pagination
     */
    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<List<UserResponse>>
    
    /**
     * Get a specific user by ID
     */
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: String): Response<UserResponse>
    
    /**
     * Create a new user
     */
    @POST("users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<UserResponse>
    
    /**
     * Update an existing user
     */
    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>
    
    /**
     * Partially update a user
     */
    @PATCH("users/{id}")
    suspend fun patchUser(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>
    
    /**
     * Delete a user
     */
    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): Response<Unit>
    
    /**
     * Get user profile
     */
    @GET("users/{id}/profile")
    suspend fun getUserProfile(@Path("id") userId: String): Response<UserResponse>
    
    /**
     * Update user profile
     */
    @PUT("users/{id}/profile")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>
    
    /**
     * Get users by location
     */
    @GET("users/location")
    suspend fun getUsersByLocation(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double = 10.0,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<UserResponse>>
    
    /**
     * Search users by name
     */
    @GET("users/search")
    suspend fun searchUsers(
        @Query("name") name: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<UserResponse>>
}