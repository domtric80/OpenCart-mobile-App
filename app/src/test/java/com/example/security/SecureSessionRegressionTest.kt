package com.example.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureSessionRegressionTest {
    private val activitySource = File("src/main/java/com/example/MainActivity.kt").readText()
    private val viewModelSource = File("src/main/java/com/example/ui/MainViewModel.kt").readText()
    private val manifestSource = File("src/main/AndroidManifest.xml").readText()
    private val configSource = File("src/main/java/com/example/ui/screens/ConfigScreen.kt").readText()
    private val messagingSource = File(
        "src/main/java/com/example/notification/CartAdminFirebaseMessagingService.kt"
    ).readText()
    private val notificationSource = File(
        "src/main/java/com/example/notification/NotificationHelper.kt"
    ).readText()
    private val apiClientSource = File(
        "src/main/java/com/example/network/OpenCartApiClient.kt"
    ).readText()
    private val databaseSource = File(
        "src/main/java/com/example/data/local/AppDatabase.kt"
    ).readText()

    @Test
    fun sensitiveViewModelExistsOnlyInsideAnUnlockedSession() {
        val lockedBranch = activitySource.substring(
            activitySource.indexOf("if (!isUnlocked)"),
            activitySource.indexOf("fun MainAppContent")
        )

        assertTrue(activitySource.contains("WindowManager.LayoutParams.FLAG_SECURE"))
        assertTrue(activitySource.contains("window.decorView.filterTouchesWhenObscured = true"))
        assertTrue(activitySource.contains("window.setHideOverlayWindows(true)"))
        assertTrue(activitySource.contains("Lifecycle.Event.ON_STOP"))
        assertTrue(activitySource.contains("delay(SecurityManager.TIMEOUT_INACTIVITY_MS)"))
        assertTrue(activitySource.contains("securityManager.lockSession()"))
        assertTrue(activitySource.contains("sessionOwner?.clear()"))
        assertTrue(lockedBranch.contains("CompositionLocalProvider(LocalViewModelStoreOwner provides owner)"))
        assertTrue(lockedBranch.contains("val secureViewModel: MainViewModel = viewModel()"))
        assertFalse(activitySource.contains("private val viewModel: MainViewModel"))
        assertTrue(manifestSource.contains("android.permission.HIDE_OVERLAY_WINDOWS"))
        assertFalse(configSource.contains("TOKEN DISPOSITIVO FCM"))
        assertFalse(configSource.contains("Copia Token"))
        assertFalse(messagingSource.contains("OrderEntity"))
        assertFalse(messagingSource.contains("customer_email"))
        assertFalse(messagingSource.contains("customer_name"))
        assertTrue(notificationSource.contains("NotificationCompat.VISIBILITY_SECRET"))
        assertFalse(notificationSource.contains("Cliente:"))
        assertFalse(apiClientSource.contains("route=api/login"))
        assertFalse(apiClientSource.contains("HTTP \${response.code}: \$body"))
    }

    @Test
    fun remoteDataIsPurgedAndNeverWrittenToRoomByTheViewModel() {
        assertTrue(viewModelSource.contains("purgeSensitiveLocalCache()"))
        assertTrue(viewModelSource.contains("clearAllOrderItems()"))
        assertTrue(viewModelSource.contains("clearAllOrders()"))
        assertTrue(viewModelSource.contains("clearAllSubscriptions()"))
        assertTrue(viewModelSource.contains("clearAllReturns()"))
        assertTrue(viewModelSource.contains("clearAllAuditLogs()"))
        assertTrue(viewModelSource.contains("clearAllProducts()"))
        assertTrue(viewModelSource.contains("clearAllCategories()"))
        assertTrue(viewModelSource.contains("repository.clearSensitiveSession()"))
        assertFalse(viewModelSource.contains("OfflineOrderRepository"))
        assertFalse(viewModelSource.contains("OfflineAuditRepository"))
        assertFalse(viewModelSource.contains("insertOrder("))
        assertFalse(viewModelSource.contains("insertSubscription("))
        assertFalse(viewModelSource.contains("insertReturn("))
        assertFalse(viewModelSource.contains("OfflineCatalogRepository"))
        assertFalse(viewModelSource.contains("productDao().insertProduct("))
        assertFalse(viewModelSource.contains("categoryDao().insertCategory("))
        assertTrue(databaseSource.contains("DELETE FROM products_cache"))
        assertTrue(databaseSource.contains("DELETE FROM categories_cache"))
        assertFalse(File("src/main/java/com/example/notification/FcmTokenManager.kt").exists())
    }
}
