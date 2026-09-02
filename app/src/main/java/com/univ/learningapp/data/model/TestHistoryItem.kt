package com.univ.learningapp.data.model

data class TestHistoryItem(
    val resultId: String,
    val examId: String,
    val examTitle: String,
    val marksObtained: Double,
    val totalMarks: Int = 200,
    val percentage: Double,
    val accuracy: Double,
    val rank: Int,
    val percentile: Double,
    val dateAttemptedFormatted: String,
    val timeTakenSeconds: Long
)
