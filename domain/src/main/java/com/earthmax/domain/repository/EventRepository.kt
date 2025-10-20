package com.earthmax.domain.repository

import com.earthmax.domain.model.DomainEvent
import com.earthmax.domain.model.EventCategory
import com.earthmax.domain.model.EventStatus
import com.earthmax.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for event operations.
 * This defines the contract for event data access in the domain layer.
 */
interface EventRepository {
    
    /**
     * Get all events with optional filtering
     */
    fun getEvents(
        category: EventCategory? = null,
        status: EventStatus? = null,
        location: String? = null
    ): Flow<Result<List<DomainEvent>>>
    
    /**
     * Get a specific event by ID
     */
    suspend fun getEventById(eventId: String): Result<DomainEvent>
    
    /**
     * Create a new event
     */
    suspend fun createEvent(event: DomainEvent): Result<DomainEvent>
    
    /**
     * Update an existing event
     */
    suspend fun updateEvent(event: DomainEvent): Result<DomainEvent>
    
    /**
     * Delete an event
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
    
    /**
     * Join an event as a participant
     */
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit>
    
    /**
     * Leave an event
     */
    suspend fun leaveEvent(eventId: String, userId: String): Result<Unit>
    
    /**
     * Get events created by a specific user
     */
    fun getEventsByUser(userId: String): Flow<Result<List<DomainEvent>>>
    
    /**
     * Get events that a user has joined
     */
    fun getJoinedEvents(userId: String): Flow<Result<List<DomainEvent>>>
    
    /**
     * Search events by query
     */
    suspend fun searchEvents(query: String): Result<List<DomainEvent>>
}