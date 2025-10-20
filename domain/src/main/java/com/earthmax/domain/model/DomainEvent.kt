package com.earthmax.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model for events in the EarthMAX application.
 * This represents the business logic view of an event, separate from data layer DTOs.
 */
data class DomainEvent(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val startDate: Instant,
    val endDate: Instant?,
    val category: EventCategory,
    val severity: EventSeverity,
    val status: EventStatus,
    val organizerId: String,
    val participantCount: Int = 0,
    val maxParticipants: Int? = null,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class EventCategory {
    CLEANUP,
    TREE_PLANTING,
    AWARENESS,
    CONSERVATION,
    RECYCLING,
    EDUCATION,
    OTHER
}

enum class EventSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    ONGOING,
    COMPLETED,
    CANCELLED
}