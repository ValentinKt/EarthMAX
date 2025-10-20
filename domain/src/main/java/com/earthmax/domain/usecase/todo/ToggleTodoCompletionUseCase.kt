package com.earthmax.domain.usecase.todo

import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.repository.TodoRepository
import com.earthmax.domain.usecase.BaseUseCase
import javax.inject.Inject

/**
 * Use case for toggling the completion status of a todo item.
 */
class ToggleTodoCompletionUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) : BaseUseCase<ToggleTodoCompletionUseCase.Params, DomainTodoItem>() {
    
    data class Params(
        val todoId: String
    )
    
    override suspend fun execute(parameters: Params): DomainTodoItem {
        require(parameters.todoId.isNotBlank()) { "Todo item ID cannot be blank" }
        
        // Toggle completion status through repository
        return when (val result = todoRepository.toggleTodoCompletion(parameters.todoId)) {
            is com.earthmax.domain.model.Result.Success -> result.data
            is com.earthmax.domain.model.Result.Error -> throw result.exception
            is com.earthmax.domain.model.Result.Loading -> throw IllegalStateException("Unexpected loading state")
        }
    }
}