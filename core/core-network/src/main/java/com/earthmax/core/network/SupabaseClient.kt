package com.earthmax.core.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.android.*
import javax.inject.Singleton

@Singleton
object SupabaseClient {
    
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Enable session persistence
            alwaysAutoRefresh = true
            autoSaveToStorage = true
            autoLoadFromStorage = true
            
            // Minimal configuration to fix login issues
            // Disable custom redirect URLs temporarily
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
        
        // Configure Ktor client with timeout settings
        httpEngine = Android.create {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }
}