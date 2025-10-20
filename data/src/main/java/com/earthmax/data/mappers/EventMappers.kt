package com.earthmax.data.mappers

import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory as CoreEventCategory
import com.earthmax.domain.model.DomainEvent
import com.earthmax.domain.model.EventCategory
import com.earthmax.domain.model.EventSeverity
import com.earthmax.domain.model.EventStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension function to convert Core Event to Domain Event
 */
fun Event.toDomainEvent(): DomainEvent {
    return DomainEvent(
        id = this.id,
        title = this.title,
        description = this.description,
        location = this.location,
        latitude = this.latitude,
        longitude = this.longitude,
        startDate = parseDate(this.date),
        endDate = parseDate(this.date), // Assuming same date for now, can be enhanced
        category = this.category.toDomainCategory(),
        severity = EventSeverity.MEDIUM, // Default severity, can be enhanced
        status = EventStatus.ACTIVE, // Default status, can be enhanced
        organizerId = this.organizerId,
        participantCount = this.currentParticipants,
        maxParticipants = this.maxParticipants,
        tags = emptyList(), // Can be enhanced to parse from description or add tags field
        imageUrl = this.imageUrl,
        createdAt = parseDate(this.createdAt),
        updatedAt = parseDate(this.createdAt) // Using createdAt as updatedAt for now
    )
}

/**
 * Extension function to convert Domain Event to Core Event
 */
fun DomainEvent.toEvent(): Event {
    return Event(
        id = this.id,
        title = this.title,
        description = this.description,
        date = formatDate(this.startDate),
        location = this.location,
        latitude = this.latitude,
        longitude = this.longitude,
        organizerId = this.organizerId,
        organizerName = "", // Will be populated by the repository if needed
        imageUrl = this.imageUrl,
        maxParticipants = this.maxParticipants,
        currentParticipants = this.participantCount,
        category = this.category.toCoreCategory(),
        createdAt = formatDate(this.createdAt),
        isJoined = false // Will be determined by the repository based on user context
    )
}

/**
 * Convert Core EventCategory to Domain EventCategory
 */
fun CoreEventCategory.toDomainCategory(): EventCategory {
    return when (this) {
        CoreEventCategory.CLEANUP -> EventCategory.CLEANUP
        CoreEventCategory.TREE_PLANTING -> EventCategory.TREE_PLANTING
        CoreEventCategory.RECYCLING -> EventCategory.RECYCLING
        CoreEventCategory.EDUCATION -> EventCategory.EDUCATION
        CoreEventCategory.CONSERVATION -> EventCategory.CONSERVATION
        CoreEventCategory.RENEWABLE_ENERGY -> EventCategory.RENEWABLE_ENERGY
        CoreEventCategory.SUSTAINABLE_TRANSPORT -> EventCategory.SUSTAINABLE_TRANSPORT
        CoreEventCategory.WILDLIFE_PROTECTION -> EventCategory.WILDLIFE_PROTECTION
        CoreEventCategory.WATER_CONSERVATION -> EventCategory.WATER_CONSERVATION
        CoreEventCategory.COMMUNITY_GARDEN -> EventCategory.COMMUNITY_GARDEN
        CoreEventCategory.OTHER -> EventCategory.OTHER
    }
}

/**
 * Convert Domain EventCategory to Core EventCategory
 */
fun EventCategory.toCoreCategory(): CoreEventCategory {
    return when (this) {
        EventCategory.CLEANUP -> CoreEventCategory.CLEANUP
        EventCategory.TREE_PLANTING -> CoreEventCategory.TREE_PLANTING
        EventCategory.RECYCLING -> CoreEventCategory.RECYCLING
        EventCategory.EDUCATION -> CoreEventCategory.EDUCATION
        EventCategory.CONSERVATION -> CoreEventCategory.CONSERVATION
        EventCategory.RENEWABLE_ENERGY -> CoreEventCategory.RENEWABLE_ENERGY
        EventCategory.SUSTAINABLE_TRANSPORT -> CoreEventCategory.SUSTAINABLE_TRANSPORT
        EventCategory.WILDLIFE_PROTECTION -> CoreEventCategory.WILDLIFE_PROTECTION
        EventCategory.WATER_CONSERVATION -> CoreEventCategory.WATER_CONSERVATION
        EventCategory.COMMUNITY_GARDEN -> CoreEventCategory.COMMUNITY_GARDEN
        EventCategory.OTHER -> CoreEventCategory.OTHER
    }
}

/**
 * Parse date string to Instant
 */
private fun parseDate(dateString: String): Instant {
    return try {
        // Try ISO format first
        Instant.parse(dateString)
    } catch (e: Exception) {
        try {
            // Fallback to custom format
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = formatter.parse(dateString)
            date?.toInstant()?.toKotlinInstant() ?: Instant.DISTANT_PAST
        } catch (e: Exception) {
            Instant.DISTANT_PAST
        }
    }
}

/**
 * Format Instant to date string
 */
private fun formatDate(instant: Instant): String {
    return instant.toString()
}