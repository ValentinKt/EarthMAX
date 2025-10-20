package com.earthmax.core.di

import com.earthmax.core.cache.AdvancedCacheManager
import com.earthmax.core.cache.CacheManager
import com.earthmax.core.error.AdvancedErrorHandler
import com.earthmax.core.error.ErrorHandler
import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.core.monitoring.NetworkMonitor
import com.earthmax.core.monitoring.PerformanceMonitor
import com.earthmax.core.utils.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing core components
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideCacheManager(logger: Logger): CacheManager {
        return CacheManager(logger)
    }

    @Provides
    @Singleton
    fun provideAdvancedCacheManager(
        logger: Logger,
        metricsCollector: MetricsCollector
    ): AdvancedCacheManager {
        return AdvancedCacheManager(logger, metricsCollector)
    }

    /**
     * Provides ErrorHandler singleton
     */
    @Provides
    @Singleton
    fun provideErrorHandler(logger: Logger): ErrorHandler {
        return ErrorHandler(logger)
    }

    /**
     * Provides AdvancedErrorHandler singleton
     */
    @Provides
    @Singleton
    fun provideAdvancedErrorHandler(
        logger: Logger,
        metricsCollector: MetricsCollector
    ): AdvancedErrorHandler {
        return AdvancedErrorHandler(logger, metricsCollector)
    }

    /**
     * Provides Logger singleton
     */
    @Provides
    @Singleton
    fun provideLogger(): Logger {
        return Logger
    }

    /**
     * Provides PerformanceMonitor singleton
     */
    @Provides
    @Singleton
    fun providePerformanceMonitor(): PerformanceMonitor {
        return PerformanceMonitor
    }

    /**
     * Provides MetricsCollector singleton
     */
    @Provides
    @Singleton
    fun provideMetricsCollector(): MetricsCollector {
        return MetricsCollector()
    }

    /**
     * Provides NetworkMonitor singleton
     */
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
}