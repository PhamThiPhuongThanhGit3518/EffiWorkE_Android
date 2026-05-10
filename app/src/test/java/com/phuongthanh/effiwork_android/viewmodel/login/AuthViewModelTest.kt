package com.phuongthanh.effiwork_android.viewmodel.login

import com.phuongthanh.effiwork_android.data.model.response.UserResponse
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.api.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var authViewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mock(AuthRepository::class.java)
        authViewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_idle() = runTest {
        assertTrue(authViewModel.uiState.value is AuthUiState.Idle)
    }

    @Test
    fun resetState_sets_state_to_idle() = runTest {
        authViewModel.resetState()
        assertTrue(authViewModel.uiState.value is AuthUiState.Idle)
    }
}