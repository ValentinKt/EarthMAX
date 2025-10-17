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
            // Temporarily disable custom redirect URLs to test email validation
            // scheme = "earthmax"
            // host = "earthmax.app"
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
        
        // Configure Ktor client
        httpEngine = Android.create()
    }
}