package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Order
import com.example.model.OrderDetail
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.security.ExplicitExternalIntentFactory
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
import com.example.ui.theme.ThemeSecondary

/**
 * Detailed view component displaying complete order information (items list, shipping/billing
 * addresses, financial breakdown, and live Room local cache status) with interactive status actions and note editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailSheet(
    orderDetail: OrderDetail,
    onStatusChange: (OrderStatus) -> Unit,
    onSaveNotes: (String) -> Unit = {},
    onUpdateOrder: (OrderStatus, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val order = orderDetail.order

    var currentNotes by remember(orderDetail.customerNotes, order.notes) {
        mutableStateOf(orderDetail.customerNotes ?: order.notes ?: "")
    }
    var selectedStatus by remember(order.status) {
        mutableStateOf(order.status)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ordine ${order.orderNumber}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Inserito: ${order.dateAdded}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val shareText = "Ordine ${order.orderNumber}\nCliente: ${order.customerName}\nTotale: €%.2f\nStato: ${order.status.label}\nSpedizione: ${orderDetail.shippingAddress}".format(order.total)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Condividi riepilogo ordine"))
                        },
                        modifier = Modifier.testTag("share_order_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Condividi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_order_detail")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Room Database Local Cache Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ThemePrimary.copy(alpha = 0.08f))
                    .border(1.dp, ThemePrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = ThemePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Cache Locale Room attiva • Accesso offline disponibile",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = ThemePrimary
                )
            }

            // Hero Summary Card with Total & Status
            val (statusBg, statusFg) = when (selectedStatus) {
                OrderStatus.PENDING -> StatusPendingGoldBg to StatusPendingGold
                OrderStatus.PROCESSING -> StatusProcessingPurpleBg to StatusProcessingPurple
                OrderStatus.CONFIRMED -> StatusConfirmedBlueBg to StatusConfirmedBlue
                OrderStatus.SHIPPED -> StatusShippedGreenBg to StatusShippedGreen
                OrderStatus.DELIVERED, OrderStatus.COMPLETE -> StatusDeliveredGreenBg to StatusDeliveredGreen
                OrderStatus.CANCELLED -> StatusAlertRedBg to StatusAlertRed
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ThemePrimaryContainer)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTALE COMPLESSIVO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            ),
                            color = ThemeOnPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "€%.2f".format(orderDetail.grandTotal.takeIf { it > 0 } ?: order.total),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ),
                            color = ThemeOnPrimaryContainer
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = order.status.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = statusFg
                        )
                    }
                }
            }

            // Section: Articoli Ordinati (Items List)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARTICOLI ORDINATI (${orderDetail.items.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${orderDetail.items.sumOf { it.quantity }.coerceAtLeast(order.itemsCount)} pezzi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (orderDetail.items.isNotEmpty()) {
                    orderDetail.items.forEach { item ->
                        OrderItemRow(item = item)
                    }
                } else {
                    // Fallback item card if items list is single
                    OrderItemRow(
                        item = OrderItem(
                            id = "default_${order.id}",
                            orderId = order.id,
                            productId = "prod_oc",
                            name = "Articolo da catalogo OpenCart",
                            model = "OC-${order.orderNumber.replace("#", "")}",
                            quantity = order.itemsCount.coerceAtLeast(1),
                            price = order.total / order.itemsCount.coerceAtLeast(1),
                            total = order.total
                        )
                    )
                }
            }

            // Section: Spedizione & Cliente
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SPEDIZIONE & CLIENTE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Customer Info Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurfaceLight)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ThemePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ThemePrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = order.customerName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = order.customerEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (orderDetail.customerPhone.isNotBlank()) {
                                Text(
                                    text = orderDetail.customerPhone,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Contact action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cleanPhone = orderDetail.customerPhone.filter { it.isDigit() || it == '+' }
                                if (cleanPhone.isNotBlank()) {
                                    val launched = ExplicitExternalIntentFactory.dial(context, cleanPhone)
                                    if (!launched) {
                                        Toast.makeText(context, "Nessuna app di composizione telefonica disponibile", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Numero telefonico non disponibile", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Chiama", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val cleanEmail = order.customerEmail.trim()
                                if (cleanEmail.isNotBlank() && cleanEmail.contains("@")) {
                                    val launched = ExplicitExternalIntentFactory.email(
                                        context,
                                        cleanEmail,
                                        "Assistenza Ordine ${order.orderNumber}"
                                    )
                                    if (!launched) {
                                        Toast.makeText(context, "Nessun client email disponibile sul dispositivo", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Indirizzo email non valido o mancante", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Email", fontSize = 12.sp)
                        }
                    }
                }

                // Shipping Address Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurfaceLight)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Indirizzo di Consegna",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = orderDetail.shippingAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = ThemeSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = order.shippingMethod,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Text(
                            text = if (orderDetail.shippingCost == 0.0) "Gratis" else "€%.2f".format(orderDetail.shippingCost),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Section: Pagamento & Fatturazione
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PAGAMENTO & FATTURAZIONE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurfaceLight)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ThemeSecondary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = ThemeSecondary)
                    }
                    Column {
                        Text(
                            text = "Metodo Selezionato",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            text = order.paymentMethod,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                // Customer Notes if present
                if (!orderDetail.customerNotes.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NOTE DEL CLIENTE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = orderDetail.customerNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section: Riepilogo Costi e Totali
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurfaceLight)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Riepilogo Importi",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                PriceRow(label = "Subtotale Articoli", value = "€%.2f".format(orderDetail.subtotal))
                PriceRow(label = "Spedizione", value = if (orderDetail.shippingCost == 0.0) "Gratis" else "€%.2f".format(orderDetail.shippingCost))
                PriceRow(label = "IVA (22% inclusa)", value = "€%.2f".format(orderDetail.taxAmount))
                if (orderDetail.discountAmount > 0) {
                    PriceRow(
                        label = "Sconto / Promozione",
                        value = "-€%.2f".format(orderDetail.discountAmount),
                        isDiscount = true
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Totale Finale",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "€%.2f".format(orderDetail.grandTotal.takeIf { it > 0 } ?: order.total),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ThemePrimary
                        )
                    )
                }
            }

            // Section: MODIFICA STATO ORDINE (ROOM DB & LIVE STORE)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardSurfaceLight)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = ThemePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Cambia Stato Ordine",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                val statusList = listOf(
                    OrderStatus.PENDING to ("Pending (In attesa)" to Icons.Default.Schedule),
                    OrderStatus.PROCESSING to ("Processing (In lavoraz.)" to Icons.Default.Autorenew),
                    OrderStatus.CONFIRMED to ("Confirmed (Confermato)" to Icons.Default.Verified),
                    OrderStatus.SHIPPED to ("Shipped (Spedito)" to Icons.Default.LocalShipping),
                    OrderStatus.DELIVERED to ("Delivered (Consegnato)" to Icons.Default.CheckCircle),
                    OrderStatus.CANCELLED to ("Cancelled (Annullato)" to Icons.Default.Cancel)
                )

                for (chunk in statusList.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { (st, pair) ->
                            val (lbl, ic) = pair
                            val isCurrent = selectedStatus == st
                            val (btnBg, btnFg) = when (st) {
                                OrderStatus.PENDING -> StatusPendingGoldBg to StatusPendingGold
                                OrderStatus.PROCESSING -> StatusProcessingPurpleBg to StatusProcessingPurple
                                OrderStatus.CONFIRMED -> StatusConfirmedBlueBg to StatusConfirmedBlue
                                OrderStatus.SHIPPED -> StatusShippedGreenBg to StatusShippedGreen
                                OrderStatus.DELIVERED, OrderStatus.COMPLETE -> StatusDeliveredGreenBg to StatusDeliveredGreen
                                OrderStatus.CANCELLED -> StatusAlertRedBg to StatusAlertRed
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrent) btnBg else CardSurfacePure)
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) btnFg else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedStatus = st
                                        onStatusChange(st)
                                        onUpdateOrder(st, currentNotes)
                                        Toast.makeText(context, "Stato aggiornato a ${st.englishLabel}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                                    .testTag("status_picker_${st.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = ic,
                                    contentDescription = null,
                                    tint = if (isCurrent) btnFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = st.englishLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isCurrent) btnFg else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Section: MODIFICA NOTE ORDINE (CUSTOMER & ADMIN NOTES)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardSurfaceLight)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = ThemePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Note Ordine & Istruzioni",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Aggiungi informazioni di tracking, istruzioni per il corriere o annotazioni di gestione.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Editable Notes Text Field
                OutlinedTextField(
                    value = currentNotes,
                    onValueChange = { currentNotes = it },
                    placeholder = { Text("Es. Consegna al piano 2, codice tracking corriere, note fattura...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_notes_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 6
                )

                // Quick Note Preset Chips
                Text(
                    text = "Preset Rapidi Note:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.outline
                )

                val notePresets = listOf(
                    "📦 Spedito con Tracking GLS attivo",
                    "📞 Cliente contattato via telefono",
                    "📍 Consegnare al portiere di turno",
                    "✅ Consegna completata e firmata"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    notePresets.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CardSurfacePure)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            currentNotes = if (currentNotes.isBlank()) preset else "$currentNotes • $preset"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Save Notes Button
                Button(
                    onClick = {
                        onSaveNotes(currentNotes)
                        onUpdateOrder(selectedStatus, currentNotes)
                        Toast.makeText(context, "Note e stato ordine salvati!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_order_notes_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salva Note & Stato Ordine",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Single item card in the order detail list.
 */
@Composable
private fun OrderItemRow(item: OrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurfaceLight)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ThemePrimary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = ThemePrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modello: ${item.model}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "${item.quantity}x €%.2f".format(item.price),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "€%.2f".format(item.total),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = if (isDiscount) StatusShippedGreen else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
