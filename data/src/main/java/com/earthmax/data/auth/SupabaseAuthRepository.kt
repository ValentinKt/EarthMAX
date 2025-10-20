package com.earthmax.data.auth

import com.earthmax.core.models.User
import com.earthmax.core.network.SupabaseClient
import com.earthmax.core.utils.Logger
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor() {
    
    private val auth = SupabaseClient.client.auth
    
    companion object {
        private const val TAG = "SupabaseAuthRepository"
    }
    
    suspend fun signUp(email: String, password: String, name: String): Result<User> {
        Logger.enter(TAG, "signUp", "email" to email, "name" to name)
        
        return try {
            Logger.logBusinessEvent(TAG, "User Registration Started", mapOf(
                "email" to email,
                "hasName" to name.isNotBlank()
            ))
            
            Logger.d(TAG, "Email validation - Length: ${email.length}, Contains @: ${email.contains("@")}")
            Logger.d(TAG, "Password validation - Length: ${password.length}")
            Logger.d(TAG, "Supabase client URL: ${SupabaseClient.client.supabaseUrl}")
            
            val startTime = System.currentTimeMillis()
            val result = auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val duration = System.currentTimeMillis() - startTime
            
            Logger.logPerformance(TAG, "Supabase SignUp Request", duration)
            Logger.i(TAG, "Signup request sent successfully. Email confirmation required.")
            
            val user = User(
                id = "", // Will be populated after email confirmation
                email = email,
                displayName = name
            )
            
            Logger.logBusinessEvent(TAG, "User Registration Success", mapOf(
                "email" to email,
                "requiresEmailConfirmation" to true
            ))
            
            Logger.exit(TAG, "signUp", "Success - Email confirmation required")
            Result.success(user)
        } catch (e: Exception) {
            Logger.e(TAG, "Signup failed for email: $email", e)
            
            // Log additional details for AuthRestException
            if (e is io.github.jan.supabase.auth.exception.AuthRestException) {
                Logger.e(TAG, "Auth REST Exception details: error=${e.error}, description=${e.errorDescription}, status=${e.statusCode}")
            }
            
            Logger.logBusinessEvent(TAG, "User Registration Failed", mapOf(
                "email" to email,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            
            Logger.exit(TAG, "signUp", "Failed with exception")
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        Logger.enter(TAG, "signIn", "email" to email)
        
        return try {
            Logger.logBusinessEvent(TAG, "User Login Started", mapOf("email" to email))
            
            val startTime = System.currentTimeMillis()
            val result = auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val duration = System.currentTimeMillis() - startTime
            
            Logger.logPerformance(TAG, "Supabase SignIn Request", duration)
            Logger.d(TAG, "SignIn request completed")
            
            // For gotrue-kt 2.4.1, access user through session
            val session = auth.currentSessionOrNull()
            Logger.d(TAG, "Current session after signIn: ${if (session != null) "exists" else "null"}")
            
            val userInfo = session?.user
            Logger.d(TAG, "User info: ${if (userInfo != null) "exists (${userInfo.email})" else "null"}")
            
            if (userInfo != null) {
                val user = User(
                    id = userInfo.id.toString(),
                    email = userInfo.email ?: "",
                    displayName = "", // Default empty for gotrue-kt 2.4.1
                    profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                )
                
                Logger.logBusinessEvent(TAG, "User Login Success", mapOf(
                    "userId" to user.id,
                    "email" to user.email
                ))
                
                Logger.exit(TAG, "signIn", "Success")
                Result.success(user)
            } else {
                Logger.e(TAG, "SignIn failed - no user info in session")
                Logger.logBusinessEvent(TAG, "User Login Failed", mapOf(
                    "email" to email,
                    "reason" to "No user info in session"
                ))
                
                Logger.exit(TAG, "signIn", "Failed - no user info")
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "SignIn exception: ${e.message}", e)
            Logger.logBusinessEvent(TAG, "User Login Failed", mapOf(
                "email" to email,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            
            Logger.exit(TAG, "signIn", "Failed with exception")
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        Logger.enter(TAG, "signOut")
        
        return try {
            Logger.logBusinessEvent(TAG, "User Logout Started")
            
            val startTime = System.currentTimeMillis()
            auth.signOut()
            val duration = System.currentTimeMillis() - startTime
            
            Logger.logPerformance(TAG, "Supabase SignOut Request", duration)
            Logger.logBusinessEvent(TAG, "User Logout Success")
            
            Logger.exit(TAG, "signOut", "Success")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "SignOut failed", e)
            Logger.logBusinessEvent(TAG, "User Logout Failed", mapOf(
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            
            Logger.exit(TAG, "signOut", "Failed with exception")
            Result.failure(e)
        }
    }
    
    fun getCurrentUser(): Flow<User?> = flow {
        Logger.enter(TAG, "getCurrentUser")
        
        try {
            Logger.d(TAG, "Checking initial session")
            
            // Check if there's an existing session
            val currentSession = auth.currentSessionOrNull()
            Logger.d(TAG, "Current session: ${if (currentSession != null) "exists" else "null"}")
            
            if (currentSession?.user != null) {
                val userInfo = currentSession.user!!
                Logger.d(TAG, "Found existing session for user: ${userInfo.email}")
                
                val user = User(
                    id = userInfo.id.toString(),
                    email = userInfo.email ?: "",
                    displayName = "", // Default empty for gotrue-kt 2.4.1
                    profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                )
                
                Logger.logBusinessEvent(TAG, "User Session Retrieved", mapOf(
                    "userId" to user.id,
                    "email" to user.email
                ))
                
                emit(user)
            } else {
                Logger.d(TAG, "No existing session found, emitting null")
                emit(null)
            }
            
            // Listen for session changes with proper cancellation handling
            try {
                auth.sessionStatus.collect { status ->
                    Logger.d(TAG, "Session status changed: ${status::class.simpleName}")
                    when (status) {
                        is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                            val userInfo = status.session.user
                            Logger.d(TAG, "Authenticated - user: ${userInfo?.email}")
                            if (userInfo != null) {
                                val user = User(
                                    id = userInfo.id.toString(),
                                    email = userInfo.email ?: "",
                                    displayName = "", // Default empty for gotrue-kt 2.4.1
                                    profileImageUrl = "" // Default empty for gotrue-kt 2.4.1
                                )
                                
                                Logger.logBusinessEvent(TAG, "User Session Authenticated", mapOf(
                                    "userId" to user.id,
                                    "email" to user.email
                                ))
                                
                                emit(user)
                            }
                        }
                        is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                            Logger.d(TAG, "Not authenticated - emitting null")
                            Logger.logBusinessEvent(TAG, "User Session Lost")
                            emit(null)
                        }
                        else -> {
                            Logger.d(TAG, "Other session status: ${status::class.simpleName}")
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Logger.d(TAG, "Session status collection cancelled")
                // Don't emit anything on cancellation, just let it propagate
                throw e
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Logger.d(TAG, "getCurrentUser flow cancelled")
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Error in getCurrentUser flow", e)
            Logger.logBusinessEvent(TAG, "User Session Error", mapOf(
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            emit(null)
        }
    }
    
    suspend fun resetPassword(email: String): Result<Unit> {
        Logger.enter(TAG, "resetPassword", 
            "email" to Logger.maskSensitiveData(email)
        )
        val startTime = System.currentTimeMillis()
        
        return try {
            auth.resetPasswordForEmail(email)
            
            Logger.logPerformance(
                TAG,
                "resetPassword",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "email" to Logger.maskSensitiveData(email),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "password_reset_requested",
                mapOf(
                    "email" to Logger.maskSensitiveData(email),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "resetPassword")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "resetPassword",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "email" to Logger.maskSensitiveData(email),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to reset password", e, mapOf(
                "email" to Logger.maskSensitiveData(email),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "resetPassword")
            Result.failure(e)
        }
    }
}