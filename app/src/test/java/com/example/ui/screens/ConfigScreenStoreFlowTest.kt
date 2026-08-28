package com.example.ui.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import com.example.model.Store
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigScreenStoreFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstStoreFormCreatesStoreInsteadOfUpdatingMissingProfile() {
        var added = false
        var updated = false
        var addedUrl = ""

        composeRule.setContent {
            MyApplicationTheme {
                ConfigScreen(
                    currentStore = null,
                    isTestingConnection = false,
                    connectionResult = null,
                    onTestConnection = { _, _, _ -> },
                    onSaveStoreCredentials = { _, _, _, _, _, _ -> updated = true },
                    onAddStore = { _, url, _, _, _ ->
                        added = true
                        addedUrl = url
                    },
                    onTriggerSync = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("config_screen_list").performScrollToIndex(4)
        composeRule.onNodeWithTag("first_store_help")
        composeRule.onNodeWithTag("config_store_url_input")
            .performScrollTo()
            .performTextInput("https://shop.example")
        composeRule.onNodeWithTag("config_screen_list").performScrollToIndex(4)
        composeRule.onNodeWithTag("config_api_key_input")
            .performScrollTo()
            .performTextInput("ca_test_token")
        composeRule.onNodeWithTag("save_opencart_credentials_btn")
            .performScrollTo()
            .performClick()

        assertTrue(added)
        assertFalse(updated)
        assertEquals("https://shop.example", addedUrl)
    }

    @Test
    fun savedTokenIsNotRenderedButRemainsAvailableForApiTest() {
        var testedKey = ""
        val store = Store(
            id = "store_1",
            name = "Shop",
            url = "https://shop.example",
            version = "OpenCart 4.1.x",
            apiUsername = "operator",
            apiKey = "ca_saved_secret"
        )

        composeRule.setContent {
            MyApplicationTheme {
                ConfigScreen(
                    currentStore = store,
                    isTestingConnection = false,
                    connectionResult = null,
                    onTestConnection = { _, _, key -> testedKey = key },
                    onSaveStoreCredentials = { _, _, _, _, _, _ -> },
                    onAddStore = { _, _, _, _, _ -> },
                    onTriggerSync = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("config_screen_list").performScrollToIndex(4)
        composeRule.onNodeWithTag("config_api_key_input")
            .performScrollTo()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString("")
                )
            )
        composeRule.onNodeWithTag("test_opencart_connection_btn")
            .performScrollTo()
            .performClick()

        assertEquals("ca_saved_secret", testedKey)
    }
}
