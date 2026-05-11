package com.phuongthanh.effiwork_android.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.local.AppPreferences
import com.phuongthanh.effiwork_android.data.model.response.ProjectDetailResponse
import com.phuongthanh.effiwork_android.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    sealed class HomeUiState {
        data object Loading : HomeUiState()
        data object NoProject : HomeUiState()
        data class Success(val project: ProjectDetailResponse) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSelectedProject()
    }

    fun loadSelectedProject() {
        val projectId = appPreferences.getSelectedProjectId()
        if (projectId.isNullOrEmpty()) {
            _uiState.value = HomeUiState.NoProject
            return
        }

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            when (val result = projectRepository.getProject(projectId)) {
                is ApiResult.Success -> {
                    _uiState.value = HomeUiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = HomeUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = HomeUiState.Loading
                }
            }
        }
    }
}