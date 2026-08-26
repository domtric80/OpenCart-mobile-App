package com.example.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TlsPinnedClientTest {

    @Test
    fun `cleartext OpenCart requests are rejected before network access`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = TlsPinnedClient(context)
        val request = Request.Builder().url("http://shop.example.test/api").build()

        val error = runCatching { client.execute(request) }.exceptionOrNull()

        assertTrue(error is IOException)
        assertTrue(error?.message?.contains("HTTPS") == true)
    }
}
