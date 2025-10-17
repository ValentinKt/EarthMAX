package com.earthmax.feature.profile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.earthmax.feature.profile.ProfileScreen
import com.earthmax.feature.profile.EditProfileScreen
import com.earthmax.feature.profile.SettingsScreen

const val PROFILE_GRAPH_ROUTE = "profile_graph"
const val PROFILE_ROUTE = "profile"
const val EDIT_PROFILE_ROUTE = "edit_profile"
const val SETTINGS_ROUTE = "settings"

fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    onNavigateToAuth: () -> Unit
) {
    navigation(
        startDestination = PROFILE_ROUTE,
        route = PROFILE_GRAPH_ROUTE
    ) {
        composable(PROFILE_ROUTE) {
            ProfileScreen(
                onNavigateToAuth = onNavigateToAuth,
                onNavigateToSettings = {
                    navController.navigate(SETTINGS_ROUTE)
                },
                onNavigateToEditProfile = {
                    navController.navigate(EDIT_PROFILE_ROUTE)
                }
            )
        }
        
        composable(EDIT_PROFILE_ROUTE) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}