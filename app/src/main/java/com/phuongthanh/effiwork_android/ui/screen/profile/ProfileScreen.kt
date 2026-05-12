package com.phuongthanh.effiwork_android.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.response.UserResponse
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.login.AuthViewModel
import com.phuongthanh.effiwork_android.viewmodel.profile.ProfileEffect
import com.phuongthanh.effiwork_android.viewmodel.profile.ProfileUiState
import com.phuongthanh.effiwork_android.viewmodel.profile.ProfileViewModel

@Preview
@Composable
private fun ProfileScreenPreview() {
    ProfileContent(
        user = UserResponse(
            id = "1",
            email = "user@example.com",
            fullName = "Nguyen Van A",
            phone = "0123456789",
            avatarUrl = null,
            status = null,
            createdAt = "2024-01-01",
            updatedAt = "2024-01-01"
        ),
        onSaveClick = { _, _ -> },
        onLogoutClick = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        profileViewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Hồ sơ cá nhân", fontWeight = FontWeight.Bold) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Blue500)
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        user = state.user,
                        onSaveClick = { fullName, phone ->
                            profileViewModel.updateProfile(fullName, phone)
                        },
                        onLogoutClick = { authViewModel.logout() }
                    )
                }
                is ProfileUiState.Error -> {
                    Text(state.message, modifier = Modifier.align(Alignment.Center), color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: com.phuongthanh.effiwork_android.data.model.response.UserResponse,
    onSaveClick: (String, String?) -> Unit,
    onLogoutClick: () -> Unit
) {
    var fullName by remember(user) { mutableStateOf(user.fullName ?: "") }
    var phone by remember(user) { mutableStateOf(user.phone ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = Color.LightGray.copy(alpha = 0.3f)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp),
                            tint = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(user.fullName ?: "Chưa cập nhật", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(user.email, fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        }

        item {
            Text("Thông tin cá nhân", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoField(
                label = "Họ và tên",
                value = fullName,
                onValueChange = { fullName = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoField(
                label = "Email đăng nhập",
                value = user.email,
                onValueChange = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoField(
                label = "Số điện thoại",
                value = phone,
                onValueChange = { phone = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSaveClick(fullName, phone.ifBlank { null }) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue500)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu thay đổi")
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileInfoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = label == "Email đăng nhập",
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Blue500
            )
        )
    }
}