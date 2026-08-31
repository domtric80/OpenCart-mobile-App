package com.example.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class FullPageEditorRegressionTest {
    private val catalog = File("src/main/java/com/example/ui/screens/CatalogScreen.kt").readText()
    private val admin = File("src/main/java/com/example/ui/screens/AdminModuleScreen.kt").readText()
    private val activity = File("src/main/java/com/example/MainActivity.kt").readText()
    private val viewModel = File("src/main/java/com/example/ui/MainViewModel.kt").readText()

    @Test
    fun newProductUsesAFullPageWithRichDescription() {
        assertTrue(catalog.contains("if (showAddProductDialog)"))
        assertTrue(catalog.contains("ProductCreatePage("))
        assertTrue(catalog.contains("testTag(\"product_create_page\")"))
        assertTrue(catalog.contains("modifier = Modifier.testTag(\"create_product_description\")"))
        assertTrue(catalog.contains("modifier = Modifier.testTag(\"edit_product_description\")"))
        assertTrue(catalog.contains("LaunchedEffect(categories)"))
        assertTrue(catalog.contains("selectedCategory = categories.firstOrNull()?.name"))
        assertTrue(catalog.contains("testTag(\"product_categories_empty\")"))
        assertTrue(activity.contains("onRefreshCategories = viewModel::refreshCatalogCategories"))
        assertTrue(viewModel.contains("fun refreshCatalogCategories()"))
        assertTrue(viewModel.contains("apiClient.fetchCategories(store.url, store.apiKey"))
    }

    @Test
    fun newArticleUsesAFullPageWithRichContentAndSeoFields() {
        assertTrue(admin.contains("if (showCreateDialog && module == AdminModule.ARTICLES)"))
        assertTrue(admin.contains("ArticleCreatePage("))
        assertTrue(admin.contains("testTag(\"article_create_page\")"))
        assertTrue(admin.contains("modifier = Modifier.testTag(\"create_content_body\")"))
        assertTrue(admin.contains("Meta tag Titolo *"))
        assertTrue(admin.contains("Meta tag Descrizione"))
        assertTrue(admin.contains("Meta tag Parola Chiave"))
    }
}
