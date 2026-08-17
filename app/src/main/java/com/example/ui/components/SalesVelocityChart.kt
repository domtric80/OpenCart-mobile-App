package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HourlySalesPoint
import com.example.ui.theme.CardSurfaceLight
import com.example.ui.theme.CardSurfacePure
import com.example.ui.theme.StatusConfirmedBlue
import com.example.ui.theme.StatusShippedGreen
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer
import com.example.ui.theme.ThemeSecondary
import com.example.ui.theme.TrendGreen
import com.example.ui.theme.TrendGreenLight
import java.text.NumberFormat
import java.util.Locale

enum class ChartDisplayType(val label: String) {
    AREA("Area Fluida"),
    BARS("Istogramma")
}

/**
 * Recharts-inspired Real-Time Sales Overview Component.
 * Displays live revenue dynamics with interactive spline area curves, Cartesian grids,
 * dynamic gradient shading, active cursor tooltips, and bar chart mode.
 */
@Composable
fun SalesVelocityChart(
    hourlySales: List<HourlySalesPoint>,
    modifier: Modifier = Modifier
) {
    if (hourlySales.isEmpty()) return

    var chartType by remember { mutableStateOf(ChartDisplayType.AREA) }
    val maxRevenue = remember(hourlySales) {
        (hourlySales.maxOfOrNull { it.revenue } ?: 1.0).coerceAtLeast(1.0)
    }
    val totalRevenueInPeriod = remember(hourlySales) { hourlySales.sumOf { it.revenue } }
    val totalOrdersInPeriod = remember(hourlySales) { hourlySales.sumOf { it.orderCount } }

    var selectedIndex by remember {
        mutableIntStateOf(hourlySales.indexOfFirst { it.isCurrentPeak }.takeIf { it >= 0 } ?: 0)
    }
    val activePoint = hourlySales.getOrNull(selectedIndex) ?: hourlySales.first()

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.GERMANY).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    // Live pulse animation for real-time indicator
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardSurfaceLight)
            .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("sales_velocity_chart_container")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Live Badge & Chart Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title & Live Pulse Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TrendGreenLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(TrendGreen.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "REAL-TIME OVERVIEW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = TrendGreen
                            )
                        }
                    }

                    Text(
                        text = "Metriche Ricavi",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Recharts-style Toggle: Area Curve vs Bar Histogram
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (chartType == ChartDisplayType.AREA) CardSurfacePure else Color.Transparent)
                            .clickable { chartType = ChartDisplayType.AREA }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = if (chartType == ChartDisplayType.AREA) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Area",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (chartType == ChartDisplayType.AREA) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (chartType == ChartDisplayType.AREA) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (chartType == ChartDisplayType.BARS) CardSurfacePure else Color.Transparent)
                            .clickable { chartType = ChartDisplayType.BARS }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = if (chartType == ChartDisplayType.BARS) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Barre",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (chartType == ChartDisplayType.BARS) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (chartType == ChartDisplayType.BARS) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Interactive Tooltip Callout Card (Recharts <Tooltip> style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurfacePure)
                    .border(1.dp, ThemeOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Intervallo ${activePoint.hourLabel}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activePoint.isCurrentPeak) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ThemePrimaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PICCO MASSIMO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        ),
                                        color = ThemeOnPrimaryContainer
                                    )
                                }
                            }
                        }

                        Text(
                            text = currencyFormatter.format(activePoint.revenue).replace("EUR", "€").trim(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            color = ThemePrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = StatusConfirmedBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${activePoint.orderCount} ordini",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = StatusConfirmedBlue
                            )
                        }

                        Text(
                            text = "Tocca un punto per dettagli",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Chart Render Canvas Area
            when (chartType) {
                ChartDisplayType.AREA -> {
                    RechartsAreaChartCanvas(
                        data = hourlySales,
                        selectedIndex = selectedIndex,
                        onSelectIndex = { selectedIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
                ChartDisplayType.BARS -> {
                    RechartsBarChart(
                        hourlySales = hourlySales,
                        selectedIndex = selectedIndex,
                        maxRevenue = maxRevenue,
                        onSelectIndex = { selectedIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }

            // X-Axis Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                hourlySales.forEachIndexed { idx, item ->
                    val isSelected = idx == selectedIndex
                    Text(
                        text = item.hourLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        ),
                        color = if (isSelected) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { selectedIndex = idx }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Recharts-style Area Chart with Spline Bezier curve, gradient fill, Cartesian grid, and interactive scrubber cursor.
 */
@Composable
private fun RechartsAreaChartCanvas(
    data: List<HourlySalesPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = ThemePrimary
    val gradientColor = ThemePrimary.copy(alpha = 0.25f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val maxVal = remember(data) { (data.maxOfOrNull { it.revenue } ?: 1.0) * 1.15 }

    Canvas(
        modifier = modifier
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val stepWidth = size.width / (data.size - 1).coerceAtLeast(1)
                    val index = ((offset.x + stepWidth / 2) / stepWidth).toInt().coerceIn(0, data.size - 1)
                    onSelectIndex(index)
                }
            }
            .pointerInput(data) {
                detectDragGestures { change, _ ->
                    val stepWidth = size.width / (data.size - 1).coerceAtLeast(1)
                    val index = ((change.position.x + stepWidth / 2) / stepWidth).toInt().coerceIn(0, data.size - 1)
                    onSelectIndex(index)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val pointCount = data.size
        if (pointCount < 2) return@Canvas

        val stepX = width / (pointCount - 1)
        val points = data.mapIndexed { idx, pt ->
            val x = idx * stepX
            val y = height - ((pt.revenue / maxVal).toFloat() * height * 0.85f) - (height * 0.08f)
            Offset(x, y)
        }

        // 1. Draw Cartesian Horizontal Reference Grid Lines (Recharts style)
        val gridLinesCount = 3
        for (i in 1..gridLinesCount) {
            val gridY = height * (i.toFloat() / (gridLinesCount + 1))
            drawLine(
                color = gridColor,
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        // 2. Build Smooth Cubic Bezier Spline Path
        val strokePath = Path()
        val fillPath = Path()

        strokePath.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, height)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

            strokePath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p1.x, p1.y
            )
            fillPath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p1.x, p1.y
            )
        }

        fillPath.lineTo(points.last().x, height)
        fillPath.close()

        // 3. Draw Vertical Area Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // 4. Draw Main Spline Line Stroke
        drawPath(
            path = strokePath,
            color = primaryColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // 5. Draw Active Selected Point Cursor & Marker
        if (selectedIndex in points.indices) {
            val activePoint = points[selectedIndex]

            // Vertical Cursor line (Recharts tooltip cursor)
            drawLine(
                color = primaryColor.copy(alpha = 0.5f),
                start = Offset(activePoint.x, 0f),
                end = Offset(activePoint.x, height),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Outer glowing dot
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = 10.dp.toPx(),
                center = activePoint
            )
            // Inner solid dot
            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = activePoint
            )
            // Core white center
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = activePoint
            )
        }
    }
}

/**
 * Recharts-style Animated Bar Chart.
 */
@Composable
private fun RechartsBarChart(
    hourlySales: List<HourlySalesPoint>,
    selectedIndex: Int,
    maxRevenue: Double,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        hourlySales.forEachIndexed { index, point ->
            val isSelected = index == selectedIndex
            val heightFraction = (point.revenue / maxRevenue).toFloat().coerceIn(0.12f, 1f)

            val animatedHeight by animateFloatAsState(
                targetValue = heightFraction,
                animationSpec = tween(durationMillis = 500),
                label = "recharts_bar_height_$index"
            )

            val barColor by animateColorAsState(
                targetValue = when {
                    isSelected -> ThemePrimary
                    point.isCurrentPeak -> ThemePrimary.copy(alpha = 0.75f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "recharts_bar_color_$index"
            )

            val interactionSource = remember { MutableInteractionSource() }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelectIndex(index) }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (point.isCurrentPeak) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(TrendGreen)
                    )
                } else {
                    Spacer(modifier = Modifier.height(9.dp))
                }

                Box(
                    modifier = Modifier
                        .width(if (isSelected) 22.dp else 18.dp)
                        .fillMaxHeight(animatedHeight)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(ThemePrimary, ThemePrimary.copy(alpha = 0.7f))
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(barColor, barColor.copy(alpha = 0.85f))
                                )
                            }
                        )
                )
            }
        }
    }
}
