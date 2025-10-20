package com.earthmax.data.api.repository

import com.earthmax.core.utils.Logger
import com.earthmax.data.api.EventApiService
import com.earthmax.data.api.dto.CreateEventRequest
import com.earthmax.data.api.dto.EventResponse
import com.earthmax.data.api.dto.UpdateEventRequest
import com.earthmax.data.api.dto.UserResponse
import com.earthmax.data.api.validation.ApiValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing event-related API calls
 */
@Singleton
class EventApiRepository @Inject constructor(
    private val eventApiService: EventApiService,
    private val apiValidator: ApiValidator
) {
    
    companion object {
        private const val TAG = "EventApiRepository"
    }
    
    suspend fun createEvent(request: CreateEventRequest): Flow<Result<EventResponse>> = flow {
        Logger.enter(TAG, "createEvent", 
            "title" to request.title
        )
        val startTime = System.currentTimeMillis()

        try {
            // Validate request
            val validation = apiValidator.validateCreateEventRequest(request)
            if (!validation.isValid) {
                Logger.logError(TAG, "Event creation validation failed", null, mapOf(
                    "validationError" to validation.getErrorMessage() as Any,
                    "title" to request.title as Any
                ))
                emit(Result.failure(IllegalArgumentException(validation.getErrorMessage())))
                return@flow
            }
            
            val response = eventApiService.createEvent(request)
            if (response.isSuccessful) {
                response.body()?.let { eventResponse ->
                    Logger.logPerformance(
                        TAG,
                        "createEvent",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventId" to Logger.maskSensitiveData(eventResponse.id),
                            "title" to request.title,
                            "success" to "true"
                        )
                    )
                    
                    Logger.logBusinessEvent(
                        TAG,
                        "event_created_api",
                        mapOf(
                            "eventId" to Logger.maskSensitiveData(eventResponse.id),
                            "title" to request.title,
                            "category" to request.category,
                            "title" to request.title,
                            "source" to "api"
                        )
                    )
                    
                    Logger.exit(TAG, "createEvent")
                    emit(Result.success(eventResponse))
                } ?: run {
                    Logger.logError(TAG, "Empty response body from API", null, mapOf(
                        "title" to request.title as Any,
                        "httpCode" to response.code().toString() as Any
                    ))
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                Logger.logError(TAG, "API request failed", null, mapOf(
                    "title" to request.title as Any,
                    "httpCode" to response.code().toString() as Any,
                    "httpMessage" to response.message() as Any
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "createEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "title" to request.title as Any,
                    "success" to "false" as Any,
                    "errorType" to e::class.simpleName.toString() as Any
                )
            )
            
            Logger.logError(TAG, "Failed to create event via API", e, mapOf(
                "title" to request.title as Any,
                "category" to request.category as Any,
                "errorType" to e::class.simpleName.toString() as Any,
                "errorMessage" to e.message.toString() as Any
            ))
            
            Logger.exit(TAG, "createEvent")
            emit(Result.failure(e))
        }
    }
    
    suspend fun getEventById(eventId: String): Flow<Result<EventResponse>> = flow {
        Logger.enter(TAG, "getEventById", "eventId" to Logger.maskSensitiveData(eventId))
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.getEventById(eventId)
            if (response.isSuccessful) {
                response.body()?.let { eventResponse ->
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
                        "event_retrieved_api",
                        mapOf(
                            "eventId" to Logger.maskSensitiveData(eventId),
                            "title" to eventResponse.title,
                            "source" to "api"
                        )
                    )
                    
                    Logger.exit(TAG, "getEventById")
                    emit(Result.success(eventResponse))
                } ?: run {
                    Logger.logError(TAG, "Event not found in API response", null, mapOf(
                        "eventId" to Logger.maskSensitiveData(eventId),
                        "httpCode" to response.code().toString()
                    ))
                    emit(Result.failure(Exception("Event not found")))
                }
            } else {
                Logger.logError(TAG, "Failed to retrieve event from API", null, mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
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
            
            Logger.logError(TAG, "Failed to get event by ID via API", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "getEventById")
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateEvent(eventId: String, request: UpdateEventRequest): Flow<Result<EventResponse>> = flow {
        Logger.enter(TAG, "updateEvent", 
            "eventId" to Logger.maskSensitiveData(eventId),
            "title" to request.title
        )
        val startTime = System.currentTimeMillis()
        
        try {
            // Validate request
            val validation = apiValidator.validateUpdateEventRequest(request)
            if (!validation.isValid) {
                Logger.logError(TAG, "Event update validation failed", null, mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId) as Any,
                    "validationError" to validation.getErrorMessage() as Any,
                    "title" to request.title as Any
                ))
                emit(Result.failure(IllegalArgumentException(validation.getErrorMessage())))
                return@flow
            }
            
            val response = eventApiService.updateEvent(eventId, request)
            if (response.isSuccessful) {
                response.body()?.let { eventResponse ->
                    Logger.logPerformance(
                        TAG,
                        "updateEvent",
                        System.currentTimeMillis() - startTime,
                        mapOf<String, Any>(
                            "eventId" to Logger.maskSensitiveData(eventId),
                            "title" to (request.title ?: ""),
                            "success" to "true"
                        )
                    )
                    
                    Logger.logBusinessEvent(
                        TAG,
                        "event_updated_api",
                        mapOf<String, Any>(
                            "eventId" to Logger.maskSensitiveData(eventId),
                            "title" to (request.title ?: ""),
                            "source" to "api"
                        )
                    )
                    
                    Logger.exit(TAG, "updateEvent")
                    emit(Result.success(eventResponse))
                } ?: run {
                    Logger.logError(TAG, "Empty response body from API", null, mapOf(
                        "eventId" to Logger.maskSensitiveData(eventId),
                        "httpCode" to response.code().toString()
                    ))
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                Logger.logError(TAG, "Failed to update event via API", null, mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "updateEvent",
                System.currentTimeMillis() - startTime,
                mapOf<String, Any>(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "title" to (request.title ?: ""),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to update event via API", e, mapOf<String, Any>(
                "eventId" to Logger.maskSensitiveData(eventId),
                "title" to (request.title ?: ""),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to (e.message ?: "")
            ))
            
            Logger.exit(TAG, "updateEvent")
            emit(Result.failure(e))
        }
    }
    
    suspend fun deleteEvent(eventId: String): Flow<Result<Unit>> = flow {
        Logger.enter(TAG, "deleteEvent", 
            "eventId" to Logger.maskSensitiveData(eventId)
        )
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.deleteEvent(eventId)
            if (response.isSuccessful) {
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
                    "event_deleted_api",
                    mapOf(
                        "eventId" to Logger.maskSensitiveData(eventId),
                        "source" to "api"
                    )
                )
                
                Logger.exit(TAG, "deleteEvent")
                emit(Result.success(Unit))
            } else {
                Logger.logError(TAG, "Failed to delete event via API", null, mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
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
            
            Logger.logError(TAG, "Failed to delete event via API", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "deleteEvent")
            emit(Result.failure(e))
        }
    }
    
    suspend fun getAllEvents(
        page: Int = 1,
        limit: Int = 20,
        category: String? = null,
        featured: Boolean? = null
    ): Flow<Result<List<EventResponse>>> = flow {
        Logger.enter(TAG, "getAllEvents", 
            "page" to page.toString(),
            "limit" to limit.toString(),
            "category" to (category ?: "all"),
            "featured" to (featured?.toString() ?: "null")
        )
        val startTime = System.currentTimeMillis()
        
        if (featured != null) {
            try {
                val response = eventApiService.getFeaturedEvents(page, limit)
                if (response.isSuccessful) {
                    response.body()?.let { events ->
                        Logger.logPerformance(
                            TAG,
                            "getAllEvents",
                            System.currentTimeMillis() - startTime,
                            mapOf(
                                "eventCount" to events.size.toString(),
                                "featured" to "true",
                                "success" to "true"
                            )
                        )
                        
                        Logger.logBusinessEvent(
                            TAG,
                            "events_retrieved_api",
                            mapOf(
                                "eventCount" to events.size.toString(),
                                "featured" to "true",
                                "source" to "api"
                            )
                        )
                        
                        Logger.exit(TAG, "getAllEvents")
                        emit(Result.success(events))
                    } ?: run {
                        Logger.logPerformance(
                            TAG,
                            "getAllEvents",
                            System.currentTimeMillis() - startTime,
                            mapOf(
                                "eventCount" to "0",
                                "featured" to "true",
                                "success" to "true"
                            )
                        )
                        Logger.exit(TAG, "getAllEvents")
                        emit(Result.success(emptyList()))
                    }
                } else {
                    Logger.logError(TAG, "Failed to get featured events via API", null, mapOf(
                        "httpCode" to response.code().toString(),
                        "httpMessage" to response.message(),
                        "featured" to "true"
                    ))
                    emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
                }
            } catch (e: Exception) {
                Logger.logPerformance(
                    TAG,
                    "getAllEvents",
                    System.currentTimeMillis() - startTime,
                    mapOf(
                        "featured" to "true",
                        "success" to "false",
                        "errorType" to e::class.simpleName.toString()
                    )
                )
                
                Logger.logError(TAG, "Failed to get featured events via API", e, mapOf(
                    "featured" to "true",
                    "errorType" to e::class.simpleName.toString(),
                    "errorMessage" to e.message.toString()
                ))
                
                Logger.exit(TAG, "getAllEvents")
                emit(Result.failure(e))
            }
        } else {
            try {
                val response = eventApiService.getEvents(page, limit, category)
                if (response.isSuccessful) {
                    response.body()?.let { events ->
                        Logger.logPerformance(
                            TAG,
                            "getAllEvents",
                            System.currentTimeMillis() - startTime,
                            mapOf(
                                "eventCount" to events.size.toString(),
                                "category" to (category ?: "all"),
                                "success" to "true"
                            )
                        )
                        
                        Logger.logBusinessEvent(
                            TAG,
                            "events_retrieved_api",
                            mapOf(
                                "eventCount" to events.size.toString(),
                                "category" to (category ?: "all"),
                                "source" to "api"
                            )
                        )
                        
                        Logger.exit(TAG, "getAllEvents")
                        emit(Result.success(events))
                    } ?: run {
                        Logger.logPerformance(
                            TAG,
                            "getAllEvents",
                            System.currentTimeMillis() - startTime,
                            mapOf(
                                "eventCount" to "0",
                                "category" to (category ?: "all"),
                                "success" to "true"
                            )
                        )
                        Logger.exit(TAG, "getAllEvents")
                        emit(Result.success(emptyList()))
                    }
                } else {
                    Logger.logError(TAG, "Failed to get events via API", null, mapOf(
                        "httpCode" to response.code().toString(),
                        "httpMessage" to response.message(),
                        "category" to (category ?: "all")
                    ))
                    emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
                }
            } catch (e: Exception) {
                Logger.logPerformance(
                    TAG,
                    "getAllEvents",
                    System.currentTimeMillis() - startTime,
                    mapOf(
                        "category" to (category ?: "all"),
                        "success" to "false",
                        "errorType" to e::class.simpleName.toString()
                    )
                )
                
                Logger.logError(TAG, "Failed to get events via API", e, mapOf(
                    "category" to (category ?: "all"),
                    "errorType" to e::class.simpleName.toString(),
                    "errorMessage" to e.message.toString()
                ))
                
                Logger.exit(TAG, "getAllEvents")
                emit(Result.failure(e))
            }
        }
    }
    
    suspend fun getEventsByLocation(
        latitude: Double,
        longitude: Double,
        radius: Double = 10.0,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<EventResponse>>> = flow {
        Logger.enter(TAG, "getEventsByLocation", 
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString(),
            "radius" to radius.toString(),
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.getEventsByLocation(latitude, longitude, radius, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    Logger.logPerformance(
                        TAG,
                        "getEventsByLocation",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventCount" to events.size.toString(),
                            "radius" to radius.toString(),
                            "success" to "true"
                        )
                    )
                    
                    Logger.logBusinessEvent(
                        TAG,
                        "events_retrieved_by_location_api",
                        mapOf(
                            "eventCount" to events.size.toString(),
                            "radius" to radius.toString(),
                            "source" to "api"
                        )
                    )
                    
                    Logger.exit(TAG, "getEventsByLocation")
                    emit(Result.success(events))
                } ?: run {
                    Logger.logPerformance(
                        TAG,
                        "getEventsByLocation",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventCount" to "0",
                            "radius" to radius.toString(),
                            "success" to "true"
                        )
                    )
                    Logger.exit(TAG, "getEventsByLocation")
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.logError(TAG, "Failed to get events by location via API", null, mapOf(
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "radius" to radius.toString()
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "getEventsByLocation",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "radius" to radius.toString(),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to get events by location via API", e, mapOf(
                "radius" to radius.toString(),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "getEventsByLocation")
            emit(Result.failure(e))
        }
    }
    
    suspend fun searchEvents(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<EventResponse>>> = flow {
        Logger.enter(TAG, "searchEvents", 
            "query" to query,
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.searchEvents(query, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    Logger.logPerformance(
                        TAG,
                        "searchEvents",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventCount" to events.size.toString(),
                            "query" to query,
                            "success" to "true"
                        )
                    )
                    
                    Logger.logBusinessEvent(
                        TAG,
                        "events_searched_api",
                        mapOf(
                            "eventCount" to events.size.toString(),
                            "query" to query,
                            "source" to "api"
                        )
                    )
                    
                    Logger.exit(TAG, "searchEvents")
                    emit(Result.success(events))
                } ?: run {
                    Logger.logPerformance(
                        TAG,
                        "searchEvents",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventCount" to "0",
                            "query" to query,
                            "success" to "true"
                        )
                    )
                    Logger.exit(TAG, "searchEvents")
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.logError(TAG, "Failed to search events via API", null, mapOf(
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "query" to query
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "searchEvents",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "query" to query,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to search events via API", e, mapOf(
                "query" to query,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "searchEvents")
            emit(Result.failure(e))
        }
    }
    
    suspend fun getEventsByDateRange(
        startDate: String,
        endDate: String,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<EventResponse>>> = flow {
        Logger.enter(TAG, "getEventsByDateRange", 
            "startDate" to startDate,
            "endDate" to endDate,
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.getEventsByDateRange(startDate, endDate, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    Logger.logPerformance(
                        TAG,
                        "getEventsByDateRange",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventCount" to events.size.toString(),
                            "startDate" to startDate,
                            "endDate" to endDate,
                            "success" to "true"
                        )
                    )
                    
                    Logger.logBusinessEvent(
                        TAG,
                        "events_retrieved_by_date_range_api",
                        mapOf(
                            "eventCount" to events.size.toString(),
                            "startDate" to startDate,
                            "endDate" to endDate,
                            "source" to "api"
                        )
                    )
                    
                    Logger.exit(TAG, "getEventsByDateRange")
                    emit(Result.success(events))
                } ?: run {
                    Logger.logPerformance(
                        TAG,
                        "getEventsByDateRange",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventCount" to "0",
                            "startDate" to startDate,
                            "endDate" to endDate,
                            "success" to "true"
                        )
                    )
                    Logger.exit(TAG, "getEventsByDateRange")
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.logError(TAG, "Failed to get events by date range via API", null, mapOf(
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "startDate" to startDate,
                    "endDate" to endDate
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "getEventsByDateRange",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "startDate" to startDate,
                    "endDate" to endDate,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to get events by date range via API", e, mapOf(
                "startDate" to startDate,
                "endDate" to endDate,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "getEventsByDateRange")
            emit(Result.failure(e))
        }
    }
    
    suspend fun joinEvent(eventId: String, userId: String): Flow<Result<Unit>> = flow {
        Logger.enter(TAG, "joinEvent", "eventId" to eventId, "userId" to userId)
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.joinEvent(eventId, userId)
            if (response.isSuccessful) {
                Logger.logPerformance(
                    TAG,
                    "joinEvent",
                    System.currentTimeMillis() - startTime,
                    mapOf(
                        "eventId" to eventId,
                        "userId" to userId,
                        "success" to "true"
                    )
                )
                
                Logger.logBusinessEvent(
                    TAG,
                    "event_joined_api",
                    mapOf(
                        "eventId" to eventId,
                        "userId" to userId,
                        "source" to "api"
                    )
                )
                
                Logger.exit(TAG, "joinEvent")
                emit(Result.success(Unit))
            } else {
                Logger.logError(TAG, "Failed to join event via API", null, mapOf(
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "eventId" to eventId,
                    "userId" to userId
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "joinEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to eventId,
                    "userId" to userId,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to join event via API", e, mapOf(
                "eventId" to eventId,
                "userId" to userId,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "joinEvent")
            emit(Result.failure(e))
        }
    }
    
    suspend fun leaveEvent(eventId: String, userId: String): Flow<Result<Unit>> = flow {
        Logger.enter(TAG, "leaveEvent", 
            "eventId" to eventId,
            "userId" to userId
        )
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.leaveEvent(eventId, userId)
            if (response.isSuccessful) {
                Logger.logPerformance(
                    TAG,
                    "leaveEvent",
                    System.currentTimeMillis() - startTime,
                    mapOf(
                        "eventId" to eventId,
                        "userId" to userId,
                        "success" to "true"
                    )
                )
                
                Logger.logBusinessEvent(
                    TAG,
                    "event_left_api",
                    mapOf(
                        "eventId" to eventId,
                        "userId" to userId,
                        "source" to "api"
                    )
                )
                
                Logger.exit(TAG, "leaveEvent")
                emit(Result.success(Unit))
            } else {
                Logger.logError(TAG, "Failed to leave event via API", null, mapOf(
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "eventId" to eventId,
                    "userId" to userId
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "leaveEvent",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to eventId,
                    "userId" to userId,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to leave event via API", e, mapOf(
                "eventId" to eventId,
                "userId" to userId,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "leaveEvent")
            emit(Result.failure(e))
        }
    }
    
    suspend fun getEventParticipants(
        eventId: String,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<String>>> = flow {
        Logger.enter(TAG, "getEventParticipants", 
            "eventId" to eventId,
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.getEventParticipants(eventId, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { participants ->
                    Logger.logPerformance(
                        TAG,
                        "getEventParticipants",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventId" to eventId,
                            "participantCount" to participants.size.toString(),
                            "success" to "true"
                        )
                    )
                    
                    Logger.logBusinessEvent(
                        TAG,
                        "event_participants_retrieved_api",
                        mapOf(
                            "eventId" to eventId,
                            "participantCount" to participants.size.toString(),
                            "source" to "api"
                        )
                    )
                    
                    Logger.exit(TAG, "getEventParticipants")
                    emit(Result.success(participants))
                } ?: run {
                    Logger.logPerformance(
                        TAG,
                        "getEventParticipants",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "eventId" to eventId,
                            "participantCount" to "0",
                            "success" to "true"
                        )
                    )
                    Logger.exit(TAG, "getEventParticipants")
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.logError(TAG, "Failed to get event participants via API", null, mapOf(
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "eventId" to eventId
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "getEventParticipants",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to eventId,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to get event participants via API", e, mapOf(
                "eventId" to eventId,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "getEventParticipants")
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserEvents(
        userId: String,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<EventResponse>>> = flow {
        Logger.enter(TAG, "getUserEvents", 
            "userId" to userId,
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        val startTime = System.currentTimeMillis()
        
        try {
            val response = eventApiService.getUserEvents(userId, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    Logger.logPerformance(
                        TAG,
                        "getUserEvents",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "userId" to userId,
                            "eventCount" to events.size.toString(),
                            "success" to "true"
                        )
                    )
                    
                    Logger.logBusinessEvent(
                        TAG,
                        "user_events_retrieved_api",
                        mapOf(
                            "userId" to userId,
                            "eventCount" to events.size.toString(),
                            "source" to "api"
                        )
                    )
                    
                    Logger.exit(TAG, "getUserEvents")
                    emit(Result.success(events))
                } ?: run {
                    Logger.logPerformance(
                        TAG,
                        "getUserEvents",
                        System.currentTimeMillis() - startTime,
                        mapOf(
                            "userId" to userId,
                            "eventCount" to "0",
                            "success" to "true"
                        )
                    )
                    Logger.exit(TAG, "getUserEvents")
                    emit(Result.success(emptyList()))
                }
            } else {
                Logger.logError(TAG, "Failed to get user events via API", null, mapOf(
                    "httpCode" to response.code().toString(),
                    "httpMessage" to response.message(),
                    "userId" to userId
                ))
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "getUserEvents",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "userId" to userId,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.logError(TAG, "Failed to get user events via API", e, mapOf(
                "userId" to userId,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "getUserEvents")
            emit(Result.failure(e))
        }
    }
}