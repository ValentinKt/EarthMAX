package com.earthmax.feature.events.todo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.User
import com.earthmax.core.utils.Logger
import com.earthmax.domain.repository.TodoRepository
import com.earthmax.data.repository.UserRepository
import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import com.earthmax.domain.usecase.todo.CreateTodoItemUseCase
import com.earthmax.domain.usecase.todo.DeleteTodoItemUseCase
import com.earthmax.domain.usecase.todo.GetTodoItemsByEventUseCase
import com.earthmax.domain.usecase.todo.ToggleTodoCompletionUseCase
import com.earthmax.domain.usecase.todo.UpdateTodoItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

data class TodoListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val todoItems: List<DomainTodoItem> = emptyList(),
    val currentUser: User? = null,
    val isCreatingTodo: Boolean = false,
    val newTodoTitle: String = "",
    val newTodoDescription: String = ""
)

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val getTodoItemsByEventUseCase: GetTodoItemsByEventUseCase,
    private val createTodoItemUseCase: CreateTodoItemUseCase,
    private val updateTodoItemUseCase: UpdateTodoItemUseCase,
    private val toggleTodoCompletionUseCase: ToggleTodoCompletionUseCase,
    private val deleteTodoItemUseCase: DeleteTodoItemUseCase,
    private val todoRepository: TodoRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _uiState = MutableStateFlow(TodoListUiState())
    val uiState: StateFlow<TodoListUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        Logger.enter("TodoListViewModel", "init")
        Logger.i("TodoListViewModel", "Initializing TodoListViewModel for event: $eventId")
        observeCurrentUser()
        subscribeToTodoUpdates()
        Logger.exit("TodoListViewModel", "init")
    }

    private fun observeCurrentUser() {
        Logger.enter("TodoListViewModel", "observeCurrentUser")
        viewModelScope.launch {
            Logger.d("TodoListViewModel", "Starting to observe current user changes")
            currentUser.collect { user ->
                Logger.logBusinessEvent("TodoListViewModel", "user_state_changed", mapOf<String, Any>(
                    "hasUser" to (user != null),
                    "userId" to (user?.id ?: "null"),
                    "eventId" to eventId
                ))
                _uiState.update { it.copy(currentUser = user) }
                Logger.d("TodoListViewModel", "UI state updated with user: ${user?.displayName ?: "null"}")
            }
        }
        Logger.exit("TodoListViewModel", "observeCurrentUser")
    }

    private fun subscribeToTodoUpdates() {
        viewModelScope.launch {
            Logger.enter("TodoListViewModel", "subscribeToTodoUpdates", "eventId" to eventId)
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Use the repository's real-time subscription method
                todoRepository.subscribeToTodoUpdates(eventId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    todoItems = result.data,
                                    error = null
                                )
                            }
                            Logger.d("TodoListViewModel", "Received real-time todo update: ${result.data.size} items")
                        }
                        is Result.Error -> {
                            Logger.e("TodoListViewModel", "Error in real-time todo subscription", result.exception)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    error = result.exception.message
                                )
                            }
                        }
                        is Result.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("TodoListViewModel", "Error setting up real-time todo subscription", e)
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message) 
                }
            } finally {
                Logger.exit("TodoListViewModel", "subscribeToTodoUpdates")
            }
        }
    }

    fun loadTodoItems(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                getTodoItemsByEventUseCase(GetTodoItemsByEventUseCase.Params(eventId)).collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }
                        is Result.Success -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    todoItems = result.data,
                                    error = null
                                )
                            }
                        }
                        is Result.Error -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    error = result.exception.message ?: "Failed to load todo items"
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("TodoListViewModel", "Error loading todo items", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to load todo items"
                    )
                }
            }
        }
    }

    fun onNewTodoTitleChanged(title: String) {
        Logger.d("TodoListViewModel", "New todo title changed: $title")
        _uiState.update { it.copy(newTodoTitle = title) }
    }

    fun onNewTodoDescriptionChanged(description: String) {
        Logger.d("TodoListViewModel", "New todo description changed: $description")
        _uiState.update { it.copy(newTodoDescription = description) }
    }

    fun createTodoItem(eventId: String) {
        Logger.enter("TodoListViewModel", "createTodoItem")
        val currentState = _uiState.value
        val user = currentState.currentUser
        
        if (user == null) {
            Logger.w("TodoListViewModel", "Cannot create todo item: user not authenticated")
            _uiState.update { it.copy(error = "Please log in to create todo items") }
            return
        }

        if (currentState.newTodoTitle.isBlank()) {
            Logger.w("TodoListViewModel", "Cannot create todo item: title is blank")
            _uiState.update { it.copy(error = "Todo title cannot be empty") }
            return
        }

        viewModelScope.launch {
            Logger.d("TodoListViewModel", "Creating new todo item: ${currentState.newTodoTitle}")
            _uiState.update { it.copy(isCreatingTodo = true, error = null) }

            val newTodoItem = DomainTodoItem(
                id = "", // Will be generated by the backend
                eventId = eventId,
                title = currentState.newTodoTitle,
                description = currentState.newTodoDescription.takeIf { it.isNotBlank() },
                isCompleted = false,
                assignedTo = null,
                createdBy = user.id,
                createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                completedAt = null,
                updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )

            when (val result = createTodoItemUseCase(CreateTodoItemUseCase.Params(newTodoItem))) {
                is Result.Success -> {
                    Logger.d("TodoListViewModel", "Todo item created successfully: ${result.data.id}")
                    Logger.logBusinessEvent("TodoListViewModel", "todo_item_created", mapOf<String, Any>(
                        "eventId" to eventId,
                        "todoId" to result.data.id,
                        "title" to result.data.title
                    ))
                    _uiState.update { 
                        it.copy(
                            isCreatingTodo = false,
                            newTodoTitle = "",
                            newTodoDescription = "",
                            error = null
                        ) 
                    }
                    // Real-time subscription will automatically update the list
                }
                is Result.Error -> {
                    Logger.e("TodoListViewModel", "Failed to create todo item", result.exception)
                    Logger.logBusinessEvent("TodoListViewModel", "todo_item_create_error", mapOf<String, Any>(
                        "eventId" to eventId,
                        "error" to (result.exception.message ?: "unknown_error")
                    ))
                    _uiState.update { 
                        it.copy(
                            isCreatingTodo = false,
                            error = result.exception.message ?: "Failed to create todo item"
                        ) 
                    }
                }
                is Result.Loading -> {
                    // This shouldn't happen with the current implementation
                    Logger.w("TodoListViewModel", "Unexpected loading state in createTodoItem")
                }
            }
        }
        Logger.exit("TodoListViewModel", "createTodoItem")
    }

    fun toggleTodoCompletion(todoId: String) {
        Logger.enter("TodoListViewModel", "toggleTodoCompletion")
        viewModelScope.launch {
            Logger.d("TodoListViewModel", "Toggling completion for todo: $todoId")
            
            when (val result = toggleTodoCompletionUseCase(ToggleTodoCompletionUseCase.Params(todoId))) {
                is Result.Success -> {
                    Logger.d("TodoListViewModel", "Todo completion toggled successfully: ${result.data.id}")
                    Logger.logBusinessEvent("TodoListViewModel", "todo_completion_toggled", mapOf<String, Any>(
                        "eventId" to eventId,
                        "todoId" to result.data.id,
                        "isCompleted" to result.data.isCompleted
                    ))
                    // Real-time subscription will automatically update the list
                }
                is Result.Error -> {
                    Logger.e("TodoListViewModel", "Failed to toggle todo completion", result.exception)
                    Logger.logBusinessEvent("TodoListViewModel", "todo_completion_toggle_error", mapOf<String, Any>(
                        "eventId" to eventId,
                        "todoId" to todoId,
                        "error" to (result.exception.message ?: "unknown_error")
                    ))
                    _uiState.update { 
                        it.copy(error = result.exception.message ?: "Failed to update todo item") 
                    }
                }
                is Result.Loading -> {
                    // This shouldn't happen with the current implementation
                    Logger.w("TodoListViewModel", "Unexpected loading state in toggleTodoCompletion")
                }
            }
        }
        Logger.exit("TodoListViewModel", "toggleTodoCompletion")
    }

    fun deleteTodoItem(todoId: String) {
        Logger.enter("TodoListViewModel", "deleteTodoItem")
        viewModelScope.launch {
            Logger.d("TodoListViewModel", "Deleting todo item: $todoId")
            
            when (val result = deleteTodoItemUseCase(DeleteTodoItemUseCase.Params(todoId))) {
                is Result.Success -> {
                    Logger.d("TodoListViewModel", "Todo item deleted successfully: $todoId")
                    Logger.logBusinessEvent("TodoListViewModel", "todo_item_deleted", mapOf<String, Any>(
                        "eventId" to eventId,
                        "todoId" to todoId
                    ))
                    // Real-time subscription will automatically update the list
                }
                is Result.Error -> {
                    Logger.e("TodoListViewModel", "Failed to delete todo item", result.exception)
                    Logger.logBusinessEvent("TodoListViewModel", "todo_item_delete_error", mapOf<String, Any>(
                        "eventId" to eventId,
                        "todoId" to todoId,
                        "error" to (result.exception.message ?: "unknown_error")
                    ))
                    _uiState.update { 
                        it.copy(error = result.exception.message ?: "Failed to delete todo item") 
                    }
                }
                is Result.Loading -> {
                    // This shouldn't happen with the current implementation
                    Logger.w("TodoListViewModel", "Unexpected loading state in deleteTodoItem")
                }
            }
        }
        Logger.exit("TodoListViewModel", "deleteTodoItem")
    }

    fun clearError() {
        Logger.d("TodoListViewModel", "Clearing error state")
        _uiState.update { it.copy(error = null) }
    }

    fun startCreatingTodo() {
        Logger.d("TodoListViewModel", "Starting todo creation")
        _uiState.update { it.copy(isCreatingTodo = true, error = null) }
    }

    fun cancelCreatingTodo() {
        Logger.d("TodoListViewModel", "Cancelling todo creation")
        _uiState.update { 
            it.copy(
                isCreatingTodo = false,
                newTodoTitle = "",
                newTodoDescription = "",
                error = null
            ) 
        }
    }
}