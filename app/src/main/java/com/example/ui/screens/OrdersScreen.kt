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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.OrdersSubSection
import com.example.model.Subscription
import com.example.model.SubscriptionStatus
import com.example.model.OrderReturn
import com.example.model.ReturnStatus
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.example.ui.theme.StatusShippedGreen
import com.example.ui.theme.StatusShippedGreenBg
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer
import com.example.ui.theme.ThemeSecondaryContainer

@Composable
fun OrdersScreen(
    orders: List<Order>,
    selectedFilter: OrderStatus?,
    onSelectFilter: (OrderStatus?) -> Unit,
    onOrderClick: (Order) -> Unit,
    modifier: Modifier = Modifier,
    subSection: OrdersSubSection = OrdersSubSection.ORDERS,
    onSubSectionChange: (OrdersSubSection) -> Unit = {},
    onOpenSubSectionMenu: () -> Unit = {},
    subscriptions: List<Subscription> = emptyList(),
    selectedSubscriptionFilter: SubscriptionStatus? = null,
    onSelectSubscriptionFilter: (SubscriptionStatus?) -> Unit = {},
    onUpdateSubscriptionStatus: (String, SubscriptionStatus) -> Unit = { _, _ -> },
    returns: List<OrderReturn> = emptyList(),
    selectedReturnFilter: ReturnStatus? = null,
    onSelectReturnFilter: (ReturnStatus?) -> Unit = {},
    onUpdateReturnStatus: (String, ReturnStatus, String) -> Unit = { _, _, _ -> }
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Sub-Section Selector Tabs (Ordini | Abbonamenti | Resi)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented pill 1: Ordini
            val isOrders = subSection == OrdersSubSection.ORDERS
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isOrders) MaterialTheme.colorScheme.primary else CardSurfacePure,
                border = if (isOrders) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (isOrders) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSubSectionChange(OrdersSubSection.ORDERS) }
                    .testTag("tab_sub_orders")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isOrders) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ordini (${orders.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isOrders) FontWeight.Bold else FontWeight.Medium,
                        color = if (isOrders) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }

            // Segmented pill 2: Abbonamenti
            val isSubs = subSection == OrdersSubSection.SUBSCRIPTIONS
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSubs) MaterialTheme.colorScheme.primary else CardSurfacePure,
                border = if (isSubs) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (isSubs) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSubSectionChange(OrdersSubSection.SUBSCRIPTIONS) }
                    .testTag("tab_sub_subscriptions")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSubs) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Abbonati (${subscriptions.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSubs) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSubs) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                }
            }

            // Segmented pill 3: Resi
            val isReturns = subSection == OrdersSubSection.RETURNS
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isReturns) MaterialTheme.colorScheme.primary else CardSurfacePure,
                border = if (isReturns) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (isReturns) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(0.9f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSubSectionChange(OrdersSubSection.RETURNS) }
                    .testTag("tab_sub_returns")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentReturn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isReturns) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Resi (${returns.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isReturns) FontWeight.Bold else FontWeight.Medium,
                        color = if (isReturns) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick Menu Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardSurfacePure,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenSubSectionMenu() }
                    .testTag("btn_orders_menu")
            ) {
                Box(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu sezioni ordini",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // SubSection Content Switcher
        when (subSection) {
            OrdersSubSection.ORDERS -> {
                OrdersListContent(
                    orders = orders,
                    selectedFilter = selectedFilter,
                    onSelectFilter = onSelectFilter,
                    onOrderClick = onOrderClick
                )
            }
            OrdersSubSection.SUBSCRIPTIONS -> {
                SubscriptionsScreen(
                    subscriptions = subscriptions,
                    selectedFilter = selectedSubscriptionFilter,
                    onSelectFilter = onSelectSubscriptionFilter,
                    onUpdateStatus = onUpdateSubscriptionStatus
                )
            }
            OrdersSubSection.RETURNS -> {
                ReturnsScreen(
                    returns = returns,
                    selectedFilter = selectedReturnFilter,
                    onSelectFilter = onSelectReturnFilter,
                    onUpdateStatus = onUpdateReturnStatus
                )
            }
        }
    }
}

@Composable
private fun OrdersListContent(
    orders: List<Order>,
    selectedFilter: OrderStatus?,
    onSelectFilter: (OrderStatus?) -> Unit,
    onOrderClick: (Order) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val pendingCount = orders.count { it.status == OrderStatus.PENDING }
    val shippedCount = orders.count { it.status == OrderStatus.SHIPPED }
    val deliveredCount = orders.count { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.COMPLETE }
    val processingCount = orders.count { it.status == OrderStatus.PROCESSING }

    val filteredOrders = orders.filter { order ->
        val matchesFilter = selectedFilter == null || order.status == selectedFilter
        val matchesSearch = searchQuery.isBlank() ||
                order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                order.customerEmail.contains(searchQuery, ignoreCase = true) ||
                (order.notes != null && order.notes.contains(searchQuery, ignoreCase = true))
        matchesFilter && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title & Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ordini Recenti",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gestione stato ordini e note clienti OpenCart",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ThemePrimary.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${orders.size} ordini",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ThemePrimary
                    )
                )
            }
        }

        // Quick Status KPI Summary Bar (Pending, Shipped, Delivered, Processing)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusKpiCard(
                title = "Pending",
                count = pendingCount,
                icon = Icons.Default.Schedule,
                iconColor = StatusPendingGold,
                bgColor = StatusPendingGoldBg,
                isSelected = selectedFilter == OrderStatus.PENDING,
                onClick = {
                    onSelectFilter(if (selectedFilter == OrderStatus.PENDING) null else OrderStatus.PENDING)
                },
                modifier = Modifier.weight(1f)
            )

            StatusKpiCard(
                title = "Shipped",
                count = shippedCount,
                icon = Icons.Default.LocalShipping,
                iconColor = StatusShippedGreen,
                bgColor = StatusShippedGreenBg,
                isSelected = selectedFilter == OrderStatus.SHIPPED,
                onClick = {
                    onSelectFilter(if (selectedFilter == OrderStatus.SHIPPED) null else OrderStatus.SHIPPED)
                },
                modifier = Modifier.weight(1f)
            )

            StatusKpiCard(
                title = "Delivered",
                count = deliveredCount,
                icon = Icons.Default.CheckCircle,
                iconColor = StatusDeliveredGreen,
                bgColor = StatusDeliveredGreenBg,
                isSelected = selectedFilter == OrderStatus.DELIVERED,
                onClick = {
                    onSelectFilter(if (selectedFilter == OrderStatus.DELIVERED) null else OrderStatus.DELIVERED)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cerca per #ordine, cliente o note...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Cancella ricerca",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("orders_search_input"),
            shape = RoundedCornerShape(20.dp),
            singleLine = true
        )

        // Status Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isSelected = selectedFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelectFilter(null) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("filter_all_orders"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tutti (${orders.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(OrderStatus.entries) { status ->
                val isSelected = selectedFilter == status
                val count = orders.count { it.status == status }
                val (statusBg, statusFg) = getStatusColorPair(status)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) statusBg
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) statusFg else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectFilter(status) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("filter_${status.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${status.englishLabel} ($count)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = if (isSelected) statusFg
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Orders List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Nessun ordine trovato con questi criteri.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredOrders) { order ->
                    OrderListItem(
                        order = order,
                        onClick = { onOrderClick(order) }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatusKpiCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) bgColor else CardSurfacePure)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) iconColor else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderListItem(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusBg, statusFg) = getStatusColorPair(order.status)
    val statusIcon = getStatusIcon(order.status)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurfacePure)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("order_item_${order.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Row: Order Number, Status Indicator Badge & Total Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = order.orderNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Distinct Status Indicator Badge with Icon
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusFg,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${order.status.englishLabel} • ${order.status.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = statusFg
                    )
                }
            }

            Text(
                text = "€%.2f".format(order.total),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = ThemePrimary
            )
        }

        // Customer Info & Date Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar Circle with Customer Initial
                val initial = order.customerName.firstOrNull()?.toString() ?: "U"
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ThemeSecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemePrimary
                        )
                    )
                }

                Column {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${order.itemsCount} articoli • ${order.shippingMethod}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = order.dateAdded,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Customer / Order Notes Snippet if present
        if (!order.notes.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardSurfaceLight)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAlt,
                    contentDescription = null,
                    tint = StatusPendingGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Note: ${order.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Action Row with "Dettagli & Modifica"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = ThemePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Modifica stato e note",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ThemePrimary,
                        fontSize = 11.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = ThemePrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun getStatusColorPair(status: OrderStatus): Pair<Color, Color> {
    return when (status) {
        OrderStatus.PENDING -> StatusPendingGoldBg to StatusPendingGold
        OrderStatus.PROCESSING -> StatusProcessingPurpleBg to StatusProcessingPurple
        OrderStatus.CONFIRMED -> StatusConfirmedBlueBg to StatusConfirmedBlue
        OrderStatus.SHIPPED -> StatusShippedGreenBg to StatusShippedGreen
        OrderStatus.DELIVERED, OrderStatus.COMPLETE -> StatusDeliveredGreenBg to StatusDeliveredGreen
        OrderStatus.CANCELLED -> StatusAlertRedBg to StatusAlertRed
    }
}

private fun getStatusIcon(status: OrderStatus): ImageVector {
    return when (status) {
        OrderStatus.PENDING -> Icons.Default.Schedule
        OrderStatus.PROCESSING -> Icons.Default.Autorenew
        OrderStatus.CONFIRMED -> Icons.Default.Verified
        OrderStatus.SHIPPED -> Icons.Default.LocalShipping
        OrderStatus.DELIVERED, OrderStatus.COMPLETE -> Icons.Default.CheckCircle
        OrderStatus.CANCELLED -> Icons.Default.Cancel
    }
}
