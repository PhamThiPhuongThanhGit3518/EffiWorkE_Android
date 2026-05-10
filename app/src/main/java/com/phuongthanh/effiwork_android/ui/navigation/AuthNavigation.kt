package com.phuongthanh.effiwork_android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.phuongthanh.effiwork_android.ui.screen.login.SignInScreen
import com.phuongthanh.effiwork_android.ui.screen.login.SignUpScreen

@Composable
fun AuthNavigation(
    onLoginSuccess: () -> Unit
) {
    var showSignUp by remember { mutableStateOf(false) }

    if (showSignUp) {
        SignUpScreen(
            onSignInClick = { showSignUp = false },
            onRegisterSuccess = {
                showSignUp = false
                onLoginSuccess()
            }
        )
    } else {
        SignInScreen(
            onSignUpClick = { showSignUp = true },
            onLoginSuccess = onLoginSuccess
        )
    }
}
