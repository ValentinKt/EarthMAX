package com.earthmax.feature.events.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.earthmax.feature.events.create.CreateEventScreen
import com.earthmax.feature.events.detail.EventDetailScreen
import com.earthmax.feature.events.home.EventsHomeScreen

const val EVENTS_GRAPH_ROUTE = "events_graph"
const val EVENTS_HOME_ROUTE = "events_home"
const val CREATE_EVENT_ROUTE = "create_event"
const val EVENT_DETAIL_ROUTE = "event_detail"

fun NavGraphBuilder.eventsGraph(
    navController: NavHostController,
    onNavigateToAuth: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    navigation(
        startDestination = EVENTS_HOME_ROUTE,
        route = EVENTS_GRAPH_ROUTE
    ) {
        composable(EVENTS_HOME_ROUTE) {
            EventsHomeScreen(
                onNavigateToEventDetail = { eventId ->
                    navController.navigate("$EVENT_DETAIL_ROUTE/$eventId")
                },
                onNavigateToCreateEvent = {
                    navController.navigate(CREATE_EVENT_ROUTE)
                },
                onNavigateToMap = onNavigateToMap,
                onNavigateToProfile = onNavigateToProfile
            )
        }
        
        composable(
            CREATE_EVENT_ROUTE,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            CreateEventScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEventCreated = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = "$EVENT_DETAIL_ROUTE/{eventId}",
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) { backStackEntry ->
            EventDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMap = onNavigateToMap
            )
        }
    }
}