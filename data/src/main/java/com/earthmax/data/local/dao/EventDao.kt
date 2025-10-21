package com.earthmax.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.earthmax.core.models.EventCategory
import com.earthmax.data.local.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    
    @Query("SELECT * FROM events ORDER BY dateTime ASC")
    fun getAllEvents(): PagingSource<Int, EventEntity>
    
    @Query("SELECT * FROM events ORDER BY dateTime ASC")
    suspend fun getAllEventsSync(): List<EventEntity>
    
    @Query("SELECT * FROM events WHERE category = :category ORDER BY dateTime ASC")
    fun getEventsByCategory(category: EventCategory): PagingSource<Int, EventEntity>
    
    @Query("SELECT * FROM events WHERE category = :category ORDER BY dateTime ASC")
    suspend fun getEventsByCategorySync(category: EventCategory): List<EventEntity>
    
    @Query("SELECT * FROM events WHERE isJoined = 1 ORDER BY dateTime ASC")
    fun getJoinedEvents(): Flow<List<EventEntity>>
    
    @Query("SELECT * FROM events WHERE organizerId = :organizerId ORDER BY dateTime ASC")
    fun getEventsByOrganizer(organizerId: String): Flow<List<EventEntity>>
    
    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): EventEntity?
    
    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getEventByIdFlow(eventId: String): Flow<EventEntity?>
    
    @Query("""
        SELECT * FROM events 
        WHERE title LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%' 
        OR location LIKE '%' || :query || '%'
        ORDER BY dateTime ASC
    """)
    fun searchEvents(query: String): PagingSource<Int, EventEntity>
    
    @Query("""
        SELECT * FROM events 
        WHERE title LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%' 
        OR location LIKE '%' || :query || '%'
        ORDER BY dateTime ASC
    """)
    suspend fun searchEventsSync(query: String): List<EventEntity>
    
    @Query("""
        SELECT * FROM events 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY dateTime ASC
    """)
    fun getEventsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<EventEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)
    
    @Update
    suspend fun updateEvent(event: EventEntity)
    
    @Delete
    suspend fun deleteEvent(event: EventEntity)
    
    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)
    
    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()
    
    @Query("UPDATE events SET isJoined = :isJoined WHERE id = :eventId")
    suspend fun updateEventJoinStatus(eventId: String, isJoined: Boolean)
    
    @Query("UPDATE events SET currentParticipants = :participants WHERE id = :eventId")
    suspend fun updateEventParticipants(eventId: String, participants: Int)
}