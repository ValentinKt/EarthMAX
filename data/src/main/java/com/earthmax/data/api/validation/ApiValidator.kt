package com.earthmax.data.api.validation

import com.earthmax.core.utils.Logger
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
        private const val TAG = "ApiValidator"
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
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "validateCreateUserRequest", 
            "email" to Logger.maskSensitiveData(request.email),
            "displayName" to Logger.maskSensitiveData(request.displayName)
        )
        
        val errors = mutableListOf<String>()
        
        // Email validation
        if (request.email.isBlank()) {
            errors.add("Email is required")
            Logger.logError(TAG, "User validation failed - email required", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "email",
                "errorMessage" to "Email is required"
            ))
        } else if (!EMAIL_PATTERN.matcher(request.email).matches()) {
            errors.add("Invalid email format")
            Logger.logError(TAG, "User validation failed - invalid email format", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "email",
                "errorMessage" to "Invalid email format",
                "email" to Logger.maskSensitiveData(request.email)
            ))
        }
        
        // Display name validation
        if (request.displayName.isBlank()) {
            errors.add("Display name is required")
            Logger.logError(TAG, "User validation failed - display name required", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "displayName",
                "errorMessage" to "Display name is required"
            ))
        } else if (request.displayName.length < 2) {
            errors.add("Display name must be at least 2 characters")
            Logger.logError(TAG, "User validation failed - display name too short", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "displayName",
                "errorMessage" to "Display name must be at least 2 characters",
                "length" to request.displayName.length.toString()
            ))
        } else if (request.displayName.length > 50) {
            errors.add("Display name must not exceed 50 characters")
            Logger.logError(TAG, "User validation failed - display name too long", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "displayName",
                "errorMessage" to "Display name must not exceed 50 characters",
                "length" to request.displayName.length.toString()
            ))
        }
        
        // Phone validation (if provided)
        request.phone?.let { phone ->
            if (phone.isNotBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
                errors.add("Invalid phone number format")
                Logger.logError(TAG, "User validation failed - invalid phone format", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "phone",
                    "errorMessage" to "Invalid phone number format",
                    "phone" to Logger.maskSensitiveData(phone)
                ))
            }
        }
        
        // Bio validation (if provided)
        request.bio?.let { bio ->
            if (bio.length > 500) {
                errors.add("Bio must not exceed 500 characters")
                Logger.logError(TAG, "User validation failed - bio too long", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "bio",
                    "errorMessage" to "Bio must not exceed 500 characters",
                    "length" to bio.length.toString()
                ))
            }
        }
        
        val result = ValidationResult(errors.isEmpty(), errors)
        val duration = System.currentTimeMillis() - startTime
        
        Logger.logPerformance(TAG, "validateCreateUserRequest", duration, mapOf(
            "email" to Logger.maskSensitiveData(request.email),
            "displayName" to Logger.maskSensitiveData(request.displayName),
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        Logger.logBusinessEvent(TAG, "user_validation_success", mapOf(
            "email" to Logger.maskSensitiveData(request.email),
            "displayName" to Logger.maskSensitiveData(request.displayName)
        ))
        
        Logger.logBusinessEvent(TAG, "user_validation_failed", mapOf(
            "email" to Logger.maskSensitiveData(request.email),
            "displayName" to Logger.maskSensitiveData(request.displayName),
            "validationType" to "create_user",
            "errorCount" to errors.size.toString(),
            "errors" to errors.joinToString(", ")
        ))
        
        Logger.exit(TAG, "validateCreateUserRequest", mapOf(
            "email" to Logger.maskSensitiveData(request.email),
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        return result
    }
    
    /**
     * Validates user update request
     */
    fun validateUpdateUserRequest(request: UpdateUserRequest): ValidationResult {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "validateUpdateUserRequest", 
            "hasDisplayName" to request.displayName?.isNotBlank().toString(),
            "hasPhone" to request.phone?.isNotBlank().toString(),
            "hasBio" to request.bio?.isNotBlank().toString()
        )
        
        val errors = mutableListOf<String>()
        
        // Display name validation (if provided)
        request.displayName?.let { displayName ->
            if (displayName.isBlank()) {
                errors.add("Display name cannot be empty")
                Logger.logError(TAG, "User update validation failed - display name empty", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "displayName",
                    "errorMessage" to "Display name cannot be empty"
                ))
            } else if (displayName.length < 2) {
                errors.add("Display name must be at least 2 characters")
                Logger.logError(TAG, "User update validation failed - display name too short", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "displayName",
                    "errorMessage" to "Display name must be at least 2 characters",
                    "length" to displayName.length.toString()
                ))
            } else if (displayName.length > 50) {
                errors.add("Display name must not exceed 50 characters")
                Logger.logError(TAG, "User update validation failed - display name too long", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "displayName",
                    "errorMessage" to "Display name must not exceed 50 characters",
                    "length" to displayName.length.toString()
                ))
            }
        }
        
        // Phone validation (if provided)
        request.phone?.let { phone ->
            if (phone.isNotBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
                errors.add("Invalid phone number format")
                Logger.logError(TAG, "User update validation failed - invalid phone format", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "phone",
                    "errorMessage" to "Invalid phone number format",
                    "phone" to Logger.maskSensitiveData(phone)
                ))
            }
        }
        
        // Bio validation (if provided)
        request.bio?.let { bio ->
            if (bio.length > 500) {
                errors.add("Bio must not exceed 500 characters")
                Logger.logError(TAG, "User update validation failed - bio too long", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "bio",
                    "errorMessage" to "Bio must not exceed 500 characters",
                    "length" to bio.length.toString()
                ))
            }
        }
        
        val result = ValidationResult(errors.isEmpty(), errors)
        val duration = System.currentTimeMillis() - startTime
        
        Logger.logPerformance(TAG, "validateUpdateUserRequest", duration, mapOf(
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        if (result.isValid) {
            Logger.logBusinessEvent(TAG, "user_validation_success", mapOf(
                "validationType" to "update_user"
            ))
        } else {
            Logger.logBusinessEvent(TAG, "user_validation_failed", mapOf(
                "validationType" to "update_user",
                "errorCount" to errors.size.toString(),
                "errors" to errors.joinToString(", ")
            ))
        }
        
        Logger.exit(TAG, "validateUpdateUserRequest", mapOf(
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        return result
    }
    
    /**
     * Validates event creation request
     */
    fun validateCreateEventRequest(request: CreateEventRequest): ValidationResult {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "validateCreateEventRequest", 
            "title" to Logger.maskSensitiveData(request.title),
            "category" to request.category
        )
        
        val errors = mutableListOf<String>()
        
        // Title validation
        if (request.title.isBlank()) {
            errors.add("Event title is required")
            Logger.logError(TAG, "Event validation failed - title required", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "title",
                "errorMessage" to "Event title is required"
            ))
        } else if (request.title.length < 3) {
            errors.add("Event title must be at least 3 characters")
            Logger.logError(TAG, "Event validation failed - title too short", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "title",
                "errorMessage" to "Event title must be at least 3 characters",
                "length" to request.title.length.toString()
            ))
        } else if (request.title.length > 100) {
            errors.add("Event title must not exceed 100 characters")
            Logger.logError(TAG, "Event validation failed - title too long", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "title",
                "errorMessage" to "Event title must not exceed 100 characters",
                "length" to request.title.length.toString()
            ))
        }
        
        // Description validation
        if (request.description.isBlank()) {
            errors.add("Event description is required")
            Logger.logError(TAG, "Event validation failed - description required", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "description",
                "errorMessage" to "Event description is required"
            ))
        } else if (request.description.length < 10) {
            errors.add("Event description must be at least 10 characters")
            Logger.logError(TAG, "Event validation failed - description too short", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "description",
                "errorMessage" to "Event description must be at least 10 characters",
                "length" to request.description.length.toString()
            ))
        } else if (request.description.length > 1000) {
            errors.add("Event description must not exceed 1000 characters")
            Logger.logError(TAG, "Event validation failed - description too long", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "description",
                "errorMessage" to "Event description must not exceed 1000 characters",
                "length" to request.description.length.toString()
            ))
        }
        
        // Category validation
        if (request.category.isBlank()) {
            errors.add("Event category is required")
            Logger.logError(TAG, "Event validation failed - category required", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "category",
                "errorMessage" to "Event category is required"
            ))
        }
        
        // Location validation
        if (request.location.isBlank()) {
            errors.add("Event location is required")
            Logger.logError(TAG, "Event validation failed - location required", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "location",
                "errorMessage" to "Event location is required"
            ))
        }
        
        // Coordinates validation (if provided)
        request.latitude?.let { latitude ->
            if (latitude < -90 || latitude > 90) {
                errors.add("Invalid latitude value")
                Logger.logError(TAG, "Event validation failed - invalid latitude", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "latitude",
                    "errorMessage" to "Invalid latitude value",
                    "latitude" to latitude.toString()
                ))
            }
        }
        
        request.longitude?.let { longitude ->
            if (longitude < -180 || longitude > 180) {
                errors.add("Invalid longitude value")
                Logger.logError(TAG, "Event validation failed - invalid longitude", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "longitude",
                    "errorMessage" to "Invalid longitude value",
                    "longitude" to longitude.toString()
                ))
            }
        }
        
        // Max participants validation (if provided)
        request.maxParticipants?.let { maxParticipants ->
            if (maxParticipants <= 0) {
                errors.add("Maximum participants must be greater than 0")
                Logger.logError(TAG, "Event validation failed - invalid max participants", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "maxParticipants",
                    "errorMessage" to "Maximum participants must be greater than 0",
                    "maxParticipants" to maxParticipants.toString()
                ))
            } else if (maxParticipants > 10000) {
                errors.add("Maximum participants cannot exceed 10,000")
                Logger.logError(TAG, "Event validation failed - max participants too high", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "maxParticipants",
                    "errorMessage" to "Maximum participants cannot exceed 10,000",
                    "maxParticipants" to maxParticipants.toString()
                ))
            }
        }
        
        val result = ValidationResult(errors.isEmpty(), errors)
        val duration = System.currentTimeMillis() - startTime
        
        Logger.logPerformance(TAG, "validateCreateEventRequest", duration, mapOf(
            "title" to request.title,
            "category" to request.category,
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        if (result.isValid) {
            Logger.logBusinessEvent(TAG, "event_validation_success", mapOf(
                "title" to request.title,
                "category" to request.category,
                "validationType" to "create_event"
            ))
        } else {
            Logger.logBusinessEvent(TAG, "event_validation_failed", mapOf(
                "title" to request.title,
                "category" to request.category,
                "validationType" to "create_event",
                "errorCount" to errors.size.toString(),
                "errors" to errors.joinToString(", ")
            ))
        }
        
        Logger.exit(TAG, "validateCreateEventRequest", mapOf(
            "title" to request.title,
            "category" to request.category,
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        return result
    }
    
    /**
     * Validates event update request
     */
    fun validateUpdateEventRequest(request: UpdateEventRequest): ValidationResult {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "validateUpdateEventRequest", 
            "hasTitle" to request.title?.isNotBlank().toString(),
            "hasDescription" to request.description?.isNotBlank().toString(),
            "hasLocation" to request.location?.isNotBlank().toString()
        )
        
        val errors = mutableListOf<String>()
        
        // Title validation (if provided)
        request.title?.let { title ->
            if (title.isBlank()) {
                errors.add("Event title cannot be empty")
                Logger.logError(TAG, "Event update validation failed - title empty", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "title",
                    "errorMessage" to "Event title cannot be empty"
                ))
            } else if (title.length < 3) {
                errors.add("Event title must be at least 3 characters")
                Logger.logError(TAG, "Event update validation failed - title too short", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "title",
                    "errorMessage" to "Event title must be at least 3 characters",
                    "length" to title.length.toString()
                ))
            } else if (title.length > 100) {
                errors.add("Event title must not exceed 100 characters")
                Logger.logError(TAG, "Event update validation failed - title too long", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "title",
                    "errorMessage" to "Event title must not exceed 100 characters",
                    "length" to title.length.toString()
                ))
            }
        }
        
        // Description validation (if provided)
        request.description?.let { description ->
            if (description.isBlank()) {
                errors.add("Event description cannot be empty")
                Logger.logError(TAG, "Event update validation failed - description empty", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "description",
                    "errorMessage" to "Event description cannot be empty"
                ))
            } else if (description.length < 10) {
                errors.add("Event description must be at least 10 characters")
                Logger.logError(TAG, "Event update validation failed - description too short", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "description",
                    "errorMessage" to "Event description must be at least 10 characters",
                    "length" to description.length.toString()
                ))
            } else if (description.length > 1000) {
                errors.add("Event description must not exceed 1000 characters")
                Logger.logError(TAG, "Event update validation failed - description too long", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "description",
                    "errorMessage" to "Event description must not exceed 1000 characters",
                    "length" to description.length.toString()
                ))
            }
        }
        
        // Location validation (if provided)
        request.location?.let { location ->
            if (location.isBlank()) {
                errors.add("Event location cannot be empty")
                Logger.logError(TAG, "Event update validation failed - location empty", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "location",
                    "errorMessage" to "Event location cannot be empty"
                ))
            }
        }
        
        // Coordinates validation (if provided)
        request.latitude?.let { latitude ->
            if (latitude < -90 || latitude > 90) {
                errors.add("Invalid latitude value")
                Logger.logError(TAG, "Event update validation failed - invalid latitude", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "latitude",
                    "errorMessage" to "Invalid latitude value",
                    "latitude" to latitude.toString()
                ))
            }
        }
        
        request.longitude?.let { longitude ->
            if (longitude < -180 || longitude > 180) {
                errors.add("Invalid longitude value")
                Logger.logError(TAG, "Event update validation failed - invalid longitude", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "longitude",
                    "errorMessage" to "Invalid longitude value",
                    "longitude" to longitude.toString()
                ))
            }
        }
        
        // Max participants validation (if provided)
        request.maxParticipants?.let { maxParticipants ->
            if (maxParticipants <= 0) {
                errors.add("Maximum participants must be greater than 0")
                Logger.logError(TAG, "Event update validation failed - invalid max participants", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "maxParticipants",
                    "errorMessage" to "Maximum participants must be greater than 0",
                    "maxParticipants" to maxParticipants.toString()
                ))
            } else if (maxParticipants > 10000) {
                errors.add("Maximum participants cannot exceed 10,000")
                Logger.logError(TAG, "Event update validation failed - max participants too high", null, mapOf(
                    "errorType" to "VALIDATION_ERROR",
                    "field" to "maxParticipants",
                    "errorMessage" to "Maximum participants cannot exceed 10,000",
                    "maxParticipants" to maxParticipants.toString()
                ))
            }
        }
        
        val result = ValidationResult(errors.isEmpty(), errors)
        val duration = System.currentTimeMillis() - startTime
        
        Logger.logPerformance(TAG, "validateUpdateEventRequest", duration, mapOf(
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        if (result.isValid) {
            Logger.logBusinessEvent(TAG, "event_validation_success", mapOf(
                "validationType" to "update_event"
            ))
        } else {
            Logger.logBusinessEvent(TAG, "event_validation_failed", mapOf(
                "validationType" to "update_event",
                "errorCount" to errors.size.toString(),
                "errors" to errors.joinToString(", ")
            ))
        }
        
        Logger.exit(TAG, "validateUpdateEventRequest", mapOf(
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        return result
    }
    
    /**
     * Validates search query
     */
    fun validateSearchQuery(query: String): ValidationResult {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "validateSearchQuery", 
            "queryLength" to query.length.toString()
        )
        
        val errors = mutableListOf<String>()
        
        if (query.isBlank()) {
            errors.add("Search query cannot be empty")
            Logger.logError(TAG, "Search validation failed - query empty", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "query",
                "errorMessage" to "Search query cannot be empty"
            ))
        } else if (query.length < 2) {
            errors.add("Search query must be at least 2 characters")
            Logger.logError(TAG, "Search validation failed - query too short", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "query",
                "errorMessage" to "Search query must be at least 2 characters",
                "length" to query.length.toString()
            ))
        } else if (query.length > 100) {
            errors.add("Search query must not exceed 100 characters")
            Logger.logError(TAG, "Search validation failed - query too long", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "query",
                "errorMessage" to "Search query must not exceed 100 characters",
                "length" to query.length.toString()
            ))
        }
        
        val result = ValidationResult(errors.isEmpty(), errors)
        val duration = System.currentTimeMillis() - startTime
        
        Logger.logPerformance(TAG, "validateSearchQuery", duration, mapOf(
            "queryLength" to query.length.toString(),
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        if (result.isValid) {
            Logger.logBusinessEvent(TAG, "search_validation_success", mapOf(
                "queryLength" to query.length.toString()
            ))
        } else {
            Logger.logBusinessEvent(TAG, "search_validation_failed", mapOf(
                "queryLength" to query.length.toString(),
                "errorCount" to errors.size.toString(),
                "errors" to errors.joinToString(", ")
            ))
        }
        
        Logger.exit(TAG, "validateSearchQuery", mapOf(
            "queryLength" to query.length.toString(),
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        return result
    }
    
    /**
     * Validates pagination parameters
     */
    fun validatePaginationParams(page: Int, limit: Int): ValidationResult {
        val startTime = System.currentTimeMillis()
        Logger.enter(TAG, "validatePaginationParams", 
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        
        val errors = mutableListOf<String>()
        
        if (page < 1) {
            errors.add("Page number must be greater than 0")
            Logger.logError(TAG, "Pagination validation failed - invalid page", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "page",
                "errorMessage" to "Page number must be greater than 0",
                "page" to page.toString()
            ))
        }
        
        if (limit < 1) {
            errors.add("Limit must be greater than 0")
            Logger.logError(TAG, "Pagination validation failed - invalid limit", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "limit",
                "errorMessage" to "Limit must be greater than 0",
                "limit" to limit.toString()
            ))
        } else if (limit > 100) {
            errors.add("Limit cannot exceed 100")
            Logger.logError(TAG, "Pagination validation failed - limit too high", null, mapOf(
                "errorType" to "VALIDATION_ERROR",
                "field" to "limit",
                "errorMessage" to "Limit cannot exceed 100",
                "limit" to limit.toString()
            ))
        }
        
        val result = ValidationResult(errors.isEmpty(), errors)
        val duration = System.currentTimeMillis() - startTime
        
        Logger.logPerformance(TAG, "validatePaginationParams", duration, mapOf(
            "page" to page.toString(),
            "limit" to limit.toString(),
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        if (result.isValid) {
            Logger.logBusinessEvent(TAG, "pagination_validation_success", mapOf(
                "page" to page.toString(),
                "limit" to limit.toString()
            ))
        } else {
            Logger.logBusinessEvent(TAG, "pagination_validation_failed", mapOf(
                "page" to page.toString(),
                "limit" to limit.toString(),
                "errorCount" to errors.size.toString(),
                "errors" to errors.joinToString(", ")
            ))
        }
        
        Logger.exit(TAG, "validatePaginationParams", mapOf(
            "page" to page.toString(),
            "limit" to limit.toString(),
            "isValid" to result.isValid.toString(),
            "errorCount" to errors.size.toString()
        ))
        
        return result
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