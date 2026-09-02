package com.univ.learningapp.data.repository

import com.univ.learningapp.data.model.ExamResult
import com.univ.learningapp.data.model.Question
import com.univ.learningapp.data.model.QuestionReviewItem
import com.univ.learningapp.data.model.TestHistoryItem
import com.univ.learningapp.domain.repository.TestRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestRepositoryImpl @Inject constructor() : TestRepository {

    private val storedResults = mutableMapOf<String, ExamResult>()

    override suspend fun getMockTest(examId: String): Result<List<Question>> {
        delay(600) // Simulate fetching questions
        val questions = generate50Questions()
        return Result.success(questions)
    }

    override suspend fun submitTest(
        examId: String,
        examTitle: String,
        userAnswers: Map<Int, Int?>,
        timeTakenSeconds: Long
    ): Result<ExamResult> {
        delay(800) // Simulate processing submission
        val questions = generate50Questions()

        var correctCount = 0
        var wrongCount = 0
        var unattemptedCount = 0

        val reviewItems = mutableListOf<QuestionReviewItem>()

        questions.forEach { question ->
            val selectedOption = userAnswers[question.id]
            val isUnattempted = selectedOption == null || selectedOption < 0
            val isCorrect = !isUnattempted && selectedOption == question.correctOptionIndex

            if (isUnattempted) {
                unattemptedCount++
            } else if (isCorrect) {
                correctCount++
            } else {
                wrongCount++
            }

            reviewItems.add(
                QuestionReviewItem(
                    question = question,
                    selectedOptionIndex = if (isUnattempted) null else selectedOption,
                    isCorrect = isCorrect,
                    isUnattempted = isUnattempted
                )
            )
        }

        val marksObtained = (correctCount * 4.0) - (wrongCount * 1.0)
        val maxMarks = 200
        val percentage = (marksObtained / maxMarks) * 100
        val totalAttempted = correctCount + wrongCount
        val accuracy = if (totalAttempted > 0) (correctCount.toDouble() / totalAttempted) * 100 else 0.0

        val totalCandidates = 4500
        val calculatedRank = when {
            marksObtained >= 180 -> (1..25).random()
            marksObtained >= 150 -> (26..150).random()
            marksObtained >= 120 -> (151..450).random()
            marksObtained >= 80 -> (451..1200).random()
            marksObtained >= 40 -> (1201..2800).random()
            else -> (2801..4200).random()
        }
        val percentile = ((totalCandidates - calculatedRank).toDouble() / totalCandidates) * 100

        val resultId = "result_${examId}_${System.currentTimeMillis()}"
        val examResult = ExamResult(
            resultId = resultId,
            examId = examId,
            examTitle = examTitle,
            totalQuestions = 50,
            attemptedCount = totalAttempted,
            correctCount = correctCount,
            wrongCount = wrongCount,
            unattemptedCount = unattemptedCount,
            totalMarks = maxMarks,
            marksObtained = maxOf(0.0, marksObtained),
            percentage = maxOf(0.0, percentage),
            accuracy = accuracy,
            rank = calculatedRank,
            percentile = percentile,
            timeTakenSeconds = timeTakenSeconds,
            reviewItems = reviewItems
        )

        storedResults[resultId] = examResult

        // Record in global test history
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date())
        val historyItem = TestHistoryItem(
            resultId = resultId,
            examId = examId,
            examTitle = examTitle,
            marksObtained = maxOf(0.0, marksObtained),
            totalMarks = maxMarks,
            percentage = maxOf(0.0, percentage),
            accuracy = accuracy,
            rank = calculatedRank,
            percentile = percentile,
            dateAttemptedFormatted = formattedDate,
            timeTakenSeconds = timeTakenSeconds
        )
        LearningRepositoryImpl.globalTestHistory.add(0, historyItem)

        return Result.success(examResult)
    }

    override suspend fun getTestResult(resultId: String): Result<ExamResult> {
        delay(400)
        val result = storedResults[resultId]
        return if (result != null) {
            Result.success(result)
        } else {
            val questions = generate50Questions()
            val mockReviewItems = questions.mapIndexed { index, q ->
                QuestionReviewItem(
                    question = q,
                    selectedOptionIndex = if (index % 3 == 0) q.correctOptionIndex else (q.correctOptionIndex + 1) % 4,
                    isCorrect = index % 3 == 0,
                    isUnattempted = index % 5 == 0
                )
            }
            val mockResult = ExamResult(
                resultId = resultId,
                examId = "exam_mock_50_cs",
                examTitle = "All India Full Length CS & General Aptitude Mock Test 1",
                totalQuestions = 50,
                attemptedCount = 40,
                correctCount = 30,
                wrongCount = 10,
                unattemptedCount = 10,
                totalMarks = 200,
                marksObtained = 110.0,
                percentage = 55.0,
                accuracy = 75.0,
                rank = 284,
                percentile = 93.68,
                timeTakenSeconds = 2450,
                reviewItems = mockReviewItems
            )
            Result.success(mockResult)
        }
    }

    private fun generate50Questions(): List<Question> {
        val list = mutableListOf<Question>()

        val topics = listOf(
            "Kotlin Programming", "Data Structures", "Operating Systems",
            "Computer Networks", "Database Systems", "Software Engineering",
            "General Aptitude", "Reasoning & Logic"
        )

        val templates = listOf(
            QuestionTemplate(
                "Which of the following keywords in Kotlin is used to declare a read-only variable?",
                listOf("var", "val", "const var", "final var"),
                1,
                "In Kotlin, 'val' creates a read-only local variable (value) that cannot be reassigned once initialized.",
                "Kotlin Programming"
            ),
            QuestionTemplate(
                "What is the worst-case time complexity of Quick Sort algorithm?",
                listOf("O(n log n)", "O(n)", "O(n²)", "O(log n)"),
                2,
                "The worst-case time complexity of Quick Sort is O(n²), which occurs when the pivot element is consistently the smallest or largest element.",
                "Data Structures"
            ),
            QuestionTemplate(
                "In Android Jetpack Compose, which function is used to preserve state across recompositions and configuration changes?",
                listOf("remember { }", "rememberSaveable { }", "derivedStateOf { }", "mutableStateOf { }"),
                1,
                "'rememberSaveable' saves and restores state across Activity recreation using SavedInstance state.",
                "Android Architecture"
            ),
            QuestionTemplate(
                "Which component in Dagger Hilt indicates that dependencies should live for the entire duration of the application?",
                listOf("@ActivityRetainedComponent", "@Singleton / @ApplicationComponent", "@FragmentComponent", "@ViewModelScoped"),
                1,
                "In Dagger Hilt, @Singleton installed in SingletonComponent scope lives as long as the Application instance.",
                "Dagger Hilt"
            ),
            QuestionTemplate(
                "Which HTTP method is idempotent and used to replace an entire resource target?",
                listOf("POST", "PUT", "PATCH", "DELETE"),
                1,
                "HTTP PUT is idempotent and intended for full resource creation or complete replacement.",
                "Computer Networks"
            ),
            QuestionTemplate(
                "What is deadlock condition in Operating Systems where process A holds resource 1 and waits for resource 2?",
                listOf("Mutual Exclusion", "Hold and Wait / Circular Wait", "Starvation", "Paging"),
                1,
                "Deadlock conditions require Mutual Exclusion, Hold & Wait, No Preemption, and Circular Wait.",
                "Operating Systems"
            ),
            QuestionTemplate(
                "Which SQL constraint ensures all values in a column are distinct and unique?",
                listOf("PRIMARY KEY / UNIQUE", "CHECK", "FOREIGN KEY", "DEFAULT"),
                0,
                "The UNIQUE constraint guarantees that all values in a column or set of columns are distinct.",
                "Database Systems"
            ),
            QuestionTemplate(
                "If a train running at 72 km/h crosses a pole in 15 seconds, what is the length of the train?",
                listOf("250 m", "300 m", "360 m", "200 m"),
                1,
                "Speed = 72 km/h = 20 m/s. Distance (length) = Speed * Time = 20 m/s * 15 s = 300 meters.",
                "General Aptitude"
            ),
            QuestionTemplate(
                "In Kotlin Coroutines, which dispatcher is optimized for disk or network I/O operations?",
                listOf("Dispatchers.Main", "Dispatchers.Default", "Dispatchers.IO", "Dispatchers.Unconfined"),
                2,
                "Dispatchers.IO is designed to offload blocking I/O tasks to a shared pool of threads.",
                "Kotlin Programming"
            ),
            QuestionTemplate(
                "Which data structure follows the Last-In-First-Out (LIFO) principle?",
                listOf("Queue", "Stack", "Array", "Binary Tree"),
                1,
                "A Stack uses the LIFO principle where the element pushed last is popped first.",
                "Data Structures"
            )
        )

        for (i in 1..50) {
            val tmpl = templates[(i - 1) % templates.size]
            val questionText = if (i <= templates.size) {
                tmpl.question
            } else {
                "Q$i: [Topic: ${topics[i % topics.size]}] ${tmpl.question} (Variant $i)"
            }

            list.add(
                Question(
                    id = i,
                    questionText = questionText,
                    options = tmpl.options,
                    correctOptionIndex = tmpl.correctIndex,
                    explanation = tmpl.explanation,
                    subject = tmpl.subject
                )
            )
        }

        return list
    }

    private data class QuestionTemplate(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String,
        val subject: String
    )
}
