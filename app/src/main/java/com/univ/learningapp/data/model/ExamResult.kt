package com.univ.learningapp.data.model

data class ExamResult(
    val resultId: String,
    val examId: String,
    val examTitle: String,
    val totalQuestions: Int = 50,
    val attemptedCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unattemptedCount: Int,
    val totalMarks: Int = 200,
    val marksObtained: Double,
    val percentage: Double,
    val accuracy: Double,
    val rank: Int,
    val percentile: Double,
    val timeTakenSeconds: Long,
    val reviewItems: List<QuestionReviewItem>
)

data class QuestionReviewItem(
    val question: Question,
    val selectedOptionIndex: Int?,
    val isCorrect: Boolean,
    val isUnattempted: Boolean
)
