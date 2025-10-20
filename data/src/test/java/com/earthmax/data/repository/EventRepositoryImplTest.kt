package com.earthmax.data.repository

import com.earthmax.core.cache.AdvancedCacheManager
import com.earthmax.core.cache.CachePolicy
import com.earthmax.core.error.AdvancedErrorHandler
import com.earthmax.core.monitoring.Logger
import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.data.local.dao.EventDao
import com.earthmax.data.local.entity.EventEntity
import com.earthmax.data.mapper.EventMapper
import com.earthmax.data.remote.api.EventApi
import com.earthmax.data.remote.dto.EventDto
import com.earthmax.domain.model.Event
import com.earthmax.domain.repository.EventRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class EventRepositoryImplTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var eventApi: EventApi
    private lateinit var eventDao: EventDao
    private lateinit var cacheManager: AdvancedCacheManager
    private lateinit var errorHandler: AdvancedErrorHandler
    private lateinit var logger: Logger
    private lateinit var metricsCollector: MetricsCollector

    private val testEventDto = EventDto(
        id = "1",
        title = "Test Event",
        description = "Test Description",
        location = "Test Location",
        dateTime = "2024-01-15T10:00:00",
        maxParticipants = 50,
        currentParticipants = 10,
        organizerId = "org1",
        category = "Environment",
        tags = listOf("cleanup", "nature"),
        imageUrl = "https://example.com/image.jpg",
        isActive = true,
        createdAt = "2024-01-01T00:00:00",
        updatedAt = "2024-01-01T00:00:00"
    )

    private val testEventEntity = EventEntity(
        id = "1",
        title = "Test Event",
        description = "Test Description",
        location = "Test Location",
        dateTime = LocalDateTime.of(2024, 1, 15, 10, 0),
        maxParticipants = 50,
        currentParticipants = 10,
        organizerId = "org1",
        category = "Environment",
        tags = listOf("cleanup", "nature"),
        imageUrl = "https://example.com/image.jpg",
        isActive = true,
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2024, 1, 1, 0, 0)
    )

    private val testEvent = Event(
        id = "1",
        title = "Test Event",
        description = "Test Description",
        location = "Test Location",
        dateTime = LocalDateTime.of(2024, 1, 15, 10, 0),
        maxParticipants = 50,
        currentParticipants = 10,
        organizerId = "org1",
        category = "Environment",
        tags = listOf("cleanup", "nature"),
        imageUrl = "https://example.com/image.jpg",
        isActive = true,
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2024, 1, 1, 0, 0)
    )

    @Before
    fun setUp() {
        eventApi = mockk()
        eventDao = mockk()
        logger = mockk(relaxed = true)
        metricsCollector = mockk(relaxed = true)
        cacheManager = mockk()
        errorHandler = mockk()

        eventRepository = EventRepositoryImpl(
            eventApi = eventApi,
            eventDao = eventDao,
            cacheManager = cacheManager,
            errorHandler = errorHandler,
            logger = logger,
            metricsCollector = metricsCollector
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getEvents should return cached data when available`() = runBlocking {
        // Given
        val cachedEvents = listOf(testEvent)
        coEvery { cacheManager.get<List<Event>>("events_all") } returns cachedEvents

        // When
        val result = eventRepository.getEvents().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(cachedEvents, result[0])
        verify { logger.d("EventRepository", "Loading events from cache") }
    }

    @Test
    fun `getEvents should fetch from API when cache is empty`() = runBlocking {
        // Given
        coEvery { cacheManager.get<List<Event>>("events_all") } returns null
        coEvery { eventApi.getEvents() } returns listOf(testEventDto)
        coEvery { eventDao.getAllEvents() } returns flowOf(listOf(testEventEntity))
        coEvery { eventDao.insertEvents(any()) } just Runs
        coEvery { cacheManager.put(any(), any<List<Event>>(), any()) } just Runs

        // When
        val result = eventRepository.getEvents().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
        assertEquals(testEvent.id, result[0][0].id)
        
        coVerify { eventApi.getEvents() }
        coVerify { eventDao.insertEvents(any()) }
        coVerify { cacheManager.put("events_all", any<List<Event>>(), CachePolicy.TimeToLive(300000)) }
    }

    @Test
    fun `getEventById should return cached event when available`() = runBlocking {
        // Given
        val eventId = "1"
        coEvery { cacheManager.get<Event>("event_$eventId") } returns testEvent

        // When
        val result = eventRepository.getEventById(eventId)

        // Then
        assertEquals(testEvent, result)
        verify { logger.d("EventRepository", "Loading event $eventId from cache") }
    }

    @Test
    fun `getEventById should fetch from API when not cached`() = runBlocking {
        // Given
        val eventId = "1"
        coEvery { cacheManager.get<Event>("event_$eventId") } returns null
        coEvery { eventApi.getEventById(eventId) } returns testEventDto
        coEvery { eventDao.getEventById(eventId) } returns testEventEntity
        coEvery { eventDao.insertEvent(any()) } just Runs
        coEvery { cacheManager.put(any(), any<Event>(), any()) } just Runs

        // When
        val result = eventRepository.getEventById(eventId)

        // Then
        assertEquals(testEvent.id, result?.id)
        
        coVerify { eventApi.getEventById(eventId) }
        coVerify { eventDao.insertEvent(any()) }
        coVerify { cacheManager.put("event_$eventId", any<Event>(), CachePolicy.TimeToLive(300000)) }
    }

    @Test
    fun `createEvent should create event and update cache`() = runBlocking {
        // Given
        val newEvent = testEvent.copy(id = "")
        val createdEventDto = testEventDto.copy(id = "2")
        val createdEvent = testEvent.copy(id = "2")
        
        coEvery { eventApi.createEvent(any()) } returns createdEventDto
        coEvery { eventDao.insertEvent(any()) } just Runs
        coEvery { cacheManager.put(any(), any<Event>(), any()) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        val result = eventRepository.createEvent(newEvent)

        // Then
        assertEquals("2", result?.id)
        
        coVerify { eventApi.createEvent(any()) }
        coVerify { eventDao.insertEvent(any()) }
        coVerify { cacheManager.put("event_2", any<Event>(), CachePolicy.TimeToLive(300000)) }
        coVerify { cacheManager.invalidate(any()) }
    }

    @Test
    fun `updateEvent should update event and invalidate cache`() = runBlocking {
        // Given
        val updatedEvent = testEvent.copy(title = "Updated Event")
        val updatedEventDto = testEventDto.copy(title = "Updated Event")
        
        coEvery { eventApi.updateEvent(testEvent.id, any()) } returns updatedEventDto
        coEvery { eventDao.updateEvent(any()) } just Runs
        coEvery { cacheManager.put(any(), any<Event>(), any()) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        val result = eventRepository.updateEvent(updatedEvent)

        // Then
        assertEquals("Updated Event", result?.title)
        
        coVerify { eventApi.updateEvent(testEvent.id, any()) }
        coVerify { eventDao.updateEvent(any()) }
        coVerify { cacheManager.put("event_${testEvent.id}", any<Event>(), CachePolicy.TimeToLive(300000)) }
        coVerify { cacheManager.invalidate(any()) }
    }

    @Test
    fun `deleteEvent should delete event and invalidate cache`() = runBlocking {
        // Given
        val eventId = "1"
        coEvery { eventApi.deleteEvent(eventId) } just Runs
        coEvery { eventDao.deleteEvent(eventId) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        eventRepository.deleteEvent(eventId)

        // Then
        coVerify { eventApi.deleteEvent(eventId) }
        coVerify { eventDao.deleteEvent(eventId) }
        coVerify { cacheManager.invalidate(any()) }
    }

    @Test
    fun `joinEvent should update participation and invalidate cache`() = runBlocking {
        // Given
        val eventId = "1"
        val userId = "user1"
        coEvery { eventApi.joinEvent(eventId, userId) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        eventRepository.joinEvent(eventId, userId)

        // Then
        coVerify { eventApi.joinEvent(eventId, userId) }
        coVerify { cacheManager.invalidate(any()) }
        verify { logger.i("EventRepository", "User $userId joined event $eventId") }
    }

    @Test
    fun `leaveEvent should update participation and invalidate cache`() = runBlocking {
        // Given
        val eventId = "1"
        val userId = "user1"
        coEvery { eventApi.leaveEvent(eventId, userId) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        eventRepository.leaveEvent(eventId, userId)

        // Then
        coVerify { eventApi.leaveEvent(eventId, userId) }
        coVerify { cacheManager.invalidate(any()) }
        verify { logger.i("EventRepository", "User $userId left event $eventId") }
    }

    @Test
    fun `getEventsByCategory should filter events correctly`() = runBlocking {
        // Given
        val category = "Environment"
        val events = listOf(testEvent, testEvent.copy(id = "2", category = "Education"))
        coEvery { cacheManager.get<List<Event>>("events_category_$category") } returns null
        coEvery { eventDao.getEventsByCategory(category) } returns flowOf(listOf(testEventEntity))
        coEvery { cacheManager.put(any(), any<List<Event>>(), any()) } just Runs

        // When
        val result = eventRepository.getEventsByCategory(category).toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
        assertEquals(category, result[0][0].category)
        
        coVerify { eventDao.getEventsByCategory(category) }
        coVerify { cacheManager.put("events_category_$category", any<List<Event>>(), CachePolicy.TimeToLive(300000)) }
    }
}