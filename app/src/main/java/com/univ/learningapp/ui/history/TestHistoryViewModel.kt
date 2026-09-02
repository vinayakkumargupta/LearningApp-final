package com.univ.learningapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univ.learningapp.data.model.TestHistoryItem
import com.univ.learningapp.domain.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(val historyList: List<TestHistoryItem>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

@HiltViewModel
class TestHistoryViewModel @Inject constructor(
    private val learningRepository: LearningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            val result = learningRepository.getTestHistory()
            result.fold(
                onSuccess = { list ->
                    _uiState.value = HistoryUiState.Success(list)
                },
                onFailure = {
                    _uiState.value = HistoryUiState.Error("Failed to load test history.")
                }
            )
        }
    }
}
