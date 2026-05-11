package com.phuongthanh.effiwork_android.ui.screen.projects

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsIntent
import com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinByCodeScreen(
    viewModel: ProjectsViewModel,
    onBackClick: () -> Unit = {}
) {
    var projectCode by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
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
                onClick = {
                    viewModel.handleIntent(
                        ProjectsIntent.JoinByCode(
                            projectCode = projectCode,
                            note = note.ifBlank { null }
                        )
                    )
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = projectCode.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_join))
            }
        }
    }
}
