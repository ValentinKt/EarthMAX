package com.earthmax.feature.events.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.Event
import com.earthmax.core.models.User
import com.earthmax.core.utils.Logger
import com.earthmax.data.repository.EventRepository
import com.earthmax.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                val event = eventRepository.getEventById(eventId)
                if (event != null) {
                    Logger.logBusinessEvent("EventDetailViewModel", "event_loaded_successfully", mapOf(
                        "eventId" to eventId,
                        "eventTitle" to event.title,
                        "organizerId" to event.organizerId
                    ))
                    _uiState.update { it.copy(event = event, isLoading = false) }
                    Logger.i("EventDetailViewModel", "Event loaded successfully: ${event.title}")
                    loadOrganizer(event.organizerId)
                } else {
                    Logger.w("EventDetailViewModel", "Event not found for eventId: $eventId")
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "Event not found"
                        ) 
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
                val organizer = userRepository.getUserById(organizerId)
                Logger.logBusinessEvent("EventDetailViewModel", "organizer_loaded_successfully", mapOf(
                    "organizerId" to organizerId,
                    "organizerName" to (organizer?.displayName ?: "unknown"),
                    "eventId" to eventId
                ))
                _uiState.update { it.copy(organizer = organizer) }
                Logger.i("EventDetailViewModel", "Organizer loaded successfully: ${organizer?.displayName ?: "unknown"}")
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
                eventRepository.joinEvent(currentEvent.id, currentUser.id)
                Logger.logBusinessEvent("EventDetailViewModel", "event_joined_successfully", mapOf(
                    "eventId" to currentEvent.id,
                    "userId" to currentUser.id,
                    "eventTitle" to currentEvent.title
                ))
                _uiState.update { it.copy(isJoining = false) }
                Logger.i("EventDetailViewModel", "Successfully joined event: ${currentEvent.title}")
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
                eventRepository.leaveEvent(currentEvent.id, currentUser.id)
                Logger.logBusinessEvent("EventDetailViewModel", "event_left_successfully", mapOf(
                    "eventId" to currentEvent.id,
                    "userId" to currentUser.id,
                    "eventTitle" to currentEvent.title
                ))
                _uiState.update { it.copy(isLeaving = false) }
                Logger.i("EventDetailViewModel", "Successfully left event: ${currentEvent.title}")
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