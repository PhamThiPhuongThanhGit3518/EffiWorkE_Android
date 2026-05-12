package com.phuongthanh.effiwork_android.ui.screen.projects

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsIntent
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsViewModel
@Composable
fun JoinByCodeScreen(
    viewModel: ProjectsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    JoinByCodeContent(
        onBackClick = onBackClick,
        onJoinClick = { code, note ->
            viewModel.handleIntent(
                ProjectsIntent.JoinByCode(
                    projectCode = code,
                    note = note.ifBlank { null }
                )
            )
            onBackClick()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinByCodeContent(
    onBackClick: () -> Unit,
    onJoinClick: (String, String) -> Unit
) {
    var projectCode by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.join_by_code_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = projectCode,
                onValueChange = { projectCode = it },
                label = { Text(stringResource(R.string.hint_project_code)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.hint_note_optional)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onJoinClick(projectCode, note) },
                modifier = Modifier.fillMaxWidth(),
                enabled = projectCode.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_join))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinByCodePreviewEmpty() {
    MaterialTheme {
        JoinByCodeContent(
            onBackClick = {},
            onJoinClick = { _, _ -> }
        )
    }
}
