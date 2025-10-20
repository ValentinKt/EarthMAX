package com.earthmax.presentation.viewmodel

import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.TodoRepository
import com.earthmax.domain.usecase.CreateTodoItemUseCase
import com.earthmax.domain.usecase.DeleteTodoItemUseCase
import com.earthmax.domain.usecase.GetTodoItemsByEventUseCase
import com.earthmax.domain.usecase.ToggleTodoCompletionUseCase
import com.earthmax.presentation.ui.todo.TodoListUiState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoListViewModelTest {

    private lateinit var viewModel: TodoListViewModel
    private val mockTodoRepository = mockk<TodoRepository>()
    private val mockGetTodoItemsByEventUseCase = mockk<GetTodoItemsByEventUseCase>()
    private val mockCreateTodoItemUseCase = mockk<CreateTodoItemUseCase>()
    private val mockToggleTodoCompletionUseCase = mockk<ToggleTodoCompletionUseCase>()
    private val mockDeleteTodoItemUseCase = mockk<DeleteTodoItemUseCase>()

    private val testDispatcher = StandardTestDispatcher()
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
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)

        // Mock real-time subscription by default
        every { mockTodoRepository.subscribeToTodoUpdates(any()) } returns flowOf(Result.success(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(eventId: String = testEventId): TodoListViewModel {
        return TodoListViewModel(
            eventId = eventId,
            todoRepository = mockTodoRepository,
            getTodoItemsByEventUseCase = mockGetTodoItemsByEventUseCase,
            createTodoItemUseCase = mockCreateTodoItemUseCase,
            toggleTodoCompletionUseCase = mockToggleTodoCompletionUseCase,
            deleteTodoItemUseCase = mockDeleteTodoItemUseCase
        )
    }

    @Test
    fun `initial state should be loading`() = runTest {
        // Given
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()

        // Then
        val initialState = viewModel.uiState.first()
        assertTrue(initialState.isLoading)
        assertNull(initialState.error)
        assertTrue(initialState.todoItems.isEmpty())
    }

    @Test
    fun `subscribeToTodoUpdates should update UI state with todo items on success`() = runTest {
        // Given
        val todoItems = listOf(testDomainTodoItem)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(todoItems))

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(todoItems, state.todoItems)
        
        verify { mockTodoRepository.subscribeToTodoUpdates(testEventId) }
    }

    @Test
    fun `subscribeToTodoUpdates should update UI state with error on failure`() = runTest {
        // Given
        val exception = RuntimeException("Subscription failed")
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.failure(exception))

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertEquals("Subscription failed", state.error)
        assertTrue(state.todoItems.isEmpty())
        
        verify { mockTodoRepository.subscribeToTodoUpdates(testEventId) }
    }

    @Test
    fun `subscribeToTodoUpdates should handle multiple updates from real-time subscription`() = runTest {
        // Given
        val initialTodoItems = listOf(testDomainTodoItem)
        val updatedTodoItems = listOf(
            testDomainTodoItem,
            testDomainTodoItem.copy(id = "todo-2", title = "Second Todo")
        )
        
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(
            Result.success(initialTodoItems),
            Result.success(updatedTodoItems)
        )

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.todoItems.size)
        assertEquals(updatedTodoItems, state.todoItems)
        
        verify { mockTodoRepository.subscribeToTodoUpdates(testEventId) }
    }

    @Test
    fun `loadTodoItems should update UI state with todo items on success`() = runTest {
        // Given
        val todoItems = listOf(testDomainTodoItem)
        coEvery { mockGetTodoItemsByEventUseCase(testEventId) } returns Result.success(todoItems)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        viewModel.loadTodoItems(testEventId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(todoItems, state.todoItems)
        
        coVerify { mockGetTodoItemsByEventUseCase(testEventId) }
    }

    @Test
    fun `loadTodoItems should update UI state with error on failure`() = runTest {
        // Given
        val exception = RuntimeException("Load failed")
        coEvery { mockGetTodoItemsByEventUseCase(testEventId) } returns Result.failure(exception)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        viewModel.loadTodoItems(testEventId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertEquals("Load failed", state.error)
        assertTrue(state.todoItems.isEmpty())
        
        coVerify { mockGetTodoItemsByEventUseCase(testEventId) }
    }

    @Test
    fun `createTodoItem should call use case and not reload manually due to real-time updates`() = runTest {
        // Given
        val newTodoItem = testDomainTodoItem.copy(id = "new-todo-id", title = "New Todo")
        coEvery { mockCreateTodoItemUseCase(newTodoItem) } returns Result.success(newTodoItem)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        viewModel.createTodoItem(newTodoItem)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { mockCreateTodoItemUseCase(newTodoItem) }
        // Should not call loadTodoItems manually since real-time updates handle it
        coVerify(exactly = 0) { mockGetTodoItemsByEventUseCase(any()) }
    }

    @Test
    fun `createTodoItem should handle creation failure gracefully`() = runTest {
        // Given
        val newTodoItem = testDomainTodoItem.copy(id = "new-todo-id", title = "New Todo")
        val exception = RuntimeException("Creation failed")
        coEvery { mockCreateTodoItemUseCase(newTodoItem) } returns Result.failure(exception)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        viewModel.createTodoItem(newTodoItem)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { mockCreateTodoItemUseCase(newTodoItem) }
        
        val state = viewModel.uiState.first()
        // Error should be handled by the use case, UI state should remain stable
        assertFalse(state.isLoading)
    }

    @Test
    fun `toggleTodoCompletion should call use case and not reload manually due to real-time updates`() = runTest {
        // Given
        val completedTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = Instant.parse("2024-01-01T12:00:00Z")
        )
        coEvery { mockToggleTodoCompletionUseCase(testTodoId) } returns Result.success(completedTodoItem)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        viewModel.toggleTodoCompletion(testTodoId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { mockToggleTodoCompletionUseCase(testTodoId) }
        // Should not call loadTodoItems manually since real-time updates handle it
        coVerify(exactly = 0) { mockGetTodoItemsByEventUseCase(any()) }
    }

    @Test
    fun `toggleTodoCompletion should handle toggle failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Toggle failed")
        coEvery { mockToggleTodoCompletionUseCase(testTodoId) } returns Result.failure(exception)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        viewModel.toggleTodoCompletion(testTodoId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { mockToggleTodoCompletionUseCase(testTodoId) }
        
        val state = viewModel.uiState.first()
        // Error should be handled by the use case, UI state should remain stable
        assertFalse(state.isLoading)
    }

    @Test
    fun `deleteTodoItem should call use case and not reload manually due to real-time updates`() = runTest {
        // Given
        coEvery { mockDeleteTodoItemUseCase(testTodoId) } returns Result.success(Unit)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        viewModel.deleteTodoItem(testTodoId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { mockDeleteTodoItemUseCase(testTodoId) }
        // Should not call loadTodoItems manually since real-time updates handle it
        coVerify(exactly = 0) { mockGetTodoItemsByEventUseCase(any()) }
    }

    @Test
    fun `deleteTodoItem should handle deletion failure gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Delete failed")
        coEvery { mockDeleteTodoItemUseCase(testTodoId) } returns Result.failure(exception)
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        viewModel.deleteTodoItem(testTodoId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { mockDeleteTodoItemUseCase(testTodoId) }
        
        val state = viewModel.uiState.first()
        // Error should be handled by the use case, UI state should remain stable
        assertFalse(state.isLoading)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        // Given
        val exception = RuntimeException("Test error")
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.failure(exception))

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error is set
        var state = viewModel.uiState.first()
        assertEquals("Test error", state.error)
        
        // Clear error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        state = viewModel.uiState.first()
        assertNull(state.error)
    }

    @Test
    fun `UI state should maintain consistency during multiple operations`() = runTest {
        // Given
        val initialTodoItems = listOf(testDomainTodoItem)
        val updatedTodoItems = listOf(
            testDomainTodoItem.copy(isCompleted = true),
            testDomainTodoItem.copy(id = "todo-2", title = "Second Todo")
        )
        
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(
            Result.success(initialTodoItems),
            Result.success(updatedTodoItems)
        )
        coEvery { mockToggleTodoCompletionUseCase(testTodoId) } returns Result.success(testDomainTodoItem.copy(isCompleted = true))

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Perform toggle operation
        viewModel.toggleTodoCompletion(testTodoId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.todoItems.size)
        assertTrue(state.todoItems.first().isCompleted)
        
        coVerify { mockToggleTodoCompletionUseCase(testTodoId) }
        verify { mockTodoRepository.subscribeToTodoUpdates(testEventId) }
    }

    @Test
    fun `real-time subscription should be established on initialization`() = runTest {
        // Given
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()

        // Then
        verify { mockTodoRepository.subscribeToTodoUpdates(testEventId) }
    }

    @Test
    fun `real-time subscription should handle connection errors gracefully`() = runTest {
        // Given
        val connectionError = RuntimeException("Connection lost")
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(
            Result.success(listOf(testDomainTodoItem)),
            Result.failure(connectionError),
            Result.success(listOf(testDomainTodoItem)) // Recovery
        )

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        // Should recover and show the last successful data
        assertEquals(listOf(testDomainTodoItem), state.todoItems)
        
        verify { mockTodoRepository.subscribeToTodoUpdates(testEventId) }
    }

    @Test
    fun `viewModel should handle empty todo list gracefully`() = runTest {
        // Given
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(emptyList()))

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.todoItems.isEmpty())
    }

    @Test
    fun `viewModel should handle large todo lists efficiently`() = runTest {
        // Given
        val largeTodoList = (1..100).map { index ->
            testDomainTodoItem.copy(
                id = "todo-$index",
                title = "Todo Item $index"
            )
        }
        every { mockTodoRepository.subscribeToTodoUpdates(testEventId) } returns flowOf(Result.success(largeTodoList))

        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(100, state.todoItems.size)
        assertEquals("Todo Item 1", state.todoItems.first().title)
        assertEquals("Todo Item 100", state.todoItems.last().title)
    }
}