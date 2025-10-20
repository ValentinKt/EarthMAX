package com.earthmax.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model for users in the EarthMAX application.
 * This represents the business logic view of a user, separate from data layer DTOs.
 */
data class DomainUser(
    val id: String,
    val email: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val isVerified: Boolean = false,
    val role: UserRole = UserRole.USER,
    val preferences: UserPreferences = UserPreferences(),
    val statistics: UserStatistics = UserStatistics(),
    val createdAt: Instant,
    val lastActiveAt: Instant? = null
)

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val emailNotifications: Boolean = true,
    val locationSharing: Boolean = false,
    val theme: String = "system",
    val language: String = "en"
)

data class UserStatistics(
    val eventsCreated: Int = 0,
    val eventsParticipated: Int = 0,
    val totalImpactPoints: Int = 0,
    val badgesEarned: List<String> = emptyList()
)

enum class UserRole {
    USER,
    MODERATOR,
    ADMIN
}