package com.earthmax.app

import com.earthmax.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime
import java.util.*

/**
 * Test utilities for creating mock data and common test functions
 */
object TestUtils {

    fun createMockUser(
        id: String = UUID.randomUUID().toString(),
        email: String = "test@example.com",
        fullName: String = "Test User",
        bio: String = "Test bio",
        profileImageUrl: String? = null,
        isVerified: Boolean = false,
        joinedEvents: List<String> = emptyList(),
        createdEvents: List<String> = emptyList(),
        preferences: UserPreferences = createMockUserPreferences(),
        lastLoginTime: LocalDateTime = LocalDateTime.now(),
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): User {
        return User(
            id = id,
            email = email,
            fullName = fullName,
            bio = bio,
            profileImageUrl = profileImageUrl,
            isVerified = isVerified,
            joinedEvents = joinedEvents,
            createdEvents = createdEvents,
            preferences = preferences,
            lastLoginTime = lastLoginTime,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun createMockUserPreferences(
        notificationsEnabled: Boolean = true,
        theme: String = "light",
        language: String = "en",
        privacySettings: Map<String, Boolean> = mapOf(
            "profileVisible" to true,
            "eventsVisible" to true,
            "contactInfoVisible" to false
        )
    ): UserPreferences {
        return UserPreferences(
            notificationsEnabled = notificationsEnabled,
            theme = theme,
            language = language,
            privacySettings = privacySettings
        )
    }

    fun createMockEvent(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Event",
        description: String = "Test event description",
        category: EventCategory = EventCategory.CLEANUP,
        location: Location = createMockLocation(),
        dateTime: LocalDateTime = LocalDateTime.now().plusDays(1),
        duration: Int = 120,
        maxParticipants: Int? = 50,
        currentParticipants: Int = 10,
        organizerId: String = UUID.randomUUID().toString(),
        participants: List<String> = emptyList(),
        imageUrl: String? = null,
        tags: List<String> = listOf("environment", "cleanup"),
        isActive: Boolean = true,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): Event {
        return Event(
            id = id,
            title = title,
            description = description,
            category = category,
            location = location,
            dateTime = dateTime,
            duration = duration,
            maxParticipants = maxParticipants,
            currentParticipants = currentParticipants,
            organizerId = organizerId,
            participants = participants,
            imageUrl = imageUrl,
            tags = tags,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun createMockLocation(
        latitude: Double = 48.8566,
        longitude: Double = 2.3522,
        address: String = "Paris, France",
        name: String? = "Test Location"
    ): Location {
        return Location(
            latitude = latitude,
            longitude = longitude,
            address = address,
            name = name
        )
    }

    fun createMockEventList(count: Int = 5): List<Event> {
        return (1..count).map { index ->
            createMockEvent(
                title = "Test Event $index",
                description = "Description for test event $index",
                category = EventCategory.values()[index % EventCategory.values().size]
            )
        }
    }

    fun createMockUserList(count: Int = 3): List<User> {
        return (1..count).map { index ->
            createMockUser(
                email = "user$index@example.com",
                fullName = "Test User $index",
                bio = "Bio for test user $index"
            )
        }
    }

    /**
     * Creates a flow that emits the provided values
     */
    fun <T> createMockFlow(vararg values: T): Flow<T> {
        return flowOf(*values)
    }

    /**
     * Creates a flow that emits a single value
     */
    fun <T> createMockSingleFlow(value: T): Flow<T> {
        return flowOf(value)
    }

    /**
     * Creates an empty flow
     */
    fun <T> createMockEmptyFlow(): Flow<T> {
        return flowOf()
    }

    /**
     * Common test assertions
     */
    object Assertions {
        fun assertUserEquals(expected: User, actual: User) {
            assert(expected.id == actual.id) { "User IDs don't match" }
            assert(expected.email == actual.email) { "User emails don't match" }
            assert(expected.fullName == actual.fullName) { "User names don't match" }
            assert(expected.bio == actual.bio) { "User bios don't match" }
        }

        fun assertEventEquals(expected: Event, actual: Event) {
            assert(expected.id == actual.id) { "Event IDs don't match" }
            assert(expected.title == actual.title) { "Event titles don't match" }
            assert(expected.description == actual.description) { "Event descriptions don't match" }
            assert(expected.category == actual.category) { "Event categories don't match" }
        }

        fun assertLocationEquals(expected: Location, actual: Location) {
            assert(expected.latitude == actual.latitude) { "Latitudes don't match" }
            assert(expected.longitude == actual.longitude) { "Longitudes don't match" }
            assert(expected.address == actual.address) { "Addresses don't match" }
        }
    }

    /**
     * Test data constants
     */
    object Constants {
        const val TEST_USER_ID = "test-user-id"
        const val TEST_EVENT_ID = "test-event-id"
        const val TEST_EMAIL = "test@earthmax.com"
        const val TEST_PASSWORD = "testpassword123"
        const val TEST_TOKEN = "test-jwt-token"
        
        val TEST_LOCATION = Location(
            latitude = 48.8566,
            longitude = 2.3522,
            address = "Paris, France",
            name = "Test Location"
        )
    }
}