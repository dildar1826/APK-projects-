package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppUsageInfo
import com.example.model.HourlyUsage

@Composable
fun HourlyBarChart(
    hourlyList: List<HourlyUsage>,
    modifier: Modifier = Modifier
) {
    if (hourlyList.isEmpty()) return

    var selectedHour by remember { mutableStateOf<HourlyUsage?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val maxDuration = remember(hourlyList) {
        (hourlyList.maxOfOrNull { it.durationMillis } ?: 1L).coerceAtLeast(15 * 60 * 1000L)
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(hourlyList) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "24-Hour Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (selectedHour != null) {
                        val h = selectedHour!!.hour
                        val formattedTime = String.format("%02d:00 - %02d:00", h, (h + 1) % 24)
                        "$formattedTime: ${AppUsageInfo.formatDuration(selectedHour!!.durationMillis)}"
                    } else {
                        "Tap a bar to see hourly duration"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedHour != null) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val peakHour = remember(hourlyList) {
                hourlyList.maxByOrNull { it.durationMillis }
            }
            if (peakHour != null && peakHour.durationMillis > 0) {
                Text(
                    text = "Peak: ${String.format("%02d:00", peakHour.hour)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bar Chart Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            val totalBars = 24
            val barSpacing = 4.dp.toPx()
            val totalSpacing = barSpacing * (totalBars - 1)
            val barWidth = (size.width - totalSpacing) / totalBars

            for (i in 0 until totalBars) {
                val item = hourlyList.getOrNull(i) ?: HourlyUsage(i, 0L)
                val duration = item.durationMillis
                val ratio = (duration.toFloat() / maxDuration).coerceIn(0f, 1f) * animProgress.value
                val minBarHeight = 3.dp.toPx()
                val barHeight = (size.height * ratio).coerceAtLeast(minBarHeight)

                val x = i * (barWidth + barSpacing)
                val y = size.height - barHeight

                val isSelected = selectedHour?.hour == i
                val brush = if (duration > 0) {
                    Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(Color.White, primaryColor)
                        } else {
                            listOf(secondaryColor, primaryColor)
                        },
                        startY = y,
                        endY = size.height
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            primaryColor.copy(alpha = 0.05f)
                        )
                    )
                }

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time labels: 12 AM, 6 AM, 12 PM, 6 PM, 12 AM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("12 AM", "6 AM", "12 PM", "6 PM", "11 PM").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
