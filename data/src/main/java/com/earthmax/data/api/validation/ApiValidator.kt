package com.earthmax.data.api.validation

import com.earthmax.data.api.dto.CreateEventRequest
import com.earthmax.data.api.dto.CreateUserRequest
import com.earthmax.data.api.dto.UpdateEventRequest
import com.earthmax.data.api.dto.UpdateUserRequest
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for validating API request data
 */
@Singleton
class ApiValidator @Inject constructor() {
    
    companion object {
        private val EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        
        private val PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$")
    }
    
    /**
     * Validates user creation request
     */
    fun validateCreateUserRequest(request: CreateUserRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Email validation
        if (request.email.isBlank()) {
            errors.add("Email is required")
        } else if (!EMAIL_PATTERN.matcher(request.email).matches()) {
            errors.add("Invalid email format")
        }
        
        // Display name validation
        if (request.displayName.isBlank()) {
            errors.add("Display name is required")
        } else if (request.displayName.length < 2) {
            errors.add("Display name must be at least 2 characters")
        } else if (request.displayName.length > 50) {
            errors.add("Display name must not exceed 50 characters")
        }
        
        // Phone validation (if provided)
        request.phone?.let { phone ->
            if (phone.isNotBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
                errors.add("Invalid phone number format")
            }
        }
        
        // Bio validation (if provided)
        request.bio?.let { bio ->
            if (bio.length > 500) {
                errors.add("Bio must not exceed 500 characters")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates user update request
     */
    fun validateUpdateUserRequest(request: UpdateUserRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Display name validation (if provided)
        request.displayName?.let { displayName ->
            if (displayName.isBlank()) {
                errors.add("Display name cannot be empty")
            } else if (displayName.length < 2) {
                errors.add("Display name must be at least 2 characters")
            } else if (displayName.length > 50) {
                errors.add("Display name must not exceed 50 characters")
            }
        }
        
        // Phone validation (if provided)
        request.phone?.let { phone ->
            if (phone.isNotBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
                errors.add("Invalid phone number format")
            }
        }
        
        // Bio validation (if provided)
        request.bio?.let { bio ->
            if (bio.length > 500) {
                errors.add("Bio must not exceed 500 characters")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates event creation request
     */
    fun validateCreateEventRequest(request: CreateEventRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Title validation
        if (request.title.isBlank()) {
            errors.add("Event title is required")
        } else if (request.title.length < 3) {
            errors.add("Event title must be at least 3 characters")
        } else if (request.title.length > 100) {
            errors.add("Event title must not exceed 100 characters")
        }
        
        // Description validation
        if (request.description.isBlank()) {
            errors.add("Event description is required")
        } else if (request.description.length < 10) {
            errors.add("Event description must be at least 10 characters")
        } else if (request.description.length > 1000) {
            errors.add("Event description must not exceed 1000 characters")
        }
        
        // Category validation
        if (request.category.isBlank()) {
            errors.add("Event category is required")
        }
        
        // Location validation
        if (request.location.isBlank()) {
            errors.add("Event location is required")
        }
        
        // Coordinates validation (if provided)
        request.latitude?.let { latitude ->
            if (latitude < -90 || latitude > 90) {
                errors.add("Invalid latitude value")
            }
        }
        
        request.longitude?.let { longitude ->
            if (longitude < -180 || longitude > 180) {
                errors.add("Invalid longitude value")
            }
        }
        
        // Max participants validation (if provided)
        request.maxParticipants?.let { maxParticipants ->
            if (maxParticipants <= 0) {
                errors.add("Maximum participants must be greater than 0")
            } else if (maxParticipants > 10000) {
                errors.add("Maximum participants cannot exceed 10,000")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates event update request
     */
    fun validateUpdateEventRequest(request: UpdateEventRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Title validation (if provided)
        request.title?.let { title ->
            if (title.isBlank()) {
                errors.add("Event title cannot be empty")
            } else if (title.length < 3) {
                errors.add("Event title must be at least 3 characters")
            } else if (title.length > 100) {
                errors.add("Event title must not exceed 100 characters")
            }
        }
        
        // Description validation (if provided)
        request.description?.let { description ->
            if (description.isBlank()) {
                errors.add("Event description cannot be empty")
            } else if (description.length < 10) {
                errors.add("Event description must be at least 10 characters")
            } else if (description.length > 1000) {
                errors.add("Event description must not exceed 1000 characters")
            }
        }
        
        // Location validation (if provided)
        request.location?.let { location ->
            if (location.isBlank()) {
                errors.add("Event location cannot be empty")
            }
        }
        
        // Coordinates validation (if provided)
        request.latitude?.let { latitude ->
            if (latitude < -90 || latitude > 90) {
                errors.add("Invalid latitude value")
            }
        }
        
        request.longitude?.let { longitude ->
            if (longitude < -180 || longitude > 180) {
                errors.add("Invalid longitude value")
            }
        }
        
        // Max participants validation (if provided)
        request.maxParticipants?.let { maxParticipants ->
            if (maxParticipants <= 0) {
                errors.add("Maximum participants must be greater than 0")
            } else if (maxParticipants > 10000) {
                errors.add("Maximum participants cannot exceed 10,000")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates search query
     */
    fun validateSearchQuery(query: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (query.isBlank()) {
            errors.add("Search query cannot be empty")
        } else if (query.length < 2) {
            errors.add("Search query must be at least 2 characters")
        } else if (query.length > 100) {
            errors.add("Search query must not exceed 100 characters")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates pagination parameters
     */
    fun validatePaginationParams(page: Int, limit: Int): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (page < 1) {
            errors.add("Page number must be greater than 0")
        }
        
        if (limit < 1) {
            errors.add("Limit must be greater than 0")
        } else if (limit > 100) {
            errors.add("Limit cannot exceed 100")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
}

/**
 * Result of validation operation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    fun getErrorMessage(): String {
        return errors.joinToString("; ")
    }
}