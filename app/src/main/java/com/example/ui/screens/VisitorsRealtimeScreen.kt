package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pageview
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActivePageVisit
import com.example.model.DeviceBreakdown
import com.example.model.GeoVisitor
import com.example.model.LiveVisitorEvent
import com.example.model.LiveVisitorPoint
import com.example.model.Store
import com.example.model.TrafficSource
import com.example.model.VisitorRealtimeStats
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.CardSurfaceLight
import com.example.ui.theme.CardSurfacePure
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusAlertRedBg
import com.example.ui.theme.StatusConfirmedBlue
import com.example.ui.theme.StatusConfirmedBlueBg
import com.example.ui.theme.StatusPendingGold
import com.example.ui.theme.StatusPendingGoldBg
import com.example.ui.theme.StatusShippedGreen
import com.example.ui.theme.StatusShippedGreenBg
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer
import com.example.ui.theme.ThemeSecondary
import com.example.ui.theme.TrendGreen
import com.example.ui.theme.TrendGreenLight

@Composable
fun VisitorsRealtimeScreen(
    visitorStats: VisitorRealtimeStats,
    currentStore: Store?,
    modifier: Modifier = Modifier
) {
    // Pulse animation for Live Streaming beacon
    val infiniteTransition = rememberInfiniteTransition(label = "live_radar_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radar"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Header Title & Store Context
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Telemetria Visitatori",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Flusso traffico in tempo reale • ${currentStore?.name ?: "OpenCart Store"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Live Beacon Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TrendGreenLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TrendGreen.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = if (visitorStats.trackingEnabled) "LIVE" else "NON ATTIVO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp
                            ),
                            color = TrendGreen
                        )
                    }
                }
            }
        }

        item {
            TelemetryStatusCard(visitorStats)
        }

        // 2. Hero Active Visitors Card with Key Telemetry Metrics
        item {
            HeroActiveVisitorsCard(
                visitorStats = visitorStats,
                pulseAlpha = pulseAlpha
            )
        }

        // 3. Real-Time Minute-by-Minute Traffic Chart (Recharts Spline Style)
        item {
            RealtimeTrafficChartCard(
                history = visitorStats.trafficHistory,
                activeUsersNow = visitorStats.activeVisitorsNow
            )
        }

        // 4. Active Pages Being Viewed Right Now
        item {
            TopActivePagesCard(pages = visitorStats.topPages)
        }

        // 5. Geographic Breakdown (Countries & Cities)
        item {
            GeographicDistributionCard(countries = visitorStats.topCountries)
        }

        // 6. Traffic Sources & Acquisition Channels
        item {
            TrafficSourcesCard(sources = visitorStats.trafficSources)
        }

        // 7. Device Breakdown (Mobile vs Desktop vs Tablet)
        item {
            DeviceBreakdownCard(devices = visitorStats.deviceStats)
        }

        // 8. Live Real-Time Event Stream
        item {
            LiveVisitorEventsCard(events = visitorStats.liveEvents)
        }
    }
}

@Composable
private fun TelemetryStatusCard(stats: VisitorRealtimeStats) {
    val isReady = stats.dataAvailable && stats.trackingEnabled
    val container = when {
        !stats.dataAvailable -> MaterialTheme.colorScheme.errorContainer
        !stats.trackingEnabled -> StatusPendingGoldBg
        else -> StatusShippedGreenBg
    }
    val foreground = when {
        !stats.dataAvailable -> MaterialTheme.colorScheme.onErrorContainer
        !stats.trackingEnabled -> StatusPendingGold
        else -> StatusShippedGreen
    }
    val message = when {
        !stats.dataAvailable -> "Il bridge installato non espone ancora la telemetria. Aggiorna il plugin CartAdmin alla stessa versione dell’app."
        !stats.trackingEnabled -> "Il tracciamento OpenCart è disattivato. Nel pannello admin apri Sistema > Impostazioni > Opzioni e abilita “Clienti online”."
        else -> "Dati reali da ${stats.source.ifBlank { "OpenCart customer_online" }}${stats.lastUpdated.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()}"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("visitor_telemetry_status")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isReady) "Telemetria OpenCart attiva" else "Telemetria non disponibile",
                color = foreground,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(message, color = foreground, style = MaterialTheme.typography.bodySmall)
            if (isReady && stats.limitations.isNotBlank()) {
                Text(stats.limitations, color = foreground, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Hero Card displaying active visitors count and high-frequency telemetry counters.
 */
@Composable
private fun HeroActiveVisitorsCard(
    visitorStats: VisitorRealtimeStats,
    pulseAlpha: Float
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .testTag("hero_active_visitors_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top: Big Live Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(TrendGreen.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "VISITATORI ATTIVI ADESSO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${visitorStats.activeVisitorsNow}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 46.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        color = ThemePrimary
                    )
                }

                // Radar Icon Graphic
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ThemePrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = ThemePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 4 Telemetry Quick Counter Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Page views per min
                TelemetryMiniPill(
                    icon = Icons.Default.Visibility,
                    value = "${visitorStats.pageViewsPerMin}",
                    label = "Aggiornati / min",
                    color = StatusConfirmedBlue,
                    bgColor = StatusConfirmedBlueBg,
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Active Carts
                TelemetryMiniPill(
                    icon = Icons.Default.ShoppingCart,
                    value = "${visitorStats.activeCartsCount}",
                    label = "Carrelli attivi",
                    color = StatusPendingGold,
                    bgColor = StatusPendingGoldBg,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 3: Active Checkouts
                TelemetryMiniPill(
                    icon = Icons.Default.CreditCard,
                    value = "${visitorStats.activeCheckoutsCount}",
                    label = "Alla Cassa",
                    color = StatusShippedGreen,
                    bgColor = StatusShippedGreenBg,
                    modifier = Modifier.weight(1f)
                )

                // Metric 4: Avg Duration
                TelemetryMiniPill(
                    icon = Icons.Default.Timer,
                    value = if (visitorStats.avgDurationSeconds > 0) "${visitorStats.avgDurationSeconds / 60}m ${visitorStats.avgDurationSeconds % 60}s" else "N/D",
                    label = "Durata sessione",
                    color = ThemeSecondary,
                    bgColor = ThemeSecondary.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryMiniPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Interactive Minute-by-Minute Real-Time Traffic Spline Chart.
 */
@Composable
private fun RealtimeTrafficChartCard(
    history: List<LiveVisitorPoint>,
    activeUsersNow: Int
) {
    if (history.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(history.lastIndex) }
    val activePoint = history.getOrNull(selectedIndex) ?: history.last()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .testTag("realtime_traffic_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Interactive Tooltip Info
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = ThemePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Traffico Ultimi 30 Minuti",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Tooltip indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ThemePrimaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${activePoint.timeLabel}: ${activePoint.activeUsers} utenti • ${activePoint.pageViews} pag/min",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = ThemeOnPrimaryContainer
                    )
                }
            }

            // Spline Canvas
            val maxVal = remember(history) {
                (history.maxOfOrNull { it.activeUsers } ?: 50) * 1.2
            }

            val primaryColor = ThemePrimary
            val gradientColor = ThemePrimary.copy(alpha = 0.25f)
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(history) {
                        detectTapGestures { offset ->
                            val stepWidth = size.width / (history.size - 1).coerceAtLeast(1)
                            val index = ((offset.x + stepWidth / 2) / stepWidth).toInt().coerceIn(0, history.size - 1)
                            selectedIndex = index
                        }
                    }
                    .pointerInput(history) {
                        detectDragGestures { change, _ ->
                            val stepWidth = size.width / (history.size - 1).coerceAtLeast(1)
                            val index = ((change.position.x + stepWidth / 2) / stepWidth).toInt().coerceIn(0, history.size - 1)
                            selectedIndex = index
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val pointCount = history.size
                if (pointCount < 2) return@Canvas

                val stepX = width / (pointCount - 1)
                val points = history.mapIndexed { idx, pt ->
                    val x = idx * stepX
                    val y = height - ((pt.activeUsers / maxVal).toFloat() * height * 0.85f) - (height * 0.08f)
                    Offset(x, y)
                }

                // Reference dashed lines
                for (i in 1..2) {
                    val gridY = height * (i.toFloat() / 3)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, gridY),
                        end = Offset(width, gridY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Build Spline Path
                val strokePath = Path()
                val fillPath = Path()

                strokePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, height)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                    val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                    strokePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                    fillPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                }

                fillPath.lineTo(points.last().x, height)
                fillPath.close()

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientColor, Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw Stroke
                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Selected Point Cursor
                if (selectedIndex in points.indices) {
                    val sp = points[selectedIndex]
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(sp.x, 0f),
                        end = Offset(sp.x, height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    drawCircle(color = primaryColor.copy(alpha = 0.2f), radius = 9.dp.toPx(), center = sp)
                    drawCircle(color = primaryColor, radius = 4.5.dp.toPx(), center = sp)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = sp)
                }
            }

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                history.forEachIndexed { idx, item ->
                    val isSelected = idx == selectedIndex
                    Text(
                        text = item.timeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        ),
                        color = if (isSelected) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { selectedIndex = idx }
                    )
                }
            }
        }
    }
}

/**
 * Active Pages List Card (What users are browsing right now).
 */
@Composable
private fun TopActivePagesCard(pages: List<ActivePageVisit>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Pageview,
                        contentDescription = null,
                        tint = ThemePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Pagine Attive Adesso",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${pages.sumOf { it.activeUsers }} visitatori totali",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            pages.forEachIndexed { index, page ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ThemePrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = ThemePrimary
                                )
                            }

                            Column {
                                Text(
                                    text = page.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = page.path,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Active users pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusConfirmedBlueBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${page.activeUsers} online",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = StatusConfirmedBlue
                            )
                        }
                    }

                    // Progress bar representing share
                    LinearProgressIndicator(
                        progress = { (page.percentage / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ThemePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Geographic Distribution Card (Live Visitors by Country & Cities).
 */
@Composable
private fun GeographicDistributionCard(countries: List<GeoVisitor>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = ThemePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Provenienza Geografica",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${countries.size} Paesi Attivi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            countries.forEach { geo ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = geo.flagEmoji,
                            fontSize = 22.sp
                        )
                        Column {
                            Text(
                                text = geo.country,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = geo.topCities,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${geo.visitorsCount} visitatori",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThemePrimary
                        )
                        Text(
                            text = "%.1f%%".format(geo.percentage),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

/**
 * Traffic Sources & Marketing Channels.
 */
@Composable
private fun TrafficSourcesCard(sources: List<TrafficSource>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = ThemePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Sorgenti di Acquisizione",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            sources.forEach { src ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = src.source,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Canale: ${src.type} • Tasso Conversione: ${src.conversionRate}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeSecondary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${src.visitorsCount} (${src.percentage}%)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Devices & Platforms Breakdown.
 */
@Composable
private fun DeviceBreakdownCard(devices: List<DeviceBreakdown>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = ThemePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Dispositivi e Piattaforme",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                devices.forEach { dev ->
                    val (icon, color, bgColor) = when (dev.iconName) {
                        "phone" -> Triple(Icons.Default.Smartphone, ThemePrimary, ThemePrimaryContainer)
                        "desktop" -> Triple(Icons.Default.Computer, StatusConfirmedBlue, StatusConfirmedBlueBg)
                        else -> Triple(Icons.Default.Tablet, ThemeSecondary, ThemeSecondary.copy(alpha = 0.1f))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .padding(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                            Text(
                                text = "%.0f%%".format(dev.percentage),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                ),
                                color = color
                            )
                            Text(
                                text = dev.deviceType.split(" ").firstOrNull() ?: dev.deviceType,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live Real-Time Visitor Activity Stream.
 */
@Composable
private fun LiveVisitorEventsCard(events: List<LiveVisitorEvent>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .testTag("live_visitor_events_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = StatusPendingGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Feed Attività in Tempo Reale",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Streaming live",
                    style = MaterialTheme.typography.labelSmall,
                    color = TrendGreen
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            events.take(8).forEach { event ->
                val (icon, tintColor, bgTint) = when (event.iconType) {
                    "checkout" -> Triple(Icons.Default.CreditCard, StatusShippedGreen, StatusShippedGreenBg)
                    "cart" -> Triple(Icons.Default.ShoppingCart, StatusPendingGold, StatusPendingGoldBg)
                    "order" -> Triple(Icons.Default.ShoppingBag, ThemePrimary, ThemePrimaryContainer)
                    "search" -> Triple(Icons.Default.Search, StatusConfirmedBlue, StatusConfirmedBlueBg)
                    else -> Triple(Icons.Default.Visibility, ThemeSecondary, ThemeSecondary.copy(alpha = 0.1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = event.timestamp,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
