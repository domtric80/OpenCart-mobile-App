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
    fun unsupportedCatalogMutationsDoNotChangeLocalRepository() {
        assertFalse(viewModelSource.contains("repository.addProduct("))
        assertFalse(viewModelSource.contains("repository.deleteProduct("))
        assertFalse(viewModelSource.contains("repository.addCategory("))
        assertFalse(viewModelSource.contains("repository.deleteCategory("))
    }
}
