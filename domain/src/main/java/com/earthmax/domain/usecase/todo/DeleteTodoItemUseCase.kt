package com.earthmax.domain.usecase.todo

import com.earthmax.domain.repository.TodoRepository
import com.earthmax.domain.usecase.BaseUseCase
import javax.inject.Inject

/**
 * Use case for deleting a todo item.
 */
class DeleteTodoItemUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) : BaseUseCase<DeleteTodoItemUseCase.Params, Unit>() {
    
    data class Params(
        val todoId: String
    )
    
    override suspend fun execute(parameters: Params) {
        require(parameters.todoId.isNotBlank()) { "Todo item ID cannot be blank" }
        
        // Delete the todo item through repository
        when (val result = todoRepository.deleteTodoItem(parameters.todoId)) {
            is com.earthmax.domain.model.Result.Success -> Unit
            is com.earthmax.domain.model.Result.Error -> throw result.exception
            is com.earthmax.domain.model.Result.Loading -> throw IllegalStateException("Unexpected loading state")
        }
    }
}