package com.earthmax.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.models.User
import com.earthmax.data.repository.EventRepository
import com.earthmax.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: EventCategory? = null,
    val searchQuery: String = "",
    val currentUser: User? = null,
    val isLocationPermissionGranted: Boolean = false
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<EventCategory?>(null)
    private val _searchQuery = MutableStateFlow("")

    val events: Flow<PagingData<Event>> = combine(
        _selectedCategory,
        _searchQuery
    ) { category, query ->
        when {
            query.isNotBlank() -> eventRepository.searchEvents(query)
            category != null -> eventRepository.getEventsByCategory(category)
            else -> eventRepository.getAllEvents()
        }
    }.flatMapLatest { it }
        .cachedIn(viewModelScope)

    val currentUser: StateFlow<User?> = userRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun setSelectedCategory(category: EventCategory?) {
        _selectedCategory.value = category
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isLocationPermissionGranted = granted) }
    }

    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = currentUser.value?.id
                if (userId == null) {
                    _uiState.update { it.copy(error = "No current user") }
                } else {
                    eventRepository.joinEvent(eventId, userId)
                    clearError()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to join event") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = currentUser.value?.id
                if (userId == null) {
                    _uiState.update { it.copy(error = "No current user") }
                } else {
                    eventRepository.leaveEvent(eventId, userId)
                    clearError()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to leave event") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}