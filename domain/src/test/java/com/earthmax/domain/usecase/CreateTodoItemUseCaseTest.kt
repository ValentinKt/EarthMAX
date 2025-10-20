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
class CreateTodoItemUseCaseTest {

    private lateinit var useCase: CreateTodoItemUseCase
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
        useCase = CreateTodoItemUseCase(mockTodoRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should create todo item successfully`() = runTest {
        // Given
        coEvery { mockTodoRepository.createTodoItem(testDomainTodoItem) } returns Result.success(testDomainTodoItem)

        // When
        val result = useCase(testDomainTodoItem)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDomainTodoItem, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(testDomainTodoItem) }
    }

    @Test
    fun `invoke should handle creation failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Creation failed")
        coEvery { mockTodoRepository.createTodoItem(testDomainTodoItem) } returns Result.failure(exception)

        // When
        val result = useCase(testDomainTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(testDomainTodoItem) }
    }

    @Test
    fun `invoke should validate todo item before creation`() = runTest {
        // Given
        val invalidTodoItem = testDomainTodoItem.copy(title = "")
        coEvery { mockTodoRepository.createTodoItem(any()) } returns Result.success(invalidTodoItem)

        // When
        val result = useCase(invalidTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Todo title cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockTodoRepository.createTodoItem(any()) }
    }

    @Test
    fun `invoke should validate event ID before creation`() = runTest {
        // Given
        val invalidTodoItem = testDomainTodoItem.copy(eventId = "")

        // When
        val result = useCase(invalidTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Event ID cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockTodoRepository.createTodoItem(any()) }
    }

    @Test
    fun `invoke should validate created by user ID before creation`() = runTest {
        // Given
        val invalidTodoItem = testDomainTodoItem.copy(createdBy = "")

        // When
        val result = useCase(invalidTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Created by user ID cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockTodoRepository.createTodoItem(any()) }
    }

    @Test
    fun `invoke should allow empty description`() = runTest {
        // Given
        val todoItemWithEmptyDescription = testDomainTodoItem.copy(description = "")
        coEvery { mockTodoRepository.createTodoItem(todoItemWithEmptyDescription) } returns Result.success(todoItemWithEmptyDescription)

        // When
        val result = useCase(todoItemWithEmptyDescription)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItemWithEmptyDescription, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(todoItemWithEmptyDescription) }
    }

    @Test
    fun `invoke should allow null description`() = runTest {
        // Given
        val todoItemWithNullDescription = testDomainTodoItem.copy(description = null)
        coEvery { mockTodoRepository.createTodoItem(todoItemWithNullDescription) } returns Result.success(todoItemWithNullDescription)

        // When
        val result = useCase(todoItemWithNullDescription)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItemWithNullDescription, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(todoItemWithNullDescription) }
    }

    @Test
    fun `invoke should allow null assigned to user`() = runTest {
        // Given
        val unassignedTodoItem = testDomainTodoItem.copy(assignedTo = null)
        coEvery { mockTodoRepository.createTodoItem(unassignedTodoItem) } returns Result.success(unassignedTodoItem)

        // When
        val result = useCase(unassignedTodoItem)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(unassignedTodoItem, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(unassignedTodoItem) }
    }

    @Test
    fun `invoke should handle repository exceptions gracefully`() = runTest {
        // Given
        coEvery { mockTodoRepository.createTodoItem(testDomainTodoItem) } throws RuntimeException("Database error")

        // When
        val result = useCase(testDomainTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("Database error", result.exceptionOrNull()?.message)
        
        coVerify { mockTodoRepository.createTodoItem(testDomainTodoItem) }
    }

    @Test
    fun `invoke should trim whitespace from title`() = runTest {
        // Given
        val todoItemWithWhitespace = testDomainTodoItem.copy(title = "  Test Todo  ")
        val expectedTodoItem = todoItemWithWhitespace.copy(title = "Test Todo")
        coEvery { mockTodoRepository.createTodoItem(expectedTodoItem) } returns Result.success(expectedTodoItem)

        // When
        val result = useCase(todoItemWithWhitespace)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTodoItem, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(expectedTodoItem) }
    }

    @Test
    fun `invoke should trim whitespace from description`() = runTest {
        // Given
        val todoItemWithWhitespace = testDomainTodoItem.copy(description = "  Test Description  ")
        val expectedTodoItem = todoItemWithWhitespace.copy(description = "Test Description")
        coEvery { mockTodoRepository.createTodoItem(expectedTodoItem) } returns Result.success(expectedTodoItem)

        // When
        val result = useCase(todoItemWithWhitespace)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTodoItem, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(expectedTodoItem) }
    }

    @Test
    fun `invoke should handle null description trimming gracefully`() = runTest {
        // Given
        val todoItemWithNullDescription = testDomainTodoItem.copy(description = null)
        coEvery { mockTodoRepository.createTodoItem(todoItemWithNullDescription) } returns Result.success(todoItemWithNullDescription)

        // When
        val result = useCase(todoItemWithNullDescription)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItemWithNullDescription, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(todoItemWithNullDescription) }
    }

    @Test
    fun `invoke should reject title with only whitespace`() = runTest {
        // Given
        val todoItemWithWhitespaceTitle = testDomainTodoItem.copy(title = "   ")

        // When
        val result = useCase(todoItemWithWhitespaceTitle)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Todo title cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockTodoRepository.createTodoItem(any()) }
    }

    @Test
    fun `invoke should handle very long titles gracefully`() = runTest {
        // Given
        val longTitle = "A".repeat(1000)
        val todoItemWithLongTitle = testDomainTodoItem.copy(title = longTitle)
        coEvery { mockTodoRepository.createTodoItem(todoItemWithLongTitle) } returns Result.success(todoItemWithLongTitle)

        // When
        val result = useCase(todoItemWithLongTitle)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItemWithLongTitle, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(todoItemWithLongTitle) }
    }

    @Test
    fun `invoke should handle special characters in title`() = runTest {
        // Given
        val specialTitle = "Todo with émojis 🚀 and spëcial chars!"
        val todoItemWithSpecialTitle = testDomainTodoItem.copy(title = specialTitle)
        coEvery { mockTodoRepository.createTodoItem(todoItemWithSpecialTitle) } returns Result.success(todoItemWithSpecialTitle)

        // When
        val result = useCase(todoItemWithSpecialTitle)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItemWithSpecialTitle, result.getOrNull())
        
        coVerify { mockTodoRepository.createTodoItem(todoItemWithSpecialTitle) }
    }

    @Test
    fun `invoke should preserve todo item properties during creation`() = runTest {
        // Given
        val completedTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockTodoRepository.createTodoItem(completedTodoItem) } returns Result.success(completedTodoItem)

        // When
        val result = useCase(completedTodoItem)

        // Then
        assertTrue(result.isSuccess)
        val createdItem = result.getOrNull()!!
        assertEquals(completedTodoItem.id, createdItem.id)
        assertEquals(completedTodoItem.eventId, createdItem.eventId)
        assertEquals(completedTodoItem.title, createdItem.title)
        assertEquals(completedTodoItem.description, createdItem.description)
        assertEquals(completedTodoItem.isCompleted, createdItem.isCompleted)
        assertEquals(completedTodoItem.assignedTo, createdItem.assignedTo)
        assertEquals(completedTodoItem.createdBy, createdItem.createdBy)
        assertEquals(completedTodoItem.createdAt, createdItem.createdAt)
        assertEquals(completedTodoItem.completedAt, createdItem.completedAt)
        assertEquals(completedTodoItem.updatedAt, createdItem.updatedAt)
        
        coVerify { mockTodoRepository.createTodoItem(completedTodoItem) }
    }
}