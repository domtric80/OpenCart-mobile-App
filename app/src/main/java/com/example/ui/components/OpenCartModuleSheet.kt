package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurfacePure
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer
import com.example.ui.theme.TrendGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCartModuleSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(ThemePrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ThemePrimary)
                    }
                    Column {
                        Text("CartAdmin Bridge", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text("Estensione per OpenCart 4.1.x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Chiudi") }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(TrendGreen.copy(alpha = 0.08f))
                    .border(1.dp, TrendGreen.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = TrendGreen)
                        Text("INSTALLAZIONE DAL PANNELLO OPENCART", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TrendGreen)
                    }
                    Text(
                        "Il bridge e il token si configurano esclusivamente dal pannello amministrativo. Non creare o modificare file PHP manualmente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/domtric80/OpenCart-mobile-App/releases")))
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Scarica cartadmin.ocmod.zip")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardSurfacePure)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("PROCEDURA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                StepRow("1", "Installa l’estensione", "In OpenCart apri Estensioni > Installer, carica cartadmin.ocmod.zip e completa l’installazione.")
                StepRow("2", "Apri CartAdmin Bridge", "Vai in Estensioni > Estensioni, seleziona Moduli, installa e apri CartAdmin Bridge.")
                StepRow("3", "Genera il token", "Genera il token dal pannello e copialo subito: nel database resta soltanto il suo hash non reversibile.")
                StepRow("4", "Collega l’app", "Inserisci URL, nome operatore e token nella configurazione dell’app, quindi esegui Test API.")
            }

            Text(
                "La copia del token salvata nell’app è cifrata con Android Keystore hardware-backed. La rotazione dal pannello invalida immediatamente il token precedente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StepRow(step: String, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(ThemePrimary), contentAlignment = Alignment.Center) {
            Text(step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
