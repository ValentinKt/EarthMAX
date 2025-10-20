package com.earthmax.data.repository

import com.earthmax.core.cache.CacheManager
import com.earthmax.core.error.ErrorHandler
import com.earthmax.core.monitoring.Logger
import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.data.todo.SupabaseTodoRepository
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
class TodoRepositoryImplTest {

    private lateinit var repository: TodoRepositoryImpl
    private val mockSupabaseTodoRepository = mockk<SupabaseTodoRepository>()
    private val mockCacheManager = mockk<CacheManager>()
    private val mockErrorHandler = mockk<ErrorHandler>()
    private val mockMetricsCollector = mockk<MetricsCollector>()

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
        
        // Mock Logger static calls
        mockkObject(Logger)
        every { Logger.enter(any(), any(), *anyVararg()) } just Runs
        every { Logger.exit(any(), any()) } just Runs
        every { Logger.d(any(), any()) } just Runs
        every { Logger.e(any(), any(), any()) } just Runs

        // Mock MetricsCollector static calls
        mockkObject(MetricsCollector)
        every { MetricsCollector.recordApiCall(any(), any(), any()) } just Runs

        repository = TodoRepositoryImpl(
            supabaseTodoRepository = mockSupabaseTodoRepository,
            cacheManager = mockCacheManager,
            errorHandler = mockErrorHandler,
            metricsCollector = mockMetricsCollector
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getTodoItemsByEvent should return cached data when available and not expired`() = runTest {
        // Given
        val cachedTodoItems = listOf(testDomainTodoItem)
        every { mockCacheManager.get<List<DomainTodoItem>>(any()) } returns cachedTodoItems

        // When
        val result = repository.getTodoItemsByEvent(testEventId).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(cachedTodoItems, result.getOrNull())
        
        verify { mockCacheManager.get<List<DomainTodoItem>>("todo_items_event_$testEventId") }
        verify(exactly = 0) { mockSupabaseTodoRepository.getTodoItemsByEvent(any()) }
    }

    @Test
    fun `getTodoItemsByEvent should fetch from remote when cache is empty`() = runTest {
        // Given
        val remoteTodoItems = listOf(testDomainTodoItem)
        every { mockCacheManager.get<List<DomainTodoItem>>(any()) } returns null
        every { mockSupabaseTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.success(remoteTodoItems))
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        val result = repository.getTodoItemsByEvent(testEventId).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(remoteTodoItems, result.getOrNull())
        
        verify { mockCacheManager.get<List<DomainTodoItem>>("todo_items_event_$testEventId") }
        verify { mockSupabaseTodoRepository.getTodoItemsByEvent(testEventId) }
        verify { mockCacheManager.put("todo_items_event_$testEventId", remoteTodoItems, any()) }
    }

    @Test
    fun `getTodoItemsByEvent should handle remote fetch failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Network error")
        every { mockCacheManager.get<List<DomainTodoItem>>(any()) } returns null
        every { mockSupabaseTodoRepository.getTodoItemsByEvent(testEventId) } returns flowOf(Result.failure(exception))
        every { mockErrorHandler.handleError(any(), any()) } returns Result.failure(exception)

        // When
        val result = repository.getTodoItemsByEvent(testEventId).first()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        verify { mockSupabaseTodoRepository.getTodoItemsByEvent(testEventId) }
        verify { mockErrorHandler.handleError(exception, "getTodoItemsByEvent") }
    }

    @Test
    fun `getTodoItemById should return cached data when available`() = runTest {
        // Given
        every { mockCacheManager.get<DomainTodoItem>(any()) } returns testDomainTodoItem

        // When
        val result = repository.getTodoItemById(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDomainTodoItem, result.getOrNull())
        
        verify { mockCacheManager.get<DomainTodoItem>("todo_item_$testTodoId") }
        coVerify(exactly = 0) { mockSupabaseTodoRepository.getTodoItemById(any()) }
    }

    @Test
    fun `getTodoItemById should fetch from remote when cache is empty`() = runTest {
        // Given
        every { mockCacheManager.get<DomainTodoItem>(any()) } returns null
        coEvery { mockSupabaseTodoRepository.getTodoItemById(testTodoId) } returns Result.success(testDomainTodoItem)
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        val result = repository.getTodoItemById(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDomainTodoItem, result.getOrNull())
        
        verify { mockCacheManager.get<DomainTodoItem>("todo_item_$testTodoId") }
        coVerify { mockSupabaseTodoRepository.getTodoItemById(testTodoId) }
        verify { mockCacheManager.put("todo_item_$testTodoId", testDomainTodoItem, any()) }
    }

    @Test
    fun `getTodoItemById should handle remote fetch failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Todo not found")
        every { mockCacheManager.get<DomainTodoItem>(any()) } returns null
        coEvery { mockSupabaseTodoRepository.getTodoItemById(testTodoId) } returns Result.failure(exception)
        every { mockErrorHandler.handleError(any(), any()) } returns Result.failure(exception)

        // When
        val result = repository.getTodoItemById(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockSupabaseTodoRepository.getTodoItemById(testTodoId) }
        verify { mockErrorHandler.handleError(exception, "getTodoItemById") }
    }

    @Test
    fun `createTodoItem should create item and invalidate cache on success`() = runTest {
        // Given
        coEvery { mockSupabaseTodoRepository.createTodoItem(testDomainTodoItem) } returns Result.success(testDomainTodoItem)
        every { mockCacheManager.invalidate(any()) } just Runs
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        val result = repository.createTodoItem(testDomainTodoItem)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDomainTodoItem, result.getOrNull())
        
        coVerify { mockSupabaseTodoRepository.createTodoItem(testDomainTodoItem) }
        verify { mockCacheManager.invalidate("todo_items_event_${testDomainTodoItem.eventId}") }
        verify { mockCacheManager.put("todo_item_${testDomainTodoItem.id}", testDomainTodoItem, any()) }
    }

    @Test
    fun `createTodoItem should handle creation failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Creation failed")
        coEvery { mockSupabaseTodoRepository.createTodoItem(testDomainTodoItem) } returns Result.failure(exception)
        every { mockErrorHandler.handleError(any(), any()) } returns Result.failure(exception)

        // When
        val result = repository.createTodoItem(testDomainTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockSupabaseTodoRepository.createTodoItem(testDomainTodoItem) }
        verify { mockErrorHandler.handleError(exception, "createTodoItem") }
        verify(exactly = 0) { mockCacheManager.invalidate(any()) }
    }

    @Test
    fun `updateTodoItem should update item and invalidate cache on success`() = runTest {
        // Given
        val updatedTodoItem = testDomainTodoItem.copy(title = "Updated Title")
        coEvery { mockSupabaseTodoRepository.updateTodoItem(updatedTodoItem) } returns Result.success(updatedTodoItem)
        every { mockCacheManager.invalidate(any()) } just Runs
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        val result = repository.updateTodoItem(updatedTodoItem)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(updatedTodoItem, result.getOrNull())
        
        coVerify { mockSupabaseTodoRepository.updateTodoItem(updatedTodoItem) }
        verify { mockCacheManager.invalidate("todo_items_event_${updatedTodoItem.eventId}") }
        verify { mockCacheManager.put("todo_item_${updatedTodoItem.id}", updatedTodoItem, any()) }
    }

    @Test
    fun `updateTodoItem should handle update failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Update failed")
        coEvery { mockSupabaseTodoRepository.updateTodoItem(testDomainTodoItem) } returns Result.failure(exception)
        every { mockErrorHandler.handleError(any(), any()) } returns Result.failure(exception)

        // When
        val result = repository.updateTodoItem(testDomainTodoItem)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockSupabaseTodoRepository.updateTodoItem(testDomainTodoItem) }
        verify { mockErrorHandler.handleError(exception, "updateTodoItem") }
        verify(exactly = 0) { mockCacheManager.invalidate(any()) }
    }

    @Test
    fun `deleteTodoItem should delete item and invalidate cache on success`() = runTest {
        // Given
        coEvery { mockSupabaseTodoRepository.deleteTodoItem(testTodoId) } returns Result.success(Unit)
        every { mockCacheManager.invalidate(any()) } just Runs

        // When
        val result = repository.deleteTodoItem(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        
        coVerify { mockSupabaseTodoRepository.deleteTodoItem(testTodoId) }
        verify { mockCacheManager.invalidate("todo_item_$testTodoId") }
    }

    @Test
    fun `deleteTodoItem should handle deletion failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Delete failed")
        coEvery { mockSupabaseTodoRepository.deleteTodoItem(testTodoId) } returns Result.failure(exception)
        every { mockErrorHandler.handleError(any(), any()) } returns Result.failure(exception)

        // When
        val result = repository.deleteTodoItem(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockSupabaseTodoRepository.deleteTodoItem(testTodoId) }
        verify { mockErrorHandler.handleError(exception, "deleteTodoItem") }
        verify(exactly = 0) { mockCacheManager.invalidate(any()) }
    }

    @Test
    fun `toggleTodoCompletion should toggle completion and invalidate cache on success`() = runTest {
        // Given
        val completedTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockSupabaseTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.success(completedTodoItem)
        every { mockCacheManager.invalidate(any()) } just Runs
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        val result = repository.toggleTodoCompletion(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isCompleted == true)
        
        coVerify { mockSupabaseTodoRepository.toggleTodoCompletion(testTodoId) }
        verify { mockCacheManager.invalidate("todo_items_event_${completedTodoItem.eventId}") }
        verify { mockCacheManager.put("todo_item_${completedTodoItem.id}", completedTodoItem, any()) }
    }

    @Test
    fun `toggleTodoCompletion should handle toggle failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Toggle failed")
        coEvery { mockSupabaseTodoRepository.toggleTodoCompletion(testTodoId) } returns Result.failure(exception)
        every { mockErrorHandler.handleError(any(), any()) } returns Result.failure(exception)

        // When
        val result = repository.toggleTodoCompletion(testTodoId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockSupabaseTodoRepository.toggleTodoCompletion(testTodoId) }
        verify { mockErrorHandler.handleError(exception, "toggleTodoCompletion") }
        verify(exactly = 0) { mockCacheManager.invalidate(any()) }
    }

    @Test
    fun `assignTodoItem should assign item and invalidate cache on success`() = runTest {
        // Given
        val assignedTodoItem = testDomainTodoItem.copy(assignedTo = "new-user-id")
        coEvery { mockSupabaseTodoRepository.assignTodoItem(testTodoId, "new-user-id") } returns Result.success(assignedTodoItem)
        every { mockCacheManager.invalidate(any()) } just Runs
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        val result = repository.assignTodoItem(testTodoId, "new-user-id")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("new-user-id", result.getOrNull()?.assignedTo)
        
        coVerify { mockSupabaseTodoRepository.assignTodoItem(testTodoId, "new-user-id") }
        verify { mockCacheManager.invalidate("todo_items_event_${assignedTodoItem.eventId}") }
        verify { mockCacheManager.put("todo_item_${assignedTodoItem.id}", assignedTodoItem, any()) }
    }

    @Test
    fun `assignTodoItem should handle assignment failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Assignment failed")
        coEvery { mockSupabaseTodoRepository.assignTodoItem(testTodoId, "new-user-id") } returns Result.failure(exception)
        every { mockErrorHandler.handleError(any(), any()) } returns Result.failure(exception)

        // When
        val result = repository.assignTodoItem(testTodoId, "new-user-id")

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        coVerify { mockSupabaseTodoRepository.assignTodoItem(testTodoId, "new-user-id") }
        verify { mockErrorHandler.handleError(exception, "assignTodoItem") }
        verify(exactly = 0) { mockCacheManager.invalidate(any()) }
    }

    @Test
    fun `getTodoItemsByUser should return cached data when available`() = runTest {
        // Given
        val cachedTodoItems = listOf(testDomainTodoItem)
        every { mockCacheManager.get<List<DomainTodoItem>>(any()) } returns cachedTodoItems

        // When
        val result = repository.getTodoItemsByUser(testUserId).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(cachedTodoItems, result.getOrNull())
        
        verify { mockCacheManager.get<List<DomainTodoItem>>("todo_items_user_$testUserId") }
        verify(exactly = 0) { mockSupabaseTodoRepository.getTodoItemsByUser(any()) }
    }

    @Test
    fun `getTodoItemsByUser should fetch from remote when cache is empty`() = runTest {
        // Given
        val remoteTodoItems = listOf(testDomainTodoItem)
        every { mockCacheManager.get<List<DomainTodoItem>>(any()) } returns null
        every { mockSupabaseTodoRepository.getTodoItemsByUser(testUserId) } returns flowOf(Result.success(remoteTodoItems))
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        val result = repository.getTodoItemsByUser(testUserId).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(remoteTodoItems, result.getOrNull())
        
        verify { mockCacheManager.get<List<DomainTodoItem>>("todo_items_user_$testUserId") }
        verify { mockSupabaseTodoRepository.getTodoItemsByUser(testUserId) }
        verify { mockCacheManager.put("todo_items_user_$testUserId", remoteTodoItems, any()) }
    }

    @Test
    fun `subscribeToTodoUpdates should delegate to supabase repository`() = runTest {
        // Given
        val todoItems = listOf(testDomainTodoItem)
        every { mockSupabaseTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(todoItems))

        // When
        val result = repository.subscribeToTodoUpdates(testEventId).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(todoItems, result.getOrNull())
        
        verify { mockSupabaseTodoRepository.subscribeToTodoUpdates(testEventId) }
    }

    @Test
    fun `repository should record metrics for all operations`() = runTest {
        // Given
        every { mockCacheManager.get<DomainTodoItem>(any()) } returns null
        coEvery { mockSupabaseTodoRepository.getTodoItemById(testTodoId) } returns Result.success(testDomainTodoItem)
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        repository.getTodoItemById(testTodoId)

        // Then
        verify { MetricsCollector.recordApiCall("getTodoItemById", any(), true) }
    }

    @Test
    fun `repository should handle cache exceptions gracefully`() = runTest {
        // Given
        every { mockCacheManager.get<DomainTodoItem>(any()) } throws RuntimeException("Cache error")
        coEvery { mockSupabaseTodoRepository.getTodoItemById(testTodoId) } returns Result.success(testDomainTodoItem)
        every { mockCacheManager.put(any(), any(), any()) } just Runs

        // When
        val result = repository.getTodoItemById(testTodoId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDomainTodoItem, result.getOrNull())
        
        // Should fallback to remote when cache fails
        coVerify { mockSupabaseTodoRepository.getTodoItemById(testTodoId) }
    }
}