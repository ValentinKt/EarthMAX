package com.earthmax.domain.usecase.todo

import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.repository.TodoRepository
import com.earthmax.domain.usecase.BaseUseCase
import javax.inject.Inject

/**
 * Use case for creating a new todo item.
 */
class CreateTodoItemUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) : BaseUseCase<CreateTodoItemUseCase.Params, DomainTodoItem>() {
    
    data class Params(
        val todoItem: DomainTodoItem
    )
    
    override suspend fun execute(parameters: Params): DomainTodoItem {
        // Validate todo item data
        validateTodoItem(parameters.todoItem)
        
        // Create the todo item through repository
        return when (val result = todoRepository.createTodoItem(parameters.todoItem)) {
            is com.earthmax.domain.model.Result.Success -> result.data
            is com.earthmax.domain.model.Result.Error -> throw result.exception
            is com.earthmax.domain.model.Result.Loading -> throw IllegalStateException("Unexpected loading state")
        }
    }
    
    private fun validateTodoItem(todoItem: DomainTodoItem) {
        require(todoItem.title.isNotBlank()) { "Todo item title cannot be blank" }
        require(todoItem.eventId.isNotBlank()) { "Todo item must be associated with an event" }
        require(todoItem.createdBy.isNotBlank()) { "Todo item must have a creator" }
    }
}