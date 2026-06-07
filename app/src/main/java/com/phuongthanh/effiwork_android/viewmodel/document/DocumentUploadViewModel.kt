package com.phuongthanh.effiwork_android.viewmodel.document

import android.content.Context
import android.net.Uri
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
import javax.inject.Inject

@HiltViewModel
class DocumentUploadViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val selectedFileUri: Uri? = null,
        val selectedFileName: String = "",
        val selectedFileSizeBytes: Long = 0L,
        val isUploading: Boolean = false,
        val errorMessage: String? = null
    )

    sealed class Effect {
        data class UploadSuccess(val document: DocumentResponse) : Effect()
        data class ShowError(val message: String) : Effect()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    fun onFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val resolver = context.contentResolver
                val fileName = queryFileName(context, uri) ?: "unknown"
                val size = queryFileSize(context, uri)

                _uiState.update {
                    it.copy(
                        selectedFileUri = uri,
                        selectedFileName = fileName,
                        selectedFileSizeBytes = size
                    )
                }
            } catch (e: Exception) {
                _effect.emit(Effect.ShowError("Không đọc được file: ${e.message}"))
            }
        }
    }

    fun clearSelection() {
        _uiState.update { UiState() }
    }

    fun upload(
        projectId: String,
        visibilityType: String,
        folderId: String? = null,
        taskId: String? = null
    ) {
        val current = _uiState.value
        val uri = current.selectedFileUri ?: return
        val fileName = current.selectedFileName

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, errorMessage = null) }
            try {
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Không đọc được file")

                val mimeType = appContext.contentResolver.getType(uri)

                val uploadResult = documentRepository.uploadDocument(
                    projectId = projectId,
                    fileName = fileName,
                    fileBytes = bytes,
                    mimeType = mimeType,
                    folderId = folderId,
                    visibilityType = visibilityType,
                    customFileName = null
                )

                when (uploadResult) {
                    is ApiResult.Success -> {
                        val document = uploadResult.data
                        if (taskId != null) {
                            when (val attachResult = documentRepository.attachToTask(
                                projectId = projectId,
                                taskId = taskId,
                                documentId = document.id
                            )) {
                                is ApiResult.Success -> {
                                    _uiState.update { it.copy(isUploading = false) }
                                    _effect.emit(Effect.UploadSuccess(document))
                                }
                                is ApiResult.Error -> {
                                    _uiState.update { it.copy(isUploading = false) }
                                    _effect.emit(Effect.ShowError("Upload xong nhưng attach lỗi: ${attachResult.message}"))
                                }
                                ApiResult.Loading -> Unit
                            }
                        } else {
                            _uiState.update { it.copy(isUploading = false) }
                            _effect.emit(Effect.UploadSuccess(document))
                        }
                    }
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isUploading = false) }
                        _effect.emit(Effect.ShowError("Upload lỗi: ${uploadResult.message}"))
                    }
                    ApiResult.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false) }
                _effect.emit(Effect.ShowError(e.message ?: "Lỗi không xác định"))
            }
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        return cursor.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }

    private fun queryFileSize(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return 0L
        return cursor.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
            } else 0L
        }
    }
}
