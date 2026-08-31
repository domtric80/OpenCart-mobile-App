package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.Category
import com.example.ui.screens.CatalogScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogMediaPickerInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun newProductOffersCameraAndGallerySources() {
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
        composeRule.onNodeWithTag("product_camera").assertExists()
        composeRule.onNodeWithTag("product_gallery").assertExists()
    }
}
