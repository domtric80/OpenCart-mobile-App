package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.AdminModule
import com.example.model.AdminModuleSnapshot
import com.example.model.AdminRecord
import com.example.ui.screens.AdminModuleScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminModuleScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sensitiveActionRequiresConfirmationAndEmitsQueueRequest() {
        var submitted: Pair<String, String>? = null
        val module = AdminModule.CUSTOMER_APPROVALS
        val record = AdminRecord(
            id = "17",
            title = "Cliente di prova",
            subtitle = "test@example.invalid",
            statusLabel = "Cliente",
            actionable = true
        )

        composeRule.setContent {
            MaterialTheme {
                AdminModuleScreen(
                    module = module,
                    snapshot = AdminModuleSnapshot(module = module, records = listOf(record)),
                    onRefresh = {},
                    onSensitiveAction = { id, operation -> submitted = id to operation }
                )
            }
        }

        composeRule.onNodeWithTag("approve_customer_approvals_17").assertExists().performClick()
        composeRule.onNodeWithText("Invia al pannello").assertExists().performClick()

        composeRule.runOnIdle { assertEquals("17" to "approve", submitted) }
    }

    @Test
    fun pendingSensitiveActionCannotBeSubmittedTwice() {
        val module = AdminModule.GDPR
        val record = AdminRecord(
            id = "9",
            title = "privacy@example.invalid",
            statusLabel = "In attesa",
            actionable = true,
            pendingCommandId = "3",
            pendingOperation = "deny"
        )

        composeRule.setContent {
            MaterialTheme {
                AdminModuleScreen(
                    module = module,
                    snapshot = AdminModuleSnapshot(module = module, records = listOf(record)),
                    onRefresh = {}
                )
            }
        }

        composeRule.onNodeWithText("In attesa nel pannello: rifiuto").assertExists()
        composeRule.onNodeWithTag("approve_gdpr_9").assertDoesNotExist()
        composeRule.onNodeWithTag("deny_gdpr_9").assertDoesNotExist()
    }
}
