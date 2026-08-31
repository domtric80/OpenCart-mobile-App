package com.example.ui.components

import android.graphics.Typeface
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.ParagraphStyle
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.max
import kotlin.math.min

@Composable
fun RichHtmlEditor(
    html: String,
    onHtmlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Descrizione"
) {
    val latestChange by rememberUpdatedState(onHtmlChange)
    var editor by remember { mutableStateOf<EditText?>(null) }
    var blockMenuOpen by remember { mutableStateOf(false) }
    var colorMenuOpen by remember { mutableStateOf(false) }
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val surface = MaterialTheme.colorScheme.surface.toArgb()

    fun publishFormattingChange() {
        editor?.text?.let { latestChange(serializeRichText(it)) }
    }

    fun inline(span: Any) {
        editor?.let { applyInlineSpan(it, span) }
        publishFormattingChange()
    }

    fun block(level: Int) {
        editor?.let { applyHeading(it, level) }
        publishFormattingChange()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(260.dp).testTag("rich_html_editor"),
                factory = { context ->
                    EditText(context).apply {
                        setTextColor(foreground)
                        setBackgroundColor(surface)
                        gravity = android.view.Gravity.TOP or android.view.Gravity.START
                        setPadding(28, 24, 28, 24)
                        setText(Html.fromHtml(sanitizeEditorHtml(html), Html.FROM_HTML_MODE_COMPACT))
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                            override fun afterTextChanged(s: Editable?) {
                                s?.let { latestChange(serializeRichText(it)) }
                            }
                        })
                        editor = this
                    }
                }
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().imePadding().testTag("rich_editor_toolbar")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    EditorToolButton("P/H", "Stile paragrafo") { blockMenuOpen = true }
                    DropdownMenu(expanded = blockMenuOpen, onDismissRequest = { blockMenuOpen = false }) {
                        listOf(0 to "Paragrafo", 1 to "Titolo H1", 2 to "Titolo H2", 3 to "Titolo H3").forEach { (level, title) ->
                            DropdownMenuItem(text = { Text(title) }, onClick = {
                                block(level)
                                blockMenuOpen = false
                            })
                        }
                    }
                }
                Box {
                    EditorToolButton("●", "Colore testo") { colorMenuOpen = true }
                    DropdownMenu(expanded = colorMenuOpen, onDismissRequest = { colorMenuOpen = false }) {
                        val colors = listOf("#111827", "#DC2626", "#EA580C", "#16A34A", "#2563EB", "#7C3AED")
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            colors.forEach { hex ->
                                val color = android.graphics.Color.parseColor(hex)
                                Box(
                                    Modifier.size(30.dp).background(Color(color), CircleShape).clickable {
                                        inline(ForegroundColorSpan(color))
                                        colorMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
                EditorToolButton("B", "Grassetto") { inline(StyleSpan(Typeface.BOLD)) }
                EditorToolButton("I", "Corsivo") { inline(StyleSpan(Typeface.ITALIC)) }
                EditorToolButton("U", "Sottolineato") { inline(UnderlineSpan()) }
                EditorToolButton("• Lista", "Elenco puntato") {
                    editor?.let(::applyBullet)
                    publishFormattingChange()
                }
            }
        }
    }
}

@Composable
private fun EditorToolButton(text: String, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier.height(42.dp).testTag("editor_${description.lowercase().replace(' ', '_')}")
    ) { Text(text) }
}

private class HeadingSpan(val level: Int) : MetricAffectingSpan(), ParagraphStyle {
    private val factor = when (level) { 1 -> 1.8f; 2 -> 1.5f; else -> 1.25f }

    override fun updateDrawState(textPaint: TextPaint) = apply(textPaint)
    override fun updateMeasureState(textPaint: TextPaint) = apply(textPaint)

    private fun apply(textPaint: TextPaint) {
        textPaint.textSize *= factor
        textPaint.typeface = Typeface.create(textPaint.typeface, Typeface.BOLD)
    }
}

private fun selectedOrParagraph(editor: EditText): IntRange {
    val editable = editor.text
    var start = min(max(editor.selectionStart, 0), editable.length)
    var end = min(max(editor.selectionEnd, 0), editable.length)
    if (start > end) {
        val previousStart = start
        start = end
        end = previousStart
    }
    if (start == end) {
        start = editable.lastIndexOf('\n', max(0, start - 1)).let { if (it < 0) 0 else it + 1 }
        end = editable.indexOf('\n', start).let { if (it < 0) editable.length else it }
    }
    return start..end
}

private fun paragraphRange(editor: EditText): IntRange {
    val editable = editor.text
    val selection = selectedOrParagraph(editor)
    val start = editable.lastIndexOf('\n', max(0, selection.first - 1)).let { if (it < 0) 0 else it + 1 }
    val lineEnd = editable.indexOf('\n', selection.last).let { if (it < 0) editable.length else it + 1 }
    return start..lineEnd
}

private fun applyInlineSpan(editor: EditText, span: Any) {
    val range = selectedOrParagraph(editor)
    if (range.last > range.first) {
        editor.text.setSpan(span, range.first, range.last, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        editor.setSelection(range.last)
    }
}

private fun applyHeading(editor: EditText, level: Int) {
    val range = paragraphRange(editor)
    editor.text.getSpans(range.first, range.last, HeadingSpan::class.java).forEach(editor.text::removeSpan)
    editor.text.getSpans(range.first, range.last, RelativeSizeSpan::class.java).forEach(editor.text::removeSpan)
    if (level in 1..3 && range.last > range.first) {
        editor.text.setSpan(HeadingSpan(level), range.first, range.last, Spanned.SPAN_PARAGRAPH)
    }
}

private fun applyBullet(editor: EditText) {
    val range = paragraphRange(editor)
    val bullets = editor.text.getSpans(range.first, range.last, BulletSpan::class.java)
    if (bullets.isEmpty()) {
        editor.text.setSpan(BulletSpan(24), range.first, range.last, Spanned.SPAN_PARAGRAPH)
    } else {
        bullets.forEach(editor.text::removeSpan)
    }
}

internal fun serializeRichText(text: Spanned): String {
    val output = StringBuilder()
    var start = 0
    var listOpen = false
    do {
        val newline = text.indexOf('\n', start)
        val end = if (newline < 0) text.length else newline
        val probeEnd = max(start + 1, min(text.length, end))
        val bullet = text.getSpans(start, probeEnd, BulletSpan::class.java).isNotEmpty()
        if (bullet && !listOpen) { output.append("<ul>"); listOpen = true }
        if (!bullet && listOpen) { output.append("</ul>"); listOpen = false }
        val heading = text.getSpans(start, probeEnd, HeadingSpan::class.java).firstOrNull()?.level
            ?: text.getSpans(start, probeEnd, RelativeSizeSpan::class.java).firstOrNull()?.let {
                when { it.sizeChange >= 1.7f -> 1; it.sizeChange >= 1.4f -> 2; else -> 3 }
            }
        val tag = if (bullet) "li" else heading?.let { "h$it" } ?: "p"
        output.append('<').append(tag).append('>')
        output.append(serializeInline(text, start, end).ifEmpty { "<br>" })
        output.append("</").append(tag).append('>')
        start = if (newline < 0) text.length + 1 else newline + 1
    } while (start <= text.length)
    if (listOpen) output.append("</ul>")
    return sanitizeEditorHtml(output.toString()).take(65_535)
}

private fun serializeInline(text: Spanned, start: Int, end: Int): String {
    val output = StringBuilder()
    var position = start
    while (position < end) {
        val next = text.nextSpanTransition(position, end, Any::class.java)
        val styles = text.getSpans(position, next, StyleSpan::class.java)
        val bold = styles.any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
        val italic = styles.any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }
        val underline = text.getSpans(position, next, UnderlineSpan::class.java).isNotEmpty()
        val color = text.getSpans(position, next, ForegroundColorSpan::class.java).lastOrNull()
        color?.let { output.append(String.format("<font color=\"#%06X\">", 0xFFFFFF and it.foregroundColor)) }
        if (bold) output.append("<strong>")
        if (italic) output.append("<em>")
        if (underline) output.append("<u>")
        output.append(TextUtils.htmlEncode(text.subSequence(position, next).toString()))
        if (underline) output.append("</u>")
        if (italic) output.append("</em>")
        if (bold) output.append("</strong>")
        if (color != null) output.append("</font>")
        position = next
    }
    return output.toString()
}

internal fun sanitizeEditorHtml(html: String): String = html
    .replace(Regex("(?is)<(script|iframe|object|embed)[^>]*>.*?</\\1>"), "")
    .replace(Regex("(?is)<(script|iframe|object|embed)[^>]*/?>"), "")
    .replace(Regex("(?is)\\s+on[a-z]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)"), "")
    .replace(Regex("(?i)javascript\\s*:"), "")
    .take(65_535)
