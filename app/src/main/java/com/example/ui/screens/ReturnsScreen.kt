package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.example.model.OrderReturn
import com.example.model.ReturnStatus
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
import com.example.ui.theme.StatusProcessingPurple
import com.example.ui.theme.StatusProcessingPurpleBg
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer

@Composable
fun ReturnsScreen(
    returns: List<OrderReturn>,
    selectedFilter: ReturnStatus?,
    onSelectFilter: (ReturnStatus?) -> Unit,
    onUpdateStatus: (String, ReturnStatus, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedReturnForManage by remember { mutableStateOf<OrderReturn?>(null) }

    val pendingCount = returns.count { it.status == ReturnStatus.PENDING }
    val awaitingCount = returns.count { it.status == ReturnStatus.AWAITING_PRODUCTS }
    val completedCount = returns.count { it.status == ReturnStatus.COMPLETE_REFUNDED || it.status == ReturnStatus.COMPLETE_REPLACED }
    val deniedCount = returns.count { it.status == ReturnStatus.DENIED }

    val filteredReturns = returns.filter { ret ->
        val matchesFilter = selectedFilter == null || ret.status == selectedFilter
        val matchesSearch = searchQuery.isBlank() ||
                ret.returnId.contains(searchQuery, ignoreCase = true) ||
                ret.orderId.contains(searchQuery, ignoreCase = true) ||
                ret.customerName.contains(searchQuery, ignoreCase = true) ||
                ret.productName.contains(searchQuery, ignoreCase = true) ||
                ret.reason.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Summary KPI Banner
        ReturnsKpiBanner(
            totalPending = pendingCount,
            totalAwaiting = awaitingCount,
            totalCompleted = completedCount,
            totalReturns = returns.size
        )

        // Search Bar
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("returns_search_field"),
                placeholder = {
                    Text(
                        "Cerca RMA, ordine, cliente, prodotto o motivo...",
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
                ReturnFilterChip(
                    label = "Tutti (${returns.size})",
                    isSelected = selectedFilter == null,
                    icon = Icons.Default.FilterList,
                    onClick = { onSelectFilter(null) }
                )
            }
            item {
                ReturnFilterChip(
                    label = "Da Verificare ($pendingCount)",
                    isSelected = selectedFilter == ReturnStatus.PENDING,
                    icon = Icons.Default.HourglassTop,
                    badgeColor = StatusPendingGold,
                    onClick = { onSelectFilter(ReturnStatus.PENDING) }
                )
            }
            item {
                ReturnFilterChip(
                    label = "In Arrivo Merce ($awaitingCount)",
                    isSelected = selectedFilter == ReturnStatus.AWAITING_PRODUCTS,
                    icon = Icons.Default.LocalShipping,
                    badgeColor = StatusConfirmedBlue,
                    onClick = { onSelectFilter(ReturnStatus.AWAITING_PRODUCTS) }
                )
            }
            item {
                ReturnFilterChip(
                    label = "Completati ($completedCount)",
                    isSelected = selectedFilter == ReturnStatus.COMPLETE_REFUNDED || selectedFilter == ReturnStatus.COMPLETE_REPLACED,
                    icon = Icons.Default.CheckCircle,
                    badgeColor = StatusDeliveredGreen,
                    onClick = { onSelectFilter(ReturnStatus.COMPLETE_REFUNDED) }
                )
            }
            item {
                ReturnFilterChip(
                    label = "Rifiutati ($deniedCount)",
                    isSelected = selectedFilter == ReturnStatus.DENIED,
                    icon = Icons.Default.Cancel,
                    badgeColor = StatusAlertRed,
                    onClick = { onSelectFilter(ReturnStatus.DENIED) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Return List
        if (filteredReturns.isEmpty()) {
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
                        imageVector = Icons.Default.AssignmentReturn,
                        contentDescription = "Nessun reso",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedFilter != null)
                            "Nessun reso corrisponde ai filtri impostati"
                        else
                            "Nessun reso o RMA registrato",
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
                items(filteredReturns, key = { it.id }) { ret ->
                    ReturnItemCard(
                        orderReturn = ret,
                        onManageClick = {
                            selectedReturnForManage = ret
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal dialog to manage / update RMA return status
    selectedReturnForManage?.let { ret ->
        ReturnManageDialog(
            orderReturn = ret,
            onDismiss = { selectedReturnForManage = null },
            onUpdateStatus = { newStatus, newAction ->
                onUpdateStatus(ret.id, newStatus, newAction)
                selectedReturnForManage = null
            }
        )
    }
}

@Composable
private fun ReturnsKpiBanner(
    totalPending: Int,
    totalAwaiting: Int,
    totalCompleted: Int,
    totalReturns: Int
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
                        imageVector = Icons.Default.AssignmentReturn,
                        contentDescription = null,
                        tint = ThemeOnPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gestione Resi RMA",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalPending da autorizzare • $totalAwaiting in transito • $totalCompleted chiusi",
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
                        text = "$totalReturns RMA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReturnFilterChip(
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
fun ReturnItemCard(
    orderReturn: OrderReturn,
    onManageClick: () -> Unit
) {
    val (statusLabel, statusColor, statusBg) = when (orderReturn.status) {
        ReturnStatus.PENDING -> Triple("In Attesa", StatusPendingGold, StatusPendingGoldBg)
        ReturnStatus.AWAITING_PRODUCTS -> Triple("In Arrivo Merce", StatusConfirmedBlue, StatusConfirmedBlueBg)
        ReturnStatus.IN_INSPECTION -> Triple("In Verifica", StatusProcessingPurple, StatusProcessingPurpleBg)
        ReturnStatus.COMPLETE_REFUNDED -> Triple("Rimborsato", StatusDeliveredGreen, StatusDeliveredGreenBg)
        ReturnStatus.COMPLETE_REPLACED -> Triple("Sostituito", StatusProcessingPurple, StatusProcessingPurpleBg)
        ReturnStatus.DENIED -> Triple("Rifiutato", StatusAlertRed, StatusAlertRedBg)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onManageClick() }
            .testTag("return_card_${orderReturn.id}"),
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
            // Row 1: Return ID, Order ID & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = orderReturn.returnId,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemePrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "per Ordine ${orderReturn.orderId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Data: ${orderReturn.dateAdded}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
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

            // Row 2: Customer name & contacts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${orderReturn.customerName} • ${orderReturn.customerEmail} ${if (orderReturn.customerPhone.isNotBlank()) "• ${orderReturn.customerPhone}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Row 3: Product Info & Reason
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardSurfaceLight)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = ThemePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${orderReturn.quantity}x ${orderReturn.productName} (${orderReturn.productModel})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Motivo: ${orderReturn.reason} ${if (orderReturn.opened) "(Confezione Aperta)" else "(Sigillato)"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Row 4: Action / Next Step and Quick action trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Azione: ${orderReturn.action}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Gestisci RMA >",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThemePrimary
                )
            }
        }
    }
}

@Composable
fun ReturnManageDialog(
    orderReturn: OrderReturn,
    onDismiss: () -> Unit,
    onUpdateStatus: (ReturnStatus, String) -> Unit
) {
    var actionComment by remember { mutableStateOf(orderReturn.action) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Gestisci Reso ${orderReturn.returnId}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Prodotto: ${orderReturn.quantity}x ${orderReturn.productName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Cliente: ${orderReturn.customerName} (${orderReturn.customerEmail})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Motivo reso: ${orderReturn.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = actionComment,
                    onValueChange = { actionComment = it },
                    label = { Text("Nota / Azione RMA") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Imposta nuovo stato:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onUpdateStatus(ReturnStatus.AWAITING_PRODUCTS, actionComment.ifBlank { "In attesa merce" }) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusConfirmedBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("In Arrivo", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onUpdateStatus(ReturnStatus.COMPLETE_REFUNDED, actionComment.ifBlank { "Rimborso emesso" }) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDeliveredGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Rimborsa", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onUpdateStatus(ReturnStatus.COMPLETE_REPLACED, actionComment.ifBlank { "Sostituzione inviata" }) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusProcessingPurple),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sostituisci", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = { onUpdateStatus(ReturnStatus.DENIED, actionComment.ifBlank { "Reso respinto / non idoneo" }) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlertRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Respingi / Nega Reso", fontSize = 12.sp)
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
