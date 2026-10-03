package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppUsageInfo
import com.example.model.DailyUsageSummary
import kotlin.math.max

@Composable
fun WeeklyLineChart(
    summaries: List<DailyUsageSummary>,
    dailyGoalMillis: Long,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val goalColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)

    // Animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(summaries) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    // Determine max Y scale (at least 6 hours or 20% higher than max usage)
    val maxUsageMillis = remember(summaries, dailyGoalMillis) {
        val peak = summaries.maxOfOrNull { it.totalScreenTimeMillis } ?: 0L
        max(peak, dailyGoalMillis).coerceAtLeast(6 * 3600 * 1000L)
    }

    val selectedDay = summaries.getOrNull(selectedIndex) ?: summaries.last()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // Chart Header with active Day Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${selectedDay.dayLabel} (${selectedDay.dateFormatted})",
                    style = MaterialTheme.typography.labelLarge,
                    color = onSurfaceVariant
                )
                Text(
                    text = selectedDay.formattedDuration,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(goalColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Goal: ${AppUsageInfo.formatDuration(dailyGoalMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas Area
        val density = LocalDensity.current
        var chartWidthPx by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(summaries.size) {
                        detectTapGestures { offset ->
                            val sectionWidth = size.width / (summaries.size - 1).coerceAtLeast(1)
                            val index = ((offset.x + (sectionWidth / 2)) / sectionWidth)
                                .toInt()
                                .coerceIn(0, summaries.size - 1)
                            onSelectDay(index)
                        }
                    }
            ) {
                chartWidthPx = size.width
                val w = size.width
                val h = size.height
                val paddingBottom = 20.dp.toPx()
                val effectiveH = h - paddingBottom

                val pointCount = summaries.size
                if (pointCount < 2) return@Canvas

                val stepX = w / (pointCount - 1)

                // 1. Draw horizontal benchmark grid lines (0h, half, max)
                val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                for (ratio in gridLevels) {
                    val y = effectiveH * (1f - ratio)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // 2. Draw Daily Goal line
                val goalRatio = (dailyGoalMillis.toFloat() / maxUsageMillis).coerceIn(0f, 1f)
                val goalY = effectiveH * (1f - goalRatio)
                drawLine(
                    color = goalColor,
                    start = Offset(0f, goalY),
                    end = Offset(w, goalY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )

                // 3. Calculate curve points
                val points = summaries.mapIndexed { index, item ->
                    val x = index * stepX
                    val ratio = (item.totalScreenTimeMillis.toFloat() / maxUsageMillis).coerceIn(0f, 1f)
                    val animatedRatio = ratio * animationProgress.value
                    val y = effectiveH * (1f - animatedRatio)
                    Offset(x, y)
                }

                // 4. Construct Smooth Cubic Bezier Line & Gradient Fill
                val linePath = Path()
                val fillPath = Path()

                linePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, effectiveH)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]

                    val controlX1 = p0.x + (p1.x - p0.x) / 2
                    val controlY1 = p0.y
                    val controlX2 = p0.x + (p1.x - p0.x) / 2
                    val controlY2 = p1.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }

                fillPath.lineTo(points.last().x, effectiveH)
                fillPath.close()

                // Draw Gradient Fill under line
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = effectiveH
                )
                drawPath(path = fillPath, brush = gradientBrush)

                // Draw Stroke Curve
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(
                        width = 3.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 5. Draw Selected Point Indicator (Vertical line & glowing point)
                val selPoint = points.getOrNull(selectedIndex) ?: points.last()

                // Vertical ruler
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = Offset(selPoint.x, 0f),
                    end = Offset(selPoint.x, effectiveH),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Outer halo
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 12.dp.toPx(),
                    center = selPoint
                )
                // Inner solid
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = selPoint
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = selPoint
                )

                // Draw unselected dots
                for ((idx, pt) in points.withIndex()) {
                    if (idx != selectedIndex) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.7f),
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        // X-Axis Day Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            summaries.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectDay(index) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) primaryColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color.White else onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
