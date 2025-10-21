package com.earthmax.data.repository

import com.earthmax.core.cache.CacheManager
import com.earthmax.core.error.ErrorHandler
import com.earthmax.core.utils.Logger
import com.earthmax.data.todo.SupabaseTodoRepository
import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.TodoRepository as DomainTodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val supabaseTodoRepository: SupabaseTodoRepository,
    private val cacheManager: CacheManager,
    private val errorHandler: ErrorHandler
) : DomainTodoRepository {

    companion object {
        private const val TAG = "TodoRepositoryImpl"
        private val CACHE_TTL = 10.minutes
        private const val TODO_ITEMS_CACHE_PREFIX = "todo_items_event_"
        private const val TODO_ITEM_CACHE_PREFIX = "todo_item_"
        private const val USER_TODO_ITEMS_CACHE_PREFIX = "todo_items_user_"
    }

    override fun getTodoItemsByEvent(eventId: String): Flow<Result<List<DomainTodoItem>>> = flow {
        try {
            Logger.enter(TAG, "getTodoItemsByEvent", "eventId" to eventId)
            
            val cacheKey = "$TODO_ITEMS_CACHE_PREFIX$eventId"
            
            // Try to get from cache first
            val cached = cacheManager.get<List<DomainTodoItem>>(cacheKey)
            if (cached != null) {
                Logger.d(TAG, "Returning cached todo items for event $eventId")
                emit(Result.Success(cached))
                return@flow
            }
            
            // Fetch from remote repository
            supabaseTodoRepository.getTodoItemsByEvent(eventId)
                .catch { exception ->
                    Logger.e(TAG, "Error fetching todo items for event $eventId", exception)
                    emit(Result.Error(exception))
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val todoItems = result.data
                            // Cache the result
                            cacheManager.put(cacheKey, todoItems, CACHE_TTL)
                            Logger.d(TAG, "Successfully fetched and cached ${todoItems.size} todo items for event $eventId")
                        }
                        is Result.Error -> {
                            Logger.e(TAG, "Error in result for event $eventId", result.exception)
                        }
                        is Result.Loading -> {
                            Logger.d(TAG, "Loading todo items for event $eventId")
                        }
                    }
                    emit(result)
                }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Unexpected error in getTodoItemsByEvent", e)
            emit(Result.Error(e))
        } finally {
            Logger.exit(TAG, "getTodoItemsByEvent")
        }
    }

    override suspend fun getTodoItemById(todoId: String): Result<DomainTodoItem> {
        return try {
            Logger.enter(TAG, "getTodoItemById", "todoId" to todoId)
            
            val cacheKey = "$TODO_ITEM_CACHE_PREFIX$todoId"
            
            // Try to get from cache first
            val cached = cacheManager.get<DomainTodoItem>(cacheKey)
            if (cached != null) {
                Logger.d(TAG, "Returning cached todo item $todoId")
                return Result.Success(cached)
            }
            
            // Fetch from remote repository
            val result = supabaseTodoRepository.getTodoItemById(todoId)
            
            when (result) {
                is Result.Success -> {
                    val todoItem = result.data
                    // Cache the result
                    cacheManager.put(cacheKey, todoItem, CACHE_TTL)
                    Logger.d(TAG, "Successfully fetched and cached todo item $todoId")
                }
                is Result.Error -> {
                    Logger.e(TAG, "Error fetching todo item $todoId", result.exception)
                }
                is Result.Loading -> {
                    Logger.d(TAG, "Loading todo item $todoId")
                }
            }
            
            result
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching todo item $todoId", e)
            Result.Error(e)
        } finally {
            Logger.exit(TAG, "getTodoItemById")
        }
    }

    override suspend fun createTodoItem(todoItem: DomainTodoItem): Result<DomainTodoItem> {
        return try {
            Logger.enter(TAG, "createTodoItem", "todoItem" to todoItem.title)
            
            val result = supabaseTodoRepository.createTodoItem(todoItem)
            
            when (result) {
                is Result.Success -> {
                    val createdItem = result.data
                    // Cache the created item
                    cacheManager.put("$TODO_ITEM_CACHE_PREFIX${createdItem.id}", createdItem, CACHE_TTL)
                    
                    // Invalidate event todo items cache
                    cacheManager.remove("$TODO_ITEMS_CACHE_PREFIX${createdItem.eventId}")
                    
                    Logger.d(TAG, "Successfully created todo item: ${createdItem.title}")
                }
                is Result.Error -> {
                    Logger.e(TAG, "Error creating todo item", result.exception)
                }
                is Result.Loading -> {
                    Logger.d(TAG, "Creating todo item...")
                }
            }
            
            result
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error creating todo item", e)
            Result.Error(e)
        } finally {
            Logger.exit(TAG, "createTodoItem")
        }
    }

    override suspend fun updateTodoItem(todoItem: DomainTodoItem): Result<DomainTodoItem> {
        return try {
            Logger.enter(TAG, "updateTodoItem", "todoId" to todoItem.id)
            
            val result = supabaseTodoRepository.updateTodoItem(todoItem)
            
            when (result) {
                is Result.Success -> {
                    val updatedItem = result.data
                    // Update cache
                    cacheManager.put("$TODO_ITEM_CACHE_PREFIX${updatedItem.id}", updatedItem, CACHE_TTL)
                    
                    // Invalidate event todo items cache
                    cacheManager.remove("$TODO_ITEMS_CACHE_PREFIX${updatedItem.eventId}")
                    
                    Logger.d(TAG, "Successfully updated todo item: ${updatedItem.id}")
                }
                is Result.Error -> {
                    Logger.e(TAG, "Error updating todo item ${todoItem.id}", result.exception)
                }
                is Result.Loading -> {
                    Logger.d(TAG, "Updating todo item ${todoItem.id}...")
                }
            }
            
            result
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating todo item ${todoItem.id}", e)
            Result.Error(e)
        } finally {
            Logger.exit(TAG, "updateTodoItem")
        }
    }

    override suspend fun deleteTodoItem(todoId: String): Result<Unit> {
        return try {
            Logger.enter(TAG, "deleteTodoItem", "todoId" to todoId)
            
            // Get the todo item first to know which event cache to invalidate
            val todoItem = getTodoItemById(todoId)
            val eventId = when (todoItem) {
                is Result.Success -> todoItem.data.eventId
                else -> null
            }
            
            val result = supabaseTodoRepository.deleteTodoItem(todoId)
            
            when (result) {
                is Result.Success -> {
                    // Remove from cache
                    cacheManager.remove("$TODO_ITEM_CACHE_PREFIX$todoId")
                    
                    // Invalidate event todo items cache if we know the event ID
                    if (eventId != null) {
                        cacheManager.remove("$TODO_ITEMS_CACHE_PREFIX$eventId")
                    }
                    
                    Logger.d(TAG, "Successfully deleted todo item: $todoId")
                }
                is Result.Error -> {
                    Logger.e(TAG, "Error deleting todo item $todoId", result.exception)
                }
                is Result.Loading -> {
                    Logger.d(TAG, "Deleting todo item $todoId...")
                }
            }
            
            result
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error deleting todo item $todoId", e)
            Result.Error(e)
        } finally {
            Logger.exit(TAG, "deleteTodoItem")
        }
    }

    override suspend fun toggleTodoCompletion(todoId: String): Result<DomainTodoItem> {
        return try {
            Logger.enter(TAG, "toggleTodoCompletion", "todoId" to todoId)
            
            val result = supabaseTodoRepository.toggleTodoCompletion(todoId)
            
            when (result) {
                is Result.Success -> {
                    val updatedItem = result.data
                    // Update cache
                    cacheManager.put("$TODO_ITEM_CACHE_PREFIX${updatedItem.id}", updatedItem, CACHE_TTL)
                    
                    // Invalidate event todo items cache
                    cacheManager.remove("$TODO_ITEMS_CACHE_PREFIX${updatedItem.eventId}")
                    
                    Logger.d(TAG, "Successfully toggled completion for todo item: ${updatedItem.id}")
                }
                is Result.Error -> {
                    Logger.e(TAG, "Error toggling todo completion $todoId", result.exception)
                }
                is Result.Loading -> {
                    Logger.d(TAG, "Toggling completion for todo item $todoId...")
                }
            }
            
            result
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error toggling todo completion $todoId", e)
            Result.Error(e)
        } finally {
            Logger.exit(TAG, "toggleTodoCompletion")
        }
    }

    override suspend fun assignTodoItem(todoId: String, userId: String): Result<DomainTodoItem> {
        return try {
            Logger.enter(TAG, "assignTodoItem", "todoId" to todoId, "userId" to userId)
            
            val result = supabaseTodoRepository.assignTodoItem(todoId, userId)
            
            when (result) {
                is Result.Success -> {
                    val updatedItem = result.data
                    // Update cache
                    cacheManager.put("$TODO_ITEM_CACHE_PREFIX${updatedItem.id}", updatedItem, CACHE_TTL)
                    
                    // Invalidate related caches
                    cacheManager.remove("$TODO_ITEMS_CACHE_PREFIX${updatedItem.eventId}")
                    cacheManager.remove("$USER_TODO_ITEMS_CACHE_PREFIX$userId")
                    
                    Logger.d(TAG, "Successfully assigned todo item ${updatedItem.id} to user $userId")
                }
                is Result.Error -> {
                    Logger.e(TAG, "Error assigning todo item $todoId to user $userId", result.exception)
                }
                is Result.Loading -> {
                    Logger.d(TAG, "Assigning todo item $todoId to user $userId...")
                }
            }
            
            result
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error assigning todo item $todoId to user $userId", e)
            Result.Error(e)
        } finally {
            Logger.exit(TAG, "assignTodoItem")
        }
    }

    override fun getTodoItemsByUser(userId: String): Flow<Result<List<DomainTodoItem>>> = flow {
        try {
            Logger.enter(TAG, "getTodoItemsByUser", "userId" to userId)
            
            val cacheKey = "$USER_TODO_ITEMS_CACHE_PREFIX$userId"
            
            // Try to get from cache first
            val cached = cacheManager.get<List<DomainTodoItem>>(cacheKey)
            if (cached != null) {
                Logger.d(TAG, "Returning cached todo items for user $userId")
                emit(Result.Success(cached))
                return@flow
            }
            
            // Fetch from remote repository
            supabaseTodoRepository.getTodoItemsByUser(userId)
                .catch { exception ->
                    Logger.e(TAG, "Error fetching todo items for user $userId", exception)
                    emit(Result.Error(exception))
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val todoItems = result.data
                            // Cache the result
                            cacheManager.put(cacheKey, todoItems, CACHE_TTL)
                            Logger.d(TAG, "Successfully fetched and cached ${todoItems.size} todo items for user $userId")
                        }
                        is Result.Error -> {
                            Logger.e(TAG, "Error in result for user $userId", result.exception)
                        }
                        is Result.Loading -> {
                            Logger.d(TAG, "Loading todo items for user $userId")
                        }
                    }
                    emit(result)
                }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Unexpected error in getTodoItemsByUser", e)
            emit(Result.Error(e))
        } finally {
            Logger.exit(TAG, "getTodoItemsByUser")
        }
    }

    override fun subscribeToTodoUpdates(eventId: String): Flow<Result<List<DomainTodoItem>>> {
        return supabaseTodoRepository.subscribeToTodoUpdates(eventId)
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        val todoItems = result.data
                        // Update cache with real-time data
                        val cacheKey = "$TODO_ITEMS_CACHE_PREFIX$eventId"
                        cacheManager.put(cacheKey, todoItems, CACHE_TTL)
                        Logger.d(TAG, "Updated cache with real-time todo items for event $eventId")
                    }
                    is Result.Error -> {
                        Logger.e(TAG, "Error in real-time todo updates for event $eventId", result.exception)
                    }
                    is Result.Loading -> {
                        Logger.d(TAG, "Loading real-time todo updates for event $eventId")
                    }
                }
                result
            }
            .catch { exception ->
                Logger.e(TAG, "Error in real-time todo updates for event $eventId", exception)
                emit(Result.Error(exception))
            }
    }
}