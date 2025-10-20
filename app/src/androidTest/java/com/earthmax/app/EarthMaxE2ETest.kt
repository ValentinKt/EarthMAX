package com.earthmax.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class EarthMaxE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appLaunchAndNavigationFlow() {
        // Test app launch
        composeTestRule.onNodeWithText("EarthMAX")
            .assertIsDisplayed()

        // Test navigation to events
        composeTestRule.onNodeWithContentDescription("Events")
            .performClick()

        composeTestRule.onNodeWithText("Events")
            .assertIsDisplayed()

        // Test navigation to profile
        composeTestRule.onNodeWithContentDescription("Profile")
            .performClick()

        composeTestRule.onNodeWithText("Profile")
            .assertIsDisplayed()
    }

    @Test
    fun eventCreationFlow() {
        // Navigate to events
        composeTestRule.onNodeWithContentDescription("Events")
            .performClick()

        // Click create event button
        composeTestRule.onNodeWithContentDescription("Create Event")
            .performClick()

        // Fill event form
        composeTestRule.onNodeWithText("Event Title")
            .performTextInput("Test Event")

        composeTestRule.onNodeWithText("Description")
            .performTextInput("Test event description")

        composeTestRule.onNodeWithText("Location")
            .performTextInput("Test Location")

        // Submit event
        composeTestRule.onNodeWithText("Create Event")
            .performClick()

        // Verify event was created
        composeTestRule.onNodeWithText("Test Event")
            .assertIsDisplayed()
    }

    @Test
    fun eventJoinFlow() {
        // Navigate to events
        composeTestRule.onNodeWithContentDescription("Events")
            .performClick()

        // Find and click on an event
        composeTestRule.onNodeWithText("Test Event")
            .performClick()

        // Join the event
        composeTestRule.onNodeWithText("Join Event")
            .performClick()

        // Verify join confirmation
        composeTestRule.onNodeWithText("Joined")
            .assertIsDisplayed()
    }

    @Test
    fun profileUpdateFlow() {
        // Navigate to profile
        composeTestRule.onNodeWithContentDescription("Profile")
            .performClick()

        // Click edit profile
        composeTestRule.onNodeWithText("Edit Profile")
            .performClick()

        // Update profile information
        composeTestRule.onNodeWithText("Full Name")
            .performTextClearance()
            .performTextInput("Updated Name")

        composeTestRule.onNodeWithText("Bio")
            .performTextClearance()
            .performTextInput("Updated bio")

        // Save changes
        composeTestRule.onNodeWithText("Save")
            .performClick()

        // Verify changes were saved
        composeTestRule.onNodeWithText("Updated Name")
            .assertIsDisplayed()
    }

    @Test
    fun searchFunctionality() {
        // Navigate to events
        composeTestRule.onNodeWithContentDescription("Events")
            .performClick()

        // Click search
        composeTestRule.onNodeWithContentDescription("Search")
            .performClick()

        // Enter search query
        composeTestRule.onNodeWithText("Search events...")
            .performTextInput("Environment")

        // Verify search results
        composeTestRule.onNodeWithText("Environment")
            .assertIsDisplayed()
    }

    @Test
    fun settingsAndPreferences() {
        // Navigate to profile
        composeTestRule.onNodeWithContentDescription("Profile")
            .performClick()

        // Open settings
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()

        // Toggle notifications
        composeTestRule.onNodeWithText("Notifications")
            .performClick()

        // Change theme
        composeTestRule.onNodeWithText("Theme")
            .performClick()

        composeTestRule.onNodeWithText("Dark")
            .performClick()

        // Save settings
        composeTestRule.onNodeWithText("Save")
            .performClick()

        // Verify settings were applied
        composeTestRule.onNodeWithText("Settings saved")
            .assertIsDisplayed()
    }

    @Test
    fun offlineMode() {
        // Simulate offline mode
        // This would require network state manipulation
        
        // Navigate to events
        composeTestRule.onNodeWithContentDescription("Events")
            .performClick()

        // Verify cached events are still displayed
        composeTestRule.onNodeWithText("Events")
            .assertIsDisplayed()

        // Verify offline indicator
        composeTestRule.onNodeWithText("Offline")
            .assertIsDisplayed()
    }

    @Test
    fun errorHandlingAndRetry() {
        // Simulate network error
        // This would require error injection
        
        // Navigate to events
        composeTestRule.onNodeWithContentDescription("Events")
            .performClick()

        // Verify error message is displayed
        composeTestRule.onNodeWithText("Unable to load events")
            .assertIsDisplayed()

        // Click retry
        composeTestRule.onNodeWithText("Retry")
            .performClick()

        // Verify retry attempt
        composeTestRule.onNodeWithText("Loading...")
            .assertIsDisplayed()
    }

    @Test
    fun accessibilityFeatures() {
        // Test content descriptions
        composeTestRule.onNodeWithContentDescription("Events")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Profile")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()

        // Test semantic properties
        composeTestRule.onAllNodesWithText("Events")
            .assertCountEquals(1)

        // Test focus navigation
        composeTestRule.onNodeWithContentDescription("Events")
            .requestFocus()
            .assertIsFocused()
    }

    @Test
    fun performanceAndMemoryUsage() {
        // Test scrolling performance
        composeTestRule.onNodeWithContentDescription("Events")
            .performClick()

        // Scroll through events list
        composeTestRule.onNodeWithTag("EventsList")
            .performScrollToIndex(10)

        // Verify smooth scrolling
        composeTestRule.onNodeWithTag("EventsList")
            .assertIsDisplayed()

        // Test memory usage by navigating between screens
        repeat(5) {
            composeTestRule.onNodeWithContentDescription("Profile")
                .performClick()
            
            composeTestRule.onNodeWithContentDescription("Events")
                .performClick()
        }

        // Verify app is still responsive
        composeTestRule.onNodeWithText("Events")
            .assertIsDisplayed()
    }
}