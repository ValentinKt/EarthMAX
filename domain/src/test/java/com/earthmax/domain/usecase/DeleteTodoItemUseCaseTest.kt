package com.earthmax.domain.usecase

import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.TodoRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteTodoItemUseCaseTest {

    private lateinit var useCase: DeleteTodoItemUseCase
    private val mockTodoRepository = mockk<TodoRepository>()

    private val testTodoId = "test-todo-id"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = DeleteTodoItemUseCase(mockTodoRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should delete todo item successfully`() = runTest {
        // Given
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.success(Unit)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle deletion failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Deletion failed")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(exception)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should validate todo ID before deletion`() = runTest {
        // Given
        val emptyTodoId = ""

        // When
        val result = useCase(emptyTodoId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Todo ID cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockTodoRepository.deleteTodoItem(any()) }
    }

    @Test
    fun `invoke should handle repository exceptions gracefully`() = runTest {
        // Given
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } throws RuntimeException("Database error")

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("Database error", result.exceptionOrNull()?.message)
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should trim whitespace from todo ID`() = runTest {
        // Given
        val todoIdWithWhitespace = "  $testTodoId  "
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.success(Unit)

        // When
        val result = useCase(todoIdWithWhitespace)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
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
        
        coVerify(exactly = 0) { mockTodoRepository.deleteTodoItem(any()) }
    }

    @Test
    fun `invoke should handle special characters in todo ID`() = runTest {
        // Given
        val specialTodoId = "todo-with-special-chars-123!@#"
        coEvery { mockTodoRepository.deleteTodoItem(specialTodoId) } returns Result.success(Unit)

        // When
        val result = useCase(specialTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(specialTodoId) }
    }

    @Test
    fun `invoke should handle network timeout gracefully`() = runTest {
        // Given
        val timeoutException = RuntimeException("Network timeout")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(timeoutException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(timeoutException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle todo not found error gracefully`() = runTest {
        // Given
        val notFoundException = RuntimeException("Todo item not found")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(notFoundException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(notFoundException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle permission denied error gracefully`() = runTest {
        // Given
        val permissionException = RuntimeException("Permission denied")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(permissionException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(permissionException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle concurrent modification gracefully`() = runTest {
        // Given
        val concurrencyException = RuntimeException("Concurrent modification detected")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(concurrencyException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(concurrencyException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle multiple rapid deletions gracefully`() = runTest {
        // Given
        val todoId1 = "todo-1"
        val todoId2 = "todo-2"
        val todoId3 = "todo-3"
        coEvery { mockTodoRepository.deleteTodoItem(todoId1) } returns Result.success(Unit)
        coEvery { mockTodoRepository.deleteTodoItem(todoId2) } returns Result.success(Unit)
        coEvery { mockTodoRepository.deleteTodoItem(todoId3) } returns Result.success(Unit)

        // When - Simulate rapid deletions
        val result1 = useCase(todoId1)
        val result2 = useCase(todoId2)
        val result3 = useCase(todoId3)

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertTrue(result3.isSuccess)
        
        coVerify { mockTodoRepository.deleteTodoItem(todoId1) }
        coVerify { mockTodoRepository.deleteTodoItem(todoId2) }
        coVerify { mockTodoRepository.deleteTodoItem(todoId3) }
    }

    @Test
    fun `invoke should handle very long todo ID gracefully`() = runTest {
        // Given
        val longTodoId = "a".repeat(1000)
        coEvery { mockTodoRepository.deleteTodoItem(longTodoId) } returns Result.success(Unit)

        // When
        val result = useCase(longTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(longTodoId) }
    }

    @Test
    fun `invoke should handle UUID format todo ID`() = runTest {
        // Given
        val uuidTodoId = "550e8400-e29b-41d4-a716-446655440000"
        coEvery { mockTodoRepository.deleteTodoItem(uuidTodoId) } returns Result.success(Unit)

        // When
        val result = useCase(uuidTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(uuidTodoId) }
    }

    @Test
    fun `invoke should handle database constraint violation gracefully`() = runTest {
        // Given
        val constraintException = RuntimeException("Foreign key constraint violation")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(constraintException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(constraintException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle server error gracefully`() = runTest {
        // Given
        val serverException = RuntimeException("Internal server error")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(serverException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(serverException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle authentication error gracefully`() = runTest {
        // Given
        val authException = RuntimeException("Authentication required")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(authException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(authException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle null todo ID gracefully`() = runTest {
        // Given
        val nullTodoId: String? = null

        // When
        val result = useCase(nullTodoId ?: "")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Todo ID cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockTodoRepository.deleteTodoItem(any()) }
    }

    @Test
    fun `invoke should handle deletion of already deleted item gracefully`() = runTest {
        // Given
        val alreadyDeletedException = RuntimeException("Todo item already deleted")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(alreadyDeletedException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(alreadyDeletedException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle deletion with insufficient privileges gracefully`() = runTest {
        // Given
        val privilegeException = RuntimeException("Insufficient privileges to delete todo item")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(privilegeException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(privilegeException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }

    @Test
    fun `invoke should handle deletion during maintenance mode gracefully`() = runTest {
        // Given
        val maintenanceException = RuntimeException("Service temporarily unavailable")
        coEvery { mockTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(maintenanceException)

        // When
        val result = useCase(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(maintenanceException, result.exceptionOrNull())
        
        coVerify { mockTodoRepository.deleteTodoItem(testTodoId) }
    }
}