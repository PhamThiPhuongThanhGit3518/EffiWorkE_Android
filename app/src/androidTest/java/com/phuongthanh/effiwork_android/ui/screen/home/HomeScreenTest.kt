package com.phuongthanh.effiwork_android.ui.screen.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displays_welcome_message() {
        composeTestRule.setContent {
            HomeScreen()
        }

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun homeScreen_no_crash_on_load() {
        composeTestRule.setContent {
            HomeScreen()
        }
    }
}