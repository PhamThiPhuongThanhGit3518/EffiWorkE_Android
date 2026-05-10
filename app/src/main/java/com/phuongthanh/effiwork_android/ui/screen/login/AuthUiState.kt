package com.phuongthanh.effiwork_android.ui.screen.login

import com.phuongthanh.effiwork_android.data.model.response.UserResponse

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val user: UserResponse) : AuthUiState
    data class Error(val message: String) : AuthUiState
}