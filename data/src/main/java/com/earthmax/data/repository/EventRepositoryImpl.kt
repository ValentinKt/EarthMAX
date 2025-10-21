package com.earthmax.data.repository

import com.earthmax.core.cache.CacheManager
import com.earthmax.core.error.ErrorHandler
import com.earthmax.core.utils.Logger
import com.earthmax.data.events.SupabaseEventsRepository
import com.earthmax.data.local.dao.EventDao
import com.earthmax.data.local.entities.toEntity
import com.earthmax.data.local.entities.toEvent
import com.earthmax.data.mappers.toDomainEvent
import com.earthmax.data.mappers.toEvent
import com.earthmax.domain.model.DomainEvent
import com.earthmax.domain.model.EventCategory
import com.earthmax.domain.model.EventStatus
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.minutes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val supabaseEventsRepository: SupabaseEventsRepository,
    private val cacheManager: CacheManager,
    private val errorHandler: ErrorHandler
) : EventRepository {

    companion object {
        private const val TAG = "EventRepositoryImpl"
        private val CACHE_TTL_MINUTES = 15.minutes
        private const val EVENTS_CACHE_KEY = "events_list"
        private const val EVENT_CACHE_PREFIX = "event_"
    }

    override fun getEvents(
        category: EventCategory?,
        status: EventStatus?,
        location: String?
    ): Flow<Result<List<DomainEvent>>> = flow {
        Logger.enter(TAG, "getEvents", 
            "category" to category?.name,
            "status" to status?.name,
            "location" to location
        )
        
        try {
            emit(Result.Loading)
            
            // Try cache first
            val cacheKey = buildCacheKey(category, status, location)
            val cachedEvents = cacheManager.get<List<DomainEvent>>(cacheKey)
            if (cachedEvents != null) {
                Logger.d(TAG, "Returning cached events")
                emit(Result.Success(cachedEvents))
                return@flow
            }

            // Fetch from local database
            val localEvents = when {
                category != null -> eventDao.getEventsByCategorySync(
                    com.earthmax.core.models.EventCategory.valueOf(category.name)
                ).map { it.toEvent().toDomainEvent() }
                else -> eventDao.getAllEventsSync().map { it.toEvent().toDomainEvent() }
            }

            // Apply additional filters
            val filteredEvents = localEvents.filter { event ->
                (status == null || event.status == status) &&
                (location == null || event.location.contains(location, ignoreCase = true))
            }

            // Cache the result
            cacheManager.put(cacheKey, filteredEvents, CACHE_TTL_MINUTES)

            Logger.logBusinessEvent(TAG, "Events Retrieved", mapOf(
                "count" to filteredEvents.size.toString(),
                "source" to "local_database",
                "category" to (category?.name ?: "all"),
                "status" to (status?.name ?: "all")
            ))

            emit(Result.Success(filteredEvents))
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting events", e)
            val handledException = errorHandler.handleError(e)
            emit(Result.Error(handledException))
        }
    }.catch { e ->
        Logger.e(TAG, "Flow error in getEvents", e)
        val handledException = errorHandler.handleError(e)
        emit(Result.Error(handledException))
    }

    override suspend fun getEventById(eventId: String): Result<DomainEvent> {
        Logger.enter(TAG, "getEventById", "eventId" to Logger.maskSensitiveData(eventId))
        
        return try {
            // Try cache first
            val cacheKey = "$EVENT_CACHE_PREFIX$eventId"
            val cachedEvent = cacheManager.get<DomainEvent>(cacheKey)
            if (cachedEvent != null) {
                Logger.d(TAG, "Returning cached event")
                return Result.Success(cachedEvent)
            }

            // Try local database
            val localEvent = eventDao.getEventById(eventId)
            if (localEvent != null) {
                val domainEvent = localEvent.toEvent().toDomainEvent()
                cacheManager.put(cacheKey, domainEvent, CACHE_TTL_MINUTES)
                return Result.Success(domainEvent)
            }

            // Fallback to remote
            val remoteResult = supabaseEventsRepository.getEventById(eventId)
            if (remoteResult.isSuccess) {
                val event = remoteResult.getOrNull()!!
                val domainEvent = event.toDomainEvent()
                
                // Cache locally
                eventDao.insertEvent(event.toEntity())
                cacheManager.put(cacheKey, domainEvent, CACHE_TTL_MINUTES)
                
                Logger.logBusinessEvent(TAG, "Event Retrieved From Remote", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "cached" to "true"
                ))
                
                Result.Success(domainEvent)
            } else {
                Result.Error(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting event by ID", e)
            val handledException = errorHandler.handleError(e)
            Result.Error(handledException)
        }
    }

    override suspend fun createEvent(event: DomainEvent): Result<DomainEvent> {
        Logger.enter(TAG, "createEvent", 
            "title" to Logger.maskSensitiveData(event.title),
            "category" to event.category.name
        )
        
        return try {
            val coreEvent = event.toEvent()
            val result = supabaseEventsRepository.createEvent(coreEvent)
            
            if (result.isSuccess) {
                val createdEvent = result.getOrNull()!!
                
                // Cache locally
                eventDao.insertEvent(createdEvent.toEntity())
                
                // Invalidate relevant caches
                invalidateEventsCache()
                
                Logger.logBusinessEvent(TAG, "Event Created", mapOf(
                    "eventId" to Logger.maskSensitiveData(createdEvent.id),
                    "title" to Logger.maskSensitiveData(createdEvent.title),
                    "category" to createdEvent.category.name
                ))
                
                Result.Success(createdEvent.toDomainEvent())
            } else {
                val error = result.exceptionOrNull() ?: Exception("Failed to create event")
                val handledException = errorHandler.handleError(error)
                Result.Error(handledException)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error creating event", e)
            val handledException = errorHandler.handleError(e)
            Result.Error(handledException)
        }
    }

    override suspend fun updateEvent(event: DomainEvent): Result<DomainEvent> {
        Logger.enter(TAG, "updateEvent", 
            "eventId" to Logger.maskSensitiveData(event.id),
            "title" to Logger.maskSensitiveData(event.title)
        )
        
        return try {
            val coreEvent = event.toEvent()
            val result = supabaseEventsRepository.updateEvent(coreEvent)
            
            if (result.isSuccess) {
                // Update local cache
                eventDao.updateEvent(coreEvent.toEntity())
                
                // Invalidate relevant caches
                invalidateEventsCache()
                cacheManager.remove("$EVENT_CACHE_PREFIX${event.id}")
                
                Logger.logBusinessEvent(TAG, "Event Updated", mapOf(
                    "eventId" to Logger.maskSensitiveData(event.id),
                    "title" to Logger.maskSensitiveData(event.title)
                ))
                
                Result.Success(event)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Failed to update event")
                val handledException = errorHandler.handleError(error)
                Result.Error(handledException)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating event", e)
            val handledException = errorHandler.handleError(e)
            Result.Error(handledException)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        Logger.enter(TAG, "deleteEvent", "eventId" to Logger.maskSensitiveData(eventId))
        
        return try {
            val result = supabaseEventsRepository.deleteEvent(eventId)
            
            if (result.isSuccess) {
                // Remove from local cache
                eventDao.deleteEventById(eventId)
                
                // Invalidate relevant caches
                invalidateEventsCache()
                cacheManager.remove("$EVENT_CACHE_PREFIX$eventId")
                
                Logger.logBusinessEvent(TAG, "Event Deleted", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId)
                ))
                
                Result.Success(Unit)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Failed to delete event")
                val handledException = errorHandler.handleError(error)
                Result.Error(handledException)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error deleting event", e)
            val handledException = errorHandler.handleError(e)
            Result.Error(handledException)
        }
    }

    override suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        Logger.enter(TAG, "joinEvent", 
            "eventId" to Logger.maskSensitiveData(eventId),
            "userId" to Logger.maskSensitiveData(userId)
        )
        
        return try {
            val result = supabaseEventsRepository.joinEvent(eventId, userId)
            
            if (result.isSuccess) {
                // Invalidate relevant caches
                invalidateEventsCache()
                cacheManager.remove("$EVENT_CACHE_PREFIX$eventId")
                
                Logger.logBusinessEvent(TAG, "Event Joined", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId)
                ))
                
                Result.Success(Unit)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Failed to join event")
                val handledException = errorHandler.handleError(error)
                Result.Error(handledException)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error joining event", e)
            val handledException = errorHandler.handleError(e)
            Result.Error(handledException)
        }
    }

    override suspend fun leaveEvent(eventId: String, userId: String): Result<Unit> {
        Logger.enter(TAG, "leaveEvent", 
            "eventId" to Logger.maskSensitiveData(eventId),
            "userId" to Logger.maskSensitiveData(userId)
        )
        
        return try {
            val result = supabaseEventsRepository.leaveEvent(eventId, userId)
            
            if (result.isSuccess) {
                // Invalidate relevant caches
                invalidateEventsCache()
                cacheManager.remove("$EVENT_CACHE_PREFIX$eventId")
                
                Logger.logBusinessEvent(TAG, "Event Left", mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "userId" to Logger.maskSensitiveData(userId)
                ))
                
                Result.Success(Unit)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Failed to leave event")
                val handledException = errorHandler.handleError(error)
                Result.Error(handledException)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error leaving event", e)
            val handledException = errorHandler.handleError(e)
            Result.Error(handledException)
        }
    }

    override suspend fun searchEvents(query: String): Result<List<DomainEvent>> {
        Logger.enter(TAG, "searchEvents", "query" to Logger.maskSensitiveData(query))
        
        return try {
            val events = eventDao.searchEventsSync(query)
                .map { it.toEvent().toDomainEvent() }
            
            Logger.logBusinessEvent(TAG, "Events Searched", mapOf(
                "query" to Logger.maskSensitiveData(query),
                "resultCount" to events.size.toString()
            ))
            
            Result.Success(events)
        } catch (e: Exception) {
            Logger.e(TAG, "Error searching events", e)
            val handledException = errorHandler.handleError(e)
            Result.Error(handledException)
        }
    }

    override fun getEventsByUser(userId: String): Flow<Result<List<DomainEvent>>> = flow {
        Logger.enter(TAG, "getEventsByUser", "userId" to Logger.maskSensitiveData(userId))
        
        try {
            emit(Result.Loading)
            
            eventDao.getEventsByOrganizer(userId).collect { eventEntities ->
                val events = eventEntities.map { it.toEvent().toDomainEvent() }
                
                Logger.logBusinessEvent(TAG, "Events By User Retrieved", mapOf(
                    "userId" to Logger.maskSensitiveData(userId),
                    "eventCount" to events.size.toString()
                ))
                
                emit(Result.Success(events))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting events by user", e)
            emit(Result.Error(e))
        }
    }.catch { e ->
        Logger.e(TAG, "Flow error in getEventsByUser", e)
        emit(Result.Error(e))
    }

    override fun getJoinedEvents(userId: String): Flow<Result<List<DomainEvent>>> = flow {
        Logger.enter(TAG, "getJoinedEvents", "userId" to Logger.maskSensitiveData(userId))
        
        try {
            emit(Result.Loading)
            
            eventDao.getJoinedEvents().collect { eventEntities ->
                val events = eventEntities.map { it.toEvent().toDomainEvent() }
                
                Logger.logBusinessEvent(TAG, "Joined Events Retrieved", mapOf(
                    "userId" to Logger.maskSensitiveData(userId),
                    "eventCount" to events.size.toString()
                ))
                
                emit(Result.Success(events))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting joined events", e)
            emit(Result.Error(e))
        }
    }.catch { e ->
        Logger.e(TAG, "Flow error in getJoinedEvents", e)
        emit(Result.Error(e))
    }

    private fun buildCacheKey(
        category: EventCategory?,
        status: EventStatus?,
        location: String?
    ): String {
        return "${EVENTS_CACHE_KEY}_${category?.name ?: "all"}_${status?.name ?: "all"}_${location?.hashCode() ?: "all"}"
    }

    private suspend fun invalidateEventsCache() {
        // Remove all events-related cache entries
        cacheManager.clear() // For simplicity, clear all cache
        // In a more sophisticated implementation, we could track and remove specific keys
    }
}