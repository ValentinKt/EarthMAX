package com.earthmax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.earthmax.core.ui.theme.EarthMaxTheme
import com.earthmax.feature.events.create.CreateEventScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateEventPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EarthMaxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    CreateEventScreen(
                        onNavigateBack = { finish() },
                        onEventCreated = { /* no-op for preview */ }
                    )
                }
            }
        }
    }
}
