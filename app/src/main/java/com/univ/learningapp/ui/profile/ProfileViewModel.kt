package com.univ.learningapp.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univ.learningapp.data.model.User
import com.univ.learningapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileSaveState {
    object Idle : ProfileSaveState()
    object Saving : ProfileSaveState()
    data class Success(val message: String) : ProfileSaveState()
    data class Error(val message: String) : ProfileSaveState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: Flow<User?> = authRepository.currentUserFlow
    val initialUser: User? get() = authRepository.getCurrentUser()

    private val _saveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val saveState: StateFlow<ProfileSaveState> = _saveState.asStateFlow()

    fun saveProfile(
        phoneNumber: String,
        instagramHandle: String,
        twitterHandle: String,
        linkedinHandle: String
    ) {
        viewModelScope.launch {
            _saveState.value = ProfileSaveState.Saving
            val result = authRepository.updateProfile(
                phoneNumber = phoneNumber,
                instagramHandle = instagramHandle,
                twitterHandle = twitterHandle,
                linkedinHandle = linkedinHandle
            )
            result.fold(
                onSuccess = { _saveState.value = ProfileSaveState.Success("Profile updated.") },
                onFailure = { _saveState.value = ProfileSaveState.Error(it.message ?: "Failed to update profile.") }
            )
        }
    }

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _saveState.value = ProfileSaveState.Saving
            val result = authRepository.uploadProfilePhoto(uri)
            result.fold(
                onSuccess = { _saveState.value = ProfileSaveState.Success("Profile photo updated.") },
                onFailure = { _saveState.value = ProfileSaveState.Error(it.message ?: "Failed to upload photo.") }
            )
        }
    }

    fun resetState() {
        _saveState.value = ProfileSaveState.Idle
    }
}
