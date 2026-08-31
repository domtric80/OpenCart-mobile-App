package com.example.ui.screens

import com.example.BuildConfig

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Store
import com.example.network.OpenCartConnectionResult
import com.example.notification.FcmTokenManager
import com.example.notification.NotificationHelper
import com.example.ui.components.OpenCartModuleSheet
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.CardSurfaceLight
import com.example.ui.theme.CardSurfacePure
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer
import com.example.ui.theme.ThemeSecondaryContainer
import com.example.ui.theme.TrendGreen
import com.example.ui.theme.TrendGreenLight

@Composable
fun ConfigScreen(
    currentStore: Store?,
    isTestingConnection: Boolean,
    connectionResult: OpenCartConnectionResult?,
    onTestConnection: (url: String, username: String, key: String) -> Unit,
    onSaveStoreCredentials: (storeId: String, name: String, url: String, username: String, key: String, version: String) -> Unit,
    onAddStore: (name: String, url: String, username: String, key: String, version: String) -> Unit,
    onTriggerSync: (url: String, key: String, username: String) -> Unit,
    onClearDummyData: () -> Unit = {},
    onOpenAudit: () -> Unit = {},
    onOpenLicense: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val securityManager = remember { com.example.auth.SecurityManager(context) }
    var storeName by remember { mutableStateOf(currentStore?.name ?: "") }
    var storeUrl by remember { mutableStateOf(currentStore?.url ?: "") }
    var apiUsername by remember { mutableStateOf(currentStore?.apiUsername ?: "api_admin_sync") }
    var apiKey by remember { mutableStateOf("") }
    var storeVersion by remember { mutableStateOf(currentStore?.version ?: "OpenCart 3.0.3.8") }
    var showGuide by remember { mutableStateOf(false) }
    var showModuleSheet by remember { mutableStateOf(false) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current
    var fcmToken by remember { mutableStateOf<String?>(null) }

    // Notification Toggles
    var notifyNewOrders by remember { mutableStateOf(true) }
    var notifyStockAlerts by remember { mutableStateOf(true) }
    var notifySoundEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        fcmToken = FcmTokenManager.fetchCurrentToken(context)
    }

    LaunchedEffect(currentStore?.id) {
        if (currentStore != null) {
            storeName = currentStore.name
            storeUrl = currentStore.url
            apiUsername = currentStore.apiUsername
            apiKey = ""
            storeVersion = currentStore.version
        } else {
            storeName = ""
            storeUrl = ""
            apiUsername = "api_admin_sync"
            apiKey = ""
            storeVersion = "OpenCart 4.1.x"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("config_screen_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Integrazione & Notifiche",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Live Connection Status Card
            item {
                val isSuccess = connectionResult?.isSuccess ?: (currentStore?.isConnected == true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isSuccess) TrendGreenLight else AlertRedContainer)
                        .padding(18.dp)
                        .testTag("opencart_status_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isSuccess) TrendGreen else AlertRed,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSuccess) TrendGreen else AlertRed)
                                )
                                Text(
                                    text = if (isSuccess) "CONNESSIONE OPENCART ATTIVA" else "DISCONNESSO DA OPENCART",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.1.sp
                                    ),
                                    color = if (isSuccess) TrendGreen else AlertRed
                                )
                            }

                            Text(
                                text = currentStore?.name ?: "Negozio OpenCart",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = if (isSuccess) TrendGreen else AlertRed
                            )
                            Text(
                                text = currentStore?.let { "${it.url} • ${it.version}" }
                                    ?: "Configura il primo negozio",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = if (isSuccess) TrendGreen else AlertRed
                            )
                        }
                    }
                }
            }

            // Embedded Module Promotion Card (Included in the App)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardSurfacePure)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                        .testTag("embedded_module_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ThemeSecondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DataObject,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "ESTENSIONE OPENCART 4.1",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TrendGreenLight)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Pronto all'uso",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = TrendGreen
                                )
                            }
                        }

                        Text(
                            text = "CartAdmin Bridge",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Installa e configura il bridge dal pannello OpenCart. Il token viene mostrato una sola volta e nel database resta soltanto un hash non reversibile.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showModuleSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("open_module_sheet_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Installa e configura il bridge",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // PUSH NOTIFICATIONS & REAL-TIME ALERTS CENTER
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardSurfacePure)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                        .padding(18.dp)
                        .testTag("push_notifications_card"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ThemePrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = ThemePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "CENTRO NOTIFICHE PUSH & AVVISI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Ricevi avvisi sonori e notifiche sullo smartphone quando i clienti acquistano o quando un articolo finisce le scorte:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Toggle: Nuovi Ordini
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "Nuovi Ordini OpenCart",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Notifica istantanea con totale e nome cliente",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = notifyNewOrders,
                            onCheckedChange = { notifyNewOrders = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ThemePrimary, checkedTrackColor = ThemePrimaryContainer)
                        )
                    }

                    // Toggle: Allarmi Sottoscorta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "Allarmi Sottoscorta Magazzino",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Avviso quando le scorte scendono sotto soglia",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = notifyStockAlerts,
                            onCheckedChange = { notifyStockAlerts = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ThemePrimary, checkedTrackColor = ThemePrimaryContainer)
                        )
                    }

                    // Action Buttons for testing notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val success = NotificationHelper.sendTestNotification(context)
                                if (success) {
                                    Toast.makeText(context, "Notifica di test inviata!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Abilita i permessi di notifica nelle impostazioni", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("test_push_notification_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Notifica", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                val success = NotificationHelper.sendNewOrderNotification(
                                    context = context,
                                    orderNumber = "#" + (1050..1999).random(),
                                    customerName = "Mario Rossi",
                                    total = 149.90
                                )
                                if (success) {
                                    Toast.makeText(context, "Notifica simulazione ordine inviata!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Abilita i permessi di notifica nelle impostazioni", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("simulate_order_push_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simula Ordine", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // FCM Registration Token Display & Copy
                    if (fcmToken != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOKEN DISPOSITIVO FCM (PER PUSH SERVER)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Copia Token",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(fcmToken ?: ""))
                                                Toast.makeText(context, "Token FCM copiato negli appunti!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 2.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = fcmToken ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // OpenCart API Configuration Form
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardSurfacePure)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (currentStore == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(12.dp)
                                .testTag("first_store_help")
                        ) {
                            Text(
                                text = "Nessun negozio configurato. Compila questi dati e premi Aggiungi negozio: il profilo verrà creato e salvato sul dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PARAMETRI STORE & API",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = storeVersion,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nome Negozio") },
                        modifier = Modifier.fillMaxWidth().testTag("config_store_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = storeUrl,
                        onValueChange = { storeUrl = it },
                        label = { Text("URL Negozio OpenCart (es. https://negozio.it)") },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("config_store_url_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = apiUsername,
                        onValueChange = { apiUsername = it },
                        label = { Text("Etichetta locale / utente API nativa") },
                        leadingIcon = {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("config_api_username_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        supportingText = {
                            Text("Con CartAdmin Bridge l’operatore autorevole è quello assegnato al token nel pannello OpenCart.")
                        }
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Token CartAdmin Bridge / chiave API nativa") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("config_api_key_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = {
                            Text(
                                if (currentStore == null) {
                                    "Incolla il token generato nel pannello OpenCart. Non sarà più mostrato dopo il salvataggio."
                                } else {
                                    "Token già salvato e protetto. Lascia vuoto per mantenerlo oppure inseriscine uno nuovo per sostituirlo."
                                }
                            )
                        }
                    )

                    OutlinedTextField(
                        value = storeVersion,
                        onValueChange = { storeVersion = it },
                        label = { Text("Versione OpenCart (es. OpenCart 3.0.3.8 / 4.0.2)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Action Buttons: Test Connection & Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onTestConnection(
                                    storeUrl,
                                    apiUsername,
                                    apiKey.ifBlank { currentStore?.apiKey.orEmpty() }
                                )
                            },
                            enabled = !isTestingConnection && storeUrl.isNotBlank() &&
                                (apiKey.isNotBlank() || currentStore?.apiKey?.isNotBlank() == true),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("test_opencart_connection_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test API")
                        }

                        Button(
                            onClick = {
                                val safeName = storeName.ifBlank { "Mio Negozio OpenCart" }
                                if (currentStore == null) {
                                    onAddStore(safeName, storeUrl, apiUsername, apiKey, storeVersion)
                                    Toast.makeText(context, "Negozio aggiunto e parametri protetti.", Toast.LENGTH_SHORT).show()
                                } else {
                                    onSaveStoreCredentials(currentStore.id, safeName, storeUrl, apiUsername, apiKey, storeVersion)
                                    Toast.makeText(context, "Parametri aggiornati e protetti.", Toast.LENGTH_SHORT).show()
                                }
                                apiKey = ""
                            },
                            enabled = storeUrl.isNotBlank() &&
                                (currentStore != null || apiKey.isNotBlank()),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_opencart_credentials_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (currentStore == null) "Aggiungi" else "Salva")
                        }
                    }

                    // Direct Sync Button
                    Button(
                        onClick = {
                            onTriggerSync(
                                storeUrl,
                                apiKey.ifBlank { currentStore?.apiKey.orEmpty() },
                                apiUsername
                            )
                            Toast.makeText(context, "Sincronizzazione avviata per $storeUrl...", Toast.LENGTH_SHORT).show()
                        },
                        enabled = currentStore != null && storeUrl.isNotBlank() &&
                            (apiKey.isNotBlank() || currentStore.apiKey.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("manual_sync_now_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sincronizza Dati Adesso (Ordini & Catalogo)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Diagnostic Result Box
                    if (connectionResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (connectionResult.isSuccess) TrendGreenLight
                                    else AlertRedContainer
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (connectionResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (connectionResult.isSuccess) TrendGreen else AlertRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = connectionResult.message,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (connectionResult.isSuccess) TrendGreen else AlertRed
                                    )
                                }
                                if (!connectionResult.details.isNullOrBlank()) {
                                    Text(
                                        text = connectionResult.details,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (connectionResult.isSuccess) TrendGreen else AlertRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // OpenCart Setup Guide Accordion
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardSurfaceLight)
                        .border(1.dp, ThemeOutlineVariant, RoundedCornerShape(22.dp))
                        .clickable { showGuide = !showGuide }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = ThemePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Guida: Come configurare OpenCart",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = if (showGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showGuide) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Puoi connettere CartAdmin a OpenCart in due modalità:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            GuideStepItem(
                                stepNumber = "A",
                                title = "Metodo CartAdmin Bridge (consigliato)",
                                description = "Installa cartadmin.ocmod.zip, apri CartAdmin Bridge nel pannello OpenCart e genera il token. Non modificare file PHP manualmente."
                            )

                            GuideStepItem(
                                stepNumber = "B",
                                title = "Metodo API Nativa OpenCart",
                                description = "Nel pannello OpenCart vai su Sistema > Utenti > API. Crea un utente API, assegna la chiave e aggiungi l'IP del tuo dispositivo."
                            )
                        }
                    }
                }
            }

            // Maintenance & Full Sync Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardSurfacePure)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "MANUTENZIONE & SINCRONIZZAZIONE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            cacheClearedMessage = "Cache VQMod/OCMod e Twig template svuotate con successo!"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_cache_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Svuota Cache OpenCart (OCMod / Twig)")
                    }

                    if (cacheClearedMessage != null) {
                        Text(
                            text = cacheClearedMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TrendGreen
                        )
                    }

                    var showClearDummyDialog by remember { mutableStateOf(false) }

                    if (showClearDummyDialog) {
                        AlertDialog(
                            onDismissRequest = { showClearDummyDialog = false },
                            title = {
                                Text(
                                    text = "Eliminare i Dati Dimostrativi?",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            text = {
                                Text(
                                    text = "Questa azione rimuoverà tutti gli ordini, prodotti e categorie di esempio dal database locale, lasciando l'app pronta per collegarsi al tuo store OpenCart reale.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onClearDummyData()
                                        showClearDummyDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White)
                                ) {
                                    Text("Elimina Dati Finti")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearDummyDialog = false }) {
                                    Text("Annulla")
                                }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = { showClearDummyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_dummy_data_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AlertRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rimuovi Dati Dimostrativi (Reset Dati)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = {
                            onTriggerSync(
                                storeUrl,
                                apiKey.ifBlank { currentStore?.apiKey.orEmpty() },
                                apiUsername
                            )
                        },
                        enabled = currentStore != null && storeUrl.isNotBlank() &&
                            (apiKey.isNotBlank() || currentStore.apiKey.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("full_sync_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sincronizza Ora Ordini & Catalogo",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // SICUREZZA & PASSWORD DI ACCESSO (BANKING-GRADE)
            item {
                val authStatus = remember { securityManager.evaluateAuthStatus() }
                var isBiometricPrefEnabled by remember { mutableStateOf(securityManager.isBiometricEnabledByUser()) }
                var showChangePwdDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardSurfacePure)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "SICUREZZA & AUTENTICAZIONE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TrendGreenLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Crittografia Hardware TEE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = TrendGreen
                            )
                        }
                    }

                    Text(
                        text = "I dati e i token API OpenCart sono protetti con crittografia hardware AES-256 GCM (AndroidKeyStore). L'accesso richiede autenticazione ad ogni apertura o dopo 5 minuti di inattività.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // TOGGLE "Usa Biometrico"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (isBiometricPrefEnabled) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Usa biometrico",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (securityManager.isHardwareBiometricAvailable()) {
                                        "Accedi con impronta o volto senza digitare la password"
                                    } else {
                                        "Biometria hardware non rilevata o non registrata sul dispositivo"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isBiometricPrefEnabled,
                            onCheckedChange = { checked ->
                                isBiometricPrefEnabled = checked
                                securityManager.setBiometricEnabledByUser(checked)
                                Toast.makeText(
                                    context,
                                    if (checked) "Accesso biometrico rapido abilitato" else "Accesso biometrico disabilitato (richiesta password)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            enabled = securityManager.isHardwareBiometricAvailable(),
                            modifier = Modifier.testTag("biometric_auth_switch")
                        )
                    }

                    // Informazioni Operatore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Operatore: ${securityManager.getOperatorUsername()}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Timeout sessione: 5 min",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = ThemePrimary
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showChangePwdDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("change_security_password_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modifica Password di Accesso", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (showChangePwdDialog) {
                    var currentEnteredPwd by remember { mutableStateOf("") }
                    var newPwd by remember { mutableStateOf("") }
                    var confirmNewPwd by remember { mutableStateOf("") }
                    var pwdErrorMsg by remember { mutableStateOf<String?>(null) }

                    AlertDialog(
                        onDismissRequest = { showChangePwdDialog = false },
                        title = {
                            Text("Modifica Password Operatore", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (securityManager.isPasswordSet()) {
                                    OutlinedTextField(
                                        value = currentEnteredPwd,
                                        onValueChange = { currentEnteredPwd = it },
                                        label = { Text("Password Attuale") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                OutlinedTextField(
                                    value = newPwd,
                                    onValueChange = { newPwd = it },
                                    label = { Text("Nuova Password (min. 8 car, 1 Maiusc, 1 Num, 1 Simbolo)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = confirmNewPwd,
                                    onValueChange = { confirmNewPwd = it },
                                    label = { Text("Conferma Nuova Password") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (pwdErrorMsg != null) {
                                    Text(
                                        text = pwdErrorMsg ?: "",
                                        color = AlertRed,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (securityManager.isPasswordSet() && !securityManager.verifyPassword(currentEnteredPwd)) {
                                        pwdErrorMsg = "Password attuale errata."
                                        return@Button
                                    }
                                    if (newPwd != confirmNewPwd) {
                                        pwdErrorMsg = "Le nuove password non corrispondono."
                                        return@Button
                                    }
                                    val valCheck = securityManager.validatePasswordStrength(newPwd)
                                    if (!valCheck.isValid) {
                                        pwdErrorMsg = valCheck.message
                                        return@Button
                                    }
                                    securityManager.setPassword(newPwd)
                                    Toast.makeText(context, "Password aggiornata con successo!", Toast.LENGTH_SHORT).show()
                                    showChangePwdDialog = false
                                }
                            ) {
                                Text("Salva")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showChangePwdDialog = false }) {
                                Text("Annulla")
                            }
                        }
                    )
                }
            }

            // OPENCART ITALIA by SOLO SOLUZIONI Official Info Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardSurfacePure)
                        .border(1.5.dp, ThemePrimary.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                        .testTag("opencart_italia_official_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ThemePrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = ThemePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "OpenCart ITALIA",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "by SOLO SOLUZIONI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = ThemePrimary,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = "Versione installata ${BuildConfig.VERSION_NAME} • build ${BuildConfig.VERSION_CODE}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("installed_release_version")
                                )
                            }
                        }

                        Text(
                            text = "CartAdmin è l'applicazione ufficiale di OpenCart ITALIA sviluppata e supportata da SOLO SOLUZIONI. Per assistenza, moduli personalizzati, consulenza o sviluppo e-commerce visita i nostri siti web ufficiali:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenAudit,
                                modifier = Modifier.weight(1f).height(46.dp).testTag("open_audit_from_config"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Audit", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onOpenLicense,
                                modifier = Modifier.weight(1f).height(46.dp).testTag("open_license_from_credits"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Licenza", fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.opencartitalia.it"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemePrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OpenCart Italia", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.solosoluzioni.it"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solo Soluzioni", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // OpenCart Module Viewer Modal Sheet
        if (showModuleSheet) {
            OpenCartModuleSheet(onDismiss = { showModuleSheet = false })
        }
    }
}

@Composable
private fun GuideStepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(ThemePrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = ThemeOnPrimaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
