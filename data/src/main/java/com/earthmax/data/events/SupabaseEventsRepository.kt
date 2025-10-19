package com.earthmax.data.events

import android.net.Uri
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.network.SupabaseClient
import com.earthmax.core.utils.Logger
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
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
    
    companion object {
        private const val TAG = "SupabaseEventsRepository"
    }
    
    fun getEvents(): Flow<List<Event>> = flow {
        Logger.enter(TAG, "getEvents")
        val startTime = System.currentTimeMillis()
        
        try {
            val events = supabase.from("events").select().decodeList<EventDto>()
            val eventList = events.map { it.toEvent() }
            
            Logger.logPerformance(
                TAG,
                "getEvents",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventCount" to eventList.size.toString(),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "events_retrieved",
                mapOf(
                    "eventCount" to eventList.size.toString(),
                    "source" to "supabase"
                )
            )
            
            emit(eventList)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "getEvents",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to get events", e, mapOf(
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            emit(emptyList())
        }
        
        Logger.exit(TAG, "getEvents")
    }
    
    suspend fun getEventById(eventId: String): Result<Event> {
        Logger.enter(TAG, "getEventById", mapOf("eventId" to Logger.maskSensitiveData(eventId)))
        val startTime = System.currentTimeMillis()
        
        return try {
            val event = supabase.from("events").select {
                filter {
                    eq("id", eventId)
                }
            }.decodeSingle<EventDto>()
            
            Logger.logPerformance(
                TAG,
                "getEventById",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "event_retrieved_by_id",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "eventTitle" to event.title,
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "getEventById")
            Result.success(event.toEvent())
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "getEventById",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to get event by ID", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "getEventById")
            Result.failure(e)
        }
    }
    
    suspend fun createEvent(event: Event): Result<Event> {
        Logger.enter(TAG, "createEvent", mapOf(
            "eventTitle" to event.title,
            "eventCategory" to event.category.name,
            "organizerId" to Logger.maskSensitiveData(event.organizerId)
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            val eventDto = event.toEventDto()
            val createdEvent = supabase.from("events")
                .insert(eventDto)
                .decodeSingle<EventDto>()
            
            Logger.logPerformance(
                TAG,
                "createEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(createdEvent.id),
                    "eventTitle" to createdEvent.title,
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "event_created",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(createdEvent.id),
                    "eventTitle" to createdEvent.title,
                    "eventCategory" to createdEvent.category,
                    "organizerId" to Logger.maskSensitiveData(createdEvent.organizer_id),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "createEvent")
            Result.success(createdEvent.toEvent())
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "createEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventTitle" to event.title,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to create event", e, mapOf(
                "eventTitle" to event.title,
                "eventCategory" to event.category.name,
                "organizerId" to Logger.maskSensitiveData(event.organizerId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "createEvent")
            Result.failure(e)
        }
    }
    
    suspend fun updateEvent(event: Event): Result<Event> {
        Logger.enter(TAG, "updateEvent", mapOf(
            "eventId" to Logger.maskSensitiveData(event.id),
            "eventTitle" to event.title,
            "eventCategory" to event.category.name
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            val eventDto = event.toEventDto()
            val updatedEvent = supabase.from("events")
                .update(eventDto) {
                    filter {
                        eq("id", event.id)
                    }
                }
                .decodeSingle<EventDto>()
            
            Logger.logPerformance(
                TAG,
                "updateEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(event.id),
                    "eventTitle" to event.title,
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "event_updated",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(event.id),
                    "eventTitle" to event.title,
                    "eventCategory" to event.category.name,
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "updateEvent")
            Result.success(updatedEvent.toEvent())
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "updateEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(event.id),
                    "eventTitle" to event.title,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to update event", e, mapOf(
                "eventId" to Logger.maskSensitiveData(event.id),
                "eventTitle" to event.title,
                "eventCategory" to event.category.name,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "updateEvent")
            Result.failure(e)
        }
    }
    
    suspend fun deleteEvent(eventId: String): Result<Unit> {
        Logger.enter(TAG, "deleteEvent", mapOf("eventId" to Logger.maskSensitiveData(eventId)))
        val startTime = System.currentTimeMillis()
        
        return try {
            supabase.from("events").delete {
                filter {
                    eq("id", eventId)
                }
            }
            
            Logger.logPerformance(
                TAG,
                "deleteEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "event_deleted",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "deleteEvent")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "deleteEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to delete event", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "deleteEvent")
            Result.failure(e)
        }
    }
    
    fun getEventsByCategory(category: String): Flow<List<Event>> = flow {
        Logger.enter(TAG, "getEventsByCategory", mapOf("category" to category))
        val startTime = System.currentTimeMillis()
        
        try {
            val events = supabase.from("events").select {
                filter {
                    eq("category", category)
                }
            }.decodeList<EventDto>()
            val eventList = events.map { it.toEvent() }
            
            Logger.logPerformance(
                TAG,
                "getEventsByCategory",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "category" to category,
                    "eventCount" to eventList.size.toString(),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "events_retrieved_by_category",
                mapOf(
                    "category" to category,
                    "eventCount" to eventList.size.toString(),
                    "source" to "supabase"
                )
            )
            
            emit(eventList)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "getEventsByCategory",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "category" to category,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to get events by category", e, mapOf(
                "category" to category,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            emit(emptyList())
        }
        
        Logger.exit(TAG, "getEventsByCategory")
    }
    
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        Logger.enter(TAG, "joinEvent", mapOf(
            "eventId" to Logger.maskSensitiveData(eventId),
            "userId" to Logger.maskSensitiveData(userId)
        ))
        val startTime = System.currentTimeMillis()
        
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
            
            Logger.logPerformance(
                TAG,
                "joinEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "event_joined",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "joinEvent")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "joinEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to join event", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "userId" to Logger.maskSensitiveData(userId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "joinEvent")
            Result.failure(e)
        }
    }
    
    suspend fun leaveEvent(eventId: String, userId: String): Result<Unit> {
        Logger.enter(TAG, "leaveEvent", mapOf(
            "eventId" to Logger.maskSensitiveData(eventId),
            "userId" to Logger.maskSensitiveData(userId)
        ))
        val startTime = System.currentTimeMillis()
        
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
            
            Logger.logPerformance(
                TAG,
                "leaveEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "event_left",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "leaveEvent")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "leaveEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to leave event", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "userId" to Logger.maskSensitiveData(userId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "leaveEvent")
            Result.failure(e)
        }
    }
    
    suspend fun uploadEventImage(eventId: String, imageData: ByteArray): Result<String> {
        Logger.enter(TAG, "uploadEventImage", mapOf(
            "eventId" to Logger.maskSensitiveData(eventId),
            "imageSize" to imageData.size.toString()
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            val fileName = "event_${eventId}_${UUID.randomUUID()}.jpg"
            val bucket = supabase.storage.from("event-images")
            
            // Upload the image to Supabase Storage
            bucket.upload(fileName, imageData)
            
            // Get the public URL
            val publicUrl = bucket.publicUrl(fileName)
            
            // Update the event with the image URL
            supabase.from("events")
                .update(mapOf("image_url" to publicUrl)) {
                    filter {
                        eq("id", eventId)
                    }
                }
            
            Logger.logPerformance(
                TAG,
                "uploadEventImage",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "fileName" to fileName,
                    "imageSize" to imageData.size.toString(),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "event_image_uploaded",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "fileName" to fileName,
                    "imageSize" to imageData.size.toString(),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "uploadEventImage")
            Result.success(publicUrl)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "uploadEventImage",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "imageSize" to imageData.size.toString(),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to upload event image", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "imageSize" to imageData.size.toString(),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "uploadEventImage")
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