package com.earthmax

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.earthmax.core.network.SupabaseClient
import com.earthmax.navigation.EarthMaxNavigation
import com.earthmax.ui.theme.EarthMaxTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Handle deep link intent
        handleDeepLink(intent)
        
        setContent {
            EarthMaxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    EarthMaxNavigation()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            Log.d("MainActivity", "Deep link received: $data")
            
            // Handle email confirmation deep link
            if (data.scheme == "earthmax" && data.host == "auth" && data.path == "/callback") {
                handleEmailConfirmation(data)
            } else if (data.scheme == "https" && data.host == "earthmax.app" && data.path?.startsWith("/auth/callback") == true) {
                handleEmailConfirmation(data)
            }
        }
    }
    
    private fun handleEmailConfirmation(uri: Uri) {
        Log.d("MainActivity", "Handling email confirmation with URI: $uri")
        
        val accessToken = uri.getQueryParameter("access_token")
        val refreshToken = uri.getQueryParameter("refresh_token")
        val type = uri.getQueryParameter("type")
        
        Log.d("MainActivity", "Email confirmation type: $type")
        Log.d("MainActivity", "Access token present: ${accessToken != null}")
        Log.d("MainActivity", "Refresh token present: ${refreshToken != null}")
        
        if (accessToken != null && refreshToken != null) {
            lifecycleScope.launch {
                try {
                    Log.d("MainActivity", "Importing session with tokens")
                    val userSession = UserSession(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresIn = 3600, // Default expiration time
                        tokenType = "Bearer",
                        user = null // Will be populated by Supabase
                    )
                    SupabaseClient.client.auth.importSession(userSession)
                    Log.d("MainActivity", "Session imported successfully")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to import session", e)
                }
            }
        } else {
            Log.w("MainActivity", "Missing tokens in email confirmation URI")
        }
    }
}