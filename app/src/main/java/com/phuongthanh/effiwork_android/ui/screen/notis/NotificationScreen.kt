package com.phuongthanh.effiwork_android.ui.screen.notis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.phuongthanh.effiwork_android.R

@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(R.string.notifications_title))
    }
}

@Preview
@Composable
fun NotificationScreenPreview() {
    NotificationScreen()
}