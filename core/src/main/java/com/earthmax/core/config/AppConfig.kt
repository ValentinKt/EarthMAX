package com.earthmax.core.config

/**
 * Application configuration constants
 */
object AppConfig {

    // Cache Configuration
    object Cache {
        const val DEFAULT_CACHE_SIZE = 50L * 1024 * 1024 // 50MB
        const val DEFAULT_CACHE_DURATION_MINUTES = 30L
        const val MAX_CACHE_ENTRIES = 1000
        const val CACHE_CLEANUP_INTERVAL_MINUTES = 60L
        const val CLEANUP_INTERVAL_MS = 60L * 60 * 1000 // 1 hour in milliseconds
    }

    // Network Configuration
    object Network {
        const val DEFAULT_TIMEOUT_SECONDS = 30L
        const val RETRY_COUNT = 3
        const val RETRY_DELAY_MS = 1000L
        const val MAX_CONCURRENT_REQUESTS = 10
    }

    // Performance Configuration
    object Performance {
        const val SLOW_OPERATION_THRESHOLD_MS = 1000L
        const val VERY_SLOW_OPERATION_THRESHOLD_MS = 5000L
        const val MAX_PERFORMANCE_ENTRIES = 1000
        const val PERFORMANCE_CLEANUP_INTERVAL_MINUTES = 60L
    }

    // Logging Configuration
    object Logging {
        const val MAX_LOG_ENTRIES = 10000
        const val LOG_CLEANUP_INTERVAL_MINUTES = 120L
        const val DEBUG_MODE = true // Should be false in production
    }

    // Database Configuration
    object Database {
        const val DATABASE_NAME = "earthmax_database"
        const val DATABASE_VERSION = 1
        const val MAX_DATABASE_SIZE_MB = 100L
    }

    // UI Configuration
    object UI {
        const val ANIMATION_DURATION_MS = 300L
        const val DEBOUNCE_DELAY_MS = 500L
        const val PAGINATION_PAGE_SIZE = 20
        const val MAX_IMAGE_SIZE_MB = 5L
    }

    // Feature Flags
    object Features {
        const val ENABLE_ANALYTICS = true
        const val ENABLE_CRASH_REPORTING = true
        const val ENABLE_PERFORMANCE_MONITORING = true
        const val ENABLE_OFFLINE_MODE = true
        const val ENABLE_PUSH_NOTIFICATIONS = true
    }

    // Security Configuration
    object Security {
        const val SESSION_TIMEOUT_MINUTES = 60L
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 15L
        const val PASSWORD_MIN_LENGTH = 8
    }

    // API Configuration
    object Api {
        const val BASE_URL = "https://your-supabase-url.supabase.co"
        const val API_VERSION = "v1"
        const val REQUEST_TIMEOUT_SECONDS = 30L
        const val MAX_RETRY_ATTEMPTS = 3
    }
}