package com.earthmax.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.earthmax.feature.auth.forgot.ForgotPasswordScreen
import com.earthmax.feature.auth.login.LoginScreen
import com.earthmax.feature.auth.signup.SignUpScreen

const val AUTH_GRAPH_ROUTE = "auth"
const val LOGIN_ROUTE = "login"
const val SIGNUP_ROUTE = "signup"
const val FORGOT_PASSWORD_ROUTE = "forgot_password"

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onNavigateToEvents: () -> Unit
) {
    navigation(
        startDestination = LOGIN_ROUTE,
        route = AUTH_GRAPH_ROUTE
    ) {
        composable(LOGIN_ROUTE) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(SIGNUP_ROUTE)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(FORGOT_PASSWORD_ROUTE)
                },
                onLoginSuccess = onNavigateToEvents
            )
        }
        
        composable(SIGNUP_ROUTE) {
            SignUpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignUpSuccess = onNavigateToEvents
            )
        }
        
        composable(FORGOT_PASSWORD_ROUTE) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun AuthNavigation(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = LOGIN_ROUTE,
        modifier = modifier
    ) {
        composable(LOGIN_ROUTE) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(SIGNUP_ROUTE)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(FORGOT_PASSWORD_ROUTE)
                },
                onLoginSuccess = onAuthSuccess
            )
        }
        
        composable(SIGNUP_ROUTE) {
            SignUpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignUpSuccess = onAuthSuccess
            )
        }
        
        composable(FORGOT_PASSWORD_ROUTE) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}