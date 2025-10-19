package com.earthmax.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.models.User
import com.earthmax.core.utils.Logger
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
        Logger.enter("EventsViewModel", "init")
        Logger.i("EventsViewModel", "Initializing EventsViewModel")
        observeCurrentUser()
        Logger.exit("EventsViewModel", "init")
    }

    private fun observeCurrentUser() {
        Logger.enter("EventsViewModel", "observeCurrentUser")
        viewModelScope.launch {
            Logger.d("EventsViewModel", "Starting to observe current user changes")
            currentUser.collect { user ->
                Logger.logBusinessEvent("EventsViewModel", "user_state_changed", mapOf(
                    "hasUser" to (user != null),
                    "userId" to (user?.id ?: "null")
                ))
                _uiState.update { it.copy(currentUser = user) }
                Logger.d("EventsViewModel", "UI state updated with user: ${user?.displayName ?: "null"}")
            }
        }
        Logger.exit("EventsViewModel", "observeCurrentUser")
    }

    fun setSelectedCategory(category: EventCategory?) {
        Logger.enter("EventsViewModel", "setSelectedCategory")
        Logger.logUserAction("EventsViewModel", "category_filter_changed", mapOf(
            "previousCategory" to (_selectedCategory.value?.name ?: "null"),
            "newCategory" to (category?.name ?: "null")
        ))
        _selectedCategory.value = category
        _uiState.update { it.copy(selectedCategory = category) }
        Logger.d("EventsViewModel", "Category filter updated to: ${category?.name ?: "All"}")
        Logger.exit("EventsViewModel", "setSelectedCategory")
    }

    fun setSearchQuery(query: String) {
        Logger.enter("EventsViewModel", "setSearchQuery")
        Logger.logUserAction("EventsViewModel", "search_query_changed", mapOf(
            "queryLength" to query.length,
            "hasQuery" to query.isNotBlank()
        ))
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
        Logger.d("EventsViewModel", "Search query updated: ${if (query.isBlank()) "cleared" else "set (${query.length} chars)"}")
        Logger.exit("EventsViewModel", "setSearchQuery")
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        Logger.enter("EventsViewModel", "setLocationPermissionGranted")
        Logger.logBusinessEvent("EventsViewModel", "location_permission_changed", mapOf(
            "granted" to granted
        ))
        _uiState.update { it.copy(isLocationPermissionGranted = granted) }
        Logger.i("EventsViewModel", "Location permission ${if (granted) "granted" else "denied"}")
        Logger.exit("EventsViewModel", "setLocationPermissionGranted")
    }

    fun joinEvent(eventId: String) {
        Logger.enter("EventsViewModel", "joinEvent")
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.logUserAction("EventsViewModel", "join_event_initiated", mapOf(
                "eventId" to eventId
            ))
            _uiState.update { it.copy(isLoading = true) }
            Logger.d("EventsViewModel", "Starting join event process for eventId: $eventId")
            
            try {
                val userId = currentUser.value?.id
                if (userId == null) {
                    Logger.w("EventsViewModel", "Join event failed: No current user available")
                    _uiState.update { it.copy(error = "No current user") }
                } else {
                    Logger.d("EventsViewModel", "Calling repository to join event")
                    eventRepository.joinEvent(eventId, userId)
                    Logger.logBusinessEvent("EventsViewModel", "event_joined_successfully", mapOf(
                        "eventId" to eventId,
                        "userId" to userId
                    ))
                    Logger.i("EventsViewModel", "Successfully joined event: $eventId")
                    clearError()
                }
            } catch (e: Exception) {
                Logger.e("EventsViewModel", "Failed to join event: $eventId", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to join event") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                Logger.logPerformance("EventsViewModel", "join_event_duration", System.currentTimeMillis() - startTime)
                Logger.d("EventsViewModel", "Join event process completed")
            }
        }
        Logger.exit("EventsViewModel", "joinEvent")
    }

    fun leaveEvent(eventId: String) {
        Logger.enter("EventsViewModel", "leaveEvent")
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.logUserAction("EventsViewModel", "leave_event_initiated", mapOf(
                "eventId" to eventId
            ))
            _uiState.update { it.copy(isLoading = true) }
            Logger.d("EventsViewModel", "Starting leave event process for eventId: $eventId")
            
            try {
                val userId = currentUser.value?.id
                if (userId == null) {
                    Logger.w("EventsViewModel", "Leave event failed: No current user available")
                    _uiState.update { it.copy(error = "No current user") }
                } else {
                    Logger.d("EventsViewModel", "Calling repository to leave event")
                    eventRepository.leaveEvent(eventId, userId)
                    Logger.logBusinessEvent("EventsViewModel", "event_left_successfully", mapOf(
                        "eventId" to eventId,
                        "userId" to userId
                    ))
                    Logger.i("EventsViewModel", "Successfully left event: $eventId")
                    clearError()
                }
            } catch (e: Exception) {
                Logger.e("EventsViewModel", "Failed to leave event: $eventId", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to leave event") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                Logger.logPerformance("EventsViewModel", "leave_event_duration", System.currentTimeMillis() - startTime)
                Logger.d("EventsViewModel", "Leave event process completed")
            }
        }
        Logger.exit("EventsViewModel", "leaveEvent")
    }

    fun clearError() {
        Logger.enter("EventsViewModel", "clearError")
        Logger.d("EventsViewModel", "Clearing error state")
        _uiState.update { it.copy(error = null) }
        Logger.exit("EventsViewModel", "clearError")
    }
}