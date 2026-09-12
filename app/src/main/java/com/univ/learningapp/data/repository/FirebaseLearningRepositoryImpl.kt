package com.univ.learningapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.univ.learningapp.data.model.ExamInfo
import com.univ.learningapp.data.model.MaterialType
import com.univ.learningapp.data.model.PdfMaterial
import com.univ.learningapp.data.model.Question
import com.univ.learningapp.data.model.TestHistoryItem
import com.univ.learningapp.data.model.UserRole
import com.univ.learningapp.domain.repository.LearningRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseLearningRepositoryImpl @Inject constructor() : LearningRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val localUnlockedItems = mutableSetOf<String>()

    companion object {
        val globalTestHistory = mutableListOf<TestHistoryItem>()
    }

    override suspend fun getPdfMaterials(): Result<List<PdfMaterial>> {
        val db = firestore
        if (db != null) {
            try {
                val snapshot = db.collection("materials").get().await()
                if (!snapshot.isEmpty) {
                    val materials = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val title = doc.getString("title") ?: return@mapNotNull null
                        val subject = doc.getString("subject") ?: "General"
                        val pagesCount = (doc.getLong("pagesCount") ?: 0L).toInt()
                        val fileSizeMb = doc.getString("fileSizeMb") ?: "2.5 MB"
                        val pdfUrl = doc.getString("pdfUrl") ?: ""
                        val videoUrl = doc.getString("videoUrl")
                        val videoDuration = doc.getString("videoDuration")
                        val description = doc.getString("description") ?: ""
                        val category = doc.getString("category") ?: "Notes"
                        val typeStr = doc.getString("materialType") ?: "PDF"
                        val materialType = if (typeStr == "VIDEO") MaterialType.VIDEO else MaterialType.PDF
                        val isLockedInDb = doc.getBoolean("isLocked") ?: false
                        val price = (doc.getLong("priceInInr") ?: 0L).toInt()

                        PdfMaterial(
                            id = id,
                            title = title,
                            subject = subject,
                            pagesCount = pagesCount,
                            fileSizeMb = fileSizeMb,
                            pdfUrl = pdfUrl,
                            videoUrl = videoUrl,
                            videoDuration = videoDuration,
                            description = description,
                            category = category,
                            materialType = materialType,
                            isLocked = if (localUnlockedItems.contains(id)) false else isLockedInDb,
                            priceInInr = price
                        )
                    }
                    return Result.success(materials)
                }
            } catch (_: Exception) { }
        }

        return Result.success(getSampleMaterials())
    }

    override suspend fun getAvailableExams(): Result<List<ExamInfo>> {
        val db = firestore
        if (db != null) {
            try {
                val snapshot = db.collection("test_series").get().await()
                if (!snapshot.isEmpty) {
                    val exams = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val title = doc.getString("title") ?: return@mapNotNull null
                        val subject = doc.getString("subject") ?: "General"
                        val totalQuestions = (doc.getLong("totalQuestions") ?: 50L).toInt()
                        val durationMinutes = (doc.getLong("durationMinutes") ?: 60L).toInt()
                        val totalMarks = (doc.getLong("totalMarks") ?: 200L).toInt()
                        val passingMarks = (doc.getLong("passingMarks") ?: 80L).toInt()
                        val isCompleted = doc.getBoolean("isCompleted") ?: false
                        val previousScore = doc.getDouble("previousScore")
                        val isLockedInDb = doc.getBoolean("isLocked") ?: false
                        val price = (doc.getLong("priceInInr") ?: 0L).toInt()
                        val category = doc.getString("examCategory") ?: "MCQ Mock Test"

                        ExamInfo(
                            id = id,
                            title = title,
                            subject = subject,
                            totalQuestions = totalQuestions,
                            durationMinutes = durationMinutes,
                            totalMarks = totalMarks,
                            passingMarks = passingMarks,
                            isCompleted = isCompleted,
                            previousScore = previousScore,
                            isLocked = if (localUnlockedItems.contains(id)) false else isLockedInDb,
                            priceInInr = price,
                            examCategory = category
                        )
                    }
                    return Result.success(exams)
                }
            } catch (_: Exception) { }
        }

        return Result.success(getSampleExams())
    }

    override suspend fun unlockMaterial(materialId: String): Result<Boolean> {
        localUnlockedItems.add(materialId)
        return Result.success(true)
    }

    override suspend fun unlockExam(examId: String): Result<Boolean> {
        localUnlockedItems.add(examId)
        return Result.success(true)
    }

    override suspend fun getTestHistory(): Result<List<TestHistoryItem>> {
        val db = firestore
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (db != null && currentUser != null) {
            try {
                // First get user role to decide if we fetch all or just own
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                val roleStr = userDoc.getString("role") ?: "MEMBER"
                val isSuperAdmin = currentUser.email?.equals("vinayak678admin@learning.com", ignoreCase = true) == true
                val isAdminOrSuperAdmin = isSuperAdmin || roleStr == "ADMIN" || roleStr == "SUPER_ADMIN"

                val snapshot = if (isAdminOrSuperAdmin) {
                    db.collection("submissions").get().await()
                } else {
                    db.collection("submissions").whereEqualTo("userId", currentUser.uid).get().await()
                }
                
                if (!snapshot.isEmpty) {
                    val history = snapshot.documents.mapNotNull { doc ->
                        val resultId = doc.id
                        val examId = doc.getString("examId") ?: "exam_cs_01"
                        val examTitle = doc.getString("examTitle") ?: "Mock Test"
                        val marksObtained = doc.getDouble("marksObtained") ?: 0.0
                        val totalMarks = (doc.getLong("totalMarks") ?: 200L).toInt()
                        val percentage = doc.getDouble("percentage") ?: 0.0
                        val accuracy = doc.getDouble("accuracy") ?: 0.0
                        val rank = (doc.getLong("rank") ?: 1L).toInt()
                        val percentile = doc.getDouble("percentile") ?: 99.0
                        val date = doc.getString("dateAttemptedFormatted") ?: "Recent"
                        val timeTaken = doc.getLong("timeTakenSeconds") ?: 1800L

                        TestHistoryItem(
                            resultId = resultId,
                            examId = examId,
                            examTitle = examTitle,
                            marksObtained = marksObtained,
                            totalMarks = totalMarks,
                            percentage = percentage,
                            accuracy = accuracy,
                            rank = rank,
                            percentile = percentile,
                            dateAttemptedFormatted = date,
                            timeTakenSeconds = timeTaken
                        )
                    }
                    return Result.success(history.sortedByDescending { it.resultId })
                }
            } catch (_: Exception) { }
        }

        return Result.success(globalTestHistory.toList())
    }

    // Admin CRUD Operations
    override suspend fun saveExam(exam: ExamInfo, questions: List<Question>): Result<Boolean> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val examRef = db.collection("test_series").document(exam.id)
            val examData = hashMapOf(
                "title" to exam.title,
                "subject" to exam.subject,
                "totalQuestions" to questions.size,
                "durationMinutes" to exam.durationMinutes,
                "totalMarks" to if (exam.totalMarks > 0) exam.totalMarks else questions.size * 4,
                "passingMarks" to exam.passingMarks,
                "isCompleted" to false,
                "isLocked" to (exam.priceInInr > 0),
                "priceInInr" to exam.priceInInr,
                "examCategory" to exam.examCategory
            )
            examRef.set(examData).await()

            // Delete old questions if overwriting
            val existingQ = examRef.collection("questions").get().await()
            val batchClear = db.batch()
            existingQ.documents.forEach { batchClear.delete(it.reference) }
            batchClear.commit().await()

            // Save new questions
            val batch = db.batch()
            questions.forEachIndexed { idx, q ->
                val qRef = examRef.collection("questions").document("q${idx + 1}")
                batch.set(
                    qRef,
                    hashMapOf(
                        "id" to (idx + 1),
                        "questionText" to q.questionText,
                        "options" to q.options,
                        "correctOptionIndex" to q.correctOptionIndex,
                        "explanation" to q.explanation,
                        "subject" to if (q.subject.isBlank()) exam.subject else q.subject
                    )
                )
            }
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExam(examId: String): Result<Boolean> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val examRef = db.collection("test_series").document(examId)
            val qSnap = examRef.collection("questions").get().await()
            val batch = db.batch()
            qSnap.documents.forEach { batch.delete(it.reference) }
            batch.delete(examRef)
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveMaterial(material: PdfMaterial): Result<Boolean> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val matRef = db.collection("materials").document(material.id)
            val matData = hashMapOf(
                "title" to material.title,
                "subject" to material.subject,
                "pagesCount" to material.pagesCount,
                "fileSizeMb" to material.fileSizeMb,
                "pdfUrl" to material.pdfUrl,
                "videoUrl" to (material.videoUrl ?: ""),
                "videoDuration" to (material.videoDuration ?: ""),
                "description" to material.description,
                "category" to material.category,
                "materialType" to material.materialType.name,
                "isLocked" to (material.priceInInr > 0),
                "priceInInr" to material.priceInInr
            )
            matRef.set(matData).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMaterial(materialId: String): Result<Boolean> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("materials").document(materialId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkSaveExamsJson(jsonString: String): Result<String> {
        val raw = jsonString.trim()
        if (raw.isBlank()) return Result.failure(IllegalArgumentException("JSON string is empty."))

        return try {
            val jsonArray = if (raw.startsWith("[")) {
                JSONArray(raw)
            } else {
                JSONArray().put(JSONObject(raw))
            }

            var successCount = 0
            var totalQuestionsUploaded = 0

            for (i in 0 until jsonArray.length()) {
                val examObj = jsonArray.getJSONObject(i)
                val id = examObj.optString("id").trim()
                val title = examObj.optString("title").trim()
                val subject = examObj.optString("subject", "General").trim()
                val examCategory = examObj.optString("examCategory", "MCQ Mock Test").trim()
                val durationMinutes = examObj.optInt("durationMinutes", 60)
                val priceInInr = examObj.optInt("priceInInr", 0)

                val questionsArray = examObj.optJSONArray("questions") ?: JSONArray()
                val parsedQuestions = mutableListOf<Question>()

                for (j in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(j)
                    val qText = qObj.optString("questionText").trim()
                    val qExplain = qObj.optString("explanation", "").trim()
                    val qSubject = qObj.optString("subject", subject).trim()
                    val correctIdx = qObj.optInt("correctOptionIndex", 0)

                    val optArray = qObj.optJSONArray("options")
                    val optionsList = mutableListOf<String>()
                    if (optArray != null) {
                        for (k in 0 until optArray.length()) {
                            optionsList.add(optArray.getString(k))
                        }
                    }

                    if (qText.isNotBlank() && optionsList.isNotEmpty()) {
                        parsedQuestions.add(
                            Question(
                                id = j + 1,
                                questionText = qText,
                                options = optionsList,
                                correctOptionIndex = correctIdx,
                                explanation = qExplain,
                                subject = qSubject
                            )
                        )
                    }
                }

                if (id.isNotBlank() && title.isNotBlank() && parsedQuestions.isNotEmpty()) {
                    val totalMarks = examObj.optInt("totalMarks", parsedQuestions.size * 4)
                    val passingMarks = examObj.optInt("passingMarks", 0)

                    val exam = ExamInfo(
                        id = id,
                        title = title,
                        subject = subject,
                        totalQuestions = parsedQuestions.size,
                        durationMinutes = durationMinutes,
                        totalMarks = totalMarks,
                        passingMarks = passingMarks,
                        priceInInr = priceInInr,
                        isLocked = priceInInr > 0,
                        examCategory = examCategory
                    )

                    val saved = saveExam(exam, parsedQuestions)
                    if (saved.isSuccess) {
                        successCount++
                        totalQuestionsUploaded += parsedQuestions.size
                    }
                }
            }

            if (successCount > 0) {
                Result.success("Successfully imported $successCount exam(s) with $totalQuestionsUploaded total questions to Firestore!")
            } else {
                Result.failure(Exception("No valid exam objects found in JSON."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Invalid JSON syntax: ${e.localizedMessage}"))
        }
    }

    private fun getSampleMaterials(): List<PdfMaterial> {
        return listOf(
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
                isLocked = if (localUnlockedItems.contains("pdf_1")) false else false,
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
                isLocked = if (localUnlockedItems.contains("pdf_2")) false else false,
                priceInInr = 0
            ),
            PdfMaterial(
                id = "vid_1",
                title = "Video Masterclass: Kotlin Coroutines, Flow & Concurrency",
                subject = "Mobile Development",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                videoDuration = "45 Mins",
                description = "Complete video breakdown on Dispatchers, Structured Concurrency, Exception handling & StateFlow.",
                category = "Video Lecture",
                materialType = MaterialType.VIDEO,
                isLocked = if (localUnlockedItems.contains("vid_1")) false else true,
                priceInInr = 199
            )
        )
    }

    private fun getSampleExams(): List<ExamInfo> {
        return listOf(
            ExamInfo(
                id = "exam_mock_50_cs",
                title = "All India Full Length CS & General Aptitude Mock Test 1",
                subject = "CS & General Aptitude",
                totalQuestions = 50,
                durationMinutes = 60,
                totalMarks = 200,
                passingMarks = 80,
                isCompleted = false,
                isLocked = if (localUnlockedItems.contains("exam_mock_50_cs")) false else false,
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
                isLocked = if (localUnlockedItems.contains("exam_pyq_2025_cs")) false else false,
                priceInInr = 0,
                examCategory = "PYQ"
            )
        )
    }
}
