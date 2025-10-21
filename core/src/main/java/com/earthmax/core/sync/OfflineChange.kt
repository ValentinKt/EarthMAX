package com.earthmax.core.sync

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entity representing an offline change that needs to be synced
 */
@Entity(tableName = "offline_changes")
data class OfflineChange(
    @PrimaryKey
    val id: String,
    val entity_type: String,
    val entity_id: String,
    val operation_type: String,
    val data: String,
    val timestamp: String,
    val status: String = "PENDING",
    val retry_count: Int = 0,
    val last_error: String? = null,
    val priority: String = "NORMAL"
)