package com.univ.learningapp.data.repository

import com.univ.learningapp.data.model.User
import com.univ.learningapp.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<User?>(
        User(id = "user_001", name = "Alex Johnson", email = "student@learningapp.com")
    )
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    override suspend fun login(email: String, pass: String): Result<User> {
        delay(1000) // Simulate network call
        if (email.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty."))
        }
        if (!email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email format."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val nameFromEmail = email.substringBefore("@").replace(".", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val loggedInUser = User(
            id = "user_${System.currentTimeMillis()}",
            name = nameFromEmail.ifBlank { "Learner User" },
            email = email
        )
        _currentUserFlow.value = loggedInUser
        return Result.success(loggedInUser)
    }

    override suspend fun signup(name: String, email: String, pass: String): Result<User> {
        delay(1000) // Simulate network call
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("All fields are required."))
        }
        if (!email.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }

        val newUser = User(
            id = "user_${System.currentTimeMillis()}",
            name = name,
            email = email
        )
        _currentUserFlow.value = newUser
        return Result.success(newUser)
    }

    override suspend fun resetPassword(email: String): Result<String> {
        delay(1000) // Simulate network call
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        return Result.success("Password reset instructions sent to $email")
    }

    override suspend fun logout() {
        delay(500)
        _currentUserFlow.value = null
    }

    override fun getCurrentUser(): User? {
        return _currentUserFlow.value
    }
}
