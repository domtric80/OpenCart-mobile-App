package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

    @Test
    fun editablePageRequiresExplicitSave() {
        var submitted: AdminRecord? = null
        val module = AdminModule.PAGES
        val record = AdminRecord(
            id = "4",
            title = "Termini e condizioni",
            statusLabel = "Attivo",
            active = true,
            sortOrder = 2,
            editable = true
        )

        composeRule.setContent {
            MaterialTheme {
                AdminModuleScreen(
                    module = module,
                    snapshot = AdminModuleSnapshot(module = module, records = listOf(record)),
                    onRefresh = {},
                    onContentUpdate = { submitted = it }
                )
            }
        }

        composeRule.onNodeWithTag("edit_pages_4").assertExists().performClick()
        composeRule.onNodeWithTag("content_title").assertExists()
        composeRule.onNodeWithTag("content_sort_order").assertExists()
        composeRule.onNodeWithTag("save_content_edit").assertExists().performClick()

        composeRule.runOnIdle { assertEquals(record, submitted) }
    }

    @Test
    fun newTopicCollectsRealCmsFieldsBeforeSubmitting() {
        var submitted: AdminRecord? = null
        val module = AdminModule.TOPICS
        composeRule.setContent {
            MaterialTheme {
                AdminModuleScreen(
                    module = module,
                    snapshot = AdminModuleSnapshot(module = module),
                    onRefresh = {},
                    onContentCreate = { submitted = it }
                )
            }
        }

        composeRule.onNodeWithTag("create_topics").performClick()
        composeRule.onNodeWithTag("create_content_title").performTextInput("Novità")
        composeRule.onNodeWithTag("create_content_body").performTextInput("Notizie dal negozio")
        composeRule.onNodeWithTag("save_content_create").performClick()

        composeRule.runOnIdle {
            assertEquals("Novità", submitted?.title)
            assertEquals("Notizie dal negozio", submitted?.content)
            assertEquals(0, submitted?.sortOrder)
        }
    }

    @Test
    fun newArticleRequiresAndReturnsAnExistingTopic() {
        var submitted: AdminRecord? = null
        val module = AdminModule.ARTICLES
        val topic = AdminRecord(id = "12", title = "Guide", active = true)
        composeRule.setContent {
            MaterialTheme {
                AdminModuleScreen(
                    module = module,
                    snapshot = AdminModuleSnapshot(module = module),
                    onRefresh = {},
                    onContentCreate = { submitted = it },
                    availableTopics = listOf(topic)
                )
            }
        }

        composeRule.onNodeWithTag("create_articles").performClick()
        composeRule.onNodeWithTag("create_content_title").performTextInput("Guida acquisto")
        composeRule.onNodeWithTag("create_article_author").performTextInput("Redazione")
        composeRule.onNodeWithTag("create_content_body").performTextInput("Contenuto verificato")
        composeRule.onNodeWithTag("save_content_create").performClick()

        composeRule.runOnIdle { assertEquals(12, submitted?.parentId) }
    }
}
