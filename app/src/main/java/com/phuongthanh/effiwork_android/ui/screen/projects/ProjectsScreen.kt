package com.phuongthanh.effiwork_android.ui.screen.projects

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectDetailUiState
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectDetailViewModel
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsIntent
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsViewModel
import kotlinx.coroutines.launch

data class ProjectDashboardState(
    val projectId: String = "",
    val projectName: String = "",
    val projectCode: String = "",
    val memberCount: Int = 0,
    val taskCount: Int = 0,
    val meetings: Int = 0,
    val documents: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projectsViewModel: ProjectsViewModel,
    detailViewModel: ProjectDetailViewModel = hiltViewModel(),
    onNavigateToJoinByCode: () -> Unit = {},
    onNavigateToCreateProject: () -> Unit = {},
    onNavigateToSettings: (String) -> Unit = {},
    onNavigateToTask: (String, String) -> Unit = { _, _ -> },
    onNavigateToMeeting: (String) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        projectsViewModel.handleIntent(ProjectsIntent.LoadProjects)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ProjectsDrawerContent(
                    viewModel = projectsViewModel,
                    onProjectClick = { projectId ->
                        scope.launch {
                            drawerState.close()
                            detailViewModel.loadProject(projectId)
                        }
                    },
                    onJoinProjectClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToJoinByCode()
                    },
                    onCreateProjectClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToCreateProject()
                    }
                )
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        title = {
                            val title = when (val state = detailUiState) {
                                is ProjectDetailUiState.Success -> state.project.name
                                else -> stringResource(R.string.nav_projects)
                            }
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (val state = detailUiState) {
                        is ProjectDetailUiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Blue500)
                        }
                        is ProjectDetailUiState.Success -> {
                            ProjectDashboardContent(
                                state = ProjectDashboardState(
                                    projectId = state.project.id,
                                    projectName = state.project.name,
                                    projectCode = state.project.projectCode,
                                    memberCount = state.project.memberCount,
                                    taskCount = state.project.taskCount,
                                    meetings = state.project.meetingsCount,
                                    documents = state.project.documentsCount
                                ),
                                onSettingsClick = { onNavigateToSettings(state.project.id) },
                                onNavigateToTask = onNavigateToTask,
                                onNavigateToMeeting = onNavigateToMeeting
                            )
                        }
                        is ProjectDetailUiState.NoProject -> {
                            Text(
                                text = stringResource(R.string.projects_swipe_hint),
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        is ProjectDetailUiState.Error -> {
                            Text(state.message, modifier = Modifier.align(Alignment.Center), color = Color.Red)
                        }
                        else -> {}
                    }
                }
            }
        }
    )
}

@Composable
fun ProjectDashboardContent(
    state: ProjectDashboardState,
    onSettingsClick: () -> Unit = {},
    onNavigateToTask: (String, String) -> Unit = { _, _ -> },
    onNavigateToMeeting: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProjectHeaderCard(state.projectName, state.projectCode)
        Spacer(modifier = Modifier.height(16.dp))
        StatsBar(state.taskCount, state.meetings, state.documents, state.memberCount)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Chức năng chính",
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeaturesGrid(
            onSettingsClick = onSettingsClick,
            onNavigateToTask = onNavigateToTask,
            onNavigateToMeeting = onNavigateToMeeting,
            projectId = state.projectId,
            projectName = state.projectName
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ProjectHeaderCard(projectName: String, projectCode: String) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(projectName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = projectCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Project Code", projectCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã sao chép mã dự án", Toast.LENGTH_SHORT).show()
                        }
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Text(" Đang hoạt động", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
private fun StatsBar(taskCount: Int, meetings: Int, documents: Int, memberCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(taskCount.toString(), "Công việc")
        StatDivider()
        StatItem(meetings.toString(), "Cuộc họp")
        StatDivider()
        StatItem(documents.toString(), "Tài liệu")
        StatDivider()
        StatItem(memberCount.toString(), "Thành viên")
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun StatDivider() {
    Box(modifier = Modifier.height(30.dp).width(1.dp).background(Color.LightGray))
}

@Composable
private fun FeaturesGrid(
    onSettingsClick: () -> Unit = {},
    onNavigateToTask: (String, String) -> Unit = { _, _ -> },
    onNavigateToMeeting: (String) -> Unit = {},
    projectId: String = "",
    projectName: String = ""
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val features = listOf(
            Triple("Công việc", Icons.Default.CheckCircle, Color(0xFF2196F3)),
            Triple("Cuộc họp", Icons.Default.DateRange, Color(0xFF9C27B0)),
//            Triple("Tin nhắn", Icons.Default.Email, Color(0xFF03A9F4)),
            Triple("Cài đặt", Icons.Default.Settings, Color(0xFF607D8B))
        )

        features.chunked(3).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (label, icon, color) ->
                    val onClick: () -> Unit =
                        when (label) {
                            "Cài đặt" -> onSettingsClick
                            "Công việc" -> { { onNavigateToTask(projectId, projectName) } }
                            "Cuộc họp" -> { { onNavigateToMeeting(projectId) } }
                            else -> { {} }
                        }
                    FeatureCard(icon, label, color, Modifier.weight(1f), onClick = onClick)
                }
                if (rowItems.size < 3) Spacer(Modifier.weight(3f - rowItems.size))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
                Icon(icon, label, tint = color, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Giao diện Dashboard Dự án")
@Composable
fun ProjectsScreenPreview() {
    MaterialTheme {
        ProjectDashboardContent(
            state = ProjectDashboardState(
                projectId = "proj123",
                projectName = "Dự án NCKH",
                projectCode = "PRJ-D22DD5FB",
                memberCount = 5,
                taskCount = 4,
                meetings = 7,
                documents = 14
            ),
            onSettingsClick = {},
            onNavigateToTask = { _, _ -> },
            onNavigateToMeeting = {}
        )
    }
}

@Preview(showBackground = true, name = "Xem riêng Header Card")
@Composable
fun HeaderCardPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ProjectHeaderCard(
                projectName = "Đồ án tốt nghiệp",
                projectCode = "PRJ-E3987FNT"
            )
        }
    }
}