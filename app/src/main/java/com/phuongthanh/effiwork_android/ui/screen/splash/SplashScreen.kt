package com.phuongthanh.effiwork_android.ui.screen.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import kotlinx.coroutines.delay

@Preview
@Composable
private fun SplashScreenPreview() {
    SplashScreen(
        isLoggedIn = false,
        onNavigate = {}
    )
}

@Composable
fun SplashScreen(
    isLoggedIn: Boolean,
    onNavigate: (Boolean) -> Unit
) {
    LaunchedEffect(isLoggedIn) {
        delay(800)
        onNavigate(isLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blue500),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "EffiWork",
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}