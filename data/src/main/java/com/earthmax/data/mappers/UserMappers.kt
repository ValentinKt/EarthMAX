package com.earthmax.data.mappers

import com.earthmax.core.models.User
import com.earthmax.domain.model.DomainUser
import com.earthmax.domain.model.UserRole
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension function to convert Core User to Domain User
 */
fun User.toDomainUser(): DomainUser {
    return DomainUser(
        id = this.id,
        email = this.email,
        username = this.displayName ?: this.email.substringBefore("@"),
        firstName = this.displayName?.split(" ")?.firstOrNull() ?: "",
        lastName = this.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
        profileImageUrl = this.profileImageUrl,
        bio = this.bio ?: "",
        location = this.location ?: "",
        isVerified = false, // Default value, can be enhanced
        role = UserRole.USER, // Default role, can be enhanced
        preferences = DomainUser.UserPreferences(
            notificationsEnabled = true,
            locationSharingEnabled = true,
            emailUpdatesEnabled = true,
            preferredCategories = emptyList()
        ),
        statistics = DomainUser.UserStatistics(
            eventsOrganized = 0, // Can be calculated from events
            eventsJoined = 0, // Can be calculated from events
            ecoPoints = this.ecoPoints,
            totalImpactScore = this.ecoPoints.toDouble()
        ),
        createdAt = parseDate(this.createdAt),
        lastActiveAt = Instant.DISTANT_PAST // Default value, can be enhanced
    )
}

/**
 * Extension function to convert Domain User to Core User
 */
fun DomainUser.toUser(): User {
    return User(
        id = this.id,
        email = this.email,
        displayName = if (this.firstName.isNotEmpty() && this.lastName.isNotEmpty()) {
            "${this.firstName} ${this.lastName}"
        } else {
            this.username
        },
        profileImageUrl = this.profileImageUrl,
        bio = this.bio.takeIf { it.isNotEmpty() },
        location = this.location.takeIf { it.isNotEmpty() },
        ecoPoints = this.statistics.ecoPoints,
        createdAt = formatDate(this.createdAt)
    )
}

/**
 * Parse date string to Instant
 */
private fun parseDate(dateString: String): Instant {
    return try {
        // Try ISO format first
        Instant.parse(dateString)
    } catch (e: Exception) {
        try {
            // Fallback to custom format
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = formatter.parse(dateString)
            date?.toInstant()?.toKotlinInstant() ?: Instant.DISTANT_PAST
        } catch (e: Exception) {
            Instant.DISTANT_PAST
        }
    }
}

/**
 * Format Instant to date string
 */
private fun formatDate(instant: Instant): String {
    return instant.toString()
}