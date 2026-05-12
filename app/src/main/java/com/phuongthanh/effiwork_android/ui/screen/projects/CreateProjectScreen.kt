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
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsIntent
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsViewModel

@Preview
@Composable
private fun CreateProjectScreenPreview() {
    CreateProjectScreen(
        viewModel = null,
        onBackClick = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    viewModel: ProjectsViewModel?,
    onBackClick: () -> Unit = {}
) {
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.create_project_title)) },
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
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text(stringResource(R.string.hint_project_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = projectDescription,
                onValueChange = { projectDescription = it },
                label = { Text(stringResource(R.string.hint_project_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel?.handleIntent(
                        ProjectsIntent.CreateProject(
                            name = projectName,
                            description = projectDescription
                        )
                    )
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = projectName.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_create))
            }
        }
    }
}
