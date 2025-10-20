package com.earthmax.data.todo

import com.earthmax.core.network.SupabaseClient
import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.realtime.Realtime
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseTodoRepositoryTest {

    private lateinit var repository: SupabaseTodoRepository
    private val mockSupabaseClient = mockk<io.github.jan.supabase.SupabaseClient>()
    private val mockPostgrest = mockk<Postgrest>()
    private val mockRealtime = mockk<Realtime>()
    private val mockQueryBuilder = mockk<PostgrestQueryBuilder>()

    private val testEventId = "test-event-id"
    private val testTodoId = "test-todo-id"
    private val testUserId = "test-user-id"

    private val testTodoItemDto = TodoItemDto(
        id = testTodoId,
        event_id = testEventId,
        title = "Test Todo",
        description = "Test Description",
        is_completed = false,
        assigned_to = testUserId,
        created_by = testUserId,
        created_at = "2024-01-01T00:00:00Z",
        completed_at = null,
        updated_at = "2024-01-01T00:00:00Z"
    )

    private val testDomainTodoItem = DomainTodoItem(
        id = testTodoId,
        eventId = testEventId,
        title = "Test Todo",
        description = "Test Description",
        isCompleted = false,
        assignedTo = testUserId,
        createdBy = testUserId,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        completedAt = null,
        updatedAt = Instant.parse("2024-01-01T00:00:00Z")
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        // Mock SupabaseClient static access
        mockkObject(SupabaseClient)
        every { SupabaseClient.client } returns mockSupabaseClient
        
        // Mock Supabase client components
        every { mockSupabaseClient.pluginManager } returns mockk()
        every { mockSupabaseClient.from(any()) } returns mockQueryBuilder
        every { mockSupabaseClient.realtime } returns mockRealtime
        
        repository = SupabaseTodoRepository()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getTodoItemsByEvent should return success with todo items when API call succeeds`() = runTest {
        // Given
        val todoItemsList = listOf(testTodoItemDto)
        every { mockQueryBuilder.select() } returns mockQueryBuilder
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeList<TodoItemDto>() } returns todoItemsList

        // When
        val result = repository.getTodoItemsByEvent(testEventId).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(testDomainTodoItem.title, result.getOrNull()?.first()?.title)
        
        verify { mockQueryBuilder.select() }
        verify { mockQueryBuilder.eq("event_id", testEventId) }
        coVerify { mockQueryBuilder.decodeList<TodoItemDto>() }
    }

    @Test
    fun `getTodoItemsByEvent should return failure when API call throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Network error")
        every { mockQueryBuilder.select() } returns mockQueryBuilder
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeList<TodoItemDto>() } throws exception

        // When
        val result = repository.getTodoItemsByEvent(testEventId).first()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getTodoItemById should return success with todo item when API call succeeds`() = runTest {
        // Given
        every { mockQueryBuilder.select() } returns mockQueryBuilder
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeSingle<TodoItemDto>() } returns testTodoItemDto

        // When
        val result = repository.getTodoItemById(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDomainTodoItem.title, result.getOrNull()?.title)
        assertEquals(testDomainTodoItem.id, result.getOrNull()?.id)
        
        verify { mockQueryBuilder.select() }
        verify { mockQueryBuilder.eq("id", testTodoId) }
        coVerify { mockQueryBuilder.decodeSingle<TodoItemDto>() }
    }

    @Test
    fun `getTodoItemById should return failure when API call throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Todo not found")
        every { mockQueryBuilder.select() } returns mockQueryBuilder
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeSingle<TodoItemDto>() } throws exception

        // When
        val result = repository.getTodoItemById(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `createTodoItem should return success with created todo item when API call succeeds`() = runTest {
        // Given
        coEvery { mockQueryBuilder.insert(any<TodoItemDto>()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeSingle<TodoItemDto>() } returns testTodoItemDto

        // When
        val result = repository.createTodoItem(testDomainTodoItem)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDomainTodoItem.title, result.getOrNull()?.title)
        
        coVerify { mockQueryBuilder.insert(any<TodoItemDto>()) }
        coVerify { mockQueryBuilder.decodeSingle<TodoItemDto>() }
    }

    @Test
    fun `createTodoItem should return failure when API call throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Creation failed")
        coEvery { mockQueryBuilder.insert(any<TodoItemDto>()) } throws exception

        // When
        val result = repository.createTodoItem(testDomainTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `updateTodoItem should return success with updated todo item when API call succeeds`() = runTest {
        // Given
        val updatedTodoItemDto = testTodoItemDto.copy(title = "Updated Title")
        val updatedDomainTodoItem = testDomainTodoItem.copy(title = "Updated Title")
        
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.update(any<TodoItemDto>()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeSingle<TodoItemDto>() } returns updatedTodoItemDto

        // When
        val result = repository.updateTodoItem(updatedDomainTodoItem)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Updated Title", result.getOrNull()?.title)
        
        verify { mockQueryBuilder.eq("id", testTodoId) }
        coVerify { mockQueryBuilder.update(any<TodoItemDto>()) }
        coVerify { mockQueryBuilder.decodeSingle<TodoItemDto>() }
    }

    @Test
    fun `updateTodoItem should return failure when API call throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Update failed")
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.update(any<TodoItemDto>()) } throws exception

        // When
        val result = repository.updateTodoItem(testDomainTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `deleteTodoItem should return success when API call succeeds`() = runTest {
        // Given
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.delete() } returns mockQueryBuilder

        // When
        val result = repository.deleteTodoItem(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        
        verify { mockQueryBuilder.eq("id", testTodoId) }
        coVerify { mockQueryBuilder.delete() }
    }

    @Test
    fun `deleteTodoItem should return failure when API call throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Delete failed")
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.delete() } throws exception

        // When
        val result = repository.deleteTodoItem(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `toggleTodoCompletion should return success with toggled todo item when API call succeeds`() = runTest {
        // Given
        val completedTodoItemDto = testTodoItemDto.copy(
            is_completed = true,
            completed_at = "2024-01-01T12:00:00Z"
        )
        
        // First call to get current item
        every { mockQueryBuilder.select() } returns mockQueryBuilder
        every { mockQueryBuilder.eq("id", testTodoId) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeSingle<TodoItemDto>() } returnsMany listOf(
            testTodoItemDto, // First call to get current state
            completedTodoItemDto // Second call after update
        )
        
        // Update call
        coEvery { mockQueryBuilder.update(any<TodoItemDto>()) } returns mockQueryBuilder

        // When
        val result = repository.toggleTodoCompletion(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isCompleted == true)
        
        verify(exactly = 2) { mockQueryBuilder.select() }
        verify(exactly = 3) { mockQueryBuilder.eq("id", testTodoId) }
        coVerify { mockQueryBuilder.update(any<TodoItemDto>()) }
    }

    @Test
    fun `toggleTodoCompletion should return failure when API call throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Toggle failed")
        every { mockQueryBuilder.select() } returns mockQueryBuilder
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeSingle<TodoItemDto>() } throws exception

        // When
        val result = repository.toggleTodoCompletion(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `assignTodoItem should return success with assigned todo item when API call succeeds`() = runTest {
        // Given
        val assignedTodoItemDto = testTodoItemDto.copy(assigned_to = "new-user-id")
        
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.update(any<TodoItemDto>()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeSingle<TodoItemDto>() } returns assignedTodoItemDto

        // When
        val result = repository.assignTodoItem(testTodoId, "new-user-id")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("new-user-id", result.getOrNull()?.assignedTo)
        
        verify { mockQueryBuilder.eq("id", testTodoId) }
        coVerify { mockQueryBuilder.update(any<TodoItemDto>()) }
        coVerify { mockQueryBuilder.decodeSingle<TodoItemDto>() }
    }

    @Test
    fun `assignTodoItem should return failure when API call throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Assignment failed")
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.update(any<TodoItemDto>()) } throws exception

        // When
        val result = repository.assignTodoItem(testTodoId, "new-user-id")

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getTodoItemsByUser should return success with user's todo items when API call succeeds`() = runTest {
        // Given
        val todoItemsList = listOf(testTodoItemDto)
        every { mockQueryBuilder.select() } returns mockQueryBuilder
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeList<TodoItemDto>() } returns todoItemsList

        // When
        val result = repository.getTodoItemsByUser(testUserId).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(testDomainTodoItem.title, result.getOrNull()?.first()?.title)
        
        verify { mockQueryBuilder.select() }
        verify { mockQueryBuilder.eq("assigned_to", testUserId) }
        coVerify { mockQueryBuilder.decodeList<TodoItemDto>() }
    }

    @Test
    fun `getTodoItemsByUser should return failure when API call throws exception`() = runTest {
        // Given
        val exception = RuntimeException("User todos fetch failed")
        every { mockQueryBuilder.select() } returns mockQueryBuilder
        every { mockQueryBuilder.eq(any(), any()) } returns mockQueryBuilder
        coEvery { mockQueryBuilder.decodeList<TodoItemDto>() } throws exception

        // When
        val result = repository.getTodoItemsByUser(testUserId).first()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `toDomainTodoItem should correctly map TodoItemDto to DomainTodoItem`() {
        // Given
        val todoItemDto = TodoItemDto(
            id = "test-id",
            event_id = "test-event",
            title = "Test Title",
            description = "Test Description",
            is_completed = true,
            assigned_to = "test-user",
            created_by = "creator-user",
            created_at = "2024-01-01T00:00:00Z",
            completed_at = "2024-01-01T12:00:00Z",
            updated_at = "2024-01-01T12:00:00Z"
        )

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertEquals("test-id", domainTodoItem.id)
        assertEquals("test-event", domainTodoItem.eventId)
        assertEquals("Test Title", domainTodoItem.title)
        assertEquals("Test Description", domainTodoItem.description)
        assertTrue(domainTodoItem.isCompleted)
        assertEquals("test-user", domainTodoItem.assignedTo)
        assertEquals("creator-user", domainTodoItem.createdBy)
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), domainTodoItem.createdAt)
        assertEquals(Instant.parse("2024-01-01T12:00:00Z"), domainTodoItem.completedAt)
        assertEquals(Instant.parse("2024-01-01T12:00:00Z"), domainTodoItem.updatedAt)
    }

    @Test
    fun `toTodoItemDto should correctly map DomainTodoItem to TodoItemDto`() {
        // Given
        val domainTodoItem = DomainTodoItem(
            id = "test-id",
            eventId = "test-event",
            title = "Test Title",
            description = "Test Description",
            isCompleted = true,
            assignedTo = "test-user",
            createdBy = "creator-user",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            completedAt = Instant.parse("2024-01-01T12:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T12:00:00Z")
        )

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals("test-id", todoItemDto.id)
        assertEquals("test-event", todoItemDto.event_id)
        assertEquals("Test Title", todoItemDto.title)
        assertEquals("Test Description", todoItemDto.description)
        assertTrue(todoItemDto.is_completed)
        assertEquals("test-user", todoItemDto.assigned_to)
        assertEquals("creator-user", todoItemDto.created_by)
        assertEquals("2024-01-01T00:00:00Z", todoItemDto.created_at)
        assertEquals("2024-01-01T12:00:00Z", todoItemDto.completed_at)
        assertEquals("2024-01-01T12:00:00Z", todoItemDto.updated_at)
    }
}