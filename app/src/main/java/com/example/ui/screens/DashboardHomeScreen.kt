package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActivityItem
import com.example.model.SalesMetrics
import com.example.model.Store
import com.example.ui.Timeframe
import com.example.ui.components.HeroRevenueCard
import com.example.ui.components.RealtimeMetricsGrid
import com.example.ui.components.RecentActivitySection
import com.example.ui.components.SalesVelocityChart
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import com.example.model.VisitorRealtimeStats
import com.example.ui.components.StatsGrid
import com.example.ui.components.StoreSelectorCard
import com.example.ui.theme.CardSurfaceLight
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer
import com.example.ui.theme.TrendGreen
import com.example.ui.theme.TrendGreenLight

@Composable
fun DashboardHomeScreen(
    currentStore: Store?,
    activities: List<ActivityItem>,
    salesMetrics: SalesMetrics,
    visitorStats: VisitorRealtimeStats,
    selectedTimeframe: Timeframe,
    syncMessage: String?,
    onStoreClick: () -> Unit,
    onSelectTimeframe: (Timeframe) -> Unit,
    onPendingOrdersClick: () -> Unit,
    onStockAlertsClick: () -> Unit,
    onAovClick: () -> Unit,
    onVisitorsClick: () -> Unit,
    onViewAllActivitiesClick: () -> Unit,
    onActivityClick: (ActivityItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sync message notification if present
        AnimatedVisibility(
            visible = syncMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (syncMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TrendGreenLight)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = syncMessage,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = TrendGreen
                    )
                }
            }
        }

        // Current Store selector card
        StoreSelectorCard(
            currentStore = currentStore,
            onClick = onStoreClick
        )

        // Real-Time Visitor Live Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardSurfaceLight)
                .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .clickable(onClick = onVisitorsClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TrendGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = TrendGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${visitorStats.activeVisitorsNow} visitatori online adesso",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${visitorStats.pageViewsPerMin} pag/min • ${visitorStats.activeCartsCount} carrelli aperti",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Telemetria",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemePrimary
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Apri Telemetria",
                        tint = ThemePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Real-Time Total Revenue Hero section (Bold Display Typography)
        HeroRevenueCard(
            revenue = salesMetrics.totalRevenue,
            growthPercent = salesMetrics.revenueGrowthPercent,
            selectedTimeframe = selectedTimeframe,
            salesVelocityPerHour = salesMetrics.salesVelocityPerHour,
            onSelectTimeframe = onSelectTimeframe
        )

        // Real-Time Sales Metrics: Order Count & Average Order Value (AOV) + Conversions
        RealtimeMetricsGrid(
            metrics = salesMetrics,
            onOrdersClick = onPendingOrdersClick,
            onAovClick = onAovClick
        )

        // Real-Time Hourly Sales Velocity Chart
        if (salesMetrics.hourlySales.isNotEmpty()) {
            SalesVelocityChart(
                hourlySales = salesMetrics.hourlySales
            )
        }

        // Quick Action Fulfillment & Stock Alert Cards
        StatsGrid(
            pendingOrdersCount = salesMetrics.pendingOrdersCount,
            stockAlertsCount = currentStore?.stockAlertsCount ?: 0,
            onPendingOrdersClick = onPendingOrdersClick,
            onStockAlertsClick = onStockAlertsClick
        )

        // Live Activity & Recent Orders Section
        RecentActivitySection(
            activities = activities,
            onViewAllClick = onViewAllActivitiesClick,
            onActivityClick = onActivityClick
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
