package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdminModule
import com.example.model.AdminRecord
import com.example.model.AdminModuleSnapshot

@Composable
fun AdminModuleScreen(
    module: AdminModule,
    snapshot: AdminModuleSnapshot?,
    onRefresh: () -> Unit,
    onStatusChange: (recordId: String, active: Boolean) -> Unit = { _, _ -> },
    onAddAntispam: (keyword: String) -> Unit = {},
    onDeleteAntispam: (recordId: String) -> Unit = {},
    onContentUpdate: (record: AdminRecord) -> Unit = {},
    onContentCreate: (record: AdminRecord) -> Unit = {},
    availableTopics: List<AdminRecord> = emptyList(),
    onSensitiveAction: (recordId: String, operation: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val state = snapshot ?: AdminModuleSnapshot(module = module, isLoading = true)
    var antispamKeyword by remember(module) { mutableStateOf("") }
    var pendingSensitiveAction by remember(module) { mutableStateOf<Pair<String, String>?>(null) }
    var recordBeingEdited by remember(module) { mutableStateOf<AdminRecord?>(null) }
    var showCreateDialog by remember(module) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("admin_module_${module.apiKey}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = module.label,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onRefresh,
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("refresh_${module.apiKey}")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", modifier = Modifier.size(18.dp))
                    Text(" Aggiorna")
                }
            }
        }

        if (module == AdminModule.ANTISPAM) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = antispamKeyword,
                        onValueChange = { antispamKeyword = it.take(64) },
                        label = { Text("Nuova parola bloccata") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("antispam_keyword")
                    )
                    Button(
                        onClick = {
                            val keyword = antispamKeyword.trim()
                            if (keyword.isNotEmpty()) {
                                onAddAntispam(keyword)
                                antispamKeyword = ""
                            }
                        },
                        enabled = antispamKeyword.isNotBlank() && !state.isLoading,
                        modifier = Modifier.testTag("add_antispam_keyword")
                    ) {
                        Text("Aggiungi")
                    }
                }
            }
        }

        if (module == AdminModule.ARTICLES || module == AdminModule.TOPICS) {
            item {
                Button(
                    onClick = { showCreateDialog = true },
                    enabled = !state.isLoading && (module == AdminModule.TOPICS || availableTopics.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth().testTag("create_${module.apiKey}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(if (module == AdminModule.ARTICLES) " Nuovo articolo" else " Nuova categoria CMS")
                }
                if (module == AdminModule.ARTICLES && availableTopics.isEmpty()) {
                    Text(
                        "Crea prima una categoria CMS nella sezione Argomenti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (module == AdminModule.CUSTOMER_APPROVALS || module == AdminModule.GDPR) {
            item {
                ModuleMessageCard("Le azioni sensibili vengono accodate. Un amministratore deve confermarle nel pannello CartAdmin Bridge; solo allora OpenCart esegue eventi ed email native.")
            }
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (!state.supported) {
            item { ModuleMessageCard(state.message.ifBlank { "Modulo non disponibile su questo store." }) }
        } else if (state.message.isNotBlank() && state.records.isEmpty()) {
            item { ModuleMessageCard(state.message) }
        } else if (state.records.isEmpty()) {
            item { ModuleMessageCard("Nessun elemento presente nello store. Non vengono mostrati dati dimostrativi.") }
        } else {
            if (state.message.isNotBlank()) {
                item { ModuleMessageCard(state.message) }
            }
            items(state.records, key = { it.id }) { record ->
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("${module.apiKey}_${record.id}"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = record.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (record.statusLabel.isNotBlank()) {
                                    val badgeColor = when (record.active) {
                                        true -> MaterialTheme.colorScheme.primaryContainer
                                        false -> MaterialTheme.colorScheme.errorContainer
                                        null -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                    val badgeText = when (record.active) {
                                        true -> MaterialTheme.colorScheme.onPrimaryContainer
                                        false -> MaterialTheme.colorScheme.onErrorContainer
                                        null -> MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                    Surface(color = badgeColor, contentColor = badgeText, shape = RoundedCornerShape(50)) {
                                        Text(
                                            record.statusLabel,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (record.subtitle.isNotBlank()) {
                                Text(record.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (record.detail.isNotBlank()) {
                                Text(record.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (record.date.isNotBlank()) {
                                Text(record.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            record.active?.let { isActive ->
                                OutlinedButton(
                                    onClick = { onStatusChange(record.id, !isActive) },
                                    modifier = Modifier.testTag("toggle_${module.apiKey}_${record.id}")
                                ) {
                                    Text(if (isActive) "Disattiva" else "Attiva")
                                }
                            }
                            if (module == AdminModule.ANTISPAM) {
                                OutlinedButton(
                                    onClick = { onDeleteAntispam(record.id) },
                                    modifier = Modifier.testTag("delete_antispam_${record.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Text(" Rimuovi")
                                }
                            }
                            if (record.editable) {
                                OutlinedButton(
                                    onClick = { recordBeingEdited = record },
                                    modifier = Modifier.testTag("edit_${module.apiKey}_${record.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Text(" Modifica")
                                }
                            }
                            if ((module == AdminModule.CUSTOMER_APPROVALS || module == AdminModule.GDPR) && record.actionable) {
                                if (record.pendingOperation.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "In attesa nel pannello: ${if (record.pendingOperation == "approve") "approvazione" else "rifiuto"}",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { pendingSensitiveAction = record.id to "approve" },
                                            modifier = Modifier.testTag("approve_${module.apiKey}_${record.id}")
                                        ) { Text("Approva") }
                                        OutlinedButton(
                                            onClick = { pendingSensitiveAction = record.id to "deny" },
                                            modifier = Modifier.testTag("deny_${module.apiKey}_${record.id}")
                                        ) { Text("Rifiuta") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    pendingSensitiveAction?.let { (recordId, operation) ->
        AlertDialog(
            onDismissRequest = { pendingSensitiveAction = null },
            title = { Text(if (operation == "approve") "Richiedi approvazione" else "Richiedi rifiuto") },
            text = { Text("La richiesta verrà inviata al pannello OpenCart e non sarà eseguita finché un amministratore non la conferma.") },
            confirmButton = {
                Button(onClick = {
                    onSensitiveAction(recordId, operation)
                    pendingSensitiveAction = null
                }) { Text("Invia al pannello") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingSensitiveAction = null }) { Text("Annulla") }
            }
        )
    }

    recordBeingEdited?.let { record ->
        AdminContentEditDialog(
            module = module,
            record = record,
            onDismiss = { recordBeingEdited = null },
            onSave = { updated ->
                onContentUpdate(updated)
                recordBeingEdited = null
            }
        )
    }


    if (showCreateDialog) {
        AdminContentCreateDialog(
            module = module,
            topics = availableTopics,
            onDismiss = { showCreateDialog = false },
            onSave = { record ->
                onContentCreate(record)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun AdminContentCreateDialog(
    module: AdminModule,
    topics: List<AdminRecord>,
    onDismiss: () -> Unit,
    onSave: (AdminRecord) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf("0") }
    var active by remember { mutableStateOf(true) }
    var selectedTopic by remember(topics) { mutableStateOf(topics.firstOrNull()) }
    var topicsExpanded by remember { mutableStateOf(false) }
    val isArticle = module == AdminModule.ARTICLES
    val valid = title.isNotBlank() && content.isNotBlank() &&
        (!isArticle || (author.isNotBlank() && selectedTopic != null)) &&
        (isArticle || sortOrder.toIntOrNull() != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArticle) "Nuovo articolo" else "Nuova categoria CMS") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(title, { title = it.take(255) }, label = { Text("Titolo") }, modifier = Modifier.fillMaxWidth().testTag("create_content_title"))
                if (isArticle) {
                    OutlinedTextField(author, { author = it.take(64) }, label = { Text("Autore") }, modifier = Modifier.fillMaxWidth().testTag("create_article_author"))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { topicsExpanded = true }, modifier = Modifier.fillMaxWidth().testTag("create_article_topic")) {
                            Text(selectedTopic?.title ?: "Seleziona categoria CMS")
                        }
                        DropdownMenu(expanded = topicsExpanded, onDismissRequest = { topicsExpanded = false }) {
                            topics.forEach { topic ->
                                DropdownMenuItem(
                                    text = { Text(topic.title) },
                                    onClick = { selectedTopic = topic; topicsExpanded = false }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        sortOrder,
                        { sortOrder = it.filter(Char::isDigit).take(6) },
                        label = { Text("Ordinamento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    content,
                    { content = it.take(20000) },
                    label = { Text(if (isArticle) "Contenuto articolo" else "Descrizione categoria") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("create_content_body")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (active) "Pubblicato" else "Bozza")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                Text(
                    "Il contenuto viene creato in tutte le lingue attive e associato allo store principale. Potrai rifinire traduzioni e SEO dal pannello OpenCart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        AdminRecord(
                            id = "",
                            title = title.trim(),
                            subtitle = author.trim(),
                            content = content.trim(),
                            active = active,
                            sortOrder = if (isArticle) null else sortOrder.toIntOrNull(),
                            parentId = selectedTopic?.id?.toIntOrNull(),
                            editable = true
                        )
                    )
                },
                modifier = Modifier.testTag("save_content_create")
            ) { Text("Crea sullo store") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun AdminContentEditDialog(
    module: AdminModule,
    record: AdminRecord,
    onDismiss: () -> Unit,
    onSave: (AdminRecord) -> Unit
) {
    var title by remember(record.id) { mutableStateOf(record.title) }
    var secondary by remember(record.id) { mutableStateOf(record.subtitle) }
    var content by remember(record.id) { mutableStateOf(record.content) }
    var ratingText by remember(record.id) { mutableStateOf(record.rating?.toString().orEmpty()) }
    var sortOrderText by remember(record.id) { mutableStateOf(record.sortOrder?.toString().orEmpty()) }

    val editsTitle = module == AdminModule.PAGES || module == AdminModule.ARTICLES || module == AdminModule.TOPICS
    val editsSecondary = module == AdminModule.ARTICLES || module == AdminModule.REVIEWS
    val editsReview = module == AdminModule.REVIEWS
    val editsSortOrder = module == AdminModule.PAGES || module == AdminModule.TOPICS
    val parsedRating = ratingText.toIntOrNull()
    val parsedSortOrder = sortOrderText.toIntOrNull()
    val valid = when {
        editsTitle && title.trim().isEmpty() -> false
        editsSecondary && secondary.trim().isEmpty() -> false
        editsReview && (content.trim().isEmpty() || parsedRating !in 1..5) -> false
        editsSortOrder && (parsedSortOrder == null || parsedSortOrder < 0) -> false
        else -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica ${module.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editsTitle) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(255) },
                        label = { Text(if (module == AdminModule.PAGES) "Titolo pagina" else "Titolo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("content_title")
                    )
                }
                if (editsSecondary) {
                    OutlinedTextField(
                        value = secondary,
                        onValueChange = { secondary = it.take(64) },
                        label = { Text("Autore") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("content_secondary")
                    )
                }
                if (editsReview) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it.take(2000) },
                        label = { Text("Testo recensione") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("content_body")
                    )
                    OutlinedTextField(
                        value = ratingText,
                        onValueChange = { ratingText = it.filter(Char::isDigit).take(1) },
                        label = { Text("Valutazione (1–5)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("content_rating")
                    )
                }
                if (editsSortOrder) {
                    OutlinedTextField(
                        value = sortOrderText,
                        onValueChange = { sortOrderText = it.filter(Char::isDigit).take(6) },
                        label = { Text("Ordinamento") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("content_sort_order")
                    )
                }
                if (module == AdminModule.PAGES || module == AdminModule.ARTICLES || module == AdminModule.TOPICS) {
                    Text(
                        "Viene aggiornata la lingua principale dello store. Il contenuto HTML e le traduzioni esistenti restano invariati.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        record.copy(
                            title = title.trim(),
                            subtitle = secondary.trim(),
                            content = content.trim(),
                            rating = if (editsReview) parsedRating else record.rating,
                            sortOrder = if (editsSortOrder) parsedSortOrder else record.sortOrder
                        )
                    )
                },
                modifier = Modifier.testTag("save_content_edit")
            ) { Text("Salva sullo store") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun ModuleMessageCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
