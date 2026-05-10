package com.phuongthanh.effiwork_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.ui.navigation.AuthNavigation
import com.phuongthanh.effiwork_android.ui.screen.main.MainScreen
import com.phuongthanh.effiwork_android.ui.theme.EffiWork_AndroidTheme
import com.phuongthanh.effiwork_android.viewmodel.login.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EffiWork_AndroidTheme {
                EffiWorkApp()
            }
        }
    }
}

@Composable
fun EffiWorkApp(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    if (isLoggedIn) {
        MainScreen()
    } else {
        AuthNavigation(
            onLoginSuccess = {
                // State will auto-update due to StateFlow
            }
        )
    }
}
