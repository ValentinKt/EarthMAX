package com.earthmax.data.repository

import com.earthmax.core.models.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun getUserById(userId: String): User?
    fun getUserByIdFlow(userId: String): Flow<User?>
    suspend fun getUserByEmail(email: String): User?
    fun getAllUsers(): Flow<List<User>>
    fun searchUsers(query: String): Flow<List<User>>
    fun getTopUsersByEcoPoints(limit: Int = 10): Flow<List<User>>
    suspend fun createUser(user: User): Result<Unit>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun updateUserEcoPoints(userId: String, points: Int): Result<Unit>
    suspend fun updateUserProfileImage(userId: String, imageUrl: String): Result<Unit>
    suspend fun updateUserDisplayName(userId: String, displayName: String): Result<Unit>
    suspend fun updateUserBio(userId: String, bio: String): Result<Unit>
    suspend fun updateUserLocation(userId: String, location: String): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
}