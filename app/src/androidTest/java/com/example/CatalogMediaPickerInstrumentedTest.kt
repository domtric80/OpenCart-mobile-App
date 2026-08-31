package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.Category
import com.example.ui.screens.CatalogScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogMediaPickerInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MediaPickerHostActivity>()

    @Test
    fun newProductOffersCameraAndGallerySources() {
        showNewProductDialog()

        composeRule.onNodeWithTag("product_create_page").assertExists()
        composeRule.onNodeWithTag("product_camera").assertExists()
        composeRule.onNodeWithTag("product_gallery").assertExists()
        composeRule.onNodeWithTag("rich_html_editor").assertExists()
        composeRule.onNodeWithTag("rich_editor_toolbar").assertExists()
    }

    @Test
    fun galleryPickerCanOpenAndReturnWithoutClosingCartAdmin() {
        showNewProductDialog()
        composeRule.onNodeWithTag("product_gallery").performClick()
        Thread.sleep(1_000)
        composeRule.onNodeWithText("Can only use lower 16 bits", substring = true).assertDoesNotExist()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input keyevent KEYCODE_BACK").close()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_gallery").assertExists()
    }

    @Test
    fun cameraCanOpenAndReturnWithoutClosingCartAdmin() {
        showNewProductDialog()
        composeRule.onNodeWithTag("product_camera").performClick()
        Thread.sleep(1_000)
        composeRule.onNodeWithText("Can only use lower 16 bits", substring = true).assertDoesNotExist()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input keyevent KEYCODE_BACK").close()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_camera").assertExists()
    }

    private fun showNewProductDialog() {
        composeRule.setContent {
            MaterialTheme {
                CatalogScreen(
                    products = emptyList(),
                    categories = listOf(Category(id = "1", name = "Generale")),
                    onUpdateStock = { _, _ -> },
                    onSetDirectStock = { _, _ -> },
                    onAddNewProduct = { _, _, _, _, _, _, _, _, _, _, _ -> },
                    onUpdateProduct = { _, _ -> },
                    onDeleteProduct = {},
                    onToggleProductStatus = {},
                    onAddNewCategory = { _, _, _, _ -> },
                    onUpdateCategory = { _, _, _, _, _ -> },
                    onDeleteCategory = {},
                    onToggleCategoryStatus = {}
                )
            }
        }

        composeRule.onNodeWithTag("add_item_top_btn").performClick()
    }
}
