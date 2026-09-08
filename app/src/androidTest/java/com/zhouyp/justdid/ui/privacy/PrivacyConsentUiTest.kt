package com.zhouyp.justdid.ui.privacy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zhouyp.justdid.ui.theme.JustDidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrivacyConsentUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 隐私提示展示政策与明确选择操作() {
        var action = ""
        composeRule.setContent {
            JustDidTheme {
                PrivacyConsentDialog(
                    onViewPolicy = { action = "policy" },
                    onDecline = { action = "decline" },
                    onAccept = { action = "accept" }
                )
            }
        }

        composeRule.onNodeWithText("欢迎使用 JustDid").assertIsDisplayed()
        composeRule.onNodeWithText("《隐私政策》").assertIsDisplayed().performClick()
        assertEquals("policy", action)
        composeRule.onNodeWithText("不同意").assertIsDisplayed().performClick()
        assertEquals("decline", action)
    }

    @Test
    fun 拒绝后仅展示查看政策与重新选择() {
        var action = ""
        composeRule.setContent {
            JustDidTheme {
                DeclinedScreen(
                    onViewPolicy = { action = "policy" },
                    onChooseAgain = { action = "again" }
                )
            }
        }

        composeRule.onNodeWithText("查看隐私政策").assertIsDisplayed()
        composeRule.onNodeWithText("重新选择").assertIsDisplayed().performClick()
        assertEquals("again", action)
        composeRule.onNodeWithText("Just Did").assertDoesNotExist()
    }
}
