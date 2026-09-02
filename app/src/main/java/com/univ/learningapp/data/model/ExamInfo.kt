package com.univ.learningapp.data.model

data class ExamInfo(
    val id: String,
    val title: String,
    val subject: String,
    val totalQuestions: Int = 50,
    val durationMinutes: Int = 60,
    val totalMarks: Int = 200,
    val passingMarks: Int = 80,
    val isCompleted: Boolean = false,
    val previousScore: Double? = null,
    val isLocked: Boolean = false,
    val priceInInr: Int = 0,
    val examCategory: String = "MCQ Mock Test" // "PYQ" or "MCQ Mock Test"
)
