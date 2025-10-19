package com.earthmax.feature.profile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.earthmax.feature.profile.ProfileScreen
import com.earthmax.feature.profile.EditProfileScreen
import com.earthmax.feature.profile.SettingsScreen
import com.earthmax.feature.profile.AvatarTestScreen
import com.earthmax.feature.monitoring.MonitoringDashboard

const val PROFILE_GRAPH_ROUTE = "profile_graph"
const val PROFILE_ROUTE = "profile"
const val EDIT_PROFILE_ROUTE = "edit_profile"
const val SETTINGS_ROUTE = "settings"
const val AVATAR_TEST_ROUTE = "avatar_test"
const val MONITORING_ROUTE = "monitoring"

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
                },
                onNavigateToAvatarTest = {
                    navController.navigate(AVATAR_TEST_ROUTE)
                },
                onNavigateToMonitoring = {
                    navController.navigate(MONITORING_ROUTE)
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
                },
                onNavigateToMonitoring = {
                    navController.navigate(MONITORING_ROUTE)
                }
            )
        }
        
        composable(AVATAR_TEST_ROUTE) {
            AvatarTestScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(MONITORING_ROUTE) {
            MonitoringDashboard(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}