package com.earthmax.data.events

import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val organizer_id: String,
    val organizer_name: String,
    val image_url: String? = null,
    val max_participants: Int? = null,
    val current_participants: Int = 0,
    val category: String,
    val created_at: String
)

@Singleton
class SupabaseEventsRepository @Inject constructor() {
    
    private val supabase = SupabaseClient.client
    
    fun getEvents(): Flow<List<Event>> = flow {
        try {
            val events = supabase.from("events").select().decodeList<EventDto>()
            emit(events.map { it.toEvent() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun getEventById(eventId: String): Result<Event> {
        return try {
            val event = supabase.from("events").select {
                filter {
                    eq("id", eventId)
                }
            }.decodeSingle<EventDto>()
            Result.success(event.toEvent())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createEvent(event: Event): Result<Event> {
        return try {
            val eventDto = event.toEventDto()
            val createdEvent = supabase.from("events")
                .insert(eventDto)
                .decodeSingle<EventDto>()
            Result.success(createdEvent.toEvent())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateEvent(event: Event): Result<Event> {
        return try {
            val eventDto = event.toEventDto()
            val updatedEvent = supabase.from("events")
                .update(eventDto) {
                    filter {
                        eq("id", event.id)
                    }
                }
                .decodeSingle<EventDto>()
            Result.success(updatedEvent.toEvent())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            supabase.from("events").delete {
                filter {
                    eq("id", eventId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getEventsByCategory(category: String): Flow<List<Event>> = flow {
        try {
            val events = supabase.from("events").select {
                filter {
                    eq("category", category)
                }
            }.decodeList<EventDto>()
            emit(events.map { it.toEvent() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            // First, increment the participant count
            supabase.from("events")
                .update(mapOf("current_participants" to "current_participants + 1")) {
                    filter {
                        eq("id", eventId)
                    }
                }
            
            // Then add the user to the event_participants table
            supabase.from("event_participants")
                .insert(mapOf("event_id" to eventId, "user_id" to userId))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun leaveEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            // First, decrement the participant count
            supabase.from("events")
                .update(mapOf("current_participants" to "current_participants - 1")) {
                    filter {
                        eq("id", eventId)
                    }
                }
            
            // Then remove the user from the event_participants table
            supabase.from("event_participants").delete {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun EventDto.toEvent(): Event {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val parsedDate = try {
        dateFormat.parse(date) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    
    return Event(
        id = id,
        title = title,
        description = description,
        location = location,
        latitude = latitude,
        longitude = longitude,
        dateTime = parsedDate,
        organizerId = organizer_id,
        organizerName = organizer_name,
        maxParticipants = max_participants ?: 0,
        currentParticipants = current_participants,
        category = EventCategory.valueOf(category),
        imageUrl = image_url ?: ""
    )
}

private fun Event.toEventDto(): EventDto {
    return EventDto(
        id = id,
        title = title,
        description = description,
        date = dateTime.toString(),
        location = location,
        latitude = latitude,
        longitude = longitude,
        organizer_id = organizerId,
        organizer_name = organizerName,
        image_url = imageUrl,
        max_participants = maxParticipants,
        current_participants = currentParticipants,
        category = category.name,
        created_at = "" // This will be handled by Supabase
    )
}