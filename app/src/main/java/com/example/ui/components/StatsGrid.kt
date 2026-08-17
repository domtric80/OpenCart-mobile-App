package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurfaceLight
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimaryContainer

@Composable
fun StatsGrid(
    pendingOrdersCount: Int,
    stockAlertsCount: Int,
    onPendingOrdersClick: () -> Unit,
    onStockAlertsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pending Orders Card (HTML exact match: bg-[#EADDFF] rounded-[28px] h-32)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(132.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(ThemePrimaryContainer)
                .clickable(onClick = onPendingOrdersClick)
                .padding(16.dp)
                .testTag("pending_orders_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "Pending Orders",
                    tint = ThemeOnPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )

                Column(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = pendingOrdersCount.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = ThemeOnPrimaryContainer
                    )
                    Text(
                        text = "Pending Orders",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = ThemeOnPrimaryContainer
                    )
                }
            }
        }

        // Stock Alerts Card (HTML exact match: bg-[#F7F2FA] border border-[#CAC4D0] rounded-[28px] h-32)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(132.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(CardSurfaceLight)
                .border(1.dp, ThemeOutlineVariant, RoundedCornerShape(28.dp))
                .clickable(onClick = onStockAlertsClick)
                .padding(16.dp)
                .testTag("stock_alerts_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Stock Alerts",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )

                Column(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stockAlertsCount.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Stock Alerts",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
