package com.example.ui.components

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichHtmlEditorSecurityTest {
    private val source = File("src/main/java/com/example/ui/components/RichHtmlEditor.kt").readText()

    @Test
    fun sanitizerRemovesActiveContentAndEventHandlers() {
        val dirty = """<p onclick=steal()>Test</p><script>alert(1)</script><img onerror="steal()" src="javascript:run()">"""
        val clean = sanitizeEditorHtml(dirty)

        assertFalse(clean.contains("script", ignoreCase = true))
        assertFalse(clean.contains("onclick", ignoreCase = true))
        assertFalse(clean.contains("onerror", ignoreCase = true))
        assertFalse(clean.contains("javascript:", ignoreCase = true))
        assertTrue(clean.contains("<p>Test</p>"))
    }

    @Test
    fun editorUsesNativeSpansWithoutJavaScriptOrWebViewBridges() {
        assertTrue(source.contains("EditText(context)"))
        assertFalse(source.contains("WebView"))
        assertFalse(source.contains("javaScriptEnabled"))
        assertFalse(source.contains("addJavascriptInterface"))
        assertFalse(source.contains("@JavascriptInterface"))
    }

    @Test
    fun toolbarContainsOnlyTheRequestedCoreFormattingCommands() {
        listOf("HeadingSpan", "ForegroundColorSpan", "StyleSpan(Typeface.BOLD)", "StyleSpan(Typeface.ITALIC)", "UnderlineSpan", "BulletSpan")
            .forEach { feature -> assertTrue("Missing editor feature $feature", source.contains(feature)) }
        listOf("Paragrafo", "Titolo H1", "Titolo H2", "Titolo H3")
            .forEach { style -> assertTrue("Missing block style $style", source.contains(style)) }
        assertTrue(source.contains("Modifier.fillMaxWidth().imePadding()"))
    }
}
