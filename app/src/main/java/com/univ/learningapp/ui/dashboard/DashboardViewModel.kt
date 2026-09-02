package com.univ.learningapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univ.learningapp.data.model.ExamInfo
import com.univ.learningapp.data.model.PdfMaterial
import com.univ.learningapp.data.model.TestHistoryItem
import com.univ.learningapp.data.model.User
import com.univ.learningapp.domain.repository.AuthRepository
import com.univ.learningapp.domain.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ContentFilter {
    ALL,
    FREE,
    PREMIUM_LOCKED,
    COMPLETED
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val user: User?,
        val pdfs: List<PdfMaterial>,
        val exams: List<ExamInfo>,
        val historyList: List<TestHistoryItem> = emptyList(),
        val activeTabIndex: Int = 0, // 0: Mock Tests (50 MCQs), 1: PDFs/Videos, 2: History
        val searchQuery: String = "",
        val activeFilter: ContentFilter = ContentFilter.ALL
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val learningRepository: LearningRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            val currentState = _uiState.value as? DashboardUiState.Success
            val tabIndex = currentState?.activeTabIndex ?: 0
            val search = currentState?.searchQuery ?: ""
            val filter = currentState?.activeFilter ?: ContentFilter.ALL

            val currentUser = authRepository.getCurrentUser()
            val pdfsResult = learningRepository.getPdfMaterials()
            val examsResult = learningRepository.getAvailableExams()
            val historyResult = learningRepository.getTestHistory()

            if (pdfsResult.isSuccess && examsResult.isSuccess) {
                _uiState.value = DashboardUiState.Success(
                    user = currentUser,
                    pdfs = pdfsResult.getOrDefault(emptyList()),
                    exams = examsResult.getOrDefault(emptyList()),
                    historyList = historyResult.getOrDefault(emptyList()),
                    activeTabIndex = tabIndex,
                    searchQuery = search,
                    activeFilter = filter
                )
            } else {
                _uiState.value = DashboardUiState.Error("Failed to load dashboard content.")
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        val currentState = _uiState.value as? DashboardUiState.Success ?: return
        _uiState.value = currentState.copy(activeTabIndex = tabIndex)
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value as? DashboardUiState.Success ?: return
        _uiState.value = currentState.copy(searchQuery = query)
    }

    fun setFilter(filter: ContentFilter) {
        val currentState = _uiState.value as? DashboardUiState.Success ?: return
        _uiState.value = currentState.copy(activeFilter = filter)
    }

    fun unlockExam(examId: String) {
        viewModelScope.launch {
            val result = learningRepository.unlockExam(examId)
            if (result.isSuccess) {
                loadDashboardData()
            }
        }
    }

    fun unlockMaterial(materialId: String) {
        viewModelScope.launch {
            val result = learningRepository.unlockMaterial(materialId)
            if (result.isSuccess) {
                loadDashboardData()
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
