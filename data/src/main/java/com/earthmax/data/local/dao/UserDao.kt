package com.earthmax.data.local.dao

import androidx.room.*
import com.earthmax.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>
    
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>
    
    @Query("""
        SELECT * FROM users 
        WHERE displayName LIKE '%' || :query || '%' 
        OR email LIKE '%' || :query || '%'
        ORDER BY displayName ASC
    """)
    fun searchUsers(query: String): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users ORDER BY ecoPoints DESC LIMIT :limit")
    fun getTopUsersByEcoPoints(limit: Int): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)
    
    @Query("UPDATE users SET ecoPoints = :points WHERE id = :userId")
    suspend fun updateUserEcoPoints(userId: String, points: Int)
    
    @Query("UPDATE users SET profileImageUrl = :imageUrl WHERE id = :userId")
    suspend fun updateUserProfileImage(userId: String, imageUrl: String)
    
    @Query("UPDATE users SET displayName = :displayName WHERE id = :userId")
    suspend fun updateUserDisplayName(userId: String, displayName: String)
    
    @Query("UPDATE users SET bio = :bio WHERE id = :userId")
    suspend fun updateUserBio(userId: String, bio: String)
    
    @Query("UPDATE users SET location = :location WHERE id = :userId")
    suspend fun updateUserLocation(userId: String, location: String)
}