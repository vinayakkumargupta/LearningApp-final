package com.univ.learningapp.domain.repository

import com.univ.learningapp.data.model.ExamInfo
import com.univ.learningapp.data.model.PdfMaterial
import com.univ.learningapp.data.model.Question
import com.univ.learningapp.data.model.TestHistoryItem

interface LearningRepository {
    suspend fun getPdfMaterials(): Result<List<PdfMaterial>>
    suspend fun getAvailableExams(): Result<List<ExamInfo>>
    suspend fun unlockMaterial(materialId: String): Result<Boolean>
    suspend fun unlockExam(examId: String): Result<Boolean>
    suspend fun getTestHistory(): Result<List<TestHistoryItem>>

    // Admin Operations
    suspend fun saveExam(exam: ExamInfo, questions: List<Question>): Result<Boolean>
    suspend fun deleteExam(examId: String): Result<Boolean>
    suspend fun saveMaterial(material: PdfMaterial): Result<Boolean>
    suspend fun deleteMaterial(materialId: String): Result<Boolean>
    suspend fun bulkSaveExamsJson(jsonString: String): Result<String>
}
