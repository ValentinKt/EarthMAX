package com.earthmax.data.api

import com.earthmax.data.api.dto.CreateEventRequest
import com.earthmax.data.api.dto.EventResponse
import com.earthmax.data.api.dto.UpdateEventRequest
import retrofit2.Response
import retrofit2.http.*

/**
 * RESTful API service for events management operations
 */
interface EventApiService {
    
    /**
     * Get all events with optional filtering and pagination
     */
    @GET("events")
    suspend fun getEvents(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("location") location: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("search") search: String? = null
    ): Response<List<EventResponse>>
    
    /**
     * Get a specific event by ID
     */
    @GET("events/{id}")
    suspend fun getEventById(@Path("id") eventId: String): Response<EventResponse>
    
    /**
     * Create a new event
     */
    @POST("events")
    suspend fun createEvent(@Body request: CreateEventRequest): Response<EventResponse>
    
    /**
     * Update an existing event
     */
    @PUT("events/{id}")
    suspend fun updateEvent(
        @Path("id") eventId: String,
        @Body request: UpdateEventRequest
    ): Response<EventResponse>
    
    /**
     * Partially update an event
     */
    @PATCH("events/{id}")
    suspend fun patchEvent(
        @Path("id") eventId: String,
        @Body request: UpdateEventRequest
    ): Response<EventResponse>
    
    /**
     * Delete an event
     */
    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") eventId: String): Response<Unit>
    
    /**
     * Get events by location
     */
    @GET("events/location")
    suspend fun getEventsByLocation(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double = 10.0,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<EventResponse>>
    
    /**
     * Get events by category
     */
    @GET("events/category/{category}")
    suspend fun getEventsByCategory(@Path("category") category: String): Response<List<EventResponse>>
    
    /**
     * Join an event
     */
    @POST("events/{id}/join")
    suspend fun joinEvent(
        @Path("id") eventId: String,
        @Query("user_id") userId: String
    ): Response<Unit>
    
    /**
     * Leave an event
     */
    @DELETE("events/{id}/join")
    suspend fun leaveEvent(
        @Path("id") eventId: String,
        @Query("user_id") userId: String
    ): Response<Unit>
    
    /**
     * Get event participants
     */
    @GET("events/{id}/participants")
    suspend fun getEventParticipants(
        @Path("id") eventId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<String>>
    
    /**
     * Get upcoming events
     */
    @GET("events/upcoming")
    suspend fun getUpcomingEvents(
        @Query("limit") limit: Int = 10
    ): Response<List<EventResponse>>
    
    /**
     * Get featured events
     */
    @GET("events/featured")
    suspend fun getFeaturedEvents(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 5
    ): Response<List<EventResponse>>
    
    /**
     * Search events
     */
    @GET("events/search")
    suspend fun searchEvents(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("location") location: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<List<EventResponse>>
    
    /**
     * Get event by date range
     */
    @GET("events/date")
    suspend fun getEventsByDateRange(
        @Query("date_from") dateFrom: String,
        @Query("date_to") dateTo: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<EventResponse>>
    
    /**
     * Get user events
     */
    @GET("events/user")
    suspend fun getUserEvents(
        @Query("user_id") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<EventResponse>>

}