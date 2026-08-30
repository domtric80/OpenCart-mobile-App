package com.example.ui

import com.example.model.AdminModule
import com.example.ui.components.NavigationTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationArchitectureTest {

    @Test
    fun bottomNavigationContainsExactlyFivePrimaryDestinations() {
        val primary = NavigationTab.entries.filter { it.showInBottomBar }

        assertEquals(5, primary.size)
        assertEquals(
            listOf(
                NavigationTab.HOME,
                NavigationTab.ORDERS,
                NavigationTab.CATALOG,
                NavigationTab.CUSTOMERS,
                NavigationTab.MORE
            ),
            primary
        )
        assertFalse(NavigationTab.AUDIT.showInBottomBar)
        assertFalse(NavigationTab.LICENSE.showInBottomBar)
    }

    @Test
    fun managementApiKeysAreUniqueAndComplete() {
        val keys = AdminModule.entries.map { it.apiKey }

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.containsAll(listOf("subscription_plans", "pages", "reviews")))
        assertTrue(keys.containsAll(listOf("articles", "topics", "comments", "antispam")))
        assertTrue(keys.containsAll(listOf("customers", "customer_approvals", "gdpr")))
    }
}
