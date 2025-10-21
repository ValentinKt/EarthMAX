package com.earthmax.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.utils.Logger
import com.earthmax.data.local.dao.EventDao
import com.earthmax.data.local.entities.toEvent
import com.earthmax.data.local.entities.EventEntity
import com.earthmax.data.local.entities.toEntity
import com.earthmax.data.events.SupabaseEventsRepository
import com.earthmax.data.mappers.toDomainEvent
import com.earthmax.domain.model.DomainEvent
import com.earthmax.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventDataRepository @Inject constructor(
    private val eventDao: EventDao,
    private val supabaseEventsRepository: SupabaseEventsRepository
) : com.earthmax.domain.repository.EventRepository {
    
    companion object {
        private const val PAGE_SIZE = 20
        private const val TAG = "EventDataRepository"
    }
    
    @OptIn(ExperimentalPagingApi::class)
    fun getAllEvents(): Flow<PagingData<Event>> {
        Logger.enter(TAG, "getAllEvents")
        val startTime = System.currentTimeMillis()
        
        return try {
            val pager = Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { eventDao.getAllEvents() }
            ).flow.map { pagingData ->
                pagingData.map { it.toEvent() }
            }
            
            Logger.logBusinessEvent(TAG, "Events Paging Initialized", mapOf(
                "pageSize" to PAGE_SIZE.toString(),
                "source" to "local_database"
            ))
            Logger.logPerformance(TAG, "getAllEvents", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getAllEvents")
            pager
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing events paging", e)
            Logger.logBusinessEvent(TAG, "Events Paging Error", mapOf(
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getAllEvents_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getAllEvents")
            throw e
        }
    }
    
    @OptIn(ExperimentalPagingApi::class)
    fun getEventsByCategory(category: EventCategory): Flow<PagingData<Event>> {
        Logger.enter(TAG, "getEventsByCategory", "category" to category.name)
        val startTime = System.currentTimeMillis()
        
        return try {
            val pager = Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { eventDao.getEventsByCategory(category) }
            ).flow.map { pagingData ->
                pagingData.map { it.toEvent() }
            }
            
            Logger.logBusinessEvent(TAG, "Events By Category Paging Initialized", mapOf(
                "category" to category.name,
                "pageSize" to PAGE_SIZE.toString(),
                "source" to "local_database"
            ))
            Logger.logPerformance(TAG, "getEventsByCategory", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventsByCategory")
            pager
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing events by category paging", e)
            Logger.logBusinessEvent(TAG, "Events By Category Paging Error", mapOf(
                "category" to category.name,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getEventsByCategory_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventsByCategory")
            throw e
        }
    }
    
    fun getJoinedEvents(): Flow<List<Event>> {
        Logger.enter(TAG, "getJoinedEvents")
        val startTime = System.currentTimeMillis()
        
        return try {
            val flow = eventDao.getJoinedEvents().map { entities ->
                entities.map { it.toEvent() }
            }
            
            Logger.logBusinessEvent(TAG, "Joined Events Flow Initialized", mapOf(
                "source" to "local_database"
            ))
            Logger.logPerformance(TAG, "getJoinedEvents", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getJoinedEvents")
            flow
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing joined events flow", e)
            Logger.logBusinessEvent(TAG, "Joined Events Flow Error", mapOf(
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getJoinedEvents_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getJoinedEvents")
            throw e
        }
    }
    
    fun getEventsByOrganizer(organizerId: String): Flow<List<Event>> {
        Logger.enter(TAG, "getEventsByOrganizer", "organizerId" to Logger.maskSensitiveData(organizerId))
        val startTime = System.currentTimeMillis()
        
        return try {
            val flow = eventDao.getEventsByOrganizer(organizerId).map { entities ->
                entities.map { it.toEvent() }
            }
            
            Logger.logBusinessEvent(TAG, "Events By Organizer Flow Initialized", mapOf(
                "organizerId" to Logger.maskSensitiveData(organizerId),
                "source" to "local_database"
            ))
            Logger.logPerformance(TAG, "getEventsByOrganizer", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventsByOrganizer")
            flow
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing events by organizer flow", e)
            Logger.logBusinessEvent(TAG, "Events By Organizer Flow Error", mapOf(
                "organizerId" to Logger.maskSensitiveData(organizerId),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getEventsByOrganizer_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventsByOrganizer")
            throw e
        }
    }
    
    override suspend fun getEventById(eventId: String): Result<DomainEvent> {
        Logger.enter(TAG, "getEventById", "eventId" to Logger.maskSensitiveData(eventId))
        val startTime = System.currentTimeMillis()
        
        return try {
            // First try to get from local database
            Logger.d(TAG, "Attempting to retrieve event from local database")
            val localEvent = eventDao.getEventById(eventId)
            if (localEvent != null) {
                Logger.i(TAG, "Event found in local database")
                Logger.logBusinessEvent(TAG, "Event Retrieved From Local", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "source" to "local_database"
                ))
                Logger.logPerformance(TAG, "getEventById_local", System.currentTimeMillis() - startTime)
                val domainEvent = localEvent.toEvent().toDomainEvent()
                Logger.exit(TAG, "getEventById", domainEvent)
                return Result.Success(domainEvent)
            }
            
            // Fallback to remote
            Logger.d(TAG, "Event not found locally, fetching from remote")
            val result = supabaseEventsRepository.getEventById(eventId)
            if (result.isSuccess) {
                val event = result.getOrNull()!!
                // Cache the result locally
                Logger.d(TAG, "Caching event locally")
                eventDao.insertEvent((event as Event).toEntity())
                Logger.i(TAG, "Event retrieved from remote and cached locally")
                Logger.logBusinessEvent(TAG, "Event Retrieved From Remote", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "source" to "remote_supabase",
                    "cached" to "true"
                ))
                Logger.logPerformance(TAG, "getEventById_remote", System.currentTimeMillis() - startTime)
                val domainEvent = (event as Event).toDomainEvent()
                Logger.exit(TAG, "getEventById", domainEvent)
                Result.Success(domainEvent)
            } else {
                Logger.w(TAG, "Event not found in remote either")
                Logger.logBusinessEvent(TAG, "Event Not Found", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "searchedLocal" to "true",
                    "searchedRemote" to "true"
                ))
                Logger.logPerformance(TAG, "getEventById_not_found", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "getEventById", null)
                Result.Error(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error retrieving event by ID", e)
            Logger.logBusinessEvent(TAG, "Event Retrieval Error", mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getEventById_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventById", null)
            Result.Error(e)
        }
    }
    
    fun getEventByIdFlow(eventId: String): Flow<Event?> {
        Logger.enter(TAG, "getEventByIdFlow", "eventId" to Logger.maskSensitiveData(eventId))
        val startTime = System.currentTimeMillis()
        
        return try {
            val flow = eventDao.getEventByIdFlow(eventId).map { entity ->
                entity?.toEvent()
            }
            
            Logger.logBusinessEvent(TAG, "Event Flow Initialized", mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "source" to "local_database"
            ))
            Logger.logPerformance(TAG, "getEventByIdFlow", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventByIdFlow")
            flow
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing event flow", e)
            Logger.logBusinessEvent(TAG, "Event Flow Error", mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getEventByIdFlow_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventByIdFlow")
            throw e
        }
    }
    
    override suspend fun searchEvents(query: String): Result<List<DomainEvent>> {
        Logger.enter(TAG, "searchEvents", "query" to Logger.maskSensitiveData(query))
        val startTime = System.currentTimeMillis()
        
        return try {
            // Get events directly from DAO instead of using Pager
            val events = eventDao.searchEventsSync(query)
            val domainEvents = events.map { it.toEvent().toDomainEvent() }
            
            Logger.logBusinessEvent(TAG, "Event Search Completed", mapOf(
                "query" to Logger.maskSensitiveData(query),
                "resultCount" to domainEvents.size.toString(),
                "source" to "local_database"
            ))
            Logger.logPerformance(TAG, "searchEvents", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "searchEvents", domainEvents)
            Result.Success(domainEvents)
        } catch (e: Exception) {
            Logger.e(TAG, "Error searching events", e)
            Logger.logBusinessEvent(TAG, "Event Search Error", mapOf(
                "query" to Logger.maskSensitiveData(query),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "searchEvents_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "searchEvents")
            Result.Error(e)
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun searchEventsPaged(query: String): Flow<PagingData<Event>> {
        Logger.enter(TAG, "searchEventsPaged", "query" to Logger.maskSensitiveData(query))
        val startTime = System.currentTimeMillis()

        return try {
            val pager = Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { eventDao.searchEvents(query) }
            ).flow.map { pagingData ->
                pagingData.map { it.toEvent() }
            }

            Logger.logBusinessEvent(TAG, "Events Search Paging Initialized", mapOf(
                "query" to Logger.maskSensitiveData(query),
                "pageSize" to PAGE_SIZE.toString(),
                "source" to "local_database"
            ))
            Logger.logPerformance(TAG, "searchEventsPaged", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "searchEventsPaged")
            pager
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing search events paging", e)
            Logger.logBusinessEvent(TAG, "Events Search Paging Error", mapOf(
                "query" to Logger.maskSensitiveData(query),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "searchEventsPaged_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "searchEventsPaged")
            throw e
        }
    }
    
    fun getEventsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<Event>> {
        Logger.enter(TAG, "getEventsInBounds", 
            "minLat" to minLat.toString(),
            "maxLat" to maxLat.toString(),
            "minLng" to minLng.toString(),
            "maxLng" to maxLng.toString()
        )
        val startTime = System.currentTimeMillis()
        
        return try {
            val flow = eventDao.getEventsInBounds(minLat, maxLat, minLng, maxLng)
                .map { entities -> entities.map { it.toEvent() } }
            
            Logger.logBusinessEvent(TAG, "Events In Bounds Flow Initialized", mapOf(
                "minLat" to minLat.toString(),
                "maxLat" to maxLat.toString(),
                "minLng" to minLng.toString(),
                "maxLng" to maxLng.toString(),
                "source" to "local_database"
            ))
            Logger.logPerformance(TAG, "getEventsInBounds", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventsInBounds")
            flow
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing events in bounds flow", e)
            Logger.logBusinessEvent(TAG, "Events In Bounds Flow Error", mapOf(
                "minLat" to minLat.toString(),
                "maxLat" to maxLat.toString(),
                "minLng" to minLng.toString(),
                "maxLng" to maxLng.toString(),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "getEventsInBounds_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "getEventsInBounds")
            throw e
        }
    }
    
    suspend fun createEvent(event: Event): Result<String> {
        Logger.enter(TAG, "createEvent", 
            "eventId" to Logger.maskSensitiveData(event.id),
            "title" to Logger.maskSensitiveData(event.title),
            "category" to event.category.name
        )
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Creating event via Supabase")
            val result = supabaseEventsRepository.createEvent(event)
            if (result.isSuccess) {
                val createdEvent = result.getOrNull()!!
                Logger.d(TAG, "Event created successfully, caching locally")
                eventDao.insertEvent((createdEvent as Event).toEntity())
                Logger.i(TAG, "Event created and cached successfully")
                Logger.logBusinessEvent(TAG, "Event Created", mapOf(
                    "eventId" to Logger.maskSensitiveData(createdEvent.id),
                    "title" to Logger.maskSensitiveData(createdEvent.title),
                    "category" to createdEvent.category.name,
                    "organizerId" to Logger.maskSensitiveData(createdEvent.organizerId),
                    "cached" to "true"
                ))
                Logger.logPerformance(TAG, "createEvent", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "createEvent", Result.Success(createdEvent.id))
                Result.Success(createdEvent.id)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Failed to create event")
                Logger.w(TAG, "Failed to create event", error)
                Logger.logBusinessEvent(TAG, "Event Creation Failed", mapOf(
                    "eventId" to Logger.maskSensitiveData(event.id),
                    "title" to Logger.maskSensitiveData(event.title),
                    "category" to event.category.name,
                    "errorType" to error.javaClass.simpleName,
                    "errorMessage" to (error.message ?: "Unknown error")
                ))
                Logger.logPerformance(TAG, "createEvent_failed", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "createEvent", Result.Error(error))
                Result.Error(error)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error creating event", e)
            Logger.logBusinessEvent(TAG, "Event Creation Error", mapOf(
                "eventId" to Logger.maskSensitiveData(event.id),
                "title" to Logger.maskSensitiveData(event.title),
                "category" to event.category.name,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "createEvent_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "createEvent", Result.Error(e))
            Result.Error(e)
        }
    }
    
    suspend fun updateEvent(event: Event): Result<Unit> {
        Logger.enter(TAG, "updateEvent", 
            "eventId" to Logger.maskSensitiveData(event.id),
            "title" to Logger.maskSensitiveData(event.title),
            "category" to event.category.name
        )
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Updating event via Supabase")
            val result = supabaseEventsRepository.updateEvent(event)
            if (result.isSuccess) {
                Logger.d(TAG, "Event updated successfully, updating local cache")
                eventDao.updateEvent(event.toEntity())
                Logger.i(TAG, "Event updated and cached successfully")
                Logger.logBusinessEvent(TAG, "Event Updated", mapOf(
                    "eventId" to Logger.maskSensitiveData(event.id),
                    "title" to Logger.maskSensitiveData(event.title),
                    "category" to event.category.name,
                    "organizerId" to Logger.maskSensitiveData(event.organizerId),
                    "cached" to "true"
                ))
                Logger.logPerformance(TAG, "updateEvent", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateEvent", Result.Success(Unit))
                Result.Success(Unit)
            } else {
                val error = Exception("Failed to update event")
                Logger.w(TAG, "Failed to update event", error)
                Logger.logBusinessEvent(TAG, "Event Update Failed", mapOf(
                    "eventId" to Logger.maskSensitiveData(event.id),
                    "title" to Logger.maskSensitiveData(event.title),
                    "category" to event.category.name,
                    "errorType" to error.javaClass.simpleName,
                    "errorMessage" to (error.message ?: "Unknown error")
                ))
                Logger.logPerformance(TAG, "updateEvent_failed", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "updateEvent", Result.Error(error))
                Result.Error(error)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating event", e)
            Logger.logBusinessEvent(TAG, "Event Update Error", mapOf(
                "eventId" to Logger.maskSensitiveData(event.id),
                "title" to Logger.maskSensitiveData(event.title),
                "category" to event.category.name,
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "updateEvent_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "updateEvent", Result.Error(e))
            Result.Error(e)
        }
    }
    
    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        Logger.enter(TAG, "deleteEvent", "eventId" to Logger.maskSensitiveData(eventId))
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Deleting event via Supabase")
            val result = supabaseEventsRepository.deleteEvent(eventId)
            if (result.isSuccess) {
                Logger.d(TAG, "Event deleted successfully, removing from local cache")
                eventDao.deleteEventById(eventId)
                Logger.i(TAG, "Event deleted and removed from cache successfully")
                Logger.logBusinessEvent(TAG, "Event Deleted", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "removedFromCache" to "true"
                ))
                Logger.logPerformance(TAG, "deleteEvent", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "deleteEvent", Result.Success(Unit))
                Result.Success(Unit)
            } else {
                val error = Exception("Failed to delete event")
                Logger.w(TAG, "Failed to delete event", error)
                Logger.logBusinessEvent(TAG, "Event Deletion Failed", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "errorType" to error.javaClass.simpleName,
                    "errorMessage" to (error.message ?: "Unknown error")
                ))
                Logger.logPerformance(TAG, "deleteEvent_failed", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "deleteEvent", Result.Error(error))
                Result.Error(error)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error deleting event", e)
            Logger.logBusinessEvent(TAG, "Event Deletion Error", mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "deleteEvent_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "deleteEvent", Result.Error(e))
            Result.Error(e)
        }
    }
    
    override suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        Logger.enter(TAG, "joinEvent", 
            "eventId" to Logger.maskSensitiveData(eventId),
            "userId" to Logger.maskSensitiveData(userId)
        )
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Joining event via Supabase")
            val result = supabaseEventsRepository.joinEvent(eventId, userId)
            if (result.isSuccess) {
                Logger.d(TAG, "Event joined successfully, updating local cache")
                eventDao.updateEventJoinStatus(eventId, true)
                // Update participant count
                val event = eventDao.getEventById(eventId)
                if (event != null) {
                    val newParticipantCount = event.currentParticipants + 1
                    eventDao.updateEventParticipants(eventId, newParticipantCount)
                    Logger.d(TAG, "Updated participant count to $newParticipantCount")
                }
                Logger.i(TAG, "Event joined and cache updated successfully")
                Logger.logBusinessEvent(TAG, "Event Joined", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "participantCountUpdated" to (event != null).toString()
                ))
                Logger.logPerformance(TAG, "joinEvent", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "joinEvent", Result.Success(Unit))
                Result.Success(Unit)
            } else {
                val error = Exception("Failed to join event")
                Logger.w(TAG, "Failed to join event", error)
                Logger.logBusinessEvent(TAG, "Event Join Failed", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "errorType" to error.javaClass.simpleName,
                    "errorMessage" to (error.message ?: "Unknown error")
                ))
                Logger.logPerformance(TAG, "joinEvent_failed", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "joinEvent", Result.Error(error))
                Result.Error(error)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error joining event", e)
            Logger.logBusinessEvent(TAG, "Event Join Error", mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "userId" to Logger.maskSensitiveData(userId),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "joinEvent_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "joinEvent", Result.Error(e))
            Result.Error(e)
        }
    }
    
    override suspend fun leaveEvent(eventId: String, userId: String): Result<Unit> {
        Logger.enter(TAG, "leaveEvent", 
            "eventId" to Logger.maskSensitiveData(eventId),
            "userId" to Logger.maskSensitiveData(userId)
        )
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Leaving event via Supabase")
            val result = supabaseEventsRepository.leaveEvent(eventId, userId)
            if (result.isSuccess) {
                Logger.d(TAG, "Event left successfully, updating local cache")
                eventDao.updateEventJoinStatus(eventId, false)
                // Update participant count
                val event = eventDao.getEventById(eventId)
                if (event != null) {
                    val newParticipantCount = maxOf(0, event.currentParticipants - 1)
                    eventDao.updateEventParticipants(eventId, newParticipantCount)
                    Logger.d(TAG, "Updated participant count to $newParticipantCount")
                }
                Logger.i(TAG, "Event left and cache updated successfully")
                Logger.logBusinessEvent(TAG, "Event Left", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "participantCountUpdated" to (event != null).toString()
                ))
                Logger.logPerformance(TAG, "leaveEvent", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "leaveEvent", Result.Success(Unit))
                Result.Success(Unit)
            } else {
                val error = Exception("Failed to leave event")
                Logger.w(TAG, "Failed to leave event", error)
                Logger.logBusinessEvent(TAG, "Event Leave Failed", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId),
                    "errorType" to error.javaClass.simpleName,
                    "errorMessage" to (error.message ?: "Unknown error")
                ))
                Logger.logPerformance(TAG, "leaveEvent_failed", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "leaveEvent", Result.Error(error))
                Result.Error(error)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error leaving event", e)
            Logger.logBusinessEvent(TAG, "Event Leave Error", mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "userId" to Logger.maskSensitiveData(userId),
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "leaveEvent_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "leaveEvent", Result.Error(e))
            Result.Error(e)
        }
    }
    
    suspend fun uploadEventPhoto(eventId: String, imageData: ByteArray): Result<String> {
        Logger.enter(TAG, "uploadEventPhoto", 
            "eventId" to Logger.maskSensitiveData(eventId),
            "imageSize" to "${imageData.size} bytes"
        )
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d(TAG, "Uploading event photo via Supabase")
            val result = supabaseEventsRepository.uploadEventImage(eventId, imageData)
            if (result.isSuccess) {
                val imageUrl = result.getOrNull()!!
                Logger.i(TAG, "Event photo uploaded successfully")
                Logger.logBusinessEvent(TAG, "Event Photo Uploaded", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "imageSize" to "${imageData.size} bytes",
                    "imageUrl" to Logger.maskSensitiveData(imageUrl)
                ))
                Logger.logPerformance(TAG, "uploadEventPhoto", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "uploadEventPhoto", Result.Success(imageUrl))
                Result.Success(imageUrl)
            } else {
                val error = Exception("Failed to upload event image")
                Logger.w(TAG, "Failed to upload event photo", error)
                Logger.logBusinessEvent(TAG, "Event Photo Upload Failed", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "imageSize" to "${imageData.size} bytes",
                    "errorType" to error.javaClass.simpleName,
                    "errorMessage" to (error.message ?: "Unknown error")
                ))
                Logger.logPerformance(TAG, "uploadEventPhoto_failed", System.currentTimeMillis() - startTime)
                Logger.exit(TAG, "uploadEventPhoto", Result.Error(error))
                Result.Error(error)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error uploading event photo", e)
            Logger.logBusinessEvent(TAG, "Event Photo Upload Error", mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "imageSize" to "${imageData.size} bytes",
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "uploadEventPhoto_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "uploadEventPhoto", Result.Error(e))
            Result.Error(e)
        }
    }
    
    suspend fun refreshEvents() {
        Logger.enter(TAG, "refreshEvents")
        val startTime = System.currentTimeMillis()
        
        try {
            Logger.d(TAG, "Refreshing events from remote source")
            // This would typically involve fetching from remote and updating local cache
            // For now, we'll rely on Supabase real-time updates
            Logger.i(TAG, "Events refresh completed (using real-time updates)")
            Logger.logBusinessEvent(TAG, "Events Refreshed", mapOf(
                "source" to "supabase_realtime"
            ))
            Logger.logPerformance(TAG, "refreshEvents", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "refreshEvents")
        } catch (e: Exception) {
            Logger.e(TAG, "Error refreshing events", e)
            Logger.logBusinessEvent(TAG, "Events Refresh Error", mapOf(
                "errorType" to e.javaClass.simpleName,
                "errorMessage" to (e.message ?: "Unknown error")
            ))
            Logger.logPerformance(TAG, "refreshEvents_error", System.currentTimeMillis() - startTime)
            Logger.exit(TAG, "refreshEvents")
        }
    }

    // Missing abstract method implementations from EventRepository interface
    
    override fun getEvents(
        category: com.earthmax.domain.model.EventCategory?,
        status: com.earthmax.domain.model.EventStatus?,
        location: String?
    ): Flow<Result<List<DomainEvent>>> {
        Logger.enter(TAG, "getEvents")
        return kotlinx.coroutines.flow.flow {
            try {
                val events = eventDao.getAllEventsSync()
                val domainEvents = events.map { it.toEvent().toDomainEvent() }
                emit(Result.Success(domainEvents))
            } catch (e: Exception) {
                Logger.logError(TAG, "Failed to get events", e)
                emit(Result.Error(e))
            }
        }
    }

    override suspend fun createEvent(event: DomainEvent): Result<DomainEvent> {
        Logger.enter(TAG, "createEvent")
        return try {
            val coreEvent = Event(
                id = event.id,
                title = event.title,
                description = event.description,
                location = event.location,
                latitude = event.latitude ?: 0.0,
                longitude = event.longitude ?: 0.0,
                dateTime = java.util.Date(event.startDate.toEpochMilliseconds()),
                organizerId = event.organizerId,
                organizerName = "", // Will be populated if needed
                maxParticipants = event.maxParticipants ?: 0,
                currentParticipants = event.participantCount,
                category = EventCategory.valueOf(event.category.name),
                imageUrl = event.imageUrl ?: "",
                isJoined = false,
                todoItems = emptyList(),
                photos = emptyList(),
                createdAt = java.util.Date(event.createdAt.toEpochMilliseconds()),
                updatedAt = java.util.Date(event.updatedAt.toEpochMilliseconds())
            )
            val result = createEvent(coreEvent)
            when (result) {
                is Result.Success -> Result.Success(event.copy(id = result.data))
                is Result.Error -> Result.Error(result.exception)
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to create event", e)
            Result.Error(e)
        }
    }

    override suspend fun updateEvent(event: DomainEvent): Result<DomainEvent> {
        Logger.enter(TAG, "updateEvent")
        return try {
            val coreEvent = Event(
                id = event.id,
                title = event.title,
                description = event.description,
                location = event.location,
                latitude = event.latitude ?: 0.0,
                longitude = event.longitude ?: 0.0,
                dateTime = java.util.Date(event.startDate.toEpochMilliseconds()),
                organizerId = event.organizerId,
                organizerName = "", // Will be populated if needed
                maxParticipants = event.maxParticipants ?: 0,
                currentParticipants = event.participantCount,
                category = EventCategory.valueOf(event.category.name),
                imageUrl = event.imageUrl ?: "",
                isJoined = false,
                todoItems = emptyList(),
                photos = emptyList(),
                createdAt = java.util.Date(event.createdAt.toEpochMilliseconds()),
                updatedAt = java.util.Date(event.updatedAt.toEpochMilliseconds())
            )
            val result = updateEvent(coreEvent)
            when (result) {
                is Result.Success -> Result.Success(event)
                is Result.Error -> Result.Error(result.exception)
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to update event", e)
            Result.Error(e)
        }
    }

    override fun getEventsByUser(userId: String): Flow<Result<List<DomainEvent>>> {
        Logger.enter(TAG, "getEventsByUser")
        return eventDao.getEventsByOrganizer(userId).map { eventEntities ->
            try {
                val domainEvents = eventEntities.map { it.toEvent().toDomainEvent() }
                Result.Success(domainEvents)
            } catch (e: Exception) {
                Logger.logError(TAG, "Failed to get events by user", e)
                Result.Error(e)
            }
        }
    }

    override fun getJoinedEvents(userId: String): Flow<Result<List<DomainEvent>>> {
        Logger.enter(TAG, "getJoinedEvents")
        return kotlinx.coroutines.flow.flow {
            try {
                // This would need to be implemented based on your database schema
                // For now, returning empty list
                emit(Result.Success(emptyList<DomainEvent>()))
            } catch (e: Exception) {
                Logger.logError(TAG, "Failed to get joined events", e)
                emit(Result.Error(e))
            }
        }
    }
}