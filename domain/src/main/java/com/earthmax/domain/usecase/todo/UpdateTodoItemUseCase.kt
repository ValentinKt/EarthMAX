package com.earthmax.domain.usecase.todo

import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.repository.TodoRepository
import com.earthmax.domain.usecase.BaseUseCase
import javax.inject.Inject

/**
 * Use case for updating an existing todo item.
 */
class UpdateTodoItemUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) : BaseUseCase<UpdateTodoItemUseCase.Params, DomainTodoItem>() {
    
    data class Params(
        val todoItem: DomainTodoItem
    )
    
    override suspend fun execute(parameters: Params): DomainTodoItem {
        // Validate todo item data
        validateTodoItem(parameters.todoItem)
        
        // Update the todo item through repository
        return when (val result = todoRepository.updateTodoItem(parameters.todoItem)) {
            is com.earthmax.domain.model.Result.Success -> result.data
            is com.earthmax.domain.model.Result.Error -> throw result.exception
            is com.earthmax.domain.model.Result.Loading -> throw IllegalStateException("Unexpected loading state")
        }
    }
    
    private fun validateTodoItem(todoItem: DomainTodoItem) {
        require(todoItem.id.isNotBlank()) { "Todo item ID cannot be blank" }
        require(todoItem.title.isNotBlank()) { "Todo item title cannot be blank" }
        require(todoItem.eventId.isNotBlank()) { "Todo item must be associated with an event" }
        require(todoItem.createdBy.isNotBlank()) { "Todo item must have a creator" }
    }
}