package com.earthmax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.models.TodoItem
import java.util.Date

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val dateTime: Date,
    val organizerId: String,
    val organizerName: String,
    val maxParticipants: Int,
    val currentParticipants: Int,
    val category: EventCategory,
    val imageUrl: String,
    val isJoined: Boolean,
    val todoItems: List<TodoItem>,
    val photos: List<String>,
    val createdAt: Date,
    val updatedAt: Date
)

fun EventEntity.toEvent(): Event {
    return Event(
        id = id,
        title = title,
        description = description,
        location = location,
        latitude = latitude,
        longitude = longitude,
        dateTime = dateTime,
        organizerId = organizerId,
        organizerName = organizerName,
        maxParticipants = maxParticipants,
        currentParticipants = currentParticipants,
        category = category,
        imageUrl = imageUrl,
        isJoined = isJoined,
        todoItems = todoItems,
        photos = photos,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Event.toEntity(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        description = description,
        location = location,
        latitude = latitude,
        longitude = longitude,
        dateTime = dateTime,
        organizerId = organizerId,
        organizerName = organizerName,
        maxParticipants = maxParticipants,
        currentParticipants = currentParticipants,
        category = category,
        imageUrl = imageUrl,
        isJoined = isJoined,
        todoItems = todoItems,
        photos = photos,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}