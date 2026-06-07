package com.phuongthanh.effiwork_android.ui.screen.document

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentTab
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentUploadViewModel

@Composable
fun DocumentUploadDialog(
    activeTab: DocumentTab,
    selectedFolderId: String?,
    selectedTaskId: String?,
    projectId: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: DocumentUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
            }
            viewModel.onFileSelected(context, it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DocumentUploadViewModel.Effect.UploadSuccess -> onSuccess()
                is DocumentUploadViewModel.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val isTaskTabAndNoTask = activeTab == DocumentTab.PROJECT && selectedTaskId == null

    AlertDialog(
        onDismissRequest = { if (!uiState.isUploading) onDismiss() },
        title = { Text("Tải tài liệu lên") },
        text = {
            Column {
                if (isTaskTabAndNoTask) {
                    Text(
                        "Vui lòng chọn task trước khi tải lên tài liệu dự án",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    if (activeTab == DocumentTab.PROJECT)
                        "File sẽ được gắn vào task đã chọn"
                    else if (selectedFolderId != null)
                        "File sẽ được lưu vào thư mục đã chọn"
                    else "File sẽ được lưu ở gốc tài liệu cá nhân",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { pickFileLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isUploading
                ) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.selectedFileName.ifBlank { "Chọn file" })
                }
                if (uiState.selectedFileSizeBytes > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${uiState.selectedFileSizeBytes / 1024} KB",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val visibility = if (activeTab == DocumentTab.PROJECT) "PROJECT_SHARED"
                    else "PERSONAL"
                    viewModel.upload(
                        projectId = projectId,
                        visibilityType = visibility,
                        folderId = if (activeTab == DocumentTab.PERSONAL) selectedFolderId else null,
                        taskId = if (activeTab == DocumentTab.PROJECT) selectedTaskId else null
                    )
                },
                enabled = uiState.selectedFileUri != null && !uiState.isUploading && !isTaskTabAndNoTask
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Tải lên")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isUploading) {
                Text("Hủy")
            }
        }
    )
}

@Preview(showBackground = true, name = "Document Upload Dialog - Personal")
@Composable
fun DocumentUploadDialogPersonalPreview() {
    androidx.compose.material3.MaterialTheme {
        DocumentUploadDialog(
            activeTab = DocumentTab.PERSONAL,
            selectedFolderId = "f1",
            selectedTaskId = null,
            projectId = "p1",
            onDismiss = {},
            onSuccess = {}
        )
    }
}

@Preview(showBackground = true, name = "Document Upload Dialog - Project (no task)")
@Composable
fun DocumentUploadDialogProjectNoTaskPreview() {
    androidx.compose.material3.MaterialTheme {
        DocumentUploadDialog(
            activeTab = DocumentTab.PROJECT,
            selectedFolderId = null,
            selectedTaskId = null,
            projectId = "p1",
            onDismiss = {},
            onSuccess = {}
        )
    }
}
