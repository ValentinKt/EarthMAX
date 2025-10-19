package com.earthmax.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing log entries in local database
 */
@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val level: String,
    val tag: String,
    val message: String,
    val timestamp: Long,
    val exception: String? = null,
    val metadata: String // Serialized metadata as key=value;key=value format
)