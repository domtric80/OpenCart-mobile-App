package com.example.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SecurityManagerLockoutTest {
    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("cartadmin_security_vault", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        securityManager = SecurityManager(context)
        assertTrue(securityManager.setCredentials("admin", "Strong!Password1"))
    }

    @Test
    fun fiveFailuresTemporarilyBlockEvenTheCorrectPassword() {
        repeat(5) {
            assertFalse(securityManager.verifyPassword("Wrong!Password1"))
        }

        assertTrue(securityManager.remainingPasswordLockoutSeconds() > 0)
        assertFalse(securityManager.verifyPassword("Strong!Password1"))
    }

    @Test
    fun successfulAuthenticationClearsFailureCounter() {
        repeat(4) {
            assertFalse(securityManager.verifyPassword("Wrong!Password1"))
        }
        assertTrue(securityManager.verifyPassword("Strong!Password1"))

        repeat(4) {
            assertFalse(securityManager.verifyPassword("Wrong!Password1"))
        }
        assertTrue(securityManager.remainingPasswordLockoutSeconds() == 0L)
    }
}
