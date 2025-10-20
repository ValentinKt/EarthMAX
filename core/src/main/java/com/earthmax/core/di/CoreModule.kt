package com.earthmax.core.di

import androidx.work.WorkManager
import com.earthmax.core.cache.AdvancedCacheManager
import com.earthmax.core.cache.CacheManager
import com.earthmax.core.error.AdvancedErrorHandler
import com.earthmax.core.error.ErrorHandler
import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.core.monitoring.NetworkMonitor
import com.earthmax.core.monitoring.PerformanceMonitor
import com.earthmax.core.sync.*
import com.earthmax.performance.PerformanceMonitor as NewPerformanceMonitor
import com.earthmax.performance.FrameTimeTracker
import com.earthmax.performance.MemoryTracker
import com.earthmax.performance.NetworkTracker
import com.earthmax.performance.BatteryTracker
import com.earthmax.performance.UIPerformanceOptimizer
import com.earthmax.performance.MemoryLeakDetector
import com.earthmax.performance.DatabaseOptimizer
import com.earthmax.core.utils.Logger
import com.earthmax.core.database.EarthMaxDatabase
import androidx.room.Room
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

    /**
     * Provides SyncManager singleton
     */
    @Provides
    @Singleton
    fun provideSyncManager(
        offlineChangeTracker: OfflineChangeTracker,
        conflictResolver: ConflictResolver,
        networkMonitor: com.earthmax.core.sync.NetworkMonitor,
        cacheManager: AdvancedCacheManager,
        errorHandler: AdvancedErrorHandler,
        logger: Logger,
        metricsCollector: MetricsCollector
    ): SyncManager = SyncManager(
        offlineChangeTracker,
        conflictResolver,
        networkMonitor,
        cacheManager,
        errorHandler,
        logger,
        metricsCollector
    )

    /**
     * Provides ConflictResolver singleton
     */
    @Provides
    @Singleton
    fun provideConflictResolver(
        logger: Logger
    ): ConflictResolver = ConflictResolver(logger)

    /**
     * Provides sync NetworkMonitor singleton
     */
    @Provides
    @Singleton
    fun provideSyncNetworkMonitor(
        @ApplicationContext context: Context,
        logger: Logger
    ): com.earthmax.core.sync.NetworkMonitor = com.earthmax.core.sync.NetworkMonitor(context, logger)

    /**
     * Provides OfflineChangeTracker singleton
     */
    @Provides
    @Singleton
    fun provideOfflineChangeTracker(
        offlineChangeDao: OfflineChangeDao,
        logger: Logger
    ): OfflineChangeTracker = OfflineChangeTracker(offlineChangeDao, logger)

    /**
     * Provides SyncScheduler singleton
     */
    @Provides
    @Singleton
    fun provideSyncScheduler(
        workManager: WorkManager,
        networkMonitor: com.earthmax.core.sync.NetworkMonitor,
        logger: Logger
    ): SyncScheduler = SyncScheduler(workManager, networkMonitor, logger)

    /**
     * Provides WorkManager singleton
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    /**
     * Provides EarthMaxDatabase singleton
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EarthMaxDatabase {
        return Room.databaseBuilder(
            context,
            EarthMaxDatabase::class.java,
            "earthmax_database"
        ).fallbackToDestructiveMigration().build()
    }

    /**
     * Provides OfflineChangeDao
     */
    @Provides
    fun provideOfflineChangeDao(database: EarthMaxDatabase): OfflineChangeDao {
        return database.offlineChangeDao()
    }

    // Performance Monitoring Modules

    /**
     * Provides NewPerformanceMonitor singleton
     */
    @Provides
    @Singleton
    fun provideNewPerformanceMonitor(@ApplicationContext context: Context): NewPerformanceMonitor {
        return NewPerformanceMonitor(context)
    }

    /**
     * Provides FrameTimeTracker singleton
     */
    @Provides
    @Singleton
    fun provideFrameTimeTracker(): FrameTimeTracker {
        return FrameTimeTracker()
    }

    /**
     * Provides MemoryTracker singleton
     */
    @Provides
    @Singleton
    fun provideMemoryTracker(@ApplicationContext context: Context): MemoryTracker {
        return MemoryTracker(context)
    }

    /**
     * Provides NetworkTracker singleton
     */
    @Provides
    @Singleton
    fun provideNetworkTracker(): NetworkTracker {
        return NetworkTracker()
    }

    /**
     * Provides BatteryTracker singleton
     */
    @Provides
    @Singleton
    fun provideBatteryTracker(@ApplicationContext context: Context): BatteryTracker {
        return BatteryTracker(context)
    }

    /**
     * Provides UIPerformanceOptimizer singleton
     */
    @Provides
    @Singleton
    fun provideUIPerformanceOptimizer(@ApplicationContext context: Context): UIPerformanceOptimizer {
        return UIPerformanceOptimizer(context)
    }

    /**
     * Provides MemoryLeakDetector singleton
     */
    @Provides
    @Singleton
    fun provideMemoryLeakDetector(@ApplicationContext context: Context): MemoryLeakDetector {
        return MemoryLeakDetector(context)
    }

    /**
     * Provides DatabaseOptimizer singleton
     */
    @Provides
    @Singleton
    fun provideDatabaseOptimizer(database: EarthMaxDatabase): DatabaseOptimizer {
        return DatabaseOptimizer(database)
    }
}