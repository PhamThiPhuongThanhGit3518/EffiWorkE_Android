package com.phuongthanh.effiwork_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.socket.ChatSocketManager
import com.phuongthanh.effiwork_android.ui.navigation.AuthNavigation
import com.phuongthanh.effiwork_android.ui.navigation.MainScreen
import com.phuongthanh.effiwork_android.ui.screen.splash.SplashScreen
import com.phuongthanh.effiwork_android.ui.theme.EffiWork_AndroidTheme
import com.phuongthanh.effiwork_android.viewmodel.login.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(
            isLoggedIn = isLoggedIn,
            onNavigate = { showSplash = false }
        )
    } else {
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
}