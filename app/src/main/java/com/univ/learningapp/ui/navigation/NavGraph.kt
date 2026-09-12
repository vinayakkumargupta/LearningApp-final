package com.univ.learningapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.univ.learningapp.ui.admin.AdminConsoleScreen
import com.univ.learningapp.ui.admin.AdminViewModel
import com.univ.learningapp.ui.auth.AuthViewModel
import com.univ.learningapp.ui.auth.ForgotPasswordScreen
import com.univ.learningapp.ui.auth.LoginScreen
import com.univ.learningapp.ui.auth.SignupScreen
import com.univ.learningapp.ui.dashboard.DashboardScreen
import com.univ.learningapp.ui.dashboard.DashboardViewModel
import com.univ.learningapp.ui.history.TestHistoryScreen
import com.univ.learningapp.ui.history.TestHistoryViewModel
import com.univ.learningapp.ui.test.DetailedReviewScreen
import com.univ.learningapp.ui.test.MockTestScreen
import com.univ.learningapp.ui.test.TestResultScreen
import com.univ.learningapp.ui.test.TestViewModel

@Composable
fun LearningNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Routes
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.Signup.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            SignupScreen(
                viewModel = authViewModel,
                onSignupSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Dashboard Route
        composable(Screen.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = dashboardViewModel,
                onStartTest = { examId, examTitle ->
                    navController.navigate(Screen.MockTest.createRoute(examId, examTitle))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.TestHistory.route)
                },
                onNavigateToAdminConsole = {
                    navController.navigate(Screen.AdminConsole.route)
                },
                onViewScorecard = { resultId ->
                    navController.navigate(Screen.TestResult.createRoute(resultId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Admin Console Route
        composable(Screen.AdminConsole.route) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            AdminConsoleScreen(
                viewModel = adminViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Test History Route
        composable(Screen.TestHistory.route) {
            val historyViewModel: TestHistoryViewModel = hiltViewModel()
            TestHistoryScreen(
                viewModel = historyViewModel,
                onViewScorecard = { resultId ->
                    navController.navigate(Screen.TestResult.createRoute(resultId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Mock Test Route
        composable(
            route = Screen.MockTest.route,
            arguments = listOf(
                navArgument("examId") { type = NavType.StringType },
                navArgument("examTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: "exam_mock_50_cs"
            val encodedTitle = backStackEntry.arguments?.getString("examTitle") ?: "Mock Test"
            val examTitle = java.net.URLDecoder.decode(encodedTitle, "UTF-8")

            val testViewModel: TestViewModel = hiltViewModel()
            MockTestScreen(
                examId = examId,
                examTitle = examTitle,
                viewModel = testViewModel,
                onTestSubmitted = { resultId ->
                    navController.navigate(Screen.TestResult.createRoute(resultId)) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // Test Result Route
        composable(
            route = Screen.TestResult.route,
            arguments = listOf(
                navArgument("resultId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
            val testViewModel: TestViewModel = hiltViewModel()

            TestResultScreen(
                resultId = resultId,
                viewModel = testViewModel,
                onNavigateToReview = { resId ->
                    navController.navigate(Screen.DetailedReview.createRoute(resId))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.TestHistory.route)
                },
                onNavigateToDashboard = {
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                }
            )
        }

        // Detailed Review Route
        composable(
            route = Screen.DetailedReview.route,
            arguments = listOf(
                navArgument("resultId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
            val testViewModel: TestViewModel = hiltViewModel()

            DetailedReviewScreen(
                resultId = resultId,
                viewModel = testViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
