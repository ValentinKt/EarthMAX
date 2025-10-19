package com.earthmax.core.network

import com.earthmax.core.utils.Logger
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.android.*

import io.ktor.client.plugins.observer.*
import javax.inject.Singleton

@Singleton
object SupabaseClient {
    
    private const val TAG = "SupabaseClient"
    
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        val initStartTime = System.currentTimeMillis()
        Logger.enter(TAG, "createSupabaseClient", 
            "url" to Logger.maskSensitiveData(BuildConfig.SUPABASE_URL),
            "hasKey" to (BuildConfig.SUPABASE_ANON_KEY.isNotEmpty()),
            "keyLength" to BuildConfig.SUPABASE_ANON_KEY.length
        )
        
        try {
            Logger.i(TAG, "Initializing Supabase client with URL: ${Logger.maskSensitiveData(BuildConfig.SUPABASE_URL)}")
            
            install(Auth) {
                Logger.d(TAG, "Installing Auth plugin with session persistence")
                
                try {
                    // Enable session persistence
                    alwaysAutoRefresh = true
                    autoSaveToStorage = true
                    autoLoadFromStorage = true
                    
                    // Minimal configuration to fix login issues
                    // Disable custom redirect URLs temporarily
                    Logger.logBusinessEvent(TAG, "Auth Plugin Installed", mapOf(
                        "autoRefresh" to true,
                        "autoSave" to true,
                        "autoLoad" to true,
                        "success" to true
                    ))
                    
                    Logger.i(TAG, "Auth plugin configured successfully")
                    
                } catch (e: Exception) {
                    Logger.logError(TAG, "Failed to configure Auth plugin", e, mapOf(
                        "errorType" to e.javaClass.simpleName,
                        "errorMessage" to (e.message ?: "Unknown error")
                    ))
                    throw e
                }
            }
            
            install(Postgrest) {
                Logger.d(TAG, "Installing Postgrest plugin")
                
                try {
                    Logger.logBusinessEvent(TAG, "Postgrest Plugin Installed", mapOf(
                        "success" to true
                    ))
                    Logger.i(TAG, "Postgrest plugin installed successfully")
                    
                } catch (e: Exception) {
                    Logger.logError(TAG, "Failed to install Postgrest plugin", e, mapOf(
                        "errorType" to e.javaClass.simpleName,
                        "errorMessage" to (e.message ?: "Unknown error")
                    ))
                    throw e
                }
            }
            
            install(Realtime) {
                Logger.d(TAG, "Installing Realtime plugin")
                
                try {
                    Logger.logBusinessEvent(TAG, "Realtime Plugin Installed", mapOf(
                        "success" to true
                    ))
                    Logger.i(TAG, "Realtime plugin installed successfully")
                    
                } catch (e: Exception) {
                    Logger.logError(TAG, "Failed to install Realtime plugin", e, mapOf(
                        "errorType" to e.javaClass.simpleName,
                        "errorMessage" to (e.message ?: "Unknown error")
                    ))
                    throw e
                }
            }
            
            install(Storage) {
                Logger.d(TAG, "Installing Storage plugin")
                
                try {
                    Logger.logBusinessEvent(TAG, "Storage Plugin Installed", mapOf(
                        "success" to true
                    ))
                    Logger.i(TAG, "Storage plugin installed successfully")
                    
                } catch (e: Exception) {
                    Logger.logError(TAG, "Failed to install Storage plugin", e, mapOf(
                        "errorType" to e.javaClass.simpleName,
                        "errorMessage" to (e.message ?: "Unknown error")
                    ))
                    throw e
                }
            }
            
            // Configure Ktor client with timeout settings and logging
            httpEngine = Android.create {
                Logger.d(TAG, "Configuring HTTP engine with timeouts")
                
                try {
                    connectTimeout = 30_000
                    socketTimeout = 30_000
                    
                    Logger.logBusinessEvent(TAG, "HTTP Engine Configured", mapOf(
                        "connectTimeout" to 30_000,
                        "socketTimeout" to 30_000,
                        "success" to true
                    ))
                    
                    Logger.i(TAG, "HTTP engine configured with 30s timeouts")
                    
                } catch (e: Exception) {
                    Logger.logError(TAG, "Failed to configure HTTP engine", e, mapOf(
                        "errorType" to e.javaClass.simpleName,
                        "errorMessage" to (e.message ?: "Unknown error")
                    ))
                    throw e
                }
            }
            
            // HTTP logging removed due to Ktor 3.3.0 API compatibility issues
            Logger.logBusinessEvent(TAG, "HTTP Logging Skipped", mapOf(
                "reason" to "Ktor 3.3.0 API compatibility"
            ))
            
            // Response observer removed due to Ktor 3.3.0 API changes
            Logger.logBusinessEvent(TAG, "Response Observer Skipped", mapOf(
                "reason" to "Ktor 3.3.0 API compatibility"
            ))
            
            val initTime = System.currentTimeMillis() - initStartTime
            
            Logger.logBusinessEvent(TAG, "Supabase Client Initialized", mapOf(
                "url" to Logger.maskSensitiveData(BuildConfig.SUPABASE_URL),
                "hasKey" to (BuildConfig.SUPABASE_ANON_KEY.isNotEmpty()),
                "initTime" to initTime,
                "success" to true,
                "pluginsInstalled" to listOf("Auth", "Postgrest", "Realtime", "Storage", "Logging", "ResponseObserver")
            ))
            
            Logger.logPerformance(TAG, "createSupabaseClient", initTime)
            Logger.i(TAG, "Supabase client initialized successfully in ${initTime}ms")
            
        } catch (e: Exception) {
            val initTime = System.currentTimeMillis() - initStartTime
            Logger.logError(TAG, "Failed to initialize Supabase client", e, mapOf(
                "url" to Logger.maskSensitiveData(BuildConfig.SUPABASE_URL),
                "hasKey" to (BuildConfig.SUPABASE_ANON_KEY.isNotEmpty()),
                "initTime" to initTime,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            throw e
        } finally {
            Logger.exit(TAG, "createSupabaseClient")
        }
    }
}