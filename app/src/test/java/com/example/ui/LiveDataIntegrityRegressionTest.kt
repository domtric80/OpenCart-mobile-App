package com.example.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveDataIntegrityRegressionTest {
    private val source = File("src/main/java/com/example/ui/MainViewModel.kt").readText()

    @Test
    fun productionRuntimeDoesNotInjectDemoSubscriptionsOrReturns() {
        listOf("sub_101", "sub_102", "sub_103", "ret_501", "ret_502", "ret_503").forEach {
            demoId -> assertFalse("Demo ID found in production runtime: $demoId", source.contains(demoId))
        }
    }

    @Test
    fun successfulEmptyResponsesReplaceSubscriptionAndReturnState() {
        assertFalse(source.contains("if (liveSubs.isNotEmpty())"))
        assertFalse(source.contains("if (liveReturns.isNotEmpty())"))
        assertTrue(source.contains("repository.setSubscriptions(liveSubs)"))
        assertTrue(source.contains("repository.setReturns(liveReturns)"))
        assertTrue(source.contains("db.subscriptionDao().clearAllSubscriptions()"))
        assertTrue(source.contains("db.orderReturnDao().clearAllReturns()"))
    }
}
