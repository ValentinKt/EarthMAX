package com.earthmax.feature.profile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.earthmax.feature.profile.ProfileScreen

const val PROFILE_GRAPH_ROUTE = "profile_graph"
const val PROFILE_ROUTE = "profile"

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
                    // TODO: Navigate to settings screen when implemented
                },
                onNavigateToEditProfile = {
                    // TODO: Navigate to edit profile screen when implemented
                }
            )
        }
    }
}