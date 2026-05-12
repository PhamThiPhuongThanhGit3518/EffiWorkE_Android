package com.phuongthanh.effiwork_android.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.response.UserResponse
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: UserResponse) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class ProfileEffect {
    data class ShowToast(val message: String) : ProfileEffect()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            when (val result = authRepository.getCurrentUser()) {
                is ApiResult.Success -> _uiState.value = ProfileUiState.Success(result.data)
                is ApiResult.Error -> _uiState.value = ProfileUiState.Error(result.message)
                else -> {}
            }
        }
    }

    fun updateProfile(fullName: String, phone: String?) {
        viewModelScope.launch {
            when (val result = authRepository.updateProfile(fullName, phone, null)) {
                is ApiResult.Success -> {
                    _effect.emit(ProfileEffect.ShowToast("Cập nhật thành công"))
                    _uiState.value = ProfileUiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _effect.emit(ProfileEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }
}