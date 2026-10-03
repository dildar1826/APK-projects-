package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class AppCategory(val title: String, val color: Color) {
    SOCIAL("Social", CategorySocial),
    ENTERTAINMENT("Entertainment", CategoryEntertainment),
    PRODUCTIVITY("Productivity", CategoryProductivity),
    GAMING("Games", CategoryGaming),
    UTILITIES("Utilities", CategoryUtilities),
    SYSTEM("System", CategoryOther),
    OTHER("Other", CategoryOther);

    companion object {
        fun fromPackage(pkg: String): AppCategory {
            val lower = pkg.lowercase()
            return when {
                lower.contains("whatsapp") || lower.contains("instagram") ||
                lower.contains("facebook") || lower.contains("twitter") ||
                lower.contains("snapchat") || lower.contains("tiktok") ||
                lower.contains("telegram") || lower.contains("reddit") ||
                lower.contains("discord") || lower.contains("threads") -> SOCIAL

                lower.contains("youtube") || lower.contains("netflix") ||
                lower.contains("spotify") || lower.contains("twitch") ||
                lower.contains("primevideo") || lower.contains("music") ||
                lower.contains("podcast") || lower.contains("disney") -> ENTERTAINMENT

                lower.contains("chrome") || lower.contains("browser") ||
                lower.contains("gmail") || lower.contains("docs") ||
                lower.contains("sheets") || lower.contains("notion") ||
                lower.contains("slack") || lower.contains("drive") ||
                lower.contains("calendar") || lower.contains("keep") ||
                lower.contains("calculator") || lower.contains("clock") -> PRODUCTIVITY

                lower.contains("game") || lower.contains("pubg") ||
                lower.contains("roblox") || lower.contains("minecraft") ||
                lower.contains("supercell") || lower.contains("unity") -> GAMING

                lower.contains("systemui") || lower.contains("launcher") ||
                lower.contains("android.settings") || lower.contains("nexuslauncher") -> SYSTEM

                else -> OTHER
            }
        }
    }
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val launchCount: Int = 1,
    val category: AppCategory = AppCategory.fromPackage(packageName)
) {
    val formattedDuration: String
        get() = formatDuration(usageTimeMillis)

    companion object {
        fun formatDuration(millis: Long): String {
            val totalSeconds = millis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "${seconds}s"
            }
        }

        fun formatHoursDecimal(millis: Long): String {
            val hours = millis.toDouble() / (1000 * 60 * 60)
            return String.format("%.1fh", hours)
        }
    }
}

data class HourlyUsage(
    val hour: Int, // 0..23
    val durationMillis: Long
)

data class DailyUsageSummary(
    val dateMillis: Long,
    val dayLabel: String,         // "Mon", "Tue", "Today"
    val dateFormatted: String,    // "Oct 3"
    val totalScreenTimeMillis: Long,
    val pickupsCount: Int,
    val topApps: List<AppUsageInfo> = emptyList()
) {
    val formattedDuration: String
        get() = AppUsageInfo.formatDuration(totalScreenTimeMillis)
}

data class ScreenTimeGoal(
    val dailyGoalMinutes: Int = 240, // 4 hours default
    val appLimitsMinutes: Map<String, Int> = mapOf(
        "com.instagram.android" to 45,
        "com.google.android.youtube" to 60,
        "com.zhiliaoapp.musically" to 30
    ),
    val breakReminderMinutes: Int = 45
)
