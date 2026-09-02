package com.univ.learningapp.data.model

data class User(
    val id: String = "user_101",
    val name: String,
    val email: String,
    val token: String? = "mock_jwt_token_12345"
)
