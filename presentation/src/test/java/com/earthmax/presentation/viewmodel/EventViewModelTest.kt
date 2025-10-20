package com.earthmax.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.earthmax.domain.model.Event
import com.earthmax.domain.usecase.event.*
import com.earthmax.presentation.ui.state.EventUiState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class EventViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var eventViewModel: EventViewModel
    private lateinit var getEventsUseCase: GetEventsUseCase
    private lateinit var getEventByIdUseCase: GetEventByIdUseCase
    private lateinit var createEventUseCase: CreateEventUseCase
    private lateinit var updateEventUseCase: UpdateEventUseCase
    private lateinit var deleteEventUseCase: DeleteEventUseCase
    private lateinit var joinEventUseCase: JoinEventUseCase
    private lateinit var leaveEventUseCase: LeaveEventUseCase
    private lateinit var getEventsByCategoryUseCase: GetEventsByCategoryUseCase

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
        Dispatchers.setMain(testDispatcher)
        
        getEventsUseCase = mockk()
        getEventByIdUseCase = mockk()
        createEventUseCase = mockk()
        updateEventUseCase = mockk()
        deleteEventUseCase = mockk()
        joinEventUseCase = mockk()
        leaveEventUseCase = mockk()
        getEventsByCategoryUseCase = mockk()

        eventViewModel = EventViewModel(
            getEventsUseCase = getEventsUseCase,
            getEventByIdUseCase = getEventByIdUseCase,
            createEventUseCase = createEventUseCase,
            updateEventUseCase = updateEventUseCase,
            deleteEventUseCase = deleteEventUseCase,
            joinEventUseCase = joinEventUseCase,
            leaveEventUseCase = leaveEventUseCase,
            getEventsByCategoryUseCase = getEventsByCategoryUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadEvents should update UI state with events`() = runTest {
        // Given
        val events = listOf(testEvent)
        coEvery { getEventsUseCase() } returns flowOf(events)

        // When
        eventViewModel.loadEvents()
        advanceUntilIdle()

        // Then
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Success)
        assertEquals(events, (uiState as EventUiState.Success).events)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `loadEvents should handle error state`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { getEventsUseCase() } throws Exception(errorMessage)

        // When
        eventViewModel.loadEvents()
        advanceUntilIdle()

        // Then
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Error)
        assertEquals(errorMessage, (uiState as EventUiState.Error).message)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `loadEvents should show loading state initially`() = runTest {
        // Given
        coEvery { getEventsUseCase() } returns flowOf(emptyList())

        // When
        eventViewModel.loadEvents()

        // Then
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState.isLoading)
    }

    @Test
    fun `loadEventById should update selected event`() = runTest {
        // Given
        val eventId = "1"
        coEvery { getEventByIdUseCase(eventId) } returns testEvent

        // When
        eventViewModel.loadEventById(eventId)
        advanceUntilIdle()

        // Then
        assertEquals(testEvent, eventViewModel.selectedEvent.value)
        assertFalse(eventViewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadEventById should handle null result`() = runTest {
        // Given
        val eventId = "1"
        coEvery { getEventByIdUseCase(eventId) } returns null

        // When
        eventViewModel.loadEventById(eventId)
        advanceUntilIdle()

        // Then
        assertNull(eventViewModel.selectedEvent.value)
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Error)
        assertEquals("Event not found", (uiState as EventUiState.Error).message)
    }

    @Test
    fun `createEvent should create event and refresh list`() = runTest {
        // Given
        val newEvent = testEvent.copy(id = "")
        val createdEvent = testEvent.copy(id = "2")
        coEvery { createEventUseCase(newEvent) } returns createdEvent
        coEvery { getEventsUseCase() } returns flowOf(listOf(createdEvent))

        // When
        eventViewModel.createEvent(newEvent)
        advanceUntilIdle()

        // Then
        coVerify { createEventUseCase(newEvent) }
        coVerify { getEventsUseCase() }
        
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Success)
        assertEquals(1, (uiState as EventUiState.Success).events.size)
    }

    @Test
    fun `createEvent should handle creation failure`() = runTest {
        // Given
        val newEvent = testEvent.copy(id = "")
        val errorMessage = "Creation failed"
        coEvery { createEventUseCase(newEvent) } throws Exception(errorMessage)

        // When
        eventViewModel.createEvent(newEvent)
        advanceUntilIdle()

        // Then
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Error)
        assertEquals(errorMessage, (uiState as EventUiState.Error).message)
    }

    @Test
    fun `updateEvent should update event and refresh list`() = runTest {
        // Given
        val updatedEvent = testEvent.copy(title = "Updated Event")
        coEvery { updateEventUseCase(updatedEvent) } returns updatedEvent
        coEvery { getEventsUseCase() } returns flowOf(listOf(updatedEvent))

        // When
        eventViewModel.updateEvent(updatedEvent)
        advanceUntilIdle()

        // Then
        coVerify { updateEventUseCase(updatedEvent) }
        coVerify { getEventsUseCase() }
        
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Success)
        assertEquals("Updated Event", (uiState as EventUiState.Success).events[0].title)
    }

    @Test
    fun `deleteEvent should remove event and refresh list`() = runTest {
        // Given
        val eventId = "1"
        coEvery { deleteEventUseCase(eventId) } just Runs
        coEvery { getEventsUseCase() } returns flowOf(emptyList())

        // When
        eventViewModel.deleteEvent(eventId)
        advanceUntilIdle()

        // Then
        coVerify { deleteEventUseCase(eventId) }
        coVerify { getEventsUseCase() }
        
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Success)
        assertTrue((uiState as EventUiState.Success).events.isEmpty())
    }

    @Test
    fun `joinEvent should update event participation`() = runTest {
        // Given
        val eventId = "1"
        val userId = "user1"
        coEvery { joinEventUseCase(eventId, userId) } just Runs
        coEvery { getEventsUseCase() } returns flowOf(listOf(testEvent))

        // When
        eventViewModel.joinEvent(eventId, userId)
        advanceUntilIdle()

        // Then
        coVerify { joinEventUseCase(eventId, userId) }
        coVerify { getEventsUseCase() }
    }

    @Test
    fun `leaveEvent should update event participation`() = runTest {
        // Given
        val eventId = "1"
        val userId = "user1"
        coEvery { leaveEventUseCase(eventId, userId) } just Runs
        coEvery { getEventsUseCase() } returns flowOf(listOf(testEvent))

        // When
        eventViewModel.leaveEvent(eventId, userId)
        advanceUntilIdle()

        // Then
        coVerify { leaveEventUseCase(eventId, userId) }
        coVerify { getEventsUseCase() }
    }

    @Test
    fun `loadEventsByCategory should filter events by category`() = runTest {
        // Given
        val category = "Environment"
        val events = listOf(testEvent)
        coEvery { getEventsByCategoryUseCase(category) } returns flowOf(events)

        // When
        eventViewModel.loadEventsByCategory(category)
        advanceUntilIdle()

        // Then
        coVerify { getEventsByCategoryUseCase(category) }
        
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Success)
        assertEquals(events, (uiState as EventUiState.Success).events)
        assertEquals(category, (uiState as EventUiState.Success).events[0].category)
    }

    @Test
    fun `refreshEvents should reload events`() = runTest {
        // Given
        val events = listOf(testEvent)
        coEvery { getEventsUseCase() } returns flowOf(events)

        // When
        eventViewModel.refreshEvents()
        advanceUntilIdle()

        // Then
        coVerify { getEventsUseCase() }
        
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Success)
        assertEquals(events, (uiState as EventUiState.Success).events)
    }

    @Test
    fun `clearSelectedEvent should reset selected event`() = runTest {
        // Given
        eventViewModel.selectedEvent.value = testEvent

        // When
        eventViewModel.clearSelectedEvent()

        // Then
        assertNull(eventViewModel.selectedEvent.value)
    }

    @Test
    fun `multiple operations should maintain consistent state`() = runTest {
        // Given
        val events = listOf(testEvent)
        coEvery { getEventsUseCase() } returns flowOf(events)
        coEvery { getEventByIdUseCase("1") } returns testEvent

        // When
        eventViewModel.loadEvents()
        eventViewModel.loadEventById("1")
        advanceUntilIdle()

        // Then
        val uiState = eventViewModel.uiState.value
        assertTrue(uiState is EventUiState.Success)
        assertEquals(events, (uiState as EventUiState.Success).events)
        assertEquals(testEvent, eventViewModel.selectedEvent.value)
    }
}