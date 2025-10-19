package com.earthmax.core.di

import android.content.Context
import com.earthmax.core.utils.LocationProvider
import com.earthmax.core.monitoring.LogFilterManager
import com.earthmax.core.monitoring.PerformanceMetricsCollector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): LocationProvider {
        return LocationProvider(context)
    }
    
    @Provides
    @Singleton
    fun provideLogFilterManager(): LogFilterManager {
        return LogFilterManager()
    }
    
    @Provides
    @Singleton
    fun providePerformanceMetricsCollector(): PerformanceMetricsCollector {
        return PerformanceMetricsCollector()
    }
}