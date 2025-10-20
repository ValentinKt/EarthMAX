package com.earthmax.domain.usecase

import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.TodoRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetTodoItemsByEventUseCaseTest {

    private lateinit var useCase: GetTodoItemsByEventUseCase
    private val mockTodoRepository = mockk<TodoRepository>()

    private val testEventId = "test-event-id"
    private val testTodoId = "test-todo-id"
    private val testUserId = "test-user-id"

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
        useCase = GetTodoItemsByEventUseCase(mockTodoRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should return todo items for valid event ID`() = runTest {
        // Given
        val todoItems = listOf(testDomainTodoItem)
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(todoItems))

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItems, result.getOrNull())
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should return empty list when no todo items exist for event`() = runTest {
        // Given
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should handle repository failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.failure(exception))

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should validate event ID before fetching`() = runTest {
        // Given
        val emptyEventId = ""

        // When
        val result = useCase(emptyEventId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Event ID cannot be empty", result.exceptionOrNull()?.message)
        
        verify(exactly = 0) { mockTodoRepository.getTodoItemsByEvent(any()) }
    }

    @Test
    fun `invoke should handle repository exceptions gracefully`() = runTest {
        // Given
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } throws RuntimeException("Network error")

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("Network error", result.exceptionOrNull()?.message)
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should return multiple todo items sorted by creation date`() = runTest {
        // Given
        val todoItem1 = testDomainTodoItem.copy(
            id = "todo-1",
            title = "First Todo",
            createdAt = Instant.parse("2024-01-01T10:00:00Z")
        )
        val todoItem2 = testDomainTodoItem.copy(
            id = "todo-2",
            title = "Second Todo",
            createdAt = Instant.parse("2024-01-01T11:00:00Z")
        )
        val todoItem3 = testDomainTodoItem.copy(
            id = "todo-3",
            title = "Third Todo",
            createdAt = Instant.parse("2024-01-01T09:00:00Z")
        )
        
        val unsortedTodoItems = listOf(todoItem1, todoItem2, todoItem3)
        val expectedSortedItems = listOf(todoItem3, todoItem1, todoItem2) // Sorted by creation date ascending
        
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(unsortedTodoItems))

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isSuccess)
        val returnedItems = result.getOrNull()!!
        assertEquals(3, returnedItems.size)
        assertEquals(expectedSortedItems, returnedItems)
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should handle mixed completion status todo items`() = runTest {
        // Given
        val completedTodoItem = testDomainTodoItem.copy(
            id = "completed-todo",
            title = "Completed Todo",
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        val incompleteTodoItem = testDomainTodoItem.copy(
            id = "incomplete-todo",
            title = "Incomplete Todo",
            isCompleted = false,
            completedAt = null
        )
        
        val todoItems = listOf(completedTodoItem, incompleteTodoItem)
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(todoItems))

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isSuccess)
        val returnedItems = result.getOrNull()!!
        assertEquals(2, returnedItems.size)
        
        val completed = returnedItems.find { it.isCompleted }
        val incomplete = returnedItems.find { !it.isCompleted }
        
        assertNotNull(completed)
        assertNotNull(incomplete)
        assertEquals("Completed Todo", completed?.title)
        assertEquals("Incomplete Todo", incomplete?.title)
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should handle todo items with different assigned users`() = runTest {
        // Given
        val assignedTodoItem = testDomainTodoItem.copy(
            id = "assigned-todo",
            title = "Assigned Todo",
            assignedTo = "user-1"
        )
        val unassignedTodoItem = testDomainTodoItem.copy(
            id = "unassigned-todo",
            title = "Unassigned Todo",
            assignedTo = null
        )
        
        val todoItems = listOf(assignedTodoItem, unassignedTodoItem)
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(todoItems))

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isSuccess)
        val returnedItems = result.getOrNull()!!
        assertEquals(2, returnedItems.size)
        
        val assigned = returnedItems.find { it.assignedTo != null }
        val unassigned = returnedItems.find { it.assignedTo == null }
        
        assertNotNull(assigned)
        assertNotNull(unassigned)
        assertEquals("user-1", assigned?.assignedTo)
        assertNull(unassigned?.assignedTo)
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should handle large number of todo items efficiently`() = runTest {
        // Given
        val largeTodoList = (1..1000).map { index ->
            testDomainTodoItem.copy(
                id = "todo-$index",
                title = "Todo Item $index",
                createdAt = Instant.parse("2024-01-01T${String.format("%02d", index % 24)}:00:00Z")
            )
        }
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(largeTodoList))

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isSuccess)
        val returnedItems = result.getOrNull()!!
        assertEquals(1000, returnedItems.size)
        assertEquals("Todo Item 1", returnedItems.first().title)
        assertEquals("Todo Item 1000", returnedItems.last().title)
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should trim whitespace from event ID`() = runTest {
        // Given
        val eventIdWithWhitespace = "  $testEventId  "
        val todoItems = listOf(testDomainTodoItem)
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(todoItems))

        // When
        val result = useCase(eventIdWithWhitespace)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItems, result.getOrNull())
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should reject event ID with only whitespace`() = runTest {
        // Given
        val whitespaceEventId = "   "

        // When
        val result = useCase(whitespaceEventId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Event ID cannot be empty", result.exceptionOrNull()?.message)
        
        verify(exactly = 0) { mockTodoRepository.getTodoItemsByEvent(any()) }
    }

    @Test
    fun `invoke should handle special characters in event ID`() = runTest {
        // Given
        val specialEventId = "event-with-special-chars-123!@#"
        val todoItems = listOf(testDomainTodoItem.copy(eventId = specialEventId))
        every { mockTodoRepository.getTodoItemsByEvent(specialEventId) } returns flowOf(Result.success(todoItems))

        // When
        val result = useCase(specialEventId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItems, result.getOrNull())
        
        verify { mockTodoRepository.getTodoItemsByEvent(specialEventId) }
    }

    @Test
    fun `invoke should handle flow collection properly`() = runTest {
        // Given
        val todoItems = listOf(testDomainTodoItem)
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(
            Result.success(emptyList()),
            Result.success(todoItems)
        )

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isSuccess)
        // Should get the first emission from the flow
        assertTrue(result.getOrNull()?.isEmpty() == true)
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }

    @Test
    fun `invoke should preserve todo item properties`() = runTest {
        // Given
        val complexTodoItem = testDomainTodoItem.copy(
            title = "Complex Todo with émojis 🚀",
            description = "Detailed description with\nmultiple lines",
            isCompleted = true,
            assignedTo = "user-with-special-id-123",
            completedAt = Instant.parse("2024-01-01T15:30:45Z"),
            updatedAt = Instant.parse("2024-01-01T16:00:00Z")
        )
        val todoItems = listOf(complexTodoItem)
        every { mockTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(todoItems))

        // When
        val result = useCase(testEventId)

        // Then
        assertTrue(result.isSuccess)
        val returnedItem = result.getOrNull()!!.first()
        assertEquals(complexTodoItem.id, returnedItem.id)
        assertEquals(complexTodoItem.eventId, returnedItem.eventId)
        assertEquals(complexTodoItem.title, returnedItem.title)
        assertEquals(complexTodoItem.description, returnedItem.description)
        assertEquals(complexTodoItem.isCompleted, returnedItem.isCompleted)
        assertEquals(complexTodoItem.assignedTo, returnedItem.assignedTo)
        assertEquals(complexTodoItem.createdBy, returnedItem.createdBy)
        assertEquals(complexTodoItem.createdAt, returnedItem.createdAt)
        assertEquals(complexTodoItem.completedAt, returnedItem.completedAt)
        assertEquals(complexTodoItem.updatedAt, returnedItem.updatedAt)
        
        verify { mockTodoRepository.getTodoItemsByEvent(testEventId) }
    }
}