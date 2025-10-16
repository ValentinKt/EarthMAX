package com.earthmax.feature.events.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.Event
import com.earthmax.core.models.User
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
        observeCurrentUser()
        loadEventDetail()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun loadEventDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val event = eventRepository.getEventById(eventId)
                if (event != null) {
                    _uiState.update { it.copy(event = event, isLoading = false) }
                    loadOrganizer(event.organizerId)
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "Event not found"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to load event"
                    ) 
                }
            }
        }
    }

    private fun loadOrganizer(organizerId: String) {
        viewModelScope.launch {
            try {
                val organizer = userRepository.getUserById(organizerId)
                _uiState.update { it.copy(organizer = organizer) }
            } catch (e: Exception) {
                // Organizer loading failed, but don't show error to user
                // Event details can still be shown without organizer info
            }
        }
    }

    fun joinEvent() {
        val currentEvent = _uiState.value.event ?: return
        val currentUser = _uiState.value.currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, error = null) }
            
            try {
                eventRepository.joinEvent(currentEvent.id, currentUser.id)
                _uiState.update { it.copy(isJoining = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isJoining = false, 
                        error = e.message ?: "Failed to join event"
                    ) 
                }
            }
        }
    }

    fun leaveEvent() {
        val currentEvent = _uiState.value.event ?: return
        val currentUser = _uiState.value.currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLeaving = true, error = null) }
            
            try {
                eventRepository.leaveEvent(currentEvent.id, currentUser.id)
                _uiState.update { it.copy(isLeaving = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLeaving = false, 
                        error = e.message ?: "Failed to leave event"
                    ) 
                }
            }
        }
    }

    fun refreshEvent() {
        loadEventDetail()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}