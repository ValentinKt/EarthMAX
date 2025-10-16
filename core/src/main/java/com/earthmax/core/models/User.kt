package com.earthmax.core.models

import java.util.Date

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val location: String = "",
    val joinedEvents: List<String> = emptyList(),
    val organizedEvents: List<String> = emptyList(),
    val ecoPoints: Int = 0,
    val badges: List<Badge> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val locationSharingEnabled: Boolean = true,
    val preferredRadius: Double = 10.0, // in kilometers
    val preferredCategories: List<EventCategory> = emptyList()
)

data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val earnedAt: Date = Date()
)