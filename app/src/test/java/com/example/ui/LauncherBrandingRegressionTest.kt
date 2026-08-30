package com.example.ui

import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherBrandingRegressionTest {
    @Test
    fun launcherUsesVerifiedOpenCartItaliaAssetAtEveryDensity() {
        val source = File("src/main/res/drawable-nodpi/opencart_italia_launcher.png")
        assertTrue(source.isFile)
        assertEquals(
            "7ff68d405376390ad75cb721538d15eb24f9868032db584b3245e2fb02e99a9a",
            source.sha256()
        )

        val expectedSizes = mapOf(
            "mdpi" to 48,
            "hdpi" to 72,
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192
        )
        expectedSizes.forEach { (density, size) ->
            listOf("ic_launcher.png", "ic_launcher_round.png").forEach { name ->
                val icon = File("src/main/res/mipmap-$density/$name")
                assertTrue("Missing $density/$name", icon.isFile)
                val image = ImageIO.read(icon)
                assertEquals(size, image.width)
                assertEquals(size, image.height)
            }
            assertFalse(File("src/main/res/mipmap-$density/ic_launcher.webp").exists())
            assertFalse(File("src/main/res/mipmap-$density/ic_launcher_round.webp").exists())
        }

        val adaptiveIcon = File("src/main/res/mipmap-anydpi-v26/ic_launcher.xml").readText()
        val foreground = File("src/main/res/drawable/ic_launcher_foreground.xml").readText()
        assertTrue(foreground.contains("@drawable/opencart_italia_launcher"))
        assertTrue(adaptiveIcon.contains("@drawable/ic_launcher_foreground"))
        assertTrue(adaptiveIcon.contains("@drawable/ic_launcher_monochrome"))
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(readBytes())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
