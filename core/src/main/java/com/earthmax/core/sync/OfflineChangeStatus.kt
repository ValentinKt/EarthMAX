package com.earthmax.core.sync

/**
 * Status of an offline change
 */
enum class OfflineChangeStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    CONFLICT
}