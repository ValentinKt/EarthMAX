package com.earthmax.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.earthmax.feature.auth.navigation.AUTH_GRAPH_ROUTE
import com.earthmax.feature.auth.navigation.authGraph
import com.earthmax.feature.events.navigation.EVENTS_GRAPH_ROUTE
import com.earthmax.feature.events.navigation.eventsGraph
import com.earthmax.feature.profile.navigation.PROFILE_GRAPH_ROUTE
import com.earthmax.feature.profile.navigation.profileGraph

@Composable
fun EarthMaxNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: MainNavigationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val startDestination = if (uiState.isAuthenticated) {
        EVENTS_GRAPH_ROUTE
    } else {
        AUTH_GRAPH_ROUTE
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        authGraph(
            navController = navController,
            onNavigateToEvents = {
                navController.navigate(EVENTS_GRAPH_ROUTE) {
                    popUpTo(AUTH_GRAPH_ROUTE) { inclusive = true }
                }
            }
        )
        
        eventsGraph(
            navController = navController,
            onNavigateToAuth = {
                navController.navigate(AUTH_GRAPH_ROUTE) {
                    popUpTo(EVENTS_GRAPH_ROUTE) { inclusive = true }
                }
            },
            onNavigateToMap = {
                // TODO: Navigate to map screen when implemented
            },
            onNavigateToProfile = {
                navController.navigate(PROFILE_GRAPH_ROUTE)
            }
        )
        
        profileGraph(
            navController = navController,
            onNavigateToAuth = {
                navController.navigate(AUTH_GRAPH_ROUTE) {
                    popUpTo(PROFILE_GRAPH_ROUTE) { inclusive = true }
                }
            }
        )
    }
}