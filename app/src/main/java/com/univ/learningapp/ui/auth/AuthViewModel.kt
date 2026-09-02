package com.univ.learningapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univ.learningapp.data.model.User
import com.univ.learningapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class ResetSent(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email, pass)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success(it) },
                onFailure = { AuthUiState.Error(it.message ?: "Login failed. Please try again.") }
            )
        }
    }

    fun signup(name: String, email: String, pass: String, confirmPass: String) {
        viewModelScope.launch {
            if (pass != confirmPass) {
                _uiState.value = AuthUiState.Error("Passwords do not match.")
                return@launch
            }
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signup(name, email, pass)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success(it) },
                onFailure = { AuthUiState.Error(it.message ?: "Signup failed. Please try again.") }
            )
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.resetPassword(email)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.ResetSent(it) },
                onFailure = { AuthUiState.Error(it.message ?: "Password reset failed.") }
            )
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
