package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppUsageInfo
import kotlin.math.min

@Composable
fun CircularScreenTimeGauge(
    totalScreenTimeMillis: Long,
    dailyGoalMillis: Long,
    pickupsCount: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    val progress = remember(totalScreenTimeMillis, dailyGoalMillis) {
        if (dailyGoalMillis > 0) {
            (totalScreenTimeMillis.toFloat() / dailyGoalMillis).coerceIn(0f, 1.25f)
        } else 0f
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val percentage = (progress * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular Gauge
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = this.center

                // Background 260-degree track
                drawArc(
                    color = trackColor,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Foreground active arc with gradient
                val sweep = (260f * (animatedProgress.value.coerceIn(0f, 1f)))
                if (sweep > 0f) {
                    val brush = Brush.sweepGradient(
                        colors = listOf(secondaryColor, primaryColor, tertiaryColor),
                        center = center
                    )
                    drawArc(
                        brush = brush,
                        startAngle = 140f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppUsageInfo.formatDuration(totalScreenTimeMillis),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$percentage% of goal",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (percentage > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mini KPI Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Pickups
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(secondaryColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = "Pickups",
                        tint = secondaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$pickupsCount",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pickups",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Daily Target
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(tertiaryColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = "Daily Limit",
                        tint = tertiaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = AppUsageInfo.formatDuration(dailyGoalMillis),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Target Limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
