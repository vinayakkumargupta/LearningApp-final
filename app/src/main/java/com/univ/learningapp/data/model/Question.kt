package com.univ.learningapp.data.model

data class Question(
    val id: Int,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
    val subject: String = "General Studies"
)

data class UserQuestionState(
    val questionId: Int,
    val selectedOptionIndex: Int? = null,
    val isFlagged: Boolean = false
)
