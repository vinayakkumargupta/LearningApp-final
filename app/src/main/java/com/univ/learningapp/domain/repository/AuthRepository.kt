package com.univ.learningapp.domain.repository

import android.net.Uri
import com.univ.learningapp.data.model.User
import com.univ.learningapp.data.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserFlow: Flow<User?>
    suspend fun login(email: String, pass: String): Result<User>
    suspend fun signup(name: String, email: String, pass: String, requestAdminAccess: Boolean = false): Result<User>
    suspend fun resetPassword(email: String): Result<String>
    suspend fun logout()
    fun getCurrentUser(): User?

    // Super Admin User Management
    suspend fun getAllUsers(): Result<List<User>>
    suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Boolean>

    // Profile
    suspend fun updateProfile(
        phoneNumber: String,
        instagramHandle: String,
        twitterHandle: String,
        linkedinHandle: String
    ): Result<Boolean>
    suspend fun uploadProfilePhoto(photoUri: Uri): Result<String>
}
