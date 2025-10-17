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
    val environmentalImpact: EnvironmentalImpact = EnvironmentalImpact(),
    val profileCustomization: ProfileCustomization = ProfileCustomization(),
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

data class EnvironmentalImpact(
    val totalCO2Saved: Double = 0.0, // in kg
    val treesPlanted: Int = 0,
    val wasteRecycled: Double = 0.0, // in kg
    val eventsOrganized: Int = 0,
    val eventsAttended: Int = 0,
    val impactScore: Int = 0,
    val monthlyGoals: MonthlyGoals = MonthlyGoals(),
    val achievements: List<Achievement> = emptyList()
)

data class MonthlyGoals(
    val targetEvents: Int = 2,
    val targetCO2Reduction: Double = 10.0, // in kg
    val targetRecycling: Double = 5.0, // in kg
    val currentMonth: Int = 0,
    val currentYear: Int = 0
)

data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val category: AchievementCategory = AchievementCategory.PARTICIPATION,
    val progress: Int = 0,
    val target: Int = 100,
    val isCompleted: Boolean = false,
    val completedAt: Date? = null
)

enum class AchievementCategory {
    PARTICIPATION,
    ORGANIZATION,
    IMPACT,
    COMMUNITY,
    CONSISTENCY
}

data class ProfileCustomization(
    val theme: ProfileTheme = ProfileTheme.FOREST,
    val favoriteCategories: List<EventCategory> = emptyList(),
    val displayBadges: List<String> = emptyList(), // Badge IDs to display
    val showImpactStats: Boolean = true,
    val showMonthlyGoals: Boolean = true,
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC
)

enum class ProfileTheme {
    FOREST,
    OCEAN,
    MOUNTAIN,
    DESERT,
    ARCTIC
}

enum class ProfileVisibility {
    PUBLIC,
    FRIENDS_ONLY,
    PRIVATE
}