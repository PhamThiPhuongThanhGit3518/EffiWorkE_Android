package com.phuongthanh.effiwork_android.ui.screen.notis

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class NotificationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notificationScreen_displays_title() {
        composeTestRule.setContent {
            NotificationScreen()
        }

        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun notificationScreen_no_crash_on_load() {
        composeTestRule.setContent {
            NotificationScreen()
        }
    }
}