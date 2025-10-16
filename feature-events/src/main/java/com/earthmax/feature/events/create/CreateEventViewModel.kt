package com.earthmax.feature.events.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.models.User
import com.earthmax.data.repository.EventRepository
import com.earthmax.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class CreateEventUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEventCreated: Boolean = false,
    val currentUser: User? = null,
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val selectedDate: Date? = null,
    val selectedTime: Date? = null,
    val maxParticipants: Int = 10,
    val selectedCategory: EventCategory = EventCategory.OTHER,
    val selectedImageUri: Uri? = null,
    val isFormValid: Boolean = false
)

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        observeCurrentUser()
        observeFormValidation()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun observeFormValidation() {
        viewModelScope.launch {
            _uiState.collect { state ->
                val isValid = state.title.isNotBlank() &&
                        state.description.isNotBlank() &&
                        state.location.isNotBlank() &&
                        state.selectedDate != null &&
                        state.selectedTime != null &&
                        state.maxParticipants > 0
                
                if (state.isFormValid != isValid) {
                    _uiState.update { it.copy(isFormValid = isValid) }
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateLocation(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    fun updateSelectedDate(date: Date) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun updateSelectedTime(time: Date) {
        _uiState.update { it.copy(selectedTime = time) }
    }

    fun updateMaxParticipants(maxParticipants: Int) {
        _uiState.update { it.copy(maxParticipants = maxParticipants.coerceAtLeast(1)) }
    }

    fun updateSelectedCategory(category: EventCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateSelectedImageUri(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun createEvent() {
        val state = _uiState.value
        val currentUser = state.currentUser
        
        if (!state.isFormValid || currentUser == null) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Combine date and time
                val calendar = Calendar.getInstance()
                calendar.time = state.selectedDate!!
                
                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = state.selectedTime!!
                
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                
                val eventDateTime = calendar.time

                val event = Event(
                    id = UUID.randomUUID().toString(),
                    title = state.title,
                    description = state.description,
                    location = state.location,
                    dateTime = eventDateTime,
                    organizerId = currentUser.id,
                    maxParticipants = state.maxParticipants,
                    category = state.selectedCategory,
                    imageUrl = "", // Will be set after image upload
                    isJoined = true, // Creator automatically joins
                    todoItems = emptyList(),
                    photos = emptyList(),
                    createdAt = Date(),
                    updatedAt = Date()
                )

                val result = eventRepository.createEvent(event)
                if (result.isSuccess) {
                    val createdEventId = result.getOrNull() ?: ""
                    // Upload image if selected
                    state.selectedImageUri?.let { uri ->
                        eventRepository.uploadEventPhoto(createdEventId, uri.toString())
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEventCreated = true,
                            error = null
                        )
                    }
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to create event")
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to create event"
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetForm() {
        _uiState.update { 
            CreateEventUiState(currentUser = it.currentUser)
        }
    }
}