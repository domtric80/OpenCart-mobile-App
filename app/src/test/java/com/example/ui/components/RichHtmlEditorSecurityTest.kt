package com.example.ui.components

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichHtmlEditorSecurityTest {
    private val source = File("src/main/java/com/example/ui/components/RichHtmlEditor.kt").readText()
    private val manifest = File("src/main/AndroidManifest.xml").readText()

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
        listOf("FormatBold", "FormatItalic", "FormatUnderlined", "FormatListBulleted", "FormatColorText", "Icons.Default.Title")
            .forEach { icon -> assertTrue("Missing toolbar icon $icon", source.contains(icon)) }
        assertTrue(source.contains("DialogProperties(usePlatformDefaultWidth = false"))
        assertTrue(source.contains("SOFT_INPUT_ADJUST_RESIZE"))
        assertFalse(source.contains(".imePadding()"))
        assertTrue(source.indexOf("EditorToolbar(") < source.indexOf("AndroidView("))
        assertTrue(manifest.contains("android:windowSoftInputMode=\"adjustResize\""))
    }
}
