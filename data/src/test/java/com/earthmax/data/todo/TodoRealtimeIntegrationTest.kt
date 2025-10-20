package com.earthmax.data.todo

import com.earthmax.domain.model.DomainTodoItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TodoRealtimeIntegrationTest {

    private lateinit var mockSupabaseClient: SupabaseClient
    private lateinit var mockPostgrest: Postgrest
    private lateinit var mockRealtime: Realtime
    private lateinit var mockChannel: RealtimeChannel
    private lateinit var supabaseTodoRepository: SupabaseTodoRepository

    private val testEventId = "test-event-id"
    private val testTodoItem = DomainTodoItem(
        id = "test-todo-id",
        eventId = testEventId,
        title = "Test Todo",
        description = "Test Description",
        isCompleted = false,
        assignedTo = "test-user-id",
        createdBy = "creator-user-id",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        completedAt = null,
        updatedAt = Instant.parse("2024-01-01T12:00:00Z")
    )

    @Before
    fun setUp() {
        mockSupabaseClient = mockk()
        mockPostgrest = mockk()
        mockRealtime = mockk()
        mockChannel = mockk()

        every { mockSupabaseClient.postgrest } returns mockPostgrest
        every { mockSupabaseClient.realtime } returns mockRealtime

        supabaseTodoRepository = SupabaseTodoRepository(mockSupabaseClient)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `subscribeToTodoUpdates should establish realtime channel and emit updates`() = runTest {
        // Given
        val updatesFlow = MutableSharedFlow<DomainTodoItem>()
        
        every { mockRealtime.channel("todo_items_$testEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns flow {
            emit(
                PostgresAction.Update(
                    record = TodoItemDto(
                        id = testTodoItem.id,
                        event_id = testTodoItem.eventId,
                        title = "Updated Title",
                        description = testTodoItem.description,
                        is_completed = true,
                        assigned_to = testTodoItem.assignedTo,
                        created_by = testTodoItem.createdBy,
                        created_at = testTodoItem.createdAt.toString(),
                        completed_at = "2024-01-01T15:00:00Z",
                        updated_at = "2024-01-01T15:00:00Z"
                    ),
                    oldRecord = null
                )
            )
        }

        // When
        val resultFlow = supabaseTodoRepository.subscribeToTodoUpdates(testEventId)
        val emittedItems = mutableListOf<DomainTodoItem>()
        
        val job = launch {
            resultFlow.take(1).collect { todoItem ->
                emittedItems.add(todoItem)
            }
        }

        advanceUntilIdle()
        job.cancel()

        // Then
        verify { mockRealtime.channel("todo_items_$testEventId") }
        verify { mockChannel.subscribe() }
        coVerify { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") }
        
        assertEquals(1, emittedItems.size)
        val emittedItem = emittedItems.first()
        assertEquals(testTodoItem.id, emittedItem.id)
        assertEquals("Updated Title", emittedItem.title)
        assertTrue(emittedItem.isCompleted)
        assertNotNull(emittedItem.completedAt)
    }

    @Test
    fun `subscribeToTodoUpdates should handle INSERT actions`() = runTest {
        // Given
        val newTodoDto = TodoItemDto(
            id = "new-todo-id",
            event_id = testEventId,
            title = "New Todo",
            description = "New Description",
            is_completed = false,
            assigned_to = "new-user-id",
            created_by = "creator-user-id",
            created_at = "2024-01-01T16:00:00Z",
            completed_at = null,
            updated_at = "2024-01-01T16:00:00Z"
        )

        every { mockRealtime.channel("todo_items_$testEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns flow {
            emit(PostgresAction.Insert(record = newTodoDto))
        }

        // When
        val resultFlow = supabaseTodoRepository.subscribeToTodoUpdates(testEventId)
        val emittedItems = mutableListOf<DomainTodoItem>()
        
        val job = launch {
            resultFlow.take(1).collect { todoItem ->
                emittedItems.add(todoItem)
            }
        }

        advanceUntilIdle()
        job.cancel()

        // Then
        verify { mockRealtime.channel("todo_items_$testEventId") }
        verify { mockChannel.subscribe() }
        
        assertEquals(1, emittedItems.size)
        val emittedItem = emittedItems.first()
        assertEquals("new-todo-id", emittedItem.id)
        assertEquals("New Todo", emittedItem.title)
        assertEquals("New Description", emittedItem.description)
        assertFalse(emittedItem.isCompleted)
    }

    @Test
    fun `subscribeToTodoUpdates should handle DELETE actions`() = runTest {
        // Given
        val deletedTodoDto = TodoItemDto(
            id = "deleted-todo-id",
            event_id = testEventId,
            title = "Deleted Todo",
            description = "This will be deleted",
            is_completed = false,
            assigned_to = "user-id",
            created_by = "creator-user-id",
            created_at = "2024-01-01T10:00:00Z",
            completed_at = null,
            updated_at = "2024-01-01T10:00:00Z"
        )

        every { mockRealtime.channel("todo_items_$testEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns flow {
            emit(PostgresAction.Delete(oldRecord = deletedTodoDto))
        }

        // When
        val resultFlow = supabaseTodoRepository.subscribeToTodoUpdates(testEventId)
        val emittedItems = mutableListOf<DomainTodoItem>()
        
        val job = launch {
            resultFlow.take(1).collect { todoItem ->
                emittedItems.add(todoItem)
            }
        }

        advanceUntilIdle()
        job.cancel()

        // Then
        verify { mockRealtime.channel("todo_items_$testEventId") }
        verify { mockChannel.subscribe() }
        
        assertEquals(1, emittedItems.size)
        val emittedItem = emittedItems.first()
        assertEquals("deleted-todo-id", emittedItem.id)
        assertEquals("Deleted Todo", emittedItem.title)
    }

    @Test
    fun `subscribeToTodoUpdates should handle multiple rapid updates`() = runTest {
        // Given
        val updates = listOf(
            PostgresAction.Insert(
                record = TodoItemDto(
                    id = "todo-1",
                    event_id = testEventId,
                    title = "Todo 1",
                    description = null,
                    is_completed = false,
                    assigned_to = null,
                    created_by = "creator",
                    created_at = "2024-01-01T10:00:00Z",
                    completed_at = null,
                    updated_at = "2024-01-01T10:00:00Z"
                )
            ),
            PostgresAction.Update(
                record = TodoItemDto(
                    id = "todo-1",
                    event_id = testEventId,
                    title = "Todo 1 Updated",
                    description = "Added description",
                    is_completed = true,
                    assigned_to = "user-1",
                    created_by = "creator",
                    created_at = "2024-01-01T10:00:00Z",
                    completed_at = "2024-01-01T10:05:00Z",
                    updated_at = "2024-01-01T10:05:00Z"
                ),
                oldRecord = null
            ),
            PostgresAction.Insert(
                record = TodoItemDto(
                    id = "todo-2",
                    event_id = testEventId,
                    title = "Todo 2",
                    description = "Second todo",
                    is_completed = false,
                    assigned_to = "user-2",
                    created_by = "creator",
                    created_at = "2024-01-01T10:10:00Z",
                    completed_at = null,
                    updated_at = "2024-01-01T10:10:00Z"
                )
            )
        )

        every { mockRealtime.channel("todo_items_$testEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns flow {
            updates.forEach { emit(it) }
        }

        // When
        val resultFlow = supabaseTodoRepository.subscribeToTodoUpdates(testEventId)
        val emittedItems = mutableListOf<DomainTodoItem>()
        
        val job = launch {
            resultFlow.take(3).collect { todoItem ->
                emittedItems.add(todoItem)
            }
        }

        advanceUntilIdle()
        job.cancel()

        // Then
        assertEquals(3, emittedItems.size)
        
        // First update - INSERT
        assertEquals("todo-1", emittedItems[0].id)
        assertEquals("Todo 1", emittedItems[0].title)
        assertFalse(emittedItems[0].isCompleted)
        
        // Second update - UPDATE
        assertEquals("todo-1", emittedItems[1].id)
        assertEquals("Todo 1 Updated", emittedItems[1].title)
        assertTrue(emittedItems[1].isCompleted)
        assertEquals("Added description", emittedItems[1].description)
        
        // Third update - INSERT
        assertEquals("todo-2", emittedItems[2].id)
        assertEquals("Todo 2", emittedItems[2].title)
        assertEquals("Second todo", emittedItems[2].description)
    }

    @Test
    fun `subscribeToTodoUpdates should handle channel subscription errors gracefully`() = runTest {
        // Given
        every { mockRealtime.channel("todo_items_$testEventId") } returns mockChannel
        every { mockChannel.subscribe() } throws RuntimeException("Connection failed")

        // When & Then
        assertThrows(RuntimeException::class.java) {
            runTest {
                supabaseTodoRepository.subscribeToTodoUpdates(testEventId).first()
            }
        }

        verify { mockRealtime.channel("todo_items_$testEventId") }
        verify { mockChannel.subscribe() }
    }

    @Test
    fun `subscribeToTodoUpdates should handle malformed data gracefully`() = runTest {
        // Given
        every { mockRealtime.channel("todo_items_$testEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns flow {
            throw RuntimeException("Malformed data received")
        }

        // When & Then
        assertThrows(RuntimeException::class.java) {
            runTest {
                supabaseTodoRepository.subscribeToTodoUpdates(testEventId).first()
            }
        }

        verify { mockRealtime.channel("todo_items_$testEventId") }
        verify { mockChannel.subscribe() }
    }

    @Test
    fun `subscribeToTodoUpdates should create unique channels for different event IDs`() = runTest {
        // Given
        val eventId1 = "event-1"
        val eventId2 = "event-2"
        val mockChannel1 = mockk<RealtimeChannel>()
        val mockChannel2 = mockk<RealtimeChannel>()

        every { mockRealtime.channel("todo_items_$eventId1") } returns mockChannel1
        every { mockRealtime.channel("todo_items_$eventId2") } returns mockChannel2
        every { mockChannel1.subscribe() } returns Unit
        every { mockChannel2.subscribe() } returns Unit
        coEvery { mockChannel1.postgresChangeFlow<TodoItemDto>(schema = "public") } returns emptyFlow()
        coEvery { mockChannel2.postgresChangeFlow<TodoItemDto>(schema = "public") } returns emptyFlow()

        // When
        val flow1 = supabaseTodoRepository.subscribeToTodoUpdates(eventId1)
        val flow2 = supabaseTodoRepository.subscribeToTodoUpdates(eventId2)

        val job1 = launch { flow1.take(0).collect() }
        val job2 = launch { flow2.take(0).collect() }

        advanceUntilIdle()
        job1.cancel()
        job2.cancel()

        // Then
        verify { mockRealtime.channel("todo_items_$eventId1") }
        verify { mockRealtime.channel("todo_items_$eventId2") }
        verify { mockChannel1.subscribe() }
        verify { mockChannel2.subscribe() }
    }

    @Test
    fun `subscribeToTodoUpdates should handle empty event ID`() = runTest {
        // Given
        val emptyEventId = ""
        every { mockRealtime.channel("todo_items_$emptyEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns emptyFlow()

        // When
        val resultFlow = supabaseTodoRepository.subscribeToTodoUpdates(emptyEventId)
        val job = launch { resultFlow.take(0).collect() }

        advanceUntilIdle()
        job.cancel()

        // Then
        verify { mockRealtime.channel("todo_items_") }
        verify { mockChannel.subscribe() }
    }

    @Test
    fun `subscribeToTodoUpdates should handle special characters in event ID`() = runTest {
        // Given
        val specialEventId = "event-with-special-chars!@#$%"
        every { mockRealtime.channel("todo_items_$specialEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns emptyFlow()

        // When
        val resultFlow = supabaseTodoRepository.subscribeToTodoUpdates(specialEventId)
        val job = launch { resultFlow.take(0).collect() }

        advanceUntilIdle()
        job.cancel()

        // Then
        verify { mockRealtime.channel("todo_items_$specialEventId") }
        verify { mockChannel.subscribe() }
    }

    @Test
    fun `subscribeToTodoUpdates should handle concurrent subscriptions to same event`() = runTest {
        // Given
        every { mockRealtime.channel("todo_items_$testEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns flow {
            emit(
                PostgresAction.Insert(
                    record = TodoItemDto(
                        id = "concurrent-todo",
                        event_id = testEventId,
                        title = "Concurrent Todo",
                        description = null,
                        is_completed = false,
                        assigned_to = null,
                        created_by = "creator",
                        created_at = "2024-01-01T12:00:00Z",
                        completed_at = null,
                        updated_at = "2024-01-01T12:00:00Z"
                    )
                )
            )
        }

        // When
        val flow1 = supabaseTodoRepository.subscribeToTodoUpdates(testEventId)
        val flow2 = supabaseTodoRepository.subscribeToTodoUpdates(testEventId)

        val emittedItems1 = mutableListOf<DomainTodoItem>()
        val emittedItems2 = mutableListOf<DomainTodoItem>()

        val job1 = launch { flow1.take(1).collect { emittedItems1.add(it) } }
        val job2 = launch { flow2.take(1).collect { emittedItems2.add(it) } }

        advanceUntilIdle()
        job1.cancel()
        job2.cancel()

        // Then
        // Both subscriptions should receive the same update
        assertEquals(1, emittedItems1.size)
        assertEquals(1, emittedItems2.size)
        assertEquals("concurrent-todo", emittedItems1.first().id)
        assertEquals("concurrent-todo", emittedItems2.first().id)
        assertEquals("Concurrent Todo", emittedItems1.first().title)
        assertEquals("Concurrent Todo", emittedItems2.first().title)
    }

    @Test
    fun `subscribeToTodoUpdates should handle network reconnection scenarios`() = runTest {
        // Given
        val reconnectionFlow = MutableSharedFlow<PostgresAction<TodoItemDto>>()
        
        every { mockRealtime.channel("todo_items_$testEventId") } returns mockChannel
        every { mockChannel.subscribe() } returns Unit
        coEvery { mockChannel.postgresChangeFlow<TodoItemDto>(schema = "public") } returns reconnectionFlow.asSharedFlow()

        // When
        val resultFlow = supabaseTodoRepository.subscribeToTodoUpdates(testEventId)
        val emittedItems = mutableListOf<DomainTodoItem>()
        
        val job = launch {
            resultFlow.collect { todoItem ->
                emittedItems.add(todoItem)
            }
        }

        // Simulate network disconnection and reconnection with new data
        reconnectionFlow.emit(
            PostgresAction.Insert(
                record = TodoItemDto(
                    id = "reconnect-todo",
                    event_id = testEventId,
                    title = "Reconnection Todo",
                    description = "After reconnection",
                    is_completed = false,
                    assigned_to = null,
                    created_by = "creator",
                    created_at = "2024-01-01T14:00:00Z",
                    completed_at = null,
                    updated_at = "2024-01-01T14:00:00Z"
                )
            )
        )

        advanceUntilIdle()
        job.cancel()

        // Then
        assertEquals(1, emittedItems.size)
        assertEquals("reconnect-todo", emittedItems.first().id)
        assertEquals("Reconnection Todo", emittedItems.first().title)
        assertEquals("After reconnection", emittedItems.first().description)
    }
}