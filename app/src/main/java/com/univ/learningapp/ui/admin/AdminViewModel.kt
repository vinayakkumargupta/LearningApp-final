package com.univ.learningapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univ.learningapp.data.model.ExamInfo
import com.univ.learningapp.data.model.PdfMaterial
import com.univ.learningapp.data.model.Question
import com.univ.learningapp.data.model.User
import com.univ.learningapp.data.model.UserRole
import com.univ.learningapp.domain.repository.AuthRepository
import com.univ.learningapp.domain.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AdminUiState {
    object Idle : AdminUiState()
    object Loading : AdminUiState()
    data class Success(val message: String) : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val learningRepository: LearningRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _existingExams = MutableStateFlow<List<ExamInfo>>(emptyList())
    val existingExams: StateFlow<List<ExamInfo>> = _existingExams.asStateFlow()

    private val _existingMaterials = MutableStateFlow<List<PdfMaterial>>(emptyList())
    val existingMaterials: StateFlow<List<PdfMaterial>> = _existingMaterials.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    val currentUser: User?
        get() = authRepository.getCurrentUser()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val exams = learningRepository.getAvailableExams().getOrDefault(emptyList())
            val materials = learningRepository.getPdfMaterials().getOrDefault(emptyList())
            val users = authRepository.getAllUsers().getOrDefault(emptyList())
            _existingExams.value = exams
            _existingMaterials.value = materials
            _allUsers.value = users
        }
    }

    fun saveExam(exam: ExamInfo, questions: List<Question>) {
        viewModelScope.launch {
            if (exam.id.isBlank() || exam.title.isBlank()) {
                _uiState.value = AdminUiState.Error("Exam ID and Title are required.")
                return@launch
            }
            if (questions.isEmpty()) {
                _uiState.value = AdminUiState.Error("At least 1 question is required.")
                return@launch
            }

            _uiState.value = AdminUiState.Loading
            val result = learningRepository.saveExam(exam, questions)
            result.fold(
                onSuccess = {
                    _uiState.value = AdminUiState.Success("Saved exam '${exam.id}' with ${questions.size} questions.")
                    loadData()
                },
                onFailure = {
                    _uiState.value = AdminUiState.Error(it.message ?: "Failed to save exam.")
                }
            )
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            val result = learningRepository.deleteExam(examId)
            result.fold(
                onSuccess = {
                    _uiState.value = AdminUiState.Success("Deleted exam '$examId'.")
                    loadData()
                },
                onFailure = {
                    _uiState.value = AdminUiState.Error(it.message ?: "Failed to delete exam.")
                }
            )
        }
    }

    fun saveMaterial(material: PdfMaterial) {
        viewModelScope.launch {
            if (material.id.isBlank() || material.title.isBlank()) {
                _uiState.value = AdminUiState.Error("Material ID and Title are required.")
                return@launch
            }

            _uiState.value = AdminUiState.Loading
            val result = learningRepository.saveMaterial(material)
            result.fold(
                onSuccess = {
                    _uiState.value = AdminUiState.Success("Saved material '${material.id}'.")
                    loadData()
                },
                onFailure = {
                    _uiState.value = AdminUiState.Error(it.message ?: "Failed to save material.")
                }
            )
        }
    }

    fun deleteMaterial(materialId: String) {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            val result = learningRepository.deleteMaterial(materialId)
            result.fold(
                onSuccess = {
                    _uiState.value = AdminUiState.Success("Deleted material '$materialId'.")
                    loadData()
                },
                onFailure = {
                    _uiState.value = AdminUiState.Error(it.message ?: "Failed to delete material.")
                }
            )
        }
    }

    fun bulkUploadExamsJson(jsonString: String) {
        viewModelScope.launch {
            if (jsonString.isBlank()) {
                _uiState.value = AdminUiState.Error("JSON string cannot be empty.")
                return@launch
            }
            _uiState.value = AdminUiState.Loading
            val result = learningRepository.bulkSaveExamsJson(jsonString)
            result.fold(
                onSuccess = {
                    _uiState.value = AdminUiState.Success(it)
                    loadData()
                },
                onFailure = {
                    _uiState.value = AdminUiState.Error(it.message ?: "Bulk upload failed.")
                }
            )
        }
    }

    fun updateUserRole(userId: String, newRole: UserRole) {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            val result = authRepository.updateUserRole(userId, newRole)
            result.fold(
                onSuccess = {
                    _uiState.value = AdminUiState.Success("Updated user role to ${newRole.name}.")
                    loadData()
                },
                onFailure = {
                    _uiState.value = AdminUiState.Error(it.message ?: "Failed to update role.")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = AdminUiState.Idle
    }
}
