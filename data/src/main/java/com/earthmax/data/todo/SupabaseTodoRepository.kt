package com.earthmax.data.todo

import com.earthmax.core.network.SupabaseClient
import com.earthmax.core.utils.Logger
import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.TodoRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

import kotlinx.datetime.Clock
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseTodoRepository @Inject constructor() : TodoRepository {
    
    companion object {
        private const val TAG = "SupabaseTodoRepository"
        private const val TODO_ITEMS_TABLE = "todo_items"
    }
    
    override fun getTodoItemsByEvent(eventId: String): Flow<Result<List<DomainTodoItem>>> = flow {
        try {
            Logger.enter(TAG, "getTodoItemsByEvent", "eventId" to eventId)
            
            val response = SupabaseClient.client
                .from(TODO_ITEMS_TABLE)
                .select {
                    filter {
                        eq("event_id", eventId)
                    }
                }
                .decodeList<TodoItemDto>()
            
            val todoItems = response.map { it.toDomainTodoItem() }
            
            Logger.d(TAG, "Successfully fetched ${todoItems.size} todo items for event $eventId")
            emit(Result.Success<List<DomainTodoItem>>(todoItems))
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching todo items for event $eventId", e)
            emit(Result.Error(e))
        } finally {
            Logger.exit(TAG, "getTodoItemsByEvent")
        }
    }
    
    override suspend fun getTodoItemById(todoId: String): Result<DomainTodoItem> {
        return try {
            Logger.enter(TAG, "getTodoItemById", "todoId" to todoId)
            
            val response = SupabaseClient.client
                .from(TODO_ITEMS_TABLE)
                .select {
                    filter {
                        eq("id", todoId)
                    }
                }
                .decodeSingle<TodoItemDto>()
            
            val todoItem = response.toDomainTodoItem()
            
            Logger.d(TAG, "Successfully fetched todo item: $todoId")
            Result.Success(todoItem)
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching todo item $todoId", e)
            Result.Error(e)
        } finally {
            Logger.exit(TAG, "getTodoItemById")
        }
    }
    
    override suspend fun createTodoItem(todoItem: DomainTodoItem): Result<DomainTodoItem> {
        return try {
            Logger.enter(TAG, "createTodoItem", "title" to todoItem.title)
            
            val todoDto = todoItem.toTodoItemDto()
            
            val response = SupabaseClient.client
                .from(TODO_ITEMS_TABLE)
                .insert(todoDto)
                .decodeSingle<TodoItemDto>()
            
            val createdTodoItem = response.toDomainTodoItem()
            
            Logger.d(TAG, "Successfully created todo item: ${createdTodoItem.id}")
            com.earthmax.domain.model.Result.Success(createdTodoItem)
            
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
            
            val todoDto = todoItem.toTodoItemDto()
            
            val response = SupabaseClient.client
                .from(TODO_ITEMS_TABLE)
                .update(todoDto) {
                    filter {
                        eq("id", todoItem.id)
                    }
                }
                .decodeSingle<TodoItemDto>()
            
            val updatedTodoItem = response.toDomainTodoItem()
            
            Logger.d(TAG, "Successfully updated todo item: ${updatedTodoItem.id}")
            com.earthmax.domain.model.Result.Success(updatedTodoItem)
            
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
            
            SupabaseClient.client
                .from(TODO_ITEMS_TABLE)
                .delete {
                    filter {
                        eq("id", todoId)
                    }
                }
            
            Logger.d(TAG, "Successfully deleted todo item: $todoId")
            com.earthmax.domain.model.Result.Success(Unit)
            
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
            
            // First get the current todo item
            val currentTodo = getTodoItemById(todoId)
            if (currentTodo is Result.Error) {
                return currentTodo
            }
            
            val todo = (currentTodo as Result.Success<DomainTodoItem>).data
            val now = Instant.fromEpochMilliseconds(java.lang.System.currentTimeMillis())
            
            val updatedTodo = todo.copy(
                isCompleted = !todo.isCompleted,
                completedAt = if (!todo.isCompleted) now else null,
                updatedAt = now
            )
            
            updateTodoItem(updatedTodo)
            
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
            
            // First get the current todo item
            val currentTodo = getTodoItemById(todoId)
            if (currentTodo is Result.Error) {
                return currentTodo
            }
            
            val todo = (currentTodo as Result.Success<DomainTodoItem>).data
            val updatedTodo = todo.copy(
                assignedTo = userId,
                updatedAt = Instant.fromEpochMilliseconds(java.lang.System.currentTimeMillis())
            )
            
            updateTodoItem(updatedTodo)
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error assigning todo item $todoId to user $userId", e)
            return Result.Error(e)
        } finally {
            Logger.exit(TAG, "assignTodoItem")
        }
    }
    
    override fun getTodoItemsByUser(userId: String): Flow<Result<List<DomainTodoItem>>> = flow {
        try {
            Logger.enter(TAG, "getTodoItemsByUser", "userId" to userId)
            
            val response = SupabaseClient.client
                .from(TODO_ITEMS_TABLE)
                .select {
                    filter {
                        eq("assigned_to", userId)
                    }
                }
                .decodeList<TodoItemDto>()
            
            val todoItems = response.map { it.toDomainTodoItem() }
            
            Logger.d(TAG, "Successfully fetched ${todoItems.size} todo items for user $userId")
            emit(Result.Success(todoItems))
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching todo items for user $userId", e)
            emit(Result.Error(e))
        } finally {
            Logger.exit(TAG, "getTodoItemsByUser")
        }
    }
    
    override fun subscribeToTodoUpdates(eventId: String): Flow<Result<List<DomainTodoItem>>> {
        return flow {
            try {
                Logger.enter(TAG, "subscribeToTodoUpdates", "eventId" to eventId)
                
                val channel = SupabaseClient.client.realtime.channel("todo_updates_$eventId")
                
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public")
                
                channel.subscribe()
                
                changes.collect { change ->
                    Logger.d(TAG, "Received real-time todo update")
                    
                    // Fetch fresh data after any change
                    getTodoItemsByEvent(eventId).collect { result ->
                        emit(result)
                    }
                }
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error in todo updates subscription for event $eventId", e)
                emit(Result.Error(e))
            } finally {
                Logger.exit(TAG, "subscribeToTodoUpdates")
            }
        }
    }
}

@Serializable
data class TodoItemDto(
    val id: String,
    val event_id: String,
    val title: String,
    val description: String? = null,
    val is_completed: Boolean = false,
    val assigned_to: String? = null,
    val created_by: String,
    val created_at: String,
    val completed_at: String? = null,
    val updated_at: String
)

// Extension functions for mapping between DTO and domain models
private fun TodoItemDto.toDomainTodoItem(): DomainTodoItem {
    return DomainTodoItem(
        id = id,
        eventId = event_id,
        title = title,
        description = description,
        isCompleted = is_completed,
        assignedTo = assigned_to,
        createdBy = created_by,
        createdAt = Instant.parse(created_at),
        completedAt = completed_at?.let { Instant.parse(it) },
        updatedAt = Instant.parse(updated_at)
    )
}

private fun DomainTodoItem.toTodoItemDto(): TodoItemDto {
    return TodoItemDto(
        id = id,
        event_id = eventId,
        title = title,
        description = description,
        is_completed = isCompleted,
        assigned_to = assignedTo,
        created_by = createdBy,
        created_at = createdAt.toString(),
        completed_at = completedAt?.toString(),
        updated_at = updatedAt.toString()
    )
}