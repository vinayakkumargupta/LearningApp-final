package com.univ.learningapp.data.model

enum class UserRole {
    MEMBER,
    PENDING_ADMIN,
    ADMIN,
    SUPER_ADMIN
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.MEMBER,
    val token: String? = null,
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val instagramHandle: String = "",
    val twitterHandle: String = "",
    val linkedinHandle: String = ""
)
