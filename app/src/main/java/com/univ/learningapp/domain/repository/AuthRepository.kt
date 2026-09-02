package com.univ.learningapp.domain.repository

import com.univ.learningapp.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserFlow: Flow<User?>
    suspend fun login(email: String, pass: String): Result<User>
    suspend fun signup(name: String, email: String, pass: String): Result<User>
    suspend fun resetPassword(email: String): Result<String>
    suspend fun logout()
    fun getCurrentUser(): User?
}
