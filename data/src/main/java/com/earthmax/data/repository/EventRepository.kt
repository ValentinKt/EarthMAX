package com.earthmax.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.data.local.dao.EventDao
import com.earthmax.data.local.entities.toEvent
import com.earthmax.data.local.entities.EventEntity
import com.earthmax.data.local.entities.toEntity
import com.earthmax.data.events.SupabaseEventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val supabaseEventsRepository: SupabaseEventsRepository
) {
    
    companion object {
        private const val PAGE_SIZE = 20
    }
    
    @OptIn(ExperimentalPagingApi::class)
    fun getAllEvents(): Flow<PagingData<Event>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { eventDao.getAllEvents() }
        ).flow.map { pagingData ->
            pagingData.map { it.toEvent() }
        }
    }
    
    @OptIn(ExperimentalPagingApi::class)
    fun getEventsByCategory(category: EventCategory): Flow<PagingData<Event>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { eventDao.getEventsByCategory(category) }
        ).flow.map { pagingData ->
            pagingData.map { it.toEvent() }
        }
    }
    
    fun getJoinedEvents(): Flow<List<Event>> {
        return eventDao.getJoinedEvents().map { entities ->
            entities.map { it.toEvent() }
        }
    }
    
    fun getEventsByOrganizer(organizerId: String): Flow<List<Event>> {
        return eventDao.getEventsByOrganizer(organizerId).map { entities ->
            entities.map { it.toEvent() }
        }
    }
    
    suspend fun getEventById(eventId: String): Event? {
        // First try to get from local database
        val localEvent = eventDao.getEventById(eventId)
        if (localEvent != null) {
            return localEvent.toEvent()
        }
        
        // Fallback to remote
        val result = supabaseEventsRepository.getEventById(eventId)
        return if (result.isSuccess) {
            val event = result.getOrNull()!!
            // Cache the result locally
            eventDao.insertEvent((event as Event).toEntity())
            event
        } else {
            null
        }
    }
    
    fun getEventByIdFlow(eventId: String): Flow<Event?> {
        return eventDao.getEventByIdFlow(eventId).map { it?.toEvent() }
    }
    
    @OptIn(ExperimentalPagingApi::class)
    fun searchEvents(query: String): Flow<PagingData<Event>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { eventDao.searchEvents(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toEvent() }
        }
    }
    
    fun getEventsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<Event>> {
        return eventDao.getEventsInBounds(minLat, maxLat, minLng, maxLng)
            .map { entities -> entities.map { it.toEvent() } }
    }
    
    suspend fun createEvent(event: Event): Result<String> {
        return try {
            val result = supabaseEventsRepository.createEvent(event)
            if (result.isSuccess) {
                val createdEvent = result.getOrNull()!!
                eventDao.insertEvent((createdEvent as Event).toEntity())
                Result.success(createdEvent.id)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to create event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            val result = supabaseEventsRepository.updateEvent(event)
            if (result.isSuccess) {
                eventDao.updateEvent(event.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val result = supabaseEventsRepository.deleteEvent(eventId)
            if (result.isSuccess) {
                eventDao.deleteEventById(eventId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val result = supabaseEventsRepository.joinEvent(eventId, userId)
            if (result.isSuccess) {
                eventDao.updateEventJoinStatus(eventId, true)
                // Update participant count
                val event = eventDao.getEventById(eventId)
                if (event != null) {
                    eventDao.updateEventParticipants(eventId, event.currentParticipants + 1)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to join event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun leaveEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val result = supabaseEventsRepository.leaveEvent(eventId, userId)
            if (result.isSuccess) {
                eventDao.updateEventJoinStatus(eventId, false)
                // Update participant count
                val event = eventDao.getEventById(eventId)
                if (event != null) {
                    eventDao.updateEventParticipants(eventId, maxOf(0, event.currentParticipants - 1))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to leave event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadEventPhoto(eventId: String, photoUri: String): Result<String> {
        // For now, return a placeholder implementation
        // In a full implementation, you would upload to Supabase Storage
        return Result.success(photoUri)
    }
    
    suspend fun refreshEvents() {
        // This would typically involve fetching from remote and updating local cache
        // For now, we'll rely on Supabase real-time updates
    }
}