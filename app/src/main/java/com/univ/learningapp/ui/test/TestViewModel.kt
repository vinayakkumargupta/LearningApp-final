package com.univ.learningapp.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univ.learningapp.data.model.ExamResult
import com.univ.learningapp.data.model.Question
import com.univ.learningapp.domain.repository.TestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TestUiState {
    object Loading : TestUiState()
    data class ActiveTest(
        val questions: List<Question>,
        val currentQuestionIndex: Int = 0,
        val userAnswers: Map<Int, Int?> = emptyMap(), // questionId -> selectedOptionIndex
        val flaggedQuestions: Set<Int> = emptySet(), // questionIds flagged
        val remainingTimeSeconds: Long = 3600, // 60 mins countdown
        val isSubmitting: Boolean = false
    ) : TestUiState()
    data class Error(val message: String) : TestUiState()
}

sealed class ResultUiState {
    object Loading : ResultUiState()
    data class Success(val result: ExamResult) : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}

@HiltViewModel
class TestViewModel @Inject constructor(
    private val testRepository: TestRepository
) : ViewModel() {

    private val _testState = MutableStateFlow<TestUiState>(TestUiState.Loading)
    val testState: StateFlow<TestUiState> = _testState.asStateFlow()

    private val _resultState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val resultState: StateFlow<ResultUiState> = _resultState.asStateFlow()

    private var timerJob: Job? = null
    private var initialTimeSeconds: Long = 3600

    fun loadMockTest(examId: String) {
        viewModelScope.launch {
            _testState.value = TestUiState.Loading
            val result = testRepository.getMockTest(examId)
            result.fold(
                onSuccess = { questionList ->
                    _testState.value = TestUiState.ActiveTest(
                        questions = questionList,
                        currentQuestionIndex = 0,
                        userAnswers = emptyMap(),
                        flaggedQuestions = emptySet(),
                        remainingTimeSeconds = initialTimeSeconds
                    )
                    startTimer()
                },
                onFailure = {
                    _testState.value = TestUiState.Error("Failed to load questions.")
                }
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentState = _testState.value
                if (currentState is TestUiState.ActiveTest) {
                    val nextTime = currentState.remainingTimeSeconds - 1
                    if (nextTime <= 0) {
                        _testState.value = currentState.copy(remainingTimeSeconds = 0)
                        break
                    } else {
                        _testState.value = currentState.copy(remainingTimeSeconds = nextTime)
                    }
                } else {
                    break
                }
            }
        }
    }

    fun selectOption(questionId: Int, optionIndex: Int) {
        val currentState = _testState.value as? TestUiState.ActiveTest ?: return
        val updatedAnswers = currentState.userAnswers.toMutableMap()
        updatedAnswers[questionId] = optionIndex
        _testState.value = currentState.copy(userAnswers = updatedAnswers)
    }

    fun clearOption(questionId: Int) {
        val currentState = _testState.value as? TestUiState.ActiveTest ?: return
        val updatedAnswers = currentState.userAnswers.toMutableMap()
        updatedAnswers.remove(questionId)
        _testState.value = currentState.copy(userAnswers = updatedAnswers)
    }

    fun toggleFlag(questionId: Int) {
        val currentState = _testState.value as? TestUiState.ActiveTest ?: return
        val updatedFlags = currentState.flaggedQuestions.toMutableSet()
        if (updatedFlags.contains(questionId)) {
            updatedFlags.remove(questionId)
        } else {
            updatedFlags.add(questionId)
        }
        _testState.value = currentState.copy(flaggedQuestions = updatedFlags)
    }

    fun goToQuestion(index: Int) {
        val currentState = _testState.value as? TestUiState.ActiveTest ?: return
        if (index in currentState.questions.indices) {
            _testState.value = currentState.copy(currentQuestionIndex = index)
        }
    }

    fun nextQuestion() {
        val currentState = _testState.value as? TestUiState.ActiveTest ?: return
        if (currentState.currentQuestionIndex < currentState.questions.size - 1) {
            _testState.value = currentState.copy(
                currentQuestionIndex = currentState.currentQuestionIndex + 1
            )
        }
    }

    fun previousQuestion() {
        val currentState = _testState.value as? TestUiState.ActiveTest ?: return
        if (currentState.currentQuestionIndex > 0) {
            _testState.value = currentState.copy(
                currentQuestionIndex = currentState.currentQuestionIndex - 1
            )
        }
    }

    fun submitTest(
        examId: String,
        examTitle: String,
        onSubmitted: (resultId: String) -> Unit
    ) {
        val currentState = _testState.value as? TestUiState.ActiveTest ?: return
        timerJob?.cancel()

        viewModelScope.launch {
            _testState.value = currentState.copy(isSubmitting = true)
            val timeTakenSeconds = initialTimeSeconds - currentState.remainingTimeSeconds

            val submitResult = testRepository.submitTest(
                examId = examId,
                examTitle = examTitle,
                userAnswers = currentState.userAnswers,
                timeTakenSeconds = maxOf(1, timeTakenSeconds)
            )

            submitResult.fold(
                onSuccess = { result ->
                    _resultState.value = ResultUiState.Success(result)
                    onSubmitted(result.resultId)
                },
                onFailure = {
                    _testState.value = TestUiState.Error("Submission failed. Please try again.")
                }
            )
        }
    }

    fun loadResult(resultId: String) {
        viewModelScope.launch {
            _resultState.value = ResultUiState.Loading
            val result = testRepository.getTestResult(resultId)
            result.fold(
                onSuccess = { _resultState.value = ResultUiState.Success(it) },
                onFailure = { _resultState.value = ResultUiState.Error(it.message ?: "Could not load result.") }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
