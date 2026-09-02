package com.univ.learningapp.data.repository

import com.univ.learningapp.data.model.ExamInfo
import com.univ.learningapp.data.model.MaterialType
import com.univ.learningapp.data.model.PdfMaterial
import com.univ.learningapp.data.model.TestHistoryItem
import com.univ.learningapp.domain.repository.LearningRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearningRepositoryImpl @Inject constructor() : LearningRepository {

    private val materialsList = mutableListOf(
        PdfMaterial(
            id = "pdf_1",
            title = "Data Structures & Algorithms Cheat Sheet",
            subject = "Computer Science",
            pagesCount = 42,
            fileSizeMb = "4.2 MB",
            pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            description = "Comprehensive summary of Arrays, Linked Lists, Trees, Graphs, Sorting, and Dynamic Programming.",
            category = "Core CS",
            materialType = MaterialType.PDF,
            isLocked = false,
            priceInInr = 0
        ),
        PdfMaterial(
            id = "pdf_2",
            title = "Android Jetpack Compose & Clean Architecture Guide",
            subject = "Mobile Development",
            pagesCount = 28,
            fileSizeMb = "3.1 MB",
            pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            description = "Detailed patterns on State Management, ViewModels, Repository pattern, and Dagger Hilt DI.",
            category = "Android",
            materialType = MaterialType.PDF,
            isLocked = false,
            priceInInr = 0
        ),
        PdfMaterial(
            id = "vid_1",
            title = "Video Lecture: Advanced Masterclass on Kotlin Coroutines & Flow",
            subject = "Mobile Development",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            videoDuration = "45 Mins",
            description = "Complete video breakdown on Dispatchers, Structured Concurrency, Exception handling & StateFlow.",
            category = "Video Lecture",
            materialType = MaterialType.VIDEO,
            isLocked = true,
            priceInInr = 199
        ),
        PdfMaterial(
            id = "pdf_3",
            title = "Premium GATE & UPSC CS 10 Years Question Bank PDF",
            subject = "Computer Science",
            pagesCount = 120,
            fileSizeMb = "12.5 MB",
            pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            description = "Exhaustive question bank with detailed official solutions from 2016 to 2026.",
            category = "Premium Notes",
            materialType = MaterialType.PDF,
            isLocked = true,
            priceInInr = 299
        ),
        PdfMaterial(
            id = "vid_2",
            title = "Video Course: System Design & Microservices Architecture",
            subject = "Software Engineering",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            videoDuration = "1 Hr 15 Mins",
            description = "In-depth video analysis on Load Balancers, Sharding, Message Queues, and Distributed Caching.",
            category = "Video Course",
            materialType = MaterialType.VIDEO,
            isLocked = true,
            priceInInr = 399
        ),
        PdfMaterial(
            id = "pdf_4",
            title = "Quantitative Aptitude & Mathematical Formulas Handbook",
            subject = "Mathematics",
            pagesCount = 35,
            fileSizeMb = "2.9 MB",
            pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            description = "Quick shortcut formulas for Algebra, Geometry, Probability, and Data Interpretation.",
            category = "Aptitude",
            materialType = MaterialType.PDF,
            isLocked = false,
            priceInInr = 0
        )
    )

    private val examsList = mutableListOf(
        ExamInfo(
            id = "exam_mock_50_cs",
            title = "All India Full Length CS & General Aptitude Mock Test 1",
            subject = "CS & General Aptitude",
            totalQuestions = 50,
            durationMinutes = 60,
            totalMarks = 200,
            passingMarks = 80,
            isCompleted = false,
            isLocked = false,
            priceInInr = 0,
            examCategory = "MCQ Mock Test"
        ),
        ExamInfo(
            id = "exam_pyq_2025_cs",
            title = "Official PYQ 2025 Computer Science Previous Year Paper",
            subject = "Computer Science PYQ",
            totalQuestions = 50,
            durationMinutes = 60,
            totalMarks = 200,
            passingMarks = 80,
            isCompleted = false,
            isLocked = false,
            priceInInr = 0,
            examCategory = "PYQ"
        ),
        ExamInfo(
            id = "exam_mock_50_tech",
            title = "Android & Kotlin Software Engineer Master Speed Test",
            subject = "Android Architecture",
            totalQuestions = 50,
            durationMinutes = 60,
            totalMarks = 200,
            passingMarks = 80,
            isCompleted = true,
            previousScore = 152.0,
            isLocked = true,
            priceInInr = 149,
            examCategory = "MCQ Mock Test"
        ),
        ExamInfo(
            id = "exam_pyq_premium_combo",
            title = "Premium All India National Championship Mock Series 2026",
            subject = "Full Syllabus Combo",
            totalQuestions = 50,
            durationMinutes = 60,
            totalMarks = 200,
            passingMarks = 80,
            isCompleted = false,
            isLocked = true,
            priceInInr = 299,
            examCategory = "PYQ"
        )
    )

    companion object {
        val globalTestHistory = mutableListOf(
            TestHistoryItem(
                resultId = "hist_001",
                examId = "exam_mock_50_tech",
                examTitle = "Android & Kotlin Software Engineer Master Speed Test",
                marksObtained = 152.0,
                totalMarks = 200,
                percentage = 76.0,
                accuracy = 88.2,
                rank = 28,
                percentile = 99.37,
                dateAttemptedFormatted = "Yesterday at 4:30 PM",
                timeTakenSeconds = 2340
            )
        )
    }

    override suspend fun getPdfMaterials(): Result<List<PdfMaterial>> {
        delay(400)
        return Result.success(materialsList.toList())
    }

    override suspend fun getAvailableExams(): Result<List<ExamInfo>> {
        delay(400)
        return Result.success(examsList.toList())
    }

    override suspend fun unlockMaterial(materialId: String): Result<Boolean> {
        delay(600)
        val index = materialsList.indexOfFirst { it.id == materialId }
        if (index != -1) {
            val item = materialsList[index]
            materialsList[index] = item.copy(isLocked = false)
            return Result.success(true)
        }
        return Result.failure(IllegalArgumentException("Material not found."))
    }

    override suspend fun unlockExam(examId: String): Result<Boolean> {
        delay(600)
        val index = examsList.indexOfFirst { it.id == examId }
        if (index != -1) {
            val item = examsList[index]
            examsList[index] = item.copy(isLocked = false)
            return Result.success(true)
        }
        return Result.failure(IllegalArgumentException("Exam not found."))
    }

    override suspend fun getTestHistory(): Result<List<TestHistoryItem>> {
        delay(300)
        return Result.success(globalTestHistory.sortedByDescending { it.resultId })
    }
}
