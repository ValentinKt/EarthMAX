package com.earthmax.feature.events.create

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.models.User
import com.earthmax.core.utils.Logger
import com.earthmax.domain.repository.EventRepository
import com.earthmax.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject
import com.earthmax.data.mappers.toUser
import com.earthmax.data.mappers.toDomainEvent

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
    val imageError: String? = null,
    // Form interaction flags
    val hasInteractedWithTitle: Boolean = false,
    val hasInteractedWithDescription: Boolean = false,
    val hasInteractedWithLocation: Boolean = false,
    val hasInteractedWithMaxParticipants: Boolean = false,
    val showSuccessMessage: Boolean = false
)

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    // Map domain Result<DomainUser?> to core User? for UI
    val currentUser: StateFlow<User?> = userRepository.getCurrentUser()
        .map { result ->
            when (result) {
                is com.earthmax.domain.model.Result.Success -> result.data?.toUser()
                is com.earthmax.domain.model.Result.Error -> {
                    Logger.e("CreateEventViewModel", "Error fetching current user", result.exception)
                    null
                }
                com.earthmax.domain.model.Result.Loading -> null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        Logger.enter("CreateEventViewModel", "init")
        Logger.logBusinessEvent("CreateEventViewModel", "Initializing create event view model")
        observeCurrentUser()
        observeFormValidation()
        Logger.exit("CreateEventViewModel", "init")
    }

    private fun observeCurrentUser() {
        Logger.enter("CreateEventViewModel", "observeCurrentUser")
        viewModelScope.launch {
            currentUser.collect { user ->
                Logger.d("CreateEventViewModel", "Current user updated: ${if (user != null) "authenticated" else "not authenticated"}")
                Logger.logBusinessEvent("CreateEventViewModel", "User state changed", mapOf(
                    "isAuthenticated" to (user != null),
                    "userId" to (user?.id ?: "null")
                ))
                _uiState.update { it.copy(currentUser = user) }
            }
        }
        Logger.exit("CreateEventViewModel", "observeCurrentUser")
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
                
                val isFormValid = titleError == null && 
                                descriptionError == null && 
                                locationError == null && 
                                dateError == null && 
                                timeError == null && 
                                maxParticipantsError == null &&
                                state.imageError == null
                
                _uiState.update {
                    it.copy(
                        titleError = titleError,
                        descriptionError = descriptionError,
                        locationError = locationError,
                        dateError = dateError,
                        timeError = timeError,
                        maxParticipantsError = maxParticipantsError,
                        isFormValid = isFormValid
                    )
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title, hasInteractedWithTitle = true) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description, hasInteractedWithDescription = true) }
    }

    fun onLocationChanged(location: String) {
        _uiState.update { it.copy(location = location, hasInteractedWithLocation = true) }
    }

    fun onDateSelected(date: Date?) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onTimeSelected(time: Date?) {
        _uiState.update { it.copy(selectedTime = time) }
    }

    fun onMaxParticipantsChanged(max: Int) {
        _uiState.update { it.copy(maxParticipants = max, hasInteractedWithMaxParticipants = true) }
    }

    fun onCategorySelected(category: EventCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri, imageError = null) }
    }

    // Bridging methods for UI compatibility
    fun updateSelectedImageUri(uri: Uri?) = onImageSelected(uri)
    fun updateTitle(title: String) = onTitleChanged(title)
    fun updateDescription(description: String) = onDescriptionChanged(description)
    fun updateLocation(location: String) = onLocationChanged(location)
    fun updateSelectedDate(date: Date) = onDateSelected(date)
    fun updateSelectedTime(time: Date) = onTimeSelected(time)
    fun updateMaxParticipants(max: Int) = onMaxParticipantsChanged(max)
    fun updateSelectedCategory(category: EventCategory) = onCategorySelected(category)
    private fun validateTitle(title: String, hasInteracted: Boolean): String? {
        if (!hasInteracted) return null
        return when {
            title.isBlank() -> "Title is required"
            title.length < 5 -> "Title must be at least 5 characters"
            else -> null
        }
    }

    private fun validateDescription(description: String, hasInteracted: Boolean): String? {
        if (!hasInteracted) return null
        return when {
            description.isBlank() -> "Description is required"
            description.length < 10 -> "Description must be at least 10 characters"
            else -> null
        }
    }

    private fun validateLocation(location: String, hasInteracted: Boolean): String? {
        if (!hasInteracted) return null
        return when {
            location.isBlank() -> "Location is required"
            location.length < 3 -> "Location must be at least 3 characters"
            else -> null
        }
    }

    private fun validateDate(date: Date?): String? {
        val today = Date()
        return when {
            date == null -> "Please select a date"
            date.before(today) -> "Date must be in the future"
            else -> null
        }
    }

    private fun validateTime(time: Date?): String? {
        return when {
            time == null -> "Please select a time"
            else -> null
        }
    }

    private fun validateMaxParticipants(max: Int, hasInteracted: Boolean): String? {
        if (!hasInteracted) return null
        return when {
            max <= 0 -> "Must be greater than 0"
            max > 10000 -> "Too many participants"
            else -> null
        }
    }

    fun onTitleInteraction() { _uiState.update { it.copy(hasInteractedWithTitle = true) } }
    fun onDescriptionInteraction() { _uiState.update { it.copy(hasInteractedWithDescription = true) } }
    fun onLocationInteraction() { _uiState.update { it.copy(hasInteractedWithLocation = true) } }
    fun onMaxParticipantsInteraction() { _uiState.update { it.copy(hasInteractedWithMaxParticipants = true) } }

    private suspend fun compressImage(content: ByteArray): ByteArray {
        Logger.enter("CreateEventViewModel", "compressImage")
        val start = System.nanoTime()
        val originalBitmap = BitmapFactory.decodeByteArray(content, 0, content.size)
        val maxSize = 1024

        val scale = minOf(
            maxSize.toFloat() / originalBitmap.width,
            maxSize.toFloat() / originalBitmap.height
        )
        val targetWidth = (originalBitmap.width * scale).toInt()
        val targetHeight = (originalBitmap.height * scale).toInt()

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        val outputStream = ByteArrayOutputStream()

        var quality = 85
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        var compressedBytes = outputStream.toByteArray()

        while (compressedBytes.size > 500 * 1024 && quality > 40) {
            outputStream.reset()
            quality -= 5
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedBytes = outputStream.toByteArray()
        }

        val end = System.nanoTime()
        Logger.d("CreateEventViewModel", "Compression completed in ${(end - start) / 1_000_000} ms with quality $quality")
        Logger.exit("CreateEventViewModel", "compressImage")
        return compressedBytes
    }

    fun createEvent(userId: String) {
        Logger.enter("CreateEventViewModel", "createEvent")
        viewModelScope.launch {
            Logger.logBusinessEvent("CreateEventViewModel", "User initiated event creation")
            
            val state = _uiState.value
            if (!state.isFormValid) {
                Logger.w("CreateEventViewModel", "Cannot create event: form is invalid")
                _uiState.update { it.copy(error = "Please correct validation errors before submitting") }
                Logger.exit("CreateEventViewModel", "createEvent")
                return@launch
            }
            
            val currentUser = state.currentUser
            if (currentUser == null || currentUser.id != userId) {
                Logger.w("CreateEventViewModel", "User not authenticated or mismatched userId")
                _uiState.update { it.copy(error = "You must be logged in to create an event") }
                Logger.exit("CreateEventViewModel", "createEvent")
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val combinedDateTime = combineDateAndTime(state.selectedDate, state.selectedTime)
                
                val event = Event(
                    id = UUID.randomUUID().toString(),
                    title = state.title.trim(),
                    description = state.description.trim(),
                    location = state.location.trim(),
                    latitude = 0.0,
                    longitude = 0.0,
                    dateTime = combinedDateTime!!,
                    organizerId = userId,
                    organizerName = currentUser.displayName,
                    maxParticipants = state.maxParticipants,
                    currentParticipants = 0,
                    category = state.selectedCategory,
                    imageUrl = "",
                    isJoined = false,
                    todoItems = emptyList(),
                    photos = emptyList(),
                    createdAt = Date(),
                    updatedAt = Date()
                )

                val domainEvent = event.toDomainEvent()
                
                Logger.d("CreateEventViewModel", "Creating domain event with title: ${domainEvent.title}, category: ${domainEvent.category}")
                val startTime = System.nanoTime()
                val result = eventRepository.createEvent(domainEvent)
                val endTime = System.nanoTime()
                Logger.d("CreateEventViewModel", "createEvent call completed in ${(endTime - startTime) / 1_000_000} ms")

                when (result) {
                    is com.earthmax.domain.model.Result.Success -> {
                        Logger.i("CreateEventViewModel", "Event created successfully with id: ${result.data.id}")
                        _uiState.update { it.copy(isLoading = false, isEventCreated = true, showSuccessMessage = true) }
                    }
                    is com.earthmax.domain.model.Result.Error -> {
                        Logger.e("CreateEventViewModel", "Error creating event", result.exception)
                        _uiState.update { it.copy(isLoading = false, error = result.exception.message ?: "Unknown error") }
                    }
                    com.earthmax.domain.model.Result.Loading -> {
                        Logger.d("CreateEventViewModel", "Event creation in progress")
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: SecurityException) {
                Logger.e("CreateEventViewModel", "Permission error while creating event", e)
                _uiState.update { it.copy(isLoading = false, error = "Insufficient permissions to create event") }
            } catch (e: Exception) {
                Logger.e("CreateEventViewModel", "Unexpected error during event creation", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "An unexpected error occurred") }
            }
            
            Logger.exit("CreateEventViewModel", "createEvent")
        }
    }

    private fun combineDateAndTime(date: Date?, time: Date?): Date? {
        if (date == null || time == null) return null
        val calendarDate = Calendar.getInstance().apply { this.time = date }
        val calendarTime = Calendar.getInstance().apply { this.time = time }
        val combined = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarDate.get(Calendar.YEAR))
            set(Calendar.MONTH, calendarDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calendarDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, calendarTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, calendarTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return combined.time
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = false) }
    }

    fun resetForm() {
        _uiState.update { CreateEventUiState() }
    }

    // Overload for UI: creates event using current user
    fun createEvent() {
        val userId = _uiState.value.currentUser?.id
        if (userId.isNullOrEmpty()) {
            _uiState.update { it.copy(error = "You must be logged in to create an event") }
            return
        }
        createEvent(userId)
    }
}