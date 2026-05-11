package com.phuongthanh.effiwork_android.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.ui.screen.login.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(authRepository.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.login(identifier, password)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.Success(result.data.user)
                    _isLoggedIn.value = true
                }
                is ApiResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun register(fullName: String, email: String, phone: String?, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.register(fullName, email, phone, password)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.Success(result.data.user)
                    _isLoggedIn.value = true
                }
                is ApiResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            android.util.Log.d("LogoutDebug", "logout() called")
            authRepository.logout()
            android.util.Log.d("LogoutDebug", "after authRepository.logout(), isLoggedIn = ${authRepository.isLoggedIn()}")
            _uiState.value = AuthUiState.Idle
            _isLoggedIn.value = false
            android.util.Log.d("LogoutDebug", "_isLoggedIn set to false")
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}