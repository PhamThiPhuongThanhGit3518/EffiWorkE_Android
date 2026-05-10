package com.phuongthanh.effiwork_android.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuongthanh.effiwork_android.ui.common.CustomTextField
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.ui.theme.LightBackground
import com.phuongthanh.effiwork_android.viewmodel.login.AuthViewModel

@Composable
fun SignInScreen(
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "EffiWork",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Blue500
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Đăng nhập để tiếp tục",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )

        Spacer(modifier = Modifier.height(40.dp))

        CustomTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = "Email hoặc số điện thoại",
            placeholder = "you@company.com hoặc 0123456789",
            isEmail = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = password,
            onValueChange = { password = it },
            label = "Mật khẩu",
            placeholder = "Nhập mật khẩu",
            isPassword = true,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "Quên mật khẩu?",
                color = Blue500,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onForgotPasswordClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(identifier, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue500),
            enabled = uiState !is AuthUiState.Loading
        ) {
            when (uiState) {
                is AuthUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
                else -> Text(
                    text = "Đăng nhập",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (uiState as AuthUiState.Error).message,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
            Text(
                text = "  Hoặc  ",
                color = Color(0xFF666666),
                fontSize = 14.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { /* TODO: Handle Google sign in */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A1A1A))
        ) {
            Text(
                text = "Đăng nhập với Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chưa có tài khoản? ",
                color = Color(0xFF666666),
                fontSize = 14.sp
            )
            Text(
                text = "Đăng ký ngay",
                color = Blue500,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }
    }
}

@Preview
@Composable
fun SignInScreenPreview() {
    SignInScreen()
}