package com.example.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidxDependencyAlignmentTest {
    private val catalog = File("../gradle/libs.versions.toml").readText()

    @Test
    fun fragmentActivitySupportsModernActivityResultRequestCodes() {
        val fragmentVersion = version("fragment")
        val parts = fragmentVersion.substringBefore('-').split('.').map(String::toInt)
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }

        assertTrue("FragmentActivity 1.2.x rejects ActivityResultRegistry request codes", major > 1 || (major == 1 && minor >= 3))
        assertTrue(catalog.contains("androidx-fragment = { group = \"androidx.fragment\", name = \"fragment\", version.ref = \"fragment\" }"))
    }

    private fun version(name: String): String = Regex("(?m)^$name = \\\"([^\\\"]+)\\\"$")
        .find(catalog)
        ?.groupValues
        ?.get(1)
        ?: error("Versione $name non dichiarata")
}
