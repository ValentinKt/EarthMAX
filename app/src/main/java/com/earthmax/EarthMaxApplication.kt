package com.earthmax

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class EarthMaxApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }
}