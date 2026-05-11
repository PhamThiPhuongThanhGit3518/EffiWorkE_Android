package com.phuongthanh.effiwork_android.ui.screen.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.data.model.response.ProjectResponse
import com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsViewModel

@Composable
fun ProjectsDrawerContent(
    viewModel: ProjectsViewModel,
    onProjectClick: (String) -> Unit = {},
    onJoinProjectClick: () -> Unit = {},
    onCreateProjectClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsUiState.Idle) {
            viewModel.handleIntent(com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsIntent.LoadProjects)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.projects_drawer_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = onJoinProjectClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.btn_join_project))
        }

        OutlinedButton(
            onClick = onCreateProjectClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.btn_create_project))
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        when (val state = uiState) {
            is com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsUiState.Success -> {
                LazyColumn {
                    items(state.projects) { project ->
                        ProjectItem(
                            project = project,
                            onClick = { onProjectClick(project.projectId) }
                        )
                    }
                }
            }
            is com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có dự án nào")
                }
            }
        }
    }
}

@Composable
private fun ProjectItem(
    project: ProjectResponse,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Text(
                text = project.name,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = "${project.memberCount} thành viên",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
