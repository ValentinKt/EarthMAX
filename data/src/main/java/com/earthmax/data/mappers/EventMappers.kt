package com.earthmax.data.mappers

import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory as CoreEventCategory
import com.earthmax.domain.model.DomainEvent
import com.earthmax.domain.model.EventCategory
import com.earthmax.domain.model.EventSeverity
import com.earthmax.domain.model.EventStatus
import kotlinx.datetime.Instant

import java.util.Date

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
        startDate = Instant.fromEpochMilliseconds(this.dateTime.time),
        endDate = null,
        category = this.category.toDomainCategory(),
        severity = EventSeverity.MEDIUM,
        status = EventStatus.ONGOING,
        organizerId = this.organizerId,
        participantCount = this.currentParticipants,
        maxParticipants = this.maxParticipants,
        tags = emptyList(),
        imageUrl = this.imageUrl,
        createdAt = Instant.fromEpochMilliseconds(this.createdAt.time),
        updatedAt = Instant.fromEpochMilliseconds(this.updatedAt.time)
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
        location = this.location,
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        dateTime = Date(this.startDate.toEpochMilliseconds()),
        organizerId = this.organizerId,
        organizerName = "", // Will be populated by the repository if needed
        maxParticipants = this.maxParticipants ?: 0,
        currentParticipants = this.participantCount,
        category = this.category.toCoreCategory(),
        imageUrl = this.imageUrl ?: "",
        isJoined = false, // Will be determined by the repository based on user context
        todoItems = emptyList(),
        photos = emptyList(),
        createdAt = Date(this.createdAt.toEpochMilliseconds()),
        updatedAt = Date(this.updatedAt.toEpochMilliseconds())
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
        EventCategory.AWARENESS -> CoreEventCategory.EDUCATION
        EventCategory.OTHER -> CoreEventCategory.OTHER
    }
}


