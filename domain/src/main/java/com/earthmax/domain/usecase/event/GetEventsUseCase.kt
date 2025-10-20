package com.earthmax.domain.usecase.event

import com.earthmax.domain.model.DomainEvent
import com.earthmax.domain.model.EventCategory
import com.earthmax.domain.model.EventStatus
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving events with optional filtering.
 */
class GetEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    
    data class Params(
        val category: EventCategory? = null,
        val status: EventStatus? = null,
        val location: String? = null
    )
    
    operator fun invoke(params: Params = Params()): Flow<Result<List<DomainEvent>>> {
        return eventRepository.getEvents(
            category = params.category,
            status = params.status,
            location = params.location
        )
    }
}