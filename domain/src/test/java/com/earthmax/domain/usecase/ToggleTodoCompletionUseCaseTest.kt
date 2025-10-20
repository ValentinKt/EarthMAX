package com.earthmax.domain.usecase

import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.TodoRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ToggleTodoCompletionUseCaseTest {

    private lateinit var useCase: ToggleTodoCompletionUseCase
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
        useCase = ToggleTodoCompletionUseCase(mockTodoRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should toggle todo completion successfully`() = runTest {
        // Given
        val completedTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.success(completedTodoItem)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        val toggledItem = result.getOrNull()!!
        assertTrue(toggledItem.isCompleted)
        assertNotNull(toggledItem.completedAt)
        assertEquals(completedTodoItem, toggledItem)
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should handle toggle failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Toggle failed")
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.failure(exception)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should validate todo ID before toggling`() = runTest {
        // Given
        val emptyTodoId = ""

        // When
        val result = useCase(emptyTodoId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Todo ID cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockTodoRepository.toggleTodoCompletion(any()) }
    }

    @Test
    fun `invoke should handle repository exceptions gracefully`() = runTest {
        // Given
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } throws RuntimeException("Database error")

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("Database error", result.exceptionOrNull()?.message)
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should handle toggling from completed to incomplete`() = runTest {
        // Given
        val incompleteTodoItem = testDomainTodoItem.copy(
            isCompleted = false,
            completedAt = null
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.success(incompleteTodoItem)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        val toggledItem = result.getOrNull()!!
        assertFalse(toggledItem.isCompleted)
        assertNull(toggledItem.completedAt)
        assertEquals(incompleteTodoItem, toggledItem)
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should handle toggling from incomplete to completed`() = runTest {
        // Given
        val completedTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T15:30:00Z"),
            updatedAt = Instant.parse("2024-01-01T15:30:00Z")
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.success(completedTodoItem)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        val toggledItem = result.getOrNull()!!
        assertTrue(toggledItem.isCompleted)
        assertEquals(Instant.parse("2024-01-01T15:30:00Z"), toggledItem.completedAt)
        assertEquals(Instant.parse("2024-01-01T15:30:00Z"), toggledItem.updatedAt)
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should preserve other todo item properties during toggle`() = runTest {
        // Given
        val originalTodoItem = testDomainTodoItem.copy(
            title = "Important Todo",
            description = "This is very important",
            assignedTo = "specific-user-id",
            createdBy = "creator-user-id"
        )
        val toggledTodoItem = originalTodoItem.copy(
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.success(toggledTodoItem)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        val resultItem = result.getOrNull()!!
        assertEquals(originalTodoItem.id, resultItem.id)
        assertEquals(originalTodoItem.eventId, resultItem.eventId)
        assertEquals(originalTodoItem.title, resultItem.title)
        assertEquals(originalTodoItem.description, resultItem.description)
        assertEquals(originalTodoItem.assignedTo, resultItem.assignedTo)
        assertEquals(originalTodoItem.createdBy, resultItem.createdBy)
        assertEquals(originalTodoItem.createdAt, resultItem.createdAt)
        assertTrue(resultItem.isCompleted) // This should be toggled
        assertNotNull(resultItem.completedAt) // This should be set
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should trim whitespace from todo ID`() = runTest {
        // Given
        val todoIdWithWhitespace = "  $testTodoId  "
        val completedTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.success(completedTodoItem)

        // When
        val result = useCase(todoIdWithWhitespace)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(completedTodoItem, result.getOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should reject todo ID with only whitespace`() = runTest {
        // Given
        val whitespaceTodoId = "   "

        // When
        val result = useCase(whitespaceTodoId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Todo ID cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockTodoRepository.toggleTodoCompletion(any()) }
    }

    @Test
    fun `invoke should handle special characters in todo ID`() = runTest {
        // Given
        val specialTodoId = "todo-with-special-chars-123!@#"
        val completedTodoItem = testDomainTodoItem.copy(
            id = specialTodoId,
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(specialTodoId) } returns Result.success(completedTodoItem)

        // When
        val result = useCase(specialTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(completedTodoItem, result.getOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(specialTodoId) }
    }

    @Test
    fun `invoke should handle network timeout gracefully`() = runTest {
        // Given
        val timeoutException = RuntimeException("Network timeout")
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.failure(timeoutException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(timeoutException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should handle todo not found error gracefully`() = runTest {
        // Given
        val notFoundException = RuntimeException("Todo item not found")
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.failure(notFoundException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(notFoundException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should handle permission denied error gracefully`() = runTest {
        // Given
        val permissionException = RuntimeException("Permission denied")
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.failure(permissionException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(permissionException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should handle concurrent modification gracefully`() = runTest {
        // Given
        val concurrencyException = RuntimeException("Concurrent modification detected")
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.failure(concurrencyException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(concurrencyException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should handle multiple rapid toggles gracefully`() = runTest {
        // Given
        val completedTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.success(completedTodoItem)

        // When - Simulate rapid toggles
        val result1 = useCase(testTodoId)
        val result2 = useCase(testTodoId)
        val result3 = useCase(testTodoId)

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertTrue(result3.isSuccess)
        
        coVerify(exactly = 3) { mockTodoRepository.toggleTodoCompletion(testTodoId) }
    }

    @Test
    fun `invoke should handle very long todo ID gracefully`() = runTest {
        // Given
        val longTodoId = "a".repeat(1000)
        val completedTodoItem = testDomainTodoItem.copy(
            id = longTodoId,
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(longTodoId) } returns Result.success(completedTodoItem)

        // When
        val result = useCase(longTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(completedTodoItem, result.getOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(longTodoId) }
    }

    @Test
    fun `invoke should handle UUID format todo ID`() = runTest {
        // Given
        val uuidTodoId = "550e8400-e29b-41d4-a716-446655440000"
        val completedTodoItem = testDomainTodoItem.copy(
            id = uuidTodoId,
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockTodoRepository.toggleTodoCompletion(uuidTodoId) } returns Result.success(completedTodoItem)

        // When
        val result = useCase(uuidTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(completedTodoItem, result.getOrNull())
        
        coVerify { mockTodoRepository.toggleTodoCompletion(uuidTodoId) }
    }
}