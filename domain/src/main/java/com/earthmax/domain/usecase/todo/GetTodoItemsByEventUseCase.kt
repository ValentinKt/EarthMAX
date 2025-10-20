package com.earthmax.domain.usecase.todo

import com.earthmax.domain.model.DomainTodoItem
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving todo items for a specific event.
 */
class GetTodoItemsByEventUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    
    data class Params(
        val eventId: String
    )
    
    operator fun invoke(params: Params): Flow<Result<List<DomainTodoItem>>> {
        return todoRepository.getTodoItemsByEvent(params.eventId)
    }
}