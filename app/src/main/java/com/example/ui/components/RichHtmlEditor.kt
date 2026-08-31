package com.example.ui.components

import android.graphics.Typeface
import android.graphics.Rect
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
import android.view.WindowManager
import android.view.ViewTreeObserver
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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
    var expanded by remember { mutableStateOf(false) }
    val preview = remember(html) {
        Html.fromHtml(sanitizeEditorHtml(html), Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().testTag("open_rich_html_editor")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (preview.isBlank()) "Tocca per scrivere a schermo intero" else preview,
                        maxLines = 3,
                        color = if (preview.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text("Editor visuale a schermo intero", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (expanded) {
        FullScreenRichTextDialog(
            initialHtml = html,
            label = label,
            onHtmlChange = latestChange,
            onDismiss = { expanded = false }
        )
    }
}

@Composable
private fun FullScreenRichTextDialog(
    initialHtml: String,
    label: String,
    onHtmlChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editor by remember { mutableStateOf<EditText?>(null) }
    var blockMenuOpen by remember { mutableStateOf(false) }
    var colorMenuOpen by remember { mutableStateOf(false) }
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val surface = MaterialTheme.colorScheme.surface.toArgb()

    fun publishFormattingChange() {
        editor?.text?.let { onHtmlChange(serializeRichText(it)) }
    }

    fun inline(span: Any) {
        editor?.let { applyInlineSpan(it, span) }
        publishFormattingChange()
    }

    fun block(level: Int) {
        editor?.let { applyHeading(it, level) }
        publishFormattingChange()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        val dialogView = LocalView.current
        val density = LocalDensity.current
        var bottomOcclusionPx by remember { mutableIntStateOf(0) }
        DisposableEffect(dialogView) {
            val listener = ViewTreeObserver.OnGlobalLayoutListener {
                val visibleFrame = Rect()
                dialogView.rootView.getWindowVisibleDisplayFrame(visibleFrame)
                bottomOcclusionPx = (dialogView.rootView.height - visibleFrame.bottom).coerceAtLeast(0)
            }
            dialogView.viewTreeObserver.addOnGlobalLayoutListener(listener)
            onDispose { dialogView.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
        }
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = with(density) { bottomOcclusionPx.toDp() })
                    .testTag("rich_editor_fullscreen"),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    EditorToolbar(
                        editor = editor,
                        blockMenuOpen = blockMenuOpen,
                        onBlockMenuOpenChange = { blockMenuOpen = it },
                        colorMenuOpen = colorMenuOpen,
                        onColorMenuOpenChange = { colorMenuOpen = it },
                        onInline = ::inline,
                        onBlock = ::block,
                        onPublishFormattingChange = ::publishFormattingChange
                    )
                }
            ) { contentPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Chiudi editor") }
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("rich_editor_done")) {
                        Icon(Icons.Default.Check, contentDescription = "Conferma testo")
                    }
                }
                AndroidView(
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("rich_html_editor"),
                    factory = { context ->
                        EditText(context).apply {
                            setTextColor(foreground)
                            setBackgroundColor(surface)
                            gravity = android.view.Gravity.TOP or android.view.Gravity.START
                            setPadding(28, 24, 28, 24)
                            setText(Html.fromHtml(sanitizeEditorHtml(initialHtml), Html.FROM_HTML_MODE_COMPACT))
                            addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                                override fun afterTextChanged(s: Editable?) {
                                    s?.let { onHtmlChange(serializeRichText(it)) }
                                }
                            })
                            editor = this
                        }
                    }
                )
            }
        }
    }
}
}

@Composable
private fun EditorToolbar(
    editor: EditText?,
    blockMenuOpen: Boolean,
    onBlockMenuOpenChange: (Boolean) -> Unit,
    colorMenuOpen: Boolean,
    onColorMenuOpenChange: (Boolean) -> Unit,
    onInline: (Any) -> Unit,
    onBlock: (Int) -> Unit,
    onPublishFormattingChange: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .zIndex(2f)
            .testTag("rich_editor_toolbar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                EditorToolButton(Icons.Default.Title, "Stile paragrafo") { onBlockMenuOpenChange(true) }
                DropdownMenu(expanded = blockMenuOpen, onDismissRequest = { onBlockMenuOpenChange(false) }) {
                    listOf(0 to "Paragrafo", 1 to "Titolo H1", 2 to "Titolo H2", 3 to "Titolo H3").forEach { (level, title) ->
                        DropdownMenuItem(text = { Text(title) }, onClick = {
                            onBlock(level)
                            onBlockMenuOpenChange(false)
                        })
                    }
                }
            }
            Box {
                EditorToolButton(Icons.Default.FormatColorText, "Colore testo") { onColorMenuOpenChange(true) }
                DropdownMenu(expanded = colorMenuOpen, onDismissRequest = { onColorMenuOpenChange(false) }) {
                    val colors = listOf("#111827", "#DC2626", "#EA580C", "#16A34A", "#2563EB", "#7C3AED")
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colors.forEach { hex ->
                            val color = android.graphics.Color.parseColor(hex)
                            Box(
                                Modifier.size(30.dp).background(Color(color), CircleShape).clickable {
                                    onInline(ForegroundColorSpan(color))
                                    onColorMenuOpenChange(false)
                                }
                            )
                        }
                    }
                }
            }
            EditorToolButton(Icons.Default.FormatBold, "Grassetto") { onInline(StyleSpan(Typeface.BOLD)) }
            EditorToolButton(Icons.Default.FormatItalic, "Corsivo") { onInline(StyleSpan(Typeface.ITALIC)) }
            EditorToolButton(Icons.Default.FormatUnderlined, "Sottolineato") { onInline(UnderlineSpan()) }
            EditorToolButton(Icons.Default.FormatListBulleted, "Elenco puntato") {
                editor?.let(::applyBullet)
                onPublishFormattingChange()
            }
        }
    }
}

@Composable
private fun EditorToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).testTag("editor_${description.lowercase().replace(' ', '_')}")
    ) { Icon(icon, contentDescription = description) }
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
