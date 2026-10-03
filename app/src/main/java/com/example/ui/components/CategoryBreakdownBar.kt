package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppCategory
import com.example.model.AppUsageInfo

@Composable
fun CategoryBreakdownBar(
    apps: List<AppUsageInfo>,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return

    val totalTime = remember(apps) { apps.sumOf { it.usageTimeMillis }.coerceAtLeast(1L) }

    // Group time by category
    val categoryTotals = remember(apps) {
        apps.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.usageTimeMillis } }
            .toList()
            .sortedByDescending { it.second }
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
        Text(
            text = "Category Breakdown",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Segmented Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            for ((category, time) in categoryTotals) {
                val weight = (time.toFloat() / totalTime).coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(category.color)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend Chips (Top 4 categories)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            categoryTotals.take(4).forEach { (category, time) ->
                val pct = ((time.toDouble() / totalTime) * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(category.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${category.title} $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
