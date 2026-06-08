package com.phuongthanh.effiwork_android.viewmodel.document

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentPreviewViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val document: DocumentResponse? = null,
        val previewBytes: ByteArray? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val isDownloading: Boolean = false
    )

    sealed class Effect {
        data class ShowError(val message: String) : Effect()
        data class DownloadComplete(val file: File) : Effect()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    fun load(projectId: String, documentId: String) {
        viewModelScope.launch {
            android.util.Log.d("MeetingAttachDebug", "[Preview.load] projectId=$projectId, documentId=$documentId")
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val detailResult = documentRepository.getDocumentDetail(projectId, documentId)) {
                is ApiResult.Success -> {
                    val d = detailResult.data
                    android.util.Log.d("MeetingAttachDebug", "[Preview.load] detail OK: id=${d.id}, fileName=${d.fileName}, fileSize=${d.fileSize}, filePath=${d.filePath}")
                    _uiState.update { it.copy(document = d) }
                    when (val previewResult = documentRepository.previewDocument(projectId, documentId)) {
                        is ApiResult.Success -> {
                            _uiState.update { it.copy(previewBytes = previewResult.data, isLoading = false) }
                        }
                        is ApiResult.Error -> {
                            android.util.Log.d("MeetingAttachDebug", "[Preview.load] preview ERROR: ${previewResult.message}")
                            _uiState.update { it.copy(isLoading = false) }
                        }
                        ApiResult.Loading -> Unit
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.d("MeetingAttachDebug", "[Preview.load] detail ERROR: ${detailResult.message}")
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(Effect.ShowError(detailResult.message))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun download(projectId: String, documentId: String) {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            try {
                val destFile = File(appContext.cacheDir, "downloads/${doc.fileName}")
                destFile.parentFile?.mkdirs()
                when (val result = documentRepository.downloadDocumentToFile(projectId, documentId, destFile)) {
                    is ApiResult.Success -> {
                        _uiState.update { it.copy(isDownloading = false) }
                        _effect.emit(Effect.DownloadComplete(result.data))
                    }
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isDownloading = false) }
                        _effect.emit(Effect.ShowError("Lỗi tải xuống: ${result.message}"))
                    }
                    ApiResult.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDownloading = false) }
                _effect.emit(Effect.ShowError("Lỗi: ${e.message}"))
            }
        }
    }
}
