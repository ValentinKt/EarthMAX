package com.earthmax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.earthmax.core.models.Badge
import com.earthmax.core.models.User
import com.earthmax.core.models.UserPreferences
import java.util.Date

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val displayName: String,
    val profileImageUrl: String,
    val bio: String,
    val location: String,
    val joinedEvents: List<String>,
    val organizedEvents: List<String>,
    val ecoPoints: Int,
    val badges: List<Badge>,
    val preferences: UserPreferences,
    val createdAt: Date,
    val updatedAt: Date
)

fun UserEntity.toUser(): User {
    return User(
        id = id,
        email = email,
        displayName = displayName,
        profileImageUrl = profileImageUrl,
        bio = bio,
        location = location,
        joinedEvents = joinedEvents,
        organizedEvents = organizedEvents,
        ecoPoints = ecoPoints,
        badges = badges,
        preferences = preferences,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        displayName = displayName,
        profileImageUrl = profileImageUrl,
        bio = bio,
        location = location,
        joinedEvents = joinedEvents,
        organizedEvents = organizedEvents,
        ecoPoints = ecoPoints,
        badges = badges,
        preferences = preferences,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}