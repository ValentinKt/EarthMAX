package com.earthmax.data.repository

import com.earthmax.core.models.User
import com.earthmax.core.utils.Logger
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
    
    companion object {
        private const val TAG = "SupabaseUserRepository"
    }
    
    override fun getCurrentUser(): Flow<User?> {
        Logger.enter(TAG, "getCurrentUser")
        Logger.d(TAG, "Getting current user from auth repository")
        val result = authRepository.getCurrentUser()
        Logger.exit(TAG, "getCurrentUser")
        return result
    }
    
    override suspend fun getUserById(userId: String): User? {
        Logger.enter(TAG, "getUserById", "userId" to userId)
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Attempting to get user from local database")
            // Try local first
            val localUser = userDao.getUserById(userId)?.toUser()
            if (localUser != null) {
                Logger.i(TAG, "User found in local database")
                Logger.logBusinessEvent(TAG, "User Retrieved from Local", mapOf(
                    "userId" to userId,
                    "displayName" to (localUser.displayName ?: "unknown")
                ))
                Logger.logPerformance(TAG, "getUserById_local", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "getUserById", localUser)
                return localUser
            }
            
            Logger.w(TAG, "User not found in local database")
            Logger.logBusinessEvent(TAG, "User Not Found", mapOf("userId" to userId))
            Logger.logPerformance(TAG, "getUserById_not_found", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getUserById", null)
            // For now, return null if not found locally
            // In a full implementation, you would fetch from Supabase
            null
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting user by ID", e)
            Logger.logBusinessEvent(TAG, "Get User Error", mapOf(
                "userId" to userId,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getUserById_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getUserById", null)
            null
        }
    }
    
    override fun getUserByIdFlow(userId: String): Flow<User?> {
        Logger.enter(TAG, "getUserByIdFlow", "userId" to userId)
        Logger.d(TAG, "Creating flow for user by ID")
        val result = userDao.getUserByIdFlow(userId).map { it?.toUser() }
        Logger.exit(TAG, "getUserByIdFlow")
        return result
    }
    
    override suspend fun getUserByEmail(email: String): User? {
        Logger.enter(TAG, "getUserByEmail", "email" to Logger.maskSensitiveData(email))
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Attempting to get user by email from local database")
            val user = userDao.getUserByEmail(email)?.toUser()
            if (user != null) {
                Logger.i(TAG, "User found by email in local database")
                Logger.logBusinessEvent(TAG, "User Retrieved by Email", mapOf(
                    "email" to Logger.maskSensitiveData(email),
                    "userId" to user.id
                ))
            } else {
                Logger.w(TAG, "User not found by email in local database")
                Logger.logBusinessEvent(TAG, "User Not Found by Email", mapOf(
                    "email" to Logger.maskSensitiveData(email)
                ))
            }
            Logger.logPerformance(TAG, "getUserByEmail", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getUserByEmail", user)
            user
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting user by email", e)
            Logger.logBusinessEvent(TAG, "Get User by Email Error", mapOf(
                "email" to Logger.maskSensitiveData(email),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getUserByEmail_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getUserByEmail", null)
            null
        }
    }
    
    override fun getAllUsers(): Flow<List<User>> {
        Logger.enter(TAG, "getAllUsers")
        Logger.d(TAG, "Getting all users from local database")
        val result = userDao.getAllUsers().map { entities ->
            Logger.i(TAG, "Retrieved ${entities.size} users from local database")
            Logger.logBusinessEvent(TAG, "All Users Retrieved", mapOf("count" to entities.size.toString()))
            entities.map { it.toUser() }
        }
        Logger.exit(TAG, "getAllUsers")
        return result
    }
    
    override fun searchUsers(query: String): Flow<List<User>> {
        Logger.enter(TAG, "searchUsers", "query" to query)
        Logger.d(TAG, "Searching users with query: $query")
        val result = userDao.searchUsers(query).map { entities ->
            Logger.i(TAG, "Found ${entities.size} users matching query")
            Logger.logBusinessEvent(TAG, "Users Searched", mapOf(
                "query" to query,
                "resultCount" to entities.size.toString()
            ))
            entities.map { it.toUser() }
        }
        Logger.exit(TAG, "searchUsers")
        return result
    }
    
    override fun getTopUsersByEcoPoints(limit: Int): Flow<List<User>> {
        Logger.enter(TAG, "getTopUsersByEcoPoints", "limit" to limit.toString())
        Logger.d(TAG, "Getting top $limit users by eco points")
        val result = userDao.getTopUsersByEcoPoints(limit).map { entities ->
            Logger.i(TAG, "Retrieved ${entities.size} top users by eco points")
            Logger.logBusinessEvent(TAG, "Top Users by Eco Points Retrieved", mapOf(
                "limit" to limit.toString(),
                "actualCount" to entities.size.toString()
            ))
            entities.map { it.toUser() }
        }
        Logger.exit(TAG, "getTopUsersByEcoPoints")
        return result
    }
    
    override suspend fun createUser(user: User): Result<Unit> {
        Logger.enter(TAG, "createUser", "userId" to user.id)
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Creating user in local database")
            userDao.insertUser(user.toEntity())
            Logger.i(TAG, "User created successfully")
            Logger.logBusinessEvent(TAG, "User Created", mapOf(
                "userId" to user.id,
                "displayName" to (user.displayName ?: "unknown"),
                "email" to Logger.maskSensitiveData(user.email ?: "unknown")
            ))
            Logger.logPerformance(TAG, "createUser", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "createUser", Result.success<Unit>(Unit))
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Error creating user", e)
            Logger.logBusinessEvent(TAG, "User Creation Error", mapOf(
                "userId" to user.id,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "createUser_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "createUser", Result.failure<Unit>(e))
            Result.failure(e)
        }
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        Logger.enter(TAG, "updateUser", "userId" to user.id)
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Updating user in local database")
            userDao.updateUser(user.toEntity())
            Logger.i(TAG, "User updated successfully")
            Logger.logBusinessEvent(TAG, "User Updated", mapOf(
                "userId" to user.id,
                "displayName" to (user.displayName ?: "unknown")
            ))
            Logger.logPerformance(TAG, "updateUser", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "updateUser", Result.success<Unit>(Unit))
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user", e)
            Logger.logBusinessEvent(TAG, "User Update Error", mapOf(
                "userId" to user.id,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "updateUser_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "updateUser", Result.failure<Unit>(e))
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserEcoPoints(userId: String, points: Int): Result<Unit> {
        Logger.enter(TAG, "updateUserEcoPoints", "userId" to userId, "points" to points.toString())
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Updating user eco points")
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(ecoPoints = user.ecoPoints + points)
                userDao.updateUser(updatedUser.toEntity())
                Logger.i(TAG, "User eco points updated successfully")
                Logger.logBusinessEvent(TAG, "User Eco Points Updated", mapOf(
                    "userId" to userId,
                    "pointsAdded" to points.toString(),
                    "newTotal" to updatedUser.ecoPoints.toString()
                ))
                Logger.logPerformance(TAG, "updateUserEcoPoints", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserEcoPoints", Result.success<Unit>(Unit))
                Result.success(Unit)
            } else {
                Logger.w(TAG, "User not found for eco points update")
                Logger.logBusinessEvent(TAG, "User Eco Points Update Failed - User Not Found", mapOf(
                    "userId" to userId,
                    "pointsAttempted" to points.toString()
                ))
                Logger.logPerformance(TAG, "updateUserEcoPoints_not_found", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserEcoPoints", Result.failure<Unit>(Exception("User not found")))
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user eco points", e)
            Logger.logBusinessEvent(TAG, "User Eco Points Update Error", mapOf(
                "userId" to userId,
                "pointsAttempted" to points.toString(),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "updateUserEcoPoints_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "updateUserEcoPoints", Result.failure<Unit>(e))
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserProfileImage(userId: String, imageUrl: String): Result<Unit> {
        Logger.enter(TAG, "updateUserProfileImage", "userId" to userId, "imageUrl" to imageUrl)
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Updating user profile image")
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(profileImageUrl = imageUrl)
                userDao.updateUser(updatedUser.toEntity())
                Logger.i(TAG, "User profile image updated successfully")
                Logger.logBusinessEvent(TAG, "User Profile Image Updated", mapOf(
                    "userId" to userId,
                    "imageUrl" to imageUrl
                ))
                Logger.logPerformance(TAG, "updateUserProfileImage", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserProfileImage", Result.success<Unit>(Unit))
                Result.success(Unit)
            } else {
                Logger.w(TAG, "User not found for profile image update")
                Logger.logBusinessEvent(TAG, "User Profile Image Update Failed - User Not Found", mapOf(
                    "userId" to userId
                ))
                Logger.logPerformance(TAG, "updateUserProfileImage_not_found", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserProfileImage", Result.failure<Unit>(Exception("User not found")))
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user profile image", e)
            Logger.logBusinessEvent(TAG, "User Profile Image Update Error", mapOf(
                "userId" to userId,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "updateUserProfileImage_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "updateUserProfileImage", Result.failure<Unit>(e))
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserDisplayName(userId: String, displayName: String): Result<Unit> {
        Logger.enter(TAG, "updateUserDisplayName", "userId" to userId, "displayName" to displayName)
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Updating user display name")
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(displayName = displayName)
                userDao.updateUser(updatedUser.toEntity())
                Logger.i(TAG, "User display name updated successfully")
                Logger.logBusinessEvent(TAG, "User Display Name Updated", mapOf(
                    "userId" to userId,
                    "newDisplayName" to displayName
                ))
                Logger.logPerformance(TAG, "updateUserDisplayName", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserDisplayName", Result.success<Unit>(Unit))
                Result.success(Unit)
            } else {
                Logger.w(TAG, "User not found for display name update")
                Logger.logBusinessEvent(TAG, "User Display Name Update Failed - User Not Found", mapOf(
                    "userId" to userId
                ))
                Logger.logPerformance(TAG, "updateUserDisplayName_not_found", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserDisplayName", Result.failure<Unit>(Exception("User not found")))
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user display name", e)
            Logger.logBusinessEvent(TAG, "User Display Name Update Error", mapOf(
                "userId" to userId,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "updateUserDisplayName_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "updateUserDisplayName", Result.failure<Unit>(e))
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserBio(userId: String, bio: String): Result<Unit> {
        Logger.enter(TAG, "updateUserBio", "userId" to userId, "bio" to bio)
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Updating user bio")
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(bio = bio)
                userDao.updateUser(updatedUser.toEntity())
                Logger.i(TAG, "User bio updated successfully")
                Logger.logBusinessEvent(TAG, "User Bio Updated", mapOf(
                    "userId" to userId,
                    "bioLength" to bio.length.toString()
                ))
                Logger.logPerformance(TAG, "updateUserBio", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserBio", Result.success<Unit>(Unit))
                Result.success(Unit)
            } else {
                Logger.w(TAG, "User not found for bio update")
                Logger.logBusinessEvent(TAG, "User Bio Update Failed - User Not Found", mapOf(
                    "userId" to userId
                ))
                Logger.logPerformance(TAG, "updateUserBio_not_found", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserBio", Result.failure<Unit>(Exception("User not found")))
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user bio", e)
            Logger.logBusinessEvent(TAG, "User Bio Update Error", mapOf(
                "userId" to userId,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "updateUserBio_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "updateUserBio", Result.failure<Unit>(e))
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserLocation(userId: String, location: String): Result<Unit> {
        Logger.enter(TAG, "updateUserLocation", "userId" to userId, "location" to location)
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Updating user location")
            val user = getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(location = location)
                userDao.updateUser(updatedUser.toEntity())
                Logger.i(TAG, "User location updated successfully")
                Logger.logBusinessEvent(TAG, "User Location Updated", mapOf(
                    "userId" to userId,
                    "newLocation" to location
                ))
                Logger.logPerformance(TAG, "updateUserLocation", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserLocation", Result.success<Unit>(Unit))
                Result.success(Unit)
            } else {
                Logger.w(TAG, "User not found for location update")
                Logger.logBusinessEvent(TAG, "User Location Update Failed - User Not Found", mapOf(
                    "userId" to userId
                ))
                Logger.logPerformance(TAG, "updateUserLocation_not_found", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateUserLocation", Result.failure<Unit>(Exception("User not found")))
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user location", e)
            Logger.logBusinessEvent(TAG, "User Location Update Error", mapOf(
                "userId" to userId,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "updateUserLocation_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "updateUserLocation", Result.failure<Unit>(e))
            Result.failure(e)
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        Logger.enter(TAG, "deleteUser", "userId" to userId)
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Deleting user from local database")
            userDao.deleteUserById(userId)
            Logger.i(TAG, "User deleted successfully")
            Logger.logBusinessEvent(TAG, "User Deleted", mapOf("userId" to userId))
            Logger.logPerformance(TAG, "deleteUser", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "deleteUser", Result.success<Unit>(Unit))
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Error deleting user", e)
            Logger.logBusinessEvent(TAG, "User Deletion Error", mapOf(
                "userId" to userId,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "deleteUser_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "deleteUser", Result.failure<Unit>(e))
            Result.failure(e)
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        Logger.enter(TAG, "signOut")
        Logger.d(TAG, "Delegating sign out to auth repository")
        val result = authRepository.signOut()
        Logger.logBusinessEvent(TAG, "User Sign Out Requested", emptyMap())
        Logger.exit(TAG, "signOut", result)
        return result
    }
}