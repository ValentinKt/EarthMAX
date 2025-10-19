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
import com.earthmax.data.repository.EventRepository
import com.earthmax.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
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
    val imageError: String? = null,
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
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
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
        Logger.logUserAction("CreateEventViewModel", "updateTitle", mapOf("titleLength" to title.length))
        _uiState.update { it.copy(title = title, hasInteractedWithTitle = true) }
    }

    fun updateDescription(description: String) {
        Logger.logUserAction("CreateEventViewModel", "updateDescription", mapOf("descriptionLength" to description.length))
        _uiState.update { it.copy(description = description, hasInteractedWithDescription = true) }
    }

    fun updateLocation(location: String) {
        Logger.logUserAction("CreateEventViewModel", "updateLocation", mapOf("locationLength" to location.length))
        _uiState.update { it.copy(location = location, hasInteractedWithLocation = true) }
    }

    fun updateSelectedDate(date: Date) {
        Logger.logUserAction("CreateEventViewModel", "updateSelectedDate", mapOf("selectedDate" to date.toString()))
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun updateSelectedTime(time: Date) {
        Logger.logUserAction("CreateEventViewModel", "updateSelectedTime", mapOf("selectedTime" to time.toString()))
        _uiState.update { it.copy(selectedTime = time) }
    }

    fun updateMaxParticipants(maxParticipants: Int) {
        Logger.logUserAction("CreateEventViewModel", "updateMaxParticipants", mapOf("maxParticipants" to maxParticipants))
        _uiState.update { 
            it.copy(
                maxParticipants = maxParticipants.coerceAtLeast(0),
                hasInteractedWithMaxParticipants = true
            ) 
        }
    }

    fun updateSelectedCategory(category: EventCategory) {
        Logger.logUserAction("CreateEventViewModel", "updateSelectedCategory", mapOf("category" to category.name))
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateSelectedImageUri(uri: Uri?) {
        Logger.logUserAction("CreateEventViewModel", "updateSelectedImageUri", mapOf("hasImage" to (uri != null)))
        val imageError = validateImage(uri)
        if (imageError != null) {
            Logger.w("CreateEventViewModel", "Image validation failed: $imageError")
        }
        _uiState.update { 
            it.copy(
                selectedImageUri = uri,
                imageError = imageError
            ) 
        }
    }

    private fun validateImage(uri: Uri?): String? {
        if (uri == null) return null
        
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            
            // Check if it's a valid image format
            val validImageTypes = listOf("image/jpeg", "image/jpg", "image/png", "image/webp")
            if (mimeType !in validImageTypes) {
                return "Please select a valid image format (JPEG, PNG, or WebP)"
            }
            
            // Check file size (max 5MB)
            val inputStream = contentResolver.openInputStream(uri)
            val fileSize = inputStream?.available() ?: 0
            inputStream?.close()
            
            val maxSizeBytes = 5 * 1024 * 1024 // 5MB
            if (fileSize > maxSizeBytes) {
                return "Image size must be less than 5MB"
            }
            
            null // No error
        } catch (e: Exception) {
            "Unable to validate image. Please try selecting another image."
        }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        Logger.enter("CreateEventViewModel", "compressImage")
        val startTime = System.currentTimeMillis()
        
        return try {
            Logger.d("CreateEventViewModel", "Starting image compression for URI: $uri")
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) {
                Logger.w("CreateEventViewModel", "Failed to decode bitmap from URI")
                return null
            }
            
            // Calculate compression ratio based on original size
            val originalSize = originalBitmap.byteCount
            val maxSizeBytes = 5 * 1024 * 1024 // 5MB
            Logger.d("CreateEventViewModel", "Original image size: $originalSize bytes")
            
            // If image is already small enough, use minimal compression
            var quality = if (originalSize <= maxSizeBytes) 90 else 70
            
            // Calculate target dimensions (max 1920x1080 for large images)
            val maxWidth = 1920
            val maxHeight = 1080
            var targetWidth = originalBitmap.width
            var targetHeight = originalBitmap.height
            
            if (targetWidth > maxWidth || targetHeight > maxHeight) {
                val ratio = minOf(
                    maxWidth.toFloat() / targetWidth,
                    maxHeight.toFloat() / targetHeight
                )
                targetWidth = (targetWidth * ratio).toInt()
                targetHeight = (targetHeight * ratio).toInt()
                Logger.d("CreateEventViewModel", "Resizing image from ${originalBitmap.width}x${originalBitmap.height} to ${targetWidth}x${targetHeight}")
            }
            
            // Resize bitmap if needed
            val resizedBitmap = if (targetWidth != originalBitmap.width || targetHeight != originalBitmap.height) {
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }
            
            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            var compressedData: ByteArray
            
            do {
                outputStream.reset()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedData = outputStream.toByteArray()
                quality -= 10 // Reduce quality if still too large
                Logger.d("CreateEventViewModel", "Compressed image size: ${compressedData.size} bytes at quality $quality")
            } while (compressedData.size > maxSizeBytes && quality > 10)
            
            outputStream.close()
            
            // Clean up bitmaps
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }
            originalBitmap.recycle()
            
            val compressionTime = System.currentTimeMillis() - startTime
            Logger.logPerformance("CreateEventViewModel", "compressImage", compressionTime, mapOf(
                "originalSize" to originalSize,
                "compressedSize" to compressedData.size,
                "finalQuality" to quality
            ))
            Logger.exit("CreateEventViewModel", "compressImage")
            
            compressedData
        } catch (e: Exception) {
            Logger.e("CreateEventViewModel", "Image compression failed", e)
            Logger.exit("CreateEventViewModel", "compressImage")
            null
        }
    }

    fun createEvent() {
        Logger.enter("CreateEventViewModel", "createEvent")
        Logger.logUserAction("CreateEventViewModel", "createEvent", mapOf("action" to "attempt_create_event"))
        
        val state = _uiState.value
        val currentUser = state.currentUser
        
        Logger.d("CreateEventViewModel", "Creating event with user: ${currentUser?.id ?: "null"}")
        Logger.logBusinessEvent("CreateEventViewModel", "Event creation started", mapOf(
            "hasUser" to (currentUser != null),
            "title" to state.title,
            "category" to state.selectedCategory.name,
            "maxParticipants" to state.maxParticipants,
            "hasImage" to (state.selectedImageUri != null)
        ))
        
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
            val errorMessage = if (currentUser == null) "Please log in to create an event" 
                              else "Please fix the errors above before creating the event"
            Logger.w("CreateEventViewModel", "Event creation validation failed: $errorMessage")
            Logger.logBusinessEvent("CreateEventViewModel", "Event creation validation failed", mapOf(
                "reason" to errorMessage,
                "isFormValid" to state.isFormValid,
                "hasUser" to (currentUser != null)
            ))
            _uiState.update { 
                it.copy(error = errorMessage) 
            }
            Logger.exit("CreateEventViewModel", "createEvent")
            return
        }

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                Logger.d("CreateEventViewModel", "Processing event creation...")
                
                // Combine date and time
                val calendar = Calendar.getInstance()
                calendar.time = state.selectedDate!!
                
                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = state.selectedTime!!
                
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                
                val eventDateTime = calendar.time
                Logger.d("CreateEventViewModel", "Event date/time set to: $eventDateTime")

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

                Logger.d("CreateEventViewModel", "Calling eventRepository.createEvent with event ID: ${event.id}")
                val result = eventRepository.createEvent(event)
                
                if (result.isSuccess) {
                    val createdEventId = result.getOrNull() ?: ""
                    Logger.i("CreateEventViewModel", "Event created successfully with ID: $createdEventId")
                    Logger.logBusinessEvent("CreateEventViewModel", "Event created successfully", mapOf(
                        "eventId" to createdEventId,
                        "title" to event.title,
                        "category" to event.category.name
                    ))
                    
                    // Upload image if selected
                    state.selectedImageUri?.let { uri ->
                        try {
                            Logger.d("CreateEventViewModel", "Uploading event image...")
                            val imageData = compressImage(uri)
                            if (imageData != null) {
                                Logger.d("CreateEventViewModel", "Image compressed successfully, uploading...")
                                eventRepository.uploadEventPhoto(createdEventId, imageData)
                                Logger.i("CreateEventViewModel", "Event image uploaded successfully")
                            } else {
                                // Log compression failure but don't fail the entire operation
                                Logger.w("CreateEventViewModel", "Image compression failed, but event was created successfully")
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        error = "Image processing failed, but event was created successfully"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // Image upload failed, but event was created successfully
                            Logger.e("CreateEventViewModel", "Image upload failed, but event was created successfully", e)
                            _uiState.update { currentState ->
                                currentState.copy(
                                    error = "Image upload failed, but event was created successfully: ${e.message}"
                                )
                            }
                        }
                    }
                    
                    val creationTime = System.currentTimeMillis() - startTime
                    Logger.logPerformance("CreateEventViewModel", "createEvent", creationTime, mapOf(
                        "eventId" to createdEventId,
                        "hasImage" to (state.selectedImageUri != null)
                    ))
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEventCreated = true,
                            showSuccessMessage = true,
                            error = null
                        )
                    }
                    Logger.logBusinessEvent("CreateEventViewModel", "Event creation completed successfully", mapOf(
                        "eventId" to createdEventId,
                        "totalTime" to creationTime
                    ))
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
                
                Logger.e("CreateEventViewModel", "Event creation failed: $errorMessage", e)
                Logger.logBusinessEvent("CreateEventViewModel", "Event creation failed", mapOf(
                    "error" to errorMessage,
                    "exception" to e.javaClass.simpleName
                ))
                
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = errorMessage
                    ) 
                }
            }
            Logger.exit("CreateEventViewModel", "createEvent")
        }
    }

    fun clearError() {
        Logger.logUserAction("CreateEventViewModel", "clearError")
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSuccessMessage() {
        Logger.logUserAction("CreateEventViewModel", "dismissSuccessMessage")
        _uiState.update { it.copy(showSuccessMessage = false) }
    }

    fun resetForm() {
        Logger.logUserAction("CreateEventViewModel", "resetForm")
        Logger.logBusinessEvent("CreateEventViewModel", "Form reset", mapOf("action" to "reset_create_event_form"))
        _uiState.update { 
            CreateEventUiState(currentUser = it.currentUser)
        }
    }
}