package com.phuongthanh.effiwork_android.ui.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class CustomTextFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun customTextField_displays_label_and_placeholder() {
        composeTestRule.setContent {
            CustomTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                placeholder = "Enter your email"
            )
        }

        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter your email").assertIsDisplayed()
    }

    @Test
    fun customTextField_displays_entered_value() {
        composeTestRule.setContent {
            CustomTextField(
                value = "test@example.com",
                onValueChange = {},
                label = "Email",
                placeholder = "Enter your email"
            )
        }

        composeTestRule.onNodeWithText("test@example.com").assertIsDisplayed()
    }

    @Test
    fun customTextField_no_crash_on_load() {
        composeTestRule.setContent {
            CustomTextField(
                value = "",
                onValueChange = {},
                label = "Test Label",
                placeholder = "Test Placeholder"
            )
        }
    }
}