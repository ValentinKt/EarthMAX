package com.earthmax.data.mappers

import com.earthmax.core.models.User
import com.earthmax.core.models.UserPreferences as CoreUserPreferences
import com.earthmax.domain.model.DomainUser
import com.earthmax.domain.model.UserRole
import com.earthmax.domain.model.UserPreferences as DomainUserPreferences
import com.earthmax.domain.model.UserStatistics as DomainUserStatistics
import kotlinx.datetime.Instant
import java.util.Date

/**
 * Extension function to convert Core User to Domain User
 */
fun User.toDomainUser(): DomainUser {
    val computedUsername = if (this.displayName.isNotEmpty()) this.displayName else this.email.substringBefore("@")
    val first = this.displayName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() }
    val last = this.displayName.split(" ").drop(1).joinToString(" ").takeIf { it.isNotBlank() }

    return DomainUser(
        id = this.id,
        email = this.email,
        username = computedUsername,
        firstName = first,
        lastName = last,
        profileImageUrl = this.profileImageUrl.takeIf { it.isNotEmpty() },
        bio = this.bio.takeIf { it.isNotEmpty() },
        location = this.location.takeIf { it.isNotEmpty() },
        isVerified = false, // Default value, can be enhanced
        role = UserRole.USER, // Default role, can be enhanced
        preferences = DomainUserPreferences(
            notificationsEnabled = this.preferences.notificationsEnabled,
            emailNotifications = true,
            locationSharing = this.preferences.locationSharingEnabled,
            theme = "system",
            language = "en"
        ),
        statistics = DomainUserStatistics(
            eventsCreated = this.organizedEvents.size,
            eventsParticipated = this.joinedEvents.size,
            totalImpactPoints = this.ecoPoints,
            badgesEarned = this.badges.map { it.id }
        ),
        createdAt = Instant.fromEpochMilliseconds(this.createdAt.time),
        lastActiveAt = Instant.fromEpochMilliseconds(this.updatedAt.time)
    )
}

/**
 * Extension function to convert Domain User to Core User
 */
fun DomainUser.toUser(): User {
    val display = listOfNotNull(this.firstName?.takeIf { it.isNotBlank() }, this.lastName?.takeIf { it.isNotBlank() })
        .joinToString(" ")
        .ifEmpty { this.username }

    return User(
        id = this.id,
        email = this.email,
        displayName = display,
        profileImageUrl = this.profileImageUrl ?: "",
        bio = this.bio?.takeIf { it.isNotEmpty() } ?: "",
        location = this.location?.takeIf { it.isNotEmpty() } ?: "",
        ecoPoints = this.statistics.totalImpactPoints,
        preferences = CoreUserPreferences(
            notificationsEnabled = this.preferences.notificationsEnabled,
            locationSharingEnabled = this.preferences.locationSharing,
            preferredRadius = 10.0,
            preferredCategories = emptyList()
        ),
        createdAt = Date(this.createdAt.toEpochMilliseconds()),
        updatedAt = Date((this.lastActiveAt ?: Instant.fromEpochMilliseconds(this.createdAt.toEpochMilliseconds())).toEpochMilliseconds())
    )
}