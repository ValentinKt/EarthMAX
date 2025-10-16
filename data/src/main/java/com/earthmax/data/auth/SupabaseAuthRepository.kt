package com.earthmax.data.auth

import com.earthmax.core.models.User
import com.earthmax.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor() {
    
    private val auth = SupabaseClient.client.auth
    
    suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            val result = auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            
            // For gotrue-kt 2.4.1, access user through session
            val session = auth.currentSessionOrNull()
            val userInfo = session?.user
            if (userInfo != null) {
                Result.success(
                    User(
                        id = userInfo.id.toString(),
                        email = userInfo.email ?: "",
                        displayName = "", // Default empty for gotrue-kt 2.4.1
                        profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                    )
                )
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            // For gotrue-kt 2.4.1, access user through session
            val session = auth.currentSessionOrNull()
            val userInfo = session?.user
            if (userInfo != null) {
                Result.success(
                    User(
                        id = userInfo.id.toString(),
                        email = userInfo.email ?: "",
                        displayName = "", // Default empty for gotrue-kt 2.4.1
                        profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                    )
                )
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentUser(): Flow<User?> = flow {
        try {
            val session = auth.currentSessionOrNull()
            val userInfo = session?.user
            if (userInfo != null) {
                emit(
                    User(
                        id = userInfo.id.toString(),
                        email = userInfo.email ?: "",
                        displayName = "", // Default empty for gotrue-kt 2.4.1
                        profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                    )
                )
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}