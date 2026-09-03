package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Navigation2InstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun resetFirstAccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("cartadmin_security_vault", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun firstAccessCannotBypassStrongSystemAuthentication() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithTag("first_auth_username_input").performTextInput("CodexQA")
            composeRule.onNodeWithTag("first_auth_password_input").performTextInput("CodexQA9@x")
            composeRule.onNodeWithTag("first_auth_password_confirm_input").performTextInput("CodexQA9@x")
            composeRule.onNodeWithTag("save_first_auth_btn").performClick()

            composeRule.waitForIdle()
            composeRule.onNodeWithTag("first_auth_username_input").assertExists()
            composeRule.onNodeWithTag("tab_home").assertDoesNotExist()
            composeRule.onNodeWithTag("tab_more").assertDoesNotExist()
        }
    }
}
