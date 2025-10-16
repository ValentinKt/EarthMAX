package com.earthmax.data.api.repository

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
    
    suspend fun createEvent(request: CreateEventRequest): Flow<Result<EventResponse>> = flow {
        try {
            // Validate request
            val validation = apiValidator.validateCreateEventRequest(request)
            if (!validation.isValid) {
                emit(Result.failure(IllegalArgumentException(validation.getErrorMessage())))
                return@flow
            }
            
            val response = eventApiService.createEvent(request)
            if (response.isSuccessful) {
                response.body()?.let { eventResponse ->
                    emit(Result.success(eventResponse))
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getEventById(eventId: String): Flow<Result<EventResponse>> = flow {
        try {
            val response = eventApiService.getEventById(eventId)
            if (response.isSuccessful) {
                response.body()?.let { eventResponse ->
                    emit(Result.success(eventResponse))
                } ?: emit(Result.failure(Exception("Event not found")))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateEvent(eventId: String, request: UpdateEventRequest): Flow<Result<EventResponse>> = flow {
        try {
            // Validate request
            val validation = apiValidator.validateUpdateEventRequest(request)
            if (!validation.isValid) {
                emit(Result.failure(IllegalArgumentException(validation.getErrorMessage())))
                return@flow
            }
            
            val response = eventApiService.updateEvent(eventId, request)
            if (response.isSuccessful) {
                response.body()?.let { eventResponse ->
                    emit(Result.success(eventResponse))
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun deleteEvent(eventId: String): Flow<Result<Unit>> = flow {
        try {
            val response = eventApiService.deleteEvent(eventId)
            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getAllEvents(
        page: Int = 1,
        limit: Int = 20,
        category: String? = null,
        featured: Boolean? = null
    ): Flow<Result<List<EventResponse>>> = flow {
        if (featured != null) {
            try {
                val response = eventApiService.getFeaturedEvents(page, limit)
                if (response.isSuccessful) {
                    response.body()?.let { events ->
                        emit(Result.success(events))
                    } ?: emit(Result.success(emptyList()))
                } else {
                    emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
                }
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        } else {
            try {
                val response = eventApiService.getEvents(page, limit, category)
                if (response.isSuccessful) {
                    response.body()?.let { events ->
                        emit(Result.success(events))
                    } ?: emit(Result.success(emptyList()))
                } else {
                    emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
                }
            } catch (e: Exception) {
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
        try {
            val response = eventApiService.getEventsByLocation(latitude, longitude, radius, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    emit(Result.success(events))
                } ?: emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun searchEvents(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<EventResponse>>> = flow {
        try {
            val response = eventApiService.searchEvents(query, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    emit(Result.success(events))
                } ?: emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getEventsByDateRange(
        startDate: String,
        endDate: String,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<EventResponse>>> = flow {
        try {
            val response = eventApiService.getEventsByDateRange(startDate, endDate, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    emit(Result.success(events))
                } ?: emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun joinEvent(eventId: String, userId: String): Flow<Result<Unit>> = flow {
        try {
            val response = eventApiService.joinEvent(eventId, userId)
            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun leaveEvent(eventId: String, userId: String): Flow<Result<Unit>> = flow {
        try {
            val response = eventApiService.leaveEvent(eventId, userId)
            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getEventParticipants(
        eventId: String,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<String>>> = flow {
        try {
            val response = eventApiService.getEventParticipants(eventId, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { participants ->
                    emit(Result.success(participants))
                } ?: emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserEvents(
        userId: String,
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<List<EventResponse>>> = flow {
        try {
            val response = eventApiService.getUserEvents(userId, page, limit)
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    emit(Result.success(events))
                } ?: emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}