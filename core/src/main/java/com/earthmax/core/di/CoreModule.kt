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
import com.earthmax.core.performance.*
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
    fun provideCacheManager(): CacheManager {
        return CacheManager()
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
    fun provideErrorHandler(): ErrorHandler {
        return ErrorHandler()
    }

    /**
     * Provides AdvancedErrorHandler singleton
     */
    @Provides
    @Singleton
    fun provideAdvancedErrorHandler(
        metricsCollector: MetricsCollector
    ): AdvancedErrorHandler {
        return AdvancedErrorHandler(metricsCollector)
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
        return PerformanceMonitor()
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
        cacheManager,
        errorHandler,
        networkMonitor,
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

    // Performance Monitor Stubs

    @Provides
    @Singleton
    fun provideMemoryMonitor(): MemoryMonitor = NoOpMemoryMonitor()

    @Provides
    @Singleton
    fun provideFrameRateMonitor(): FrameRateMonitor = NoOpFrameRateMonitor()

    @Provides
    @Singleton
    fun provideNetworkPerformanceMonitor(): com.earthmax.core.performance.NetworkMonitor = NoOpNetworkMonitor()

    @Provides
    @Singleton
    fun provideBatteryMonitor(): BatteryMonitor = NoOpBatteryMonitor()

    @Provides
    @Singleton
    fun provideDatabaseMonitor(): DatabaseMonitor = NoOpDatabaseMonitor()

    @Provides
    @Singleton
    fun provideUiMonitor(): UiMonitor = NoOpUiMonitor()
}