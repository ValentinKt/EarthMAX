package com.earthmax.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing performance metrics in local database
 */
@Entity(tableName = "performance_metrics")
data class PerformanceMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operation: String,
    val tag: String,
    val duration: Double,
    val timestamp: Long,
    val metadata: String // Serialized metadata as key=value;key=value format
)