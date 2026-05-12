package com.phuongthanh.effiwork_android.ui.screen.projects

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.data.model.response.ProjectResponse
import com.phuongthanh.effiwork_android.ui.common.ProjectCard
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsIntent
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsUiState
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsViewModel

@Composable
fun ProjectsDrawerContent(
    viewModel: ProjectsViewModel = hiltViewModel(),
    onProjectClick: (String) -> Unit = {},
    onJoinProjectClick: () -> Unit = {},
    onCreateProjectClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    // Logic gọi API khi lần đầu vào Drawer
    LaunchedEffect(uiState) {
        if (uiState is ProjectsUiState.Idle) {
            viewModel.handleIntent(ProjectsIntent.LoadProjects)
        }
    }

    // Gọi hàm Internal bên dưới để vẽ giao diện
    ProjectsDrawerContentInternal(
        uiState = uiState,
        selectedProjectId = selectedProjectId,
        onProjectSelect = { projectId ->
            viewModel.selectProject(projectId)
            onProjectClick(projectId)
        },
        onJoinProjectClick = onJoinProjectClick,
        onCreateProjectClick = onCreateProjectClick
    )
}

@Composable
private fun ProjectsDrawerContentInternal(
    uiState: ProjectsUiState,
    selectedProjectId: String?,
    onProjectSelect: (String) -> Unit,
    onJoinProjectClick: () -> Unit,
    onCreateProjectClick: () -> Unit
) {
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.btn_join_project))
        }

        OutlinedButton(
            onClick = onCreateProjectClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.btn_create_project))
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        when (uiState) {
            is ProjectsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProjectsUiState.Success -> {
                LazyColumn {
                    items(uiState.projects) { project ->
                        ProjectCard(
                            project = project,
                            isSelected = project.projectId == selectedProjectId,
                            onClick = { onProjectSelect(project.projectId) }
                        )
                    }
                }
            }
            is ProjectsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có dự án nào")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Drawer - Trạng thái Thành công")
@Composable
private fun ProjectsDrawerPreviewSuccess() {
    val fakeProjects = listOf(
        ProjectResponse(projectId = "1", name = "Dự án EffiWork", projectCode = "PRJ001"),
        ProjectResponse(projectId = "2", name = "NCKH - AI Camera", projectCode = "PRJ002"),
        ProjectResponse(projectId = "3", name = "Đồ án Tốt nghiệp", projectCode = "PRJ003")
    )

    MaterialTheme {
        ModalDrawerSheet {
            ProjectsDrawerContentInternal(
                uiState = ProjectsUiState.Success(fakeProjects),
                selectedProjectId = "1",
                onProjectSelect = {},
                onJoinProjectClick = {},
                onCreateProjectClick = {}
            )
        }
    }
}
