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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.AdminModuleSnapshot

@Composable
fun AdminModuleScreen(
    module: AdminModule,
    snapshot: AdminModuleSnapshot?,
    onRefresh: () -> Unit,
    onStatusChange: (recordId: String, active: Boolean) -> Unit = { _, _ -> },
    onAddAntispam: (keyword: String) -> Unit = {},
    onDeleteAntispam: (recordId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = snapshot ?: AdminModuleSnapshot(module = module, isLoading = true)
    var antispamKeyword by remember(module) { mutableStateOf("") }

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
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
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
