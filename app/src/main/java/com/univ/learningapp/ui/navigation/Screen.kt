package com.univ.learningapp.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Dashboard : Screen("dashboard")
    object TestHistory : Screen("test_history")

    object MockTest : Screen("mock_test/{examId}/{examTitle}") {
        fun createRoute(examId: String, examTitle: String): String {
            return "mock_test/$examId/${java.net.URLEncoder.encode(examTitle, "UTF-8")}"
        }
    }

    object TestResult : Screen("test_result/{resultId}") {
        fun createRoute(resultId: String): String {
            return "test_result/$resultId"
        }
    }

    object DetailedReview : Screen("detailed_review/{resultId}") {
        fun createRoute(resultId: String): String {
            return "detailed_review/$resultId"
        }
    }
}
