package com.phuongthanh.effiwork_android.ui.screen.document

import android.content.Intent
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentPreviewViewModel
import java.io.File

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewScreen(
    projectId: String,
    documentId: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    viewModel: DocumentPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(projectId, documentId) {
        viewModel.load(projectId, documentId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DocumentPreviewViewModel.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
                is DocumentPreviewViewModel.Effect.DownloadComplete -> {
                    openDownloadedFile(context, effect.file)
                }
            }
        }
    }

    DocumentPreviewContent(
        uiState = uiState,
        projectId = projectId,
        documentId = documentId,
        onBackClick = onBackClick,
        onDownload = { viewModel.download(projectId, documentId) }
    )
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewContent(
    uiState: DocumentPreviewViewModel.UiState,
    projectId: String,
    documentId: String,
    onBackClick: () -> Unit,
    onDownload: () -> Unit
) {
    val doc = uiState.document
    val bytes = uiState.previewBytes
    val mimeType = doc?.mimeType ?: ""
    val isImage = mimeType.startsWith("image/")
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(doc?.fileName ?: "Tài liệu", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onDownload,
                        enabled = !uiState.isDownloading
                    ) {
                        if (uiState.isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, "Tải xuống")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                bytes == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Description,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Không thể xem trước file này", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Định dạng: ${mimeType.ifBlank { "không xác định" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onDownload) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tải xuống để mở")
                        }
                    }
                }
                isImage -> {
                    val cacheFile = remember(bytes) {
                        File(context.cacheDir, "preview/${doc?.id ?: "img"}.${mimeType.substringAfter("/")}")
                            .apply {
                                parentFile?.mkdirs()
                                writeBytes(bytes)
                            }
                    }
                    AsyncImage(
                        model = cacheFile,
                        contentDescription = doc?.fileName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                mimeType == "application/pdf" -> {
                    val cacheFile = remember(bytes) {
                        File(context.cacheDir, "preview/${doc?.id ?: "pdf"}.pdf").apply {
                            parentFile?.mkdirs()
                            writeBytes(bytes)
                        }
                    }
                    PdfPagesView(file = cacheFile, modifier = Modifier.fillMaxSize())
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(doc?.fileName ?: "Tài liệu", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(16.dp))
                        Text("Mở bằng app khác", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun PdfPagesView(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmaps = remember(file) {
        renderPdfPages(context, file)
    }
    if (bitmaps.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Không đọc được PDF")
        }
    } else {
        LazyColumn(modifier = modifier.padding(8.dp)) {
            items(bitmaps) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun renderPdfPages(context: android.content.Context, file: File): List<Bitmap> {
    return try {
        val bitmaps = mutableListOf<Bitmap>()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = android.graphics.pdf.PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        for (i in 0 until pageCount) {
            renderer.openPage(i).use { page ->
                val scale = 2
                val bitmap = Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
            }
        }
        renderer.close()
        pfd.close()
        bitmaps
    } catch (e: Exception) {
        emptyList()
    }
}

private fun openDownloadedFile(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Mở file"))
    } catch (e: Exception) {
        Toast.makeText(context, "Đã lưu vào: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Document Preview - No Preview Available")
@Composable
fun DocumentPreviewScreenNoPreviewPreview() {
    MaterialTheme {
        DocumentPreviewContent(
            uiState = DocumentPreviewViewModel.UiState(
                document = DocumentResponse(
                    id = "d1", fileName = "unknown-format.xyz",
                    filePath = null, mimeType = "application/x-unknown", fileSize = 12_000,
                    createdAt = null, projectId = null, folderId = null,
                    visibilityType = null, updatedAt = null,
                    uploadedBy = null, owner = null, count = null
                ),
                previewBytes = null,
                isLoading = false,
                errorMessage = null,
                isDownloading = false
            ),
            projectId = "p1",
            documentId = "d1",
            onBackClick = {},
            onDownload = {}
        )
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Document Preview - Loading")
@Composable
fun DocumentPreviewScreenLoadingPreview() {
    MaterialTheme {
        DocumentPreviewContent(
            uiState = DocumentPreviewViewModel.UiState(isLoading = true),
            projectId = "p1",
            documentId = "d1",
            onBackClick = {},
            onDownload = {}
        )
    }
}
