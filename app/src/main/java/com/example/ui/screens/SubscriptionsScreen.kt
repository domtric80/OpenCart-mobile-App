package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Subscription
import com.example.model.SubscriptionStatus
import com.example.ui.theme.CardSurfaceLight
import com.example.ui.theme.CardSurfacePure
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusAlertRedBg
import com.example.ui.theme.StatusConfirmedBlue
import com.example.ui.theme.StatusConfirmedBlueBg
import com.example.ui.theme.StatusDeliveredGreen
import com.example.ui.theme.StatusDeliveredGreenBg
import com.example.ui.theme.StatusPendingGold
import com.example.ui.theme.StatusPendingGoldBg
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer
import java.util.Locale

@Composable
fun SubscriptionsScreen(
    subscriptions: List<Subscription>,
    selectedFilter: SubscriptionStatus?,
    onSelectFilter: (SubscriptionStatus?) -> Unit,
    onUpdateStatus: (String, SubscriptionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubscriptionForManage by remember { mutableStateOf<Subscription?>(null) }

    val activeCount = subscriptions.count { it.status == SubscriptionStatus.ACTIVE }
    val suspendedCount = subscriptions.count { it.status == SubscriptionStatus.SUSPENDED }
    val expiredCount = subscriptions.count { it.status == SubscriptionStatus.EXPIRED || it.status == SubscriptionStatus.CANCELED }

    val filteredSubscriptions = subscriptions.filter { sub ->
        val matchesFilter = selectedFilter == null || sub.status == selectedFilter
        val matchesSearch = searchQuery.isBlank() ||
                sub.subscriptionId.contains(searchQuery, ignoreCase = true) ||
                sub.customerName.contains(searchQuery, ignoreCase = true) ||
                sub.customerEmail.contains(searchQuery, ignoreCase = true) ||
                sub.planName.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Summary KPI Banner
        SubscriptionKpiBanner(
            totalActive = activeCount,
            totalSuspended = suspendedCount,
            totalOther = expiredCount,
            monthlyRevenue = subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }.sumOf { it.amount }
        )

        // Search Bar
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subscriptions_search_field"),
                placeholder = {
                    Text(
                        "Cerca abbonamento, cliente, piano o email...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cerca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancella ricerca",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Status Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                SubscriptionFilterChip(
                    label = "Tutti (${subscriptions.size})",
                    isSelected = selectedFilter == null,
                    icon = Icons.Default.FilterList,
                    onClick = { onSelectFilter(null) }
                )
            }
            item {
                SubscriptionFilterChip(
                    label = "Attivi ($activeCount)",
                    isSelected = selectedFilter == SubscriptionStatus.ACTIVE,
                    icon = Icons.Default.CheckCircle,
                    badgeColor = StatusDeliveredGreen,
                    onClick = { onSelectFilter(SubscriptionStatus.ACTIVE) }
                )
            }
            item {
                SubscriptionFilterChip(
                    label = "Sospesi ($suspendedCount)",
                    isSelected = selectedFilter == SubscriptionStatus.SUSPENDED,
                    icon = Icons.Default.PauseCircle,
                    badgeColor = StatusPendingGold,
                    onClick = { onSelectFilter(SubscriptionStatus.SUSPENDED) }
                )
            }
            item {
                SubscriptionFilterChip(
                    label = "Scaduti/Annullati ($expiredCount)",
                    isSelected = selectedFilter == SubscriptionStatus.CANCELED || selectedFilter == SubscriptionStatus.EXPIRED,
                    icon = Icons.Default.Cancel,
                    badgeColor = StatusAlertRed,
                    onClick = { onSelectFilter(SubscriptionStatus.CANCELED) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Subscription list
        if (filteredSubscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Nessun abbonamento",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedFilter != null)
                            "Nessun abbonamento corrisponde ai filtri impostati"
                        else
                            "Nessun abbonamento ricorrente registrato",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSubscriptions, key = { it.id }) { subscription ->
                    SubscriptionItemCard(
                        subscription = subscription,
                        onManageClick = {
                            selectedSubscriptionForManage = subscription
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal dialog to manage / toggle subscription status
    selectedSubscriptionForManage?.let { sub ->
        SubscriptionManageDialog(
            subscription = sub,
            onDismiss = { selectedSubscriptionForManage = null },
            onUpdateStatus = { newStatus ->
                onUpdateStatus(sub.id, newStatus)
                selectedSubscriptionForManage = null
            }
        )
    }
}

@Composable
private fun SubscriptionKpiBanner(
    totalActive: Int,
    totalSuspended: Int,
    totalOther: Int,
    monthlyRevenue: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ThemePrimaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = ThemeOnPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Abbonamenti & Ricorrenze",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalActive attivi • $totalSuspended in pausa • Ricorrente: €${String.format(Locale.ITALIAN, "%.2f", monthlyRevenue)}/ciclo",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeOnPrimaryContainer.copy(alpha = 0.85f)
                )
            }
            Surface(
                shape = CircleShape,
                color = ThemePrimary,
                contentColor = Color.White
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$totalActive ON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionFilterChip(
    label: String,
    isSelected: Boolean,
    icon: ImageVector,
    badgeColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) ThemePrimary else CardSurfacePure,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ThemeOutlineVariant),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (badgeColor != null && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SubscriptionItemCard(
    subscription: Subscription,
    onManageClick: () -> Unit
) {
    val (statusLabel, statusColor, statusBg) = when (subscription.status) {
        SubscriptionStatus.ACTIVE -> Triple("Attivo", StatusDeliveredGreen, StatusDeliveredGreenBg)
        SubscriptionStatus.PENDING -> Triple("In Attesa", StatusPendingGold, StatusPendingGoldBg)
        SubscriptionStatus.SUSPENDED -> Triple("Sospeso", StatusPendingGold, StatusPendingGoldBg)
        SubscriptionStatus.CANCELED -> Triple("Annullato", StatusAlertRed, StatusAlertRedBg)
        SubscriptionStatus.EXPIRED -> Triple("Scaduto", MaterialTheme.colorScheme.outline, CardSurfaceLight)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onManageClick() }
            .testTag("subscription_card_${subscription.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        border = androidx.compose.foundation.BorderStroke(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: ID, Plan Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.subscriptionId,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemePrimary
                    )
                    Text(
                        text = subscription.planName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Row 2: Customer & Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${subscription.customerName} (${subscription.customerEmail})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Row 3: Cycle & Next Payment & Price
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardSurfaceLight)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = ThemePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${subscription.cycleFrequency} • Prossimo: ${subscription.nextPaymentDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "€${String.format(Locale.ITALIAN, "%.2f", subscription.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThemePrimary
                )
            }

            // Row 4: Payment method and Quick action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subscription.paymentMethod,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = "Gestisci stato >",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThemePrimary
                )
            }
        }
    }
}

@Composable
fun SubscriptionManageDialog(
    subscription: Subscription,
    onDismiss: () -> Unit,
    onUpdateStatus: (SubscriptionStatus) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Gestisci Abbonamento ${subscription.subscriptionId}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Piano: ${subscription.planName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Cliente: ${subscription.customerName} (${subscription.customerEmail})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Importo ricorrente: €${String.format(Locale.ITALIAN, "%.2f", subscription.amount)} / ${subscription.cycleFrequency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Stato attuale: ${subscription.status.name}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemePrimary
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Seleziona nuovo stato:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (subscription.status != SubscriptionStatus.ACTIVE) {
                        Button(
                            onClick = { onUpdateStatus(SubscriptionStatus.ACTIVE) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusDeliveredGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Attiva", fontSize = 12.sp)
                        }
                    }

                    if (subscription.status != SubscriptionStatus.SUSPENDED) {
                        Button(
                            onClick = { onUpdateStatus(SubscriptionStatus.SUSPENDED) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusPendingGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sospendi", fontSize = 12.sp)
                        }
                    }

                    if (subscription.status != SubscriptionStatus.CANCELED) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(SubscriptionStatus.CANCELED) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlertRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Annulla", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}
