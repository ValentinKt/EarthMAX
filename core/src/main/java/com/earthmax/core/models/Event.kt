package com.earthmax.core.models

import java.util.Date

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val dateTime: Date = Date(),
    val organizerId: String = "",
    val organizerName: String = "",
    val maxParticipants: Int = 0,
    val currentParticipants: Int = 0,
    val category: EventCategory = EventCategory.CLEANUP,
    val imageUrl: String = "",
    val isJoined: Boolean = false,
    val todoItems: List<TodoItem> = emptyList(),
    val photos: List<String> = emptyList(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class EventCategory {
    CLEANUP,
    TREE_PLANTING,
    RECYCLING,
    EDUCATION,
    CONSERVATION,
    OTHER
}

data class TodoItem(
    val id: String = "",
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val assignedTo: String = "",
    val createdAt: Date = Date(),
    val completedAt: Date? = null
)