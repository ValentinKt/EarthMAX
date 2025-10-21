package com.earthmax.feature.events.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.Event
import com.earthmax.core.models.User
import com.earthmax.core.utils.Logger
import com.earthmax.domain.repository.EventRepository
import com.earthmax.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.earthmax.data.mappers.toEvent
import com.earthmax.data.mappers.toUser

data class EventDetailUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val organizer: User? = null,
    val currentUser: User? = null,
    val error: String? = null,
    val isJoining: Boolean = false,
    val isLeaving: Boolean = false
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepository.getCurrentUser()
        .map { result ->
            when (result) {
                is com.earthmax.domain.model.Result.Success -> result.data?.toUser()
                is com.earthmax.domain.model.Result.Error -> {
                    Logger.w("EventDetailViewModel", "Error observing current user", result.exception)
                    null
                }
                is com.earthmax.domain.model.Result.Loading -> null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        Logger.enter("EventDetailViewModel", "init")
        Logger.i("EventDetailViewModel", "Initializing EventDetailViewModel for eventId: $eventId")
        observeCurrentUser()
        loadEventDetail()
        Logger.exit("EventDetailViewModel", "init")
    }

    private fun observeCurrentUser() {
        Logger.enter("EventDetailViewModel", "observeCurrentUser")
        viewModelScope.launch {
            Logger.d("EventDetailViewModel", "Starting to observe current user changes")
            currentUser.collect { user ->
                Logger.logBusinessEvent("EventDetailViewModel", "user_state_changed", mapOf(
                    "hasUser" to (user != null),
                    "userId" to (user?.id ?: "null"),
                    "eventId" to eventId
                ))
                _uiState.update { it.copy(currentUser = user) }
                Logger.d("EventDetailViewModel", "UI state updated with user: ${user?.displayName ?: "null"}")
            }
        }
        Logger.exit("EventDetailViewModel", "observeCurrentUser")
    }

    private fun loadEventDetail() {
        Logger.enter("EventDetailViewModel", "loadEventDetail")
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.d("EventDetailViewModel", "Starting to load event details for eventId: $eventId")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                Logger.d("EventDetailViewModel", "Calling repository to get event by ID")
                when (val result = eventRepository.getEventById(eventId)) {
                    is com.earthmax.domain.model.Result.Success -> {
                        val event = result.data.toEvent()
                        Logger.logBusinessEvent("EventDetailViewModel", "event_loaded_successfully", mapOf(
                            "eventId" to eventId,
                            "eventTitle" to event.title,
                            "organizerId" to event.organizerId
                        ))
                        _uiState.update { it.copy(event = event, isLoading = false) }
                        Logger.i("EventDetailViewModel", "Event loaded successfully: ${event.title}")
                        loadOrganizer(event.organizerId)
                    }
                    is com.earthmax.domain.model.Result.Error -> {
                        Logger.e("EventDetailViewModel", "Failed to load event details", result.exception)
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = result.exception.message ?: "Failed to load event"
                            ) 
                        }
                    }
                    is com.earthmax.domain.model.Result.Loading -> {
                        // Keep loading state
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: Exception) {
                Logger.e("EventDetailViewModel", "Failed to load event details", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to load event"
                    ) 
                }
            } finally {
                Logger.logPerformance("EventDetailViewModel", "load_event_duration", System.currentTimeMillis() - startTime)
            }
        }
        Logger.exit("EventDetailViewModel", "loadEventDetail")
    }

    private fun loadOrganizer(organizerId: String) {
        Logger.enter("EventDetailViewModel", "loadOrganizer")
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.d("EventDetailViewModel", "Loading organizer details for organizerId: $organizerId")
            try {
                when (val organizerResult = userRepository.getUserById(organizerId)) {
                    is com.earthmax.domain.model.Result.Success -> {
                        val organizer = organizerResult.data.toUser()
                        Logger.logBusinessEvent("EventDetailViewModel", "organizer_loaded_successfully", mapOf(
                            "organizerId" to organizerId,
                            "organizerName" to (organizer?.displayName ?: "unknown"),
                            "eventId" to eventId
                        ))
                        _uiState.update { it.copy(organizer = organizer) }
                        Logger.i("EventDetailViewModel", "Organizer loaded successfully: ${organizer?.displayName ?: "unknown"}")
                    }
                    is com.earthmax.domain.model.Result.Error -> {
                        Logger.w("EventDetailViewModel", "Failed to load organizer details", organizerResult.exception)
                        // Organizer loading failed, but don't show error to user
                        // Event details can still be shown without organizer info
                    }
                    is com.earthmax.domain.model.Result.Loading -> {
                        // no-op
                    }
                }
            } catch (e: Exception) {
                Logger.w("EventDetailViewModel", "Failed to load organizer details", e)
                // Organizer loading failed, but don't show error to user
                // Event details can still be shown without organizer info
            } finally {
                Logger.logPerformance("EventDetailViewModel", "load_organizer_duration", System.currentTimeMillis() - startTime)
            }
        }
        Logger.exit("EventDetailViewModel", "loadOrganizer")
    }

    fun joinEvent() {
        Logger.enter("EventDetailViewModel", "joinEvent")
        val currentEvent = _uiState.value.event ?: run {
            Logger.w("EventDetailViewModel", "Join event failed: No current event available")
            Logger.exit("EventDetailViewModel", "joinEvent")
            return
        }
        val currentUser = _uiState.value.currentUser ?: run {
            Logger.w("EventDetailViewModel", "Join event failed: No current user available")
            Logger.exit("EventDetailViewModel", "joinEvent")
            return
        }
        
        val startTime = System.currentTimeMillis()

        viewModelScope.launch {
            Logger.logUserAction("EventDetailViewModel", "join_event_initiated", mapOf(
                "eventId" to currentEvent.id,
                "userId" to currentUser.id,
                "eventTitle" to currentEvent.title
            ))
            _uiState.update { it.copy(isJoining = true, error = null) }
            Logger.d("EventDetailViewModel", "Starting join event process")
            
            try {
                Logger.d("EventDetailViewModel", "Calling repository to join event")
                when (val res = eventRepository.joinEvent(currentEvent.id, currentUser.id)) {
                    is com.earthmax.domain.model.Result.Success -> {
                        Logger.logBusinessEvent("EventDetailViewModel", "event_joined_successfully", mapOf(
                            "eventId" to currentEvent.id,
                            "userId" to currentUser.id,
                            "eventTitle" to currentEvent.title
                        ))
                        _uiState.update { it.copy(isJoining = false) }
                        Logger.i("EventDetailViewModel", "Successfully joined event: ${currentEvent.title}")
                    }
                    is com.earthmax.domain.model.Result.Error -> {
                        Logger.e("EventDetailViewModel", "Failed to join event", res.exception)
                        _uiState.update { 
                            it.copy(
                                isJoining = false, 
                                error = res.exception.message ?: "Failed to join event"
                            ) 
                        }
                    }
                    is com.earthmax.domain.model.Result.Loading -> {
                        // no-op
                    }
                }
            } catch (e: Exception) {
                Logger.e("EventDetailViewModel", "Failed to join event", e)
                _uiState.update { 
                    it.copy(
                        isJoining = false, 
                        error = e.message ?: "Failed to join event"
                    ) 
                }
            } finally {
                Logger.logPerformance("EventDetailViewModel", "join_event_duration", System.currentTimeMillis() - startTime)
            }
        }
        Logger.exit("EventDetailViewModel", "joinEvent")
    }

    fun leaveEvent() {
        Logger.enter("EventDetailViewModel", "leaveEvent")
        val currentEvent = _uiState.value.event ?: run {
            Logger.w("EventDetailViewModel", "Leave event failed: No current event available")
            Logger.exit("EventDetailViewModel", "leaveEvent")
            return
        }
        val currentUser = _uiState.value.currentUser ?: run {
            Logger.w("EventDetailViewModel", "Leave event failed: No current user available")
            Logger.exit("EventDetailViewModel", "leaveEvent")
            return
        }
        
        val startTime = System.currentTimeMillis()

        viewModelScope.launch {
            Logger.logUserAction("EventDetailViewModel", "leave_event_initiated", mapOf(
                "eventId" to currentEvent.id,
                "userId" to currentUser.id,
                "eventTitle" to currentEvent.title
            ))
            _uiState.update { it.copy(isLeaving = true, error = null) }
            Logger.d("EventDetailViewModel", "Starting leave event process")
            
            try {
                Logger.d("EventDetailViewModel", "Calling repository to leave event")
                when (val res = eventRepository.leaveEvent(currentEvent.id, currentUser.id)) {
                    is com.earthmax.domain.model.Result.Success -> {
                        Logger.logBusinessEvent("EventDetailViewModel", "event_left_successfully", mapOf(
                            "eventId" to currentEvent.id,
                            "userId" to currentUser.id,
                            "eventTitle" to currentEvent.title
                        ))
                        _uiState.update { it.copy(isLeaving = false) }
                        Logger.i("EventDetailViewModel", "Successfully left event: ${currentEvent.title}")
                    }
                    is com.earthmax.domain.model.Result.Error -> {
                        Logger.e("EventDetailViewModel", "Failed to leave event", res.exception)
                        _uiState.update { 
                            it.copy(
                                isLeaving = false, 
                                error = res.exception.message ?: "Failed to leave event"
                            ) 
                        }
                    }
                    is com.earthmax.domain.model.Result.Loading -> {
                        // no-op
                    }
                }
            } catch (e: Exception) {
                Logger.e("EventDetailViewModel", "Failed to leave event", e)
                _uiState.update { 
                    it.copy(
                        isLeaving = false, 
                        error = e.message ?: "Failed to leave event"
                    ) 
                }
            } finally {
                Logger.logPerformance("EventDetailViewModel", "leave_event_duration", System.currentTimeMillis() - startTime)
            }
        }
        Logger.exit("EventDetailViewModel", "leaveEvent")
    }

    fun refreshEvent() {
        Logger.enter("EventDetailViewModel", "refreshEvent")
        Logger.logUserAction("EventDetailViewModel", "refresh_event_initiated", mapOf(
            "eventId" to eventId
        ))
        Logger.d("EventDetailViewModel", "Refreshing event details")
        loadEventDetail()
        Logger.exit("EventDetailViewModel", "refreshEvent")
    }

    fun clearError() {
        Logger.enter("EventDetailViewModel", "clearError")
        Logger.d("EventDetailViewModel", "Clearing error state")
        _uiState.update { it.copy(error = null) }
        Logger.exit("EventDetailViewModel", "clearError")
    }
}