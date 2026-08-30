package com.example

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
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
    fun firstAccessExposesFivePrimaryMenusAndAllRequestedSections() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithTag("first_auth_username_input").performTextInput("CodexQA")
            composeRule.onNodeWithTag("first_auth_password_input").performTextInput("CodexQA9@x")
            composeRule.onNodeWithTag("first_auth_password_confirm_input").performTextInput("CodexQA9@x")
            composeRule.onNodeWithTag("save_first_auth_btn").performClick()

            listOf("tab_home", "tab_sales", "tab_catalog", "tab_customers", "tab_more").forEach { tag ->
                composeRule.waitUntil(10_000) {
                    composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.onNodeWithTag(tag).assertExists()
            }
            composeRule.onNodeWithTag("tab_catalog").performClick()
            listOf("products", "categories", "subscription_plans", "pages", "reviews").forEach {
                composeRule.onNodeWithTag("menu_$it").assertExists()
            }
            pressBack()

            composeRule.onNodeWithTag("tab_customers").performClick()
            listOf("customers", "customer_approvals", "gdpr").forEach {
                composeRule.onNodeWithTag("menu_$it").assertExists()
            }
            pressBack()

            composeRule.onNodeWithTag("tab_more").performClick()
            listOf("traffic", "cms", "config").forEach {
                composeRule.onNodeWithTag("menu_$it").assertExists()
            }
            composeRule.onNodeWithText("v2.0.1").assertExists()
            composeRule.onNodeWithTag("menu_cms").performClick()
            listOf("articles", "topics", "comments", "antispam").forEach {
                composeRule.onNodeWithTag("menu_$it").assertExists()
            }
        }
    }
}
