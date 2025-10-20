package com.earthmax.domain.usecase.event

import com.earthmax.domain.model.DomainEvent
import com.earthmax.domain.repository.EventRepository
import com.earthmax.domain.usecase.BaseUseCase
import javax.inject.Inject

/**
 * Use case for creating a new event.
 */
class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) : BaseUseCase<CreateEventUseCase.Params, DomainEvent>() {
    
    data class Params(
        val event: DomainEvent
    )
    
    override suspend fun execute(parameters: Params): DomainEvent {
        // Validate event data
        validateEvent(parameters.event)
        
        // Create the event through repository
        return when (val result = eventRepository.createEvent(parameters.event)) {
            is com.earthmax.domain.model.Result.Success -> result.data
            is com.earthmax.domain.model.Result.Error -> throw result.exception
            is com.earthmax.domain.model.Result.Loading -> throw IllegalStateException("Unexpected loading state")
        }
    }
    
    private fun validateEvent(event: DomainEvent) {
        require(event.title.isNotBlank()) { "Event title cannot be blank" }
        require(event.description.isNotBlank()) { "Event description cannot be blank" }
        require(event.location.isNotBlank()) { "Event location cannot be blank" }
        require(event.startDate <= (event.endDate ?: event.startDate)) { 
            "Start date must be before or equal to end date" 
        }
    }
}