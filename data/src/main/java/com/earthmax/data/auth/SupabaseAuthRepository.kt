package com.earthmax.data.auth

import android.util.Log
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
            Log.d("SupabaseAuth", "Starting signup process for email: $email")
            Log.d("SupabaseAuth", "Email validation - Length: ${email.length}, Contains @: ${email.contains("@")}")
            Log.d("SupabaseAuth", "Password validation - Length: ${password.length}")
            Log.d("SupabaseAuth", "Supabase client URL: ${SupabaseClient.client.supabaseUrl}")
            
            val result = auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            
            Log.d("SupabaseAuth", "Signup request sent successfully. Email confirmation required.")
            Log.d("SupabaseAuth", "Response received successfully")
            
            // When email confirmation is enabled, no session is returned immediately
            // The user needs to confirm their email first
            // We return a success result to indicate the signup request was sent
            Result.success(
                User(
                    id = "", // Will be populated after email confirmation
                    email = email,
                    displayName = name
                )
            )
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Signup failed for email: $email", e)
            Log.e("SupabaseAuth", "Exception type: ${e.javaClass.simpleName}")
            Log.e("SupabaseAuth", "Exception message: ${e.message}")
            
            // Log additional details for AuthRestException
            if (e is io.github.jan.supabase.auth.exception.AuthRestException) {
                Log.e("SupabaseAuth", "Auth REST Exception details:")
                Log.e("SupabaseAuth", "- Error code: ${e.error}")
                Log.e("SupabaseAuth", "- Error description: ${e.errorDescription}")
                Log.e("SupabaseAuth", "- HTTP status: ${e.statusCode}")
            }
            
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            Log.d("SupabaseAuth", "=== Starting signIn process ===")
            Log.d("SupabaseAuth", "Email: $email")
            Log.d("SupabaseAuth", "Password length: ${password.length}")
            
            val result = auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            Log.d("SupabaseAuth", "SignIn request completed")
            
            // For gotrue-kt 2.4.1, access user through session
            val session = auth.currentSessionOrNull()
            Log.d("SupabaseAuth", "Current session after signIn: ${if (session != null) "exists" else "null"}")
            
            val userInfo = session?.user
            Log.d("SupabaseAuth", "User info: ${if (userInfo != null) "exists (${userInfo.email})" else "null"}")
            
            if (userInfo != null) {
                Log.d("SupabaseAuth", "SignIn successful for user: ${userInfo.email}")
                Result.success(
                    User(
                        id = userInfo.id.toString(),
                        email = userInfo.email ?: "",
                        displayName = "", // Default empty for gotrue-kt 2.4.1
                        profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                    )
                )
            } else {
                Log.e("SupabaseAuth", "SignIn failed - no user info in session")
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "SignIn exception: ${e.message}", e)
            Log.e("SupabaseAuth", "Exception type: ${e.javaClass.simpleName}")
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
            Log.d("SupabaseAuth", "getCurrentUser() called - checking initial session")
            
            // Check if there's an existing session
            val currentSession = auth.currentSessionOrNull()
            Log.d("SupabaseAuth", "Current session: ${if (currentSession != null) "exists" else "null"}")
            
            if (currentSession?.user != null) {
                val userInfo = currentSession.user!!
                Log.d("SupabaseAuth", "Found existing session for user: ${userInfo.email}")
                emit(
                    User(
                        id = userInfo.id.toString(),
                        email = userInfo.email ?: "",
                        displayName = "", // Default empty for gotrue-kt 2.4.1
                        profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                    )
                )
            } else {
                Log.d("SupabaseAuth", "No existing session found, emitting null")
                emit(null)
            }
            
            // Listen for session changes
            auth.sessionStatus.collect { status ->
                Log.d("SupabaseAuth", "Session status changed: ${status::class.simpleName}")
                when (status) {
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                        val userInfo = status.session.user
                        Log.d("SupabaseAuth", "Authenticated - user: ${userInfo?.email}")
                        if (userInfo != null) {
                            emit(
                                User(
                                    id = userInfo.id.toString(),
                                    email = userInfo.email ?: "",
                                    displayName = "", // Default empty for gotrue-kt 2.4.1
                                    profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                                )
                            )
                        }
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                        Log.d("SupabaseAuth", "Not authenticated - emitting null")
                        emit(null)
                    }
                    else -> {
                        Log.d("SupabaseAuth", "Other session status: ${status::class.simpleName}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Error in getCurrentUser flow", e)
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