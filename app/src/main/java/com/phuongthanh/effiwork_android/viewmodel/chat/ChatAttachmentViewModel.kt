package com.phuongthanh.effiwork_android.viewmodel.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
class ChatAttachmentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val isUploading: Boolean = false,
        val fileName: String = ""
    )

    sealed class Effect {
        data class UploadComplete(val document: DocumentResponse) : Effect()
        data class ShowError(val message: String) : Effect()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    fun uploadDeviceFile(projectId: String, uri: Uri) {
        viewModelScope.launch {
            val fileName = queryFileName(uri) ?: "unknown"
            _uiState.update { it.copy(isUploading = true, fileName = fileName) }
            try {
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Không đọc được file")
                val mimeType = appContext.contentResolver.getType(uri)

                when (val result = documentRepository.uploadDocument(
                    projectId = projectId,
                    fileName = fileName,
                    fileBytes = bytes,
                    mimeType = mimeType,
                    folderId = null,
                    visibilityType = "PROJECT_SHARED",
                    customFileName = null
                )) {
                    is ApiResult.Success -> {
                        _uiState.update { it.copy(isUploading = false) }
                        _effect.emit(Effect.UploadComplete(result.data))
                    }
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isUploading = false) }
                        _effect.emit(Effect.ShowError("Upload lỗi: ${result.message}"))
                    }
                    ApiResult.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false) }
                _effect.emit(Effect.ShowError(e.message ?: "Lỗi không xác định"))
            }
        }
    }

    private fun queryFileName(uri: Uri): String? {
        val cursor = appContext.contentResolver.query(uri, null, null, null, null) ?: return null
        return cursor.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }
}
