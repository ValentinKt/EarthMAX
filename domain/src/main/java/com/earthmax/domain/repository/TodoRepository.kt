package com.earthmax.domain.repository

import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for todo item operations.
 * This defines the contract for todo item data access in the domain layer.
 */
interface TodoRepository {
    
    /**
     * Get all todo items for a specific event
     */
    fun getTodoItemsByEvent(eventId: String): Flow<Result<List<DomainTodoItem>>>
    
    /**
     * Get a specific todo item by ID
     */
    suspend fun getTodoItemById(todoId: String): Result<DomainTodoItem>
    
    /**
     * Create a new todo item
     */
    suspend fun createTodoItem(todoItem: DomainTodoItem): Result<DomainTodoItem>
    
    /**
     * Update an existing todo item
     */
    suspend fun updateTodoItem(todoItem: DomainTodoItem): Result<DomainTodoItem>
    
    /**
     * Delete a todo item
     */
    suspend fun deleteTodoItem(todoId: String): Result<Unit>
    
    /**
     * Toggle completion status of a todo item
     */
    suspend fun toggleTodoCompletion(todoId: String): Result<DomainTodoItem>
    
    /**
     * Assign a todo item to a user
     */
    suspend fun assignTodoItem(todoId: String, userId: String): Result<DomainTodoItem>
    
    /**
     * Get todo items assigned to a specific user
     */
    fun getTodoItemsByUser(userId: String): Flow<Result<List<DomainTodoItem>>>
    
    /**
     * Subscribe to real-time todo item updates for an event
     */
    fun subscribeToTodoUpdates(eventId: String): Flow<Result<List<DomainTodoItem>>>
}