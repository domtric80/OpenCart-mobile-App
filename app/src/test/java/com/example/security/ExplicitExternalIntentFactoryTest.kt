package com.example.security

import android.content.ComponentName
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExplicitExternalIntentFactoryTest {

    private val component = ComponentName("com.example.handler", "com.example.handler.TargetActivity")

    @Test
    fun `dial intent is explicit before it contains the phone number`() {
        val intent = ExplicitExternalIntentFactory.buildDialIntent(component, "+39021234567")

        assertEquals(component, intent.component)
        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel:+39021234567", intent.dataString)
    }

    @Test
    fun `email intent is explicit and carries the expected subject`() {
        val intent = ExplicitExternalIntentFactory.buildEmailIntent(
            component,
            "customer@example.com",
            "Assistenza Ordine 42"
        )

        assertEquals(component, intent.component)
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:customer@example.com", intent.dataString)
        assertEquals("Assistenza Ordine 42", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertNotNull(intent.data)
    }
}
