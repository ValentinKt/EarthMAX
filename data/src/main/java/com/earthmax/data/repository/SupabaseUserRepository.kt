package com.earthmax.data.repository

import com.earthmax.core.models.User
import com.earthmax.data.local.dao.UserDao
import com.earthmax.data.local.entities.toEntity
import com.earthmax.data.local.entities.toUser
import com.earthmax.data.auth.SupabaseAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseUserRepository @Inject constructor(
    private val userDao: UserDao,
    private val authRepository: SupabaseAuthRepository
) : UserRepository {
    
    override fun getCurrentUser(): Flow<User?> {
        return authRepository.getCurrentUser()
    }
    
    override suspend fun getUserById(userId: String): User? {
        // Try local first
        val localUser = userDao.getUserById(userId)?.toUser()
        if (localUser != null) {
            return localUser
        }
        
        // For now, return null if not found locally
        // In a full implementation, you would fetch from Supabase
        return null
    }
    
    override fun getUserByIdFlow(userId: String): Flow<User?> {
        return userDao.getUserByIdFlow(userId).map { it?.toUser() }
    }
    
    override suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)?.toUser()
    }
    
    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { entities ->
            entities.map { it.toUser() }
        }
    }
    
    override fun searchUsers(query: String): Flow<List<User>> {
        return userDao.searchUsers(query).map { entities ->
            entities.map { it.toUser() }
        }
    }
    
    override fun getTopUsersByEcoPoints(limit: Int): Flow<List<User>> {
        return userDao.getTopUsersByEcoPoints(limit).map { entities ->
            entities.map { it.toUser() }
        }
    }
    
    override suspend fun createUser(user: User): Result<Unit> {
        return try {
            userDao.insertUser(user.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            userDao.updateUser(user.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserEcoPoints(userId: String, points: Int): Result<Unit> {
        return try {
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(ecoPoints = user.ecoPoints + points)
                userDao.updateUser(updatedUser.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserProfileImage(userId: String, imageUrl: String): Result<Unit> {
        return try {
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(profileImageUrl = imageUrl)
                userDao.updateUser(updatedUser.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserDisplayName(userId: String, displayName: String): Result<Unit> {
        return try {
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(displayName = displayName)
                userDao.updateUser(updatedUser.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserBio(userId: String, bio: String): Result<Unit> {
        return try {
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(bio = bio)
                userDao.updateUser(updatedUser.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserLocation(userId: String, location: String): Result<Unit> {
        return try {
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(location = location)
                userDao.updateUser(updatedUser.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            userDao.deleteUserById(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return authRepository.signOut()
    }
}