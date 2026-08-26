package com.example.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationHelperTest {

    @Test
    fun `notification pending intent has an explicit immutable destination`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pendingIntent = NotificationHelper.createExplicitMainActivityPendingIntent(
            context = context,
            requestCode = 1234,
            action = "com.example.TEST"
        )

        val intent = shadowOf(pendingIntent).savedIntent
        assertNotNull(intent.component)
        assertEquals(context.packageName, intent.component?.packageName)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(pendingIntent.isImmutable)
    }
}
