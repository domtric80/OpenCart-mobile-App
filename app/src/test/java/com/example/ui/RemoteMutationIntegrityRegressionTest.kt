package com.example.ui

import com.example.model.VisitorRealtimeStats
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteMutationIntegrityRegressionTest {
    private val viewModelSource = File("src/main/java/com/example/ui/MainViewModel.kt").readText()

    @Test
    fun visitorTelemetryDefaultsNeverPretendToContainLiveData() {
        val stats = VisitorRealtimeStats()

        assertFalse(stats.trackingEnabled)
        assertFalse(stats.dataAvailable)
        assertEquals(0, stats.activeVisitorsNow)
        assertEquals(0, stats.pageViewsPerMin)
        assertTrue(stats.liveEvents.isEmpty())
    }

    @Test
    fun productUpdateWaitsForRemoteConfirmationBeforeChangingLocalState() {
        val function = viewModelSource.substringAfter("fun updateProduct(product: Product)")
            .substringBefore("fun deleteProduct")

        val remoteCall = function.indexOf("apiClient.updateProduct")
        val localUpdate = function.indexOf("repository.updateProduct")
        assertTrue(remoteCall >= 0)
        assertTrue(localUpdate > remoteCall)
        assertTrue(function.contains("if (result.getOrDefault(false))"))
    }

    @Test
    fun catalogMutationsWaitForRemoteConfirmationBeforeChangingLocalState() {
        assertRemoteBeforeLocal("fun addNewProduct(", "fun updateProduct(", "apiClient.createProduct", "repository.insertProduct")
        assertRemoteBeforeLocal("fun deleteProduct(", "fun toggleProductStatus(", "apiClient.deleteProduct", "repository.deleteProduct")
        assertRemoteBeforeLocal("fun addNewCategory(", "fun updateCategory(", "apiClient.createCategory", "repository.insertCategory")
        assertRemoteBeforeLocal("fun updateCategory(", "fun deleteCategory(", "apiClient.updateCategory", "repository.updateCategory")
        assertRemoteBeforeLocal("fun deleteCategory(", "fun toggleCategoryStatus(", "apiClient.deleteCategory", "repository.deleteCategory")
    }

    private fun assertRemoteBeforeLocal(
        functionStart: String,
        functionEnd: String,
        remoteCallName: String,
        localCallName: String
    ) {
        val function = viewModelSource.substringAfter(functionStart).substringBefore(functionEnd)
        val remoteCall = function.indexOf(remoteCallName)
        val localCall = function.indexOf(localCallName)
        assertTrue("Remote call missing for $functionStart", remoteCall >= 0)
        assertTrue("Local mutation must follow remote confirmation for $functionStart", localCall > remoteCall)
        assertTrue("Success must be checked for $functionStart", function.contains("result.getOr"))
    }
}
