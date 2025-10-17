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
    val maxParticipants: Int = 0,
    val selectedCategory: EventCategory = EventCategory.OTHER,
    val selectedImageUri: Uri? = null,
    val isFormValid: Boolean = false,
    // Field-specific validation errors
    val titleError: String? = null,
    val descriptionError: String? = null,
    val locationError: String? = null,
    val dateError: String? = null,
    val timeError: String? = null,
    val maxParticipantsError: String? = null,
    // Form interaction states
    val hasInteractedWithTitle: Boolean = false,
    val hasInteractedWithDescription: Boolean = false,
    val hasInteractedWithLocation: Boolean = false,
    val hasInteractedWithMaxParticipants: Boolean = false,
    // Success feedback
    val showSuccessMessage: Boolean = false
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
                val titleError = validateTitle(state.title, state.hasInteractedWithTitle)
                val descriptionError = validateDescription(state.description, state.hasInteractedWithDescription)
                val locationError = validateLocation(state.location, state.hasInteractedWithLocation)
                val dateError = validateDate(state.selectedDate)
                val timeError = validateTime(state.selectedTime)
                val maxParticipantsError = validateMaxParticipants(state.maxParticipants, state.hasInteractedWithMaxParticipants)
                
                val isValid = titleError == null &&
                        descriptionError == null &&
                        locationError == null &&
                        dateError == null &&
                        timeError == null &&
                        maxParticipantsError == null
                
                _uiState.update { 
                    it.copy(
                        isFormValid = isValid,
                        titleError = titleError,
                        descriptionError = descriptionError,
                        locationError = locationError,
                        dateError = dateError,
                        timeError = timeError,
                        maxParticipantsError = maxParticipantsError
                    )
                }
            }
        }
    }

    private fun validateTitle(title: String, hasInteracted: Boolean): String? {
        if (!hasInteracted) return null
        return when {
            title.isBlank() -> "Title is required"
            title.length < 3 -> "Title must be at least 3 characters"
            title.length > 100 -> "Title must be less than 100 characters"
            else -> null
        }
    }

    private fun validateDescription(description: String, hasInteracted: Boolean): String? {
        if (!hasInteracted) return null
        return when {
            description.isBlank() -> "Description is required"
            description.length < 10 -> "Description must be at least 10 characters"
            description.length > 500 -> "Description must be less than 500 characters"
            else -> null
        }
    }

    private fun validateLocation(location: String, hasInteracted: Boolean): String? {
        if (!hasInteracted) return null
        return when {
            location.isBlank() -> "Location is required"
            location.length < 3 -> "Location must be at least 3 characters"
            location.length > 100 -> "Location must be less than 100 characters"
            else -> null
        }
    }

    private fun validateDate(date: Date?): String? {
        return when {
            date == null -> "Date is required"
            date.before(Date()) -> "Date cannot be in the past"
            else -> null
        }
    }

    private fun validateTime(time: Date?): String? {
        return when {
            time == null -> "Time is required"
            else -> null
        }
    }

    private fun validateMaxParticipants(maxParticipants: Int, hasInteracted: Boolean): String? {
        if (!hasInteracted) return null
        return when {
            maxParticipants <= 0 -> "Maximum participants must be greater than 0"
            maxParticipants > 1000 -> "Maximum participants cannot exceed 1000"
            else -> null
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, hasInteractedWithTitle = true) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description, hasInteractedWithDescription = true) }
    }

    fun updateLocation(location: String) {
        _uiState.update { it.copy(location = location, hasInteractedWithLocation = true) }
    }

    fun updateSelectedDate(date: Date) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun updateSelectedTime(time: Date) {
        _uiState.update { it.copy(selectedTime = time) }
    }

    fun updateMaxParticipants(maxParticipants: Int) {
        _uiState.update { 
            it.copy(
                maxParticipants = maxParticipants.coerceAtLeast(0),
                hasInteractedWithMaxParticipants = true
            ) 
        }
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
        
        // Mark all fields as interacted to show validation errors
        _uiState.update { 
            it.copy(
                hasInteractedWithTitle = true,
                hasInteractedWithDescription = true,
                hasInteractedWithLocation = true,
                hasInteractedWithMaxParticipants = true
            )
        }
        
        if (!state.isFormValid || currentUser == null) {
            _uiState.update { 
                it.copy(
                    error = if (currentUser == null) "Please log in to create an event" 
                           else "Please fix the errors above before creating the event"
                ) 
            }
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
                    title = state.title.trim(),
                    description = state.description.trim(),
                    location = state.location.trim(),
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
                        try {
                            eventRepository.uploadEventPhoto(createdEventId, uri.toString())
                        } catch (e: Exception) {
                            // Image upload failed, but event was created successfully
                            // Log the error but don't fail the entire operation
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEventCreated = true,
                            showSuccessMessage = true,
                            error = null
                        )
                    }
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to create event")
                }
                
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your connection and try again."
                    e.message?.contains("permission", ignoreCase = true) == true -> 
                        "Permission denied. Please check your account permissions."
                    else -> e.message ?: "Failed to create event. Please try again."
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = errorMessage
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = false) }
    }

    fun resetForm() {
        _uiState.update { 
            CreateEventUiState(currentUser = it.currentUser)
        }
    }
}