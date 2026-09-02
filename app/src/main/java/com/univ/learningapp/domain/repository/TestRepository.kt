package com.univ.learningapp.domain.repository

import com.univ.learningapp.data.model.ExamResult
import com.univ.learningapp.data.model.Question

interface TestRepository {
    suspend fun getMockTest(examId: String): Result<List<Question>>
    suspend fun submitTest(
        examId: String,
        examTitle: String,
        userAnswers: Map<Int, Int?>,
        timeTakenSeconds: Long
    ): Result<ExamResult>
    suspend fun getTestResult(resultId: String): Result<ExamResult>
}
