package com.phuongthanh.effiwork_android.ui.screen.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class SignInScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signInScreen_displays_login_elements() {
        composeTestRule.setContent {
            SignInScreen()
        }

        composeTestRule.onNodeWithText("EffiWork").assertIsDisplayed()
        composeTestRule.onNodeWithText("Đăng nhập để tiếp tục").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email hoặc số điện thoại").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mật khẩu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quên mật khẩu?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Đăng nhập").assertIsDisplayed()
        composeTestRule.onNodeWithText("Đăng nhập với Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chưa có tài khoản? ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Đăng ký ngay").assertIsDisplayed()
    }

    @Test
    fun signInScreen_no_crash_on_load() {
        composeTestRule.setContent {
            SignInScreen()
        }
        // Screen loads without crash
    }
}