package com.phuongthanh.effiwork_android.viewmodel.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatImagePreviewViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    data class UiState(
        val bytesByDocumentId: Map<String, ByteArray> = emptyMap()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val loadingDocumentIds = mutableSetOf<String>()

    fun loadPreview(projectId: String, documentId: String) {
        if (documentId in _uiState.value.bytesByDocumentId) return
        if (!loadingDocumentIds.add(documentId)) return
        viewModelScope.launch {
            when (val result = documentRepository.previewDocument(projectId, documentId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(bytesByDocumentId = it.bytesByDocumentId + (documentId to result.data))
                    }
                }
                is ApiResult.Error -> Unit
                ApiResult.Loading -> Unit
            }
            loadingDocumentIds.remove(documentId)
        }
    }
}
