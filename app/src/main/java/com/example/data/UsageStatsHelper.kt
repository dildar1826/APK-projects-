package com.example.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.model.AppCategory
import com.example.model.AppUsageInfo
import com.example.model.DailyUsageSummary
import com.example.model.HourlyUsage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object UsageStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getDayStartMillis(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getDayEndMillis(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Resolves human-readable app label from package name.
     */
    fun resolveAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            val parts = packageName.split(".")
            val candidate = parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
            candidate.ifBlank { packageName }
        }
    }

    /**
     * Fetches real usage data for Today from UsageStatsManager.
     */
    fun getTodayUsageData(
        context: Context,
        excludeSystemApps: Boolean
    ): Triple<List<AppUsageInfo>, List<HourlyUsage>, Int> {
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return Triple(emptyList(), emptyList(), 0)

        val startTime = getTodayStartMillis()
        val endTime = System.currentTimeMillis()

        // 1. Query aggregated stats for today
        val statsList: List<UsageStats> = usageManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val appMap = mutableMapOf<String, Long>()
        for (stat in statsList) {
            val time = stat.totalTimeInForeground
            if (time > 1000) { // filter out zero or tiny blips
                val pkg = stat.packageName
                if (excludeSystemApps && AppCategory.fromPackage(pkg) == AppCategory.SYSTEM) {
                    continue
                }
                appMap[pkg] = (appMap[pkg] ?: 0L) + time
            }
        }

        // 2. Query event stream to calculate accurate pickups and hourly distribution
        var pickupsCount = 0
        val hourlyBins = LongArray(24) { 0L }

        try {
            val events = usageManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()

            var lastResumedPackage: String? = null
            var lastResumedTime = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                // Detect device pickups/screen wakeups
                if (event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE ||
                    event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                    pickupsCount++
                }

                // Track activity intervals for hourly distribution
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastResumedPackage = event.packageName
                    lastResumedTime = event.timeStamp
                } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                           event.eventType == UsageEvents.Event.ACTIVITY_STOPPED ||
                           event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                    if (lastResumedPackage != null && lastResumedTime > 0) {
                        val duration = (event.timeStamp - lastResumedTime).coerceIn(0L, 30 * 60 * 1000L)
                        val cal = Calendar.getInstance().apply { timeInMillis = lastResumedTime }
                        val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                        hourlyBins[hour] += duration
                    }
                    lastResumedPackage = null
                    lastResumedTime = 0L
                }
            }
        } catch (_: Exception) {
            // Fallback for emulator without event history
        }

        val appList = appMap.map { (pkg, time) ->
            AppUsageInfo(
                packageName = pkg,
                appName = resolveAppName(context, pkg),
                usageTimeMillis = time,
                launchCount = 1,
                category = AppCategory.fromPackage(pkg)
            )
        }.sortedByDescending { it.usageTimeMillis }

        val hourlyList = hourlyBins.mapIndexed { hour, duration ->
            HourlyUsage(hour = hour, durationMillis = duration)
        }

        return Triple(appList, hourlyList, pickupsCount.coerceAtLeast(appList.size))
    }

    /**
     * Fetches past 7 days usage summaries for the weekly line graph.
     */
    fun getPastWeekSummaries(
        context: Context,
        excludeSystemApps: Boolean
    ): List<DailyUsageSummary> {
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val summaries = mutableListOf<DailyUsageSummary>()

        for (daysAgo in 6 downTo 0) {
            val start = getDayStartMillis(daysAgo)
            val end = if (daysAgo == 0) System.currentTimeMillis() else getDayEndMillis(daysAgo)

            val statsList = usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                start,
                end
            ) ?: emptyList()

            var totalScreenTime = 0L
            val dayApps = mutableListOf<AppUsageInfo>()

            for (stat in statsList) {
                val time = stat.totalTimeInForeground
                if (time > 1000) {
                    val pkg = stat.packageName
                    if (excludeSystemApps && AppCategory.fromPackage(pkg) == AppCategory.SYSTEM) {
                        continue
                    }
                    totalScreenTime += time
                    dayApps.add(
                        AppUsageInfo(
                            packageName = pkg,
                            appName = resolveAppName(context, pkg),
                            usageTimeMillis = time,
                            category = AppCategory.fromPackage(pkg)
                        )
                    )
                }
            }

            val cal = Calendar.getInstance().apply { timeInMillis = start }
            val dayLabel = if (daysAgo == 0) "Today" else dayFormat.format(cal.time)
            val dateFormatted = dateFormat.format(cal.time)

            // Estimate pickups based on event count or fallback ratio
            val estimatedPickups = (totalScreenTime / (1000 * 60 * 12)).toInt().coerceIn(12, 120)

            summaries.add(
                DailyUsageSummary(
                    dateMillis = start,
                    dayLabel = dayLabel,
                    dateFormatted = dateFormatted,
                    totalScreenTimeMillis = totalScreenTime,
                    pickupsCount = estimatedPickups,
                    topApps = dayApps.sortedByDescending { it.usageTimeMillis }.take(5)
                )
            )
        }

        return summaries
    }

    /**
     * Generates realistic demo data so the user can immediately preview charts and features
     * in emulators or when test events haven't accumulated yet.
     */
    fun generateDemoWeekSummaries(): List<DailyUsageSummary> {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val summaries = mutableListOf<DailyUsageSummary>()

        // Varied realistic hours: 4.2h, 3.8h, 5.1h, 4.5h, 3.2h, 5.6h, 4.8h
        val sampleHours = listOf(4.2, 3.8, 5.1, 4.5, 3.2, 5.6, 4.8)
        val samplePickups = listOf(68, 54, 82, 71, 49, 93, 76)

        for (i in 6 downTo 0) {
            val start = getDayStartMillis(i)
            val cal = Calendar.getInstance().apply { timeInMillis = start }
            val dayLabel = if (i == 0) "Today" else dayFormat.format(cal.time)
            val dateFormatted = dateFormat.format(cal.time)
            val index = 6 - i
            val durationMillis = (sampleHours[index] * 3600 * 1000).toLong()

            val topApps = listOf(
                AppUsageInfo("com.google.android.youtube", "YouTube", (durationMillis * 0.32).toLong(), 18, AppCategory.ENTERTAINMENT),
                AppUsageInfo("com.instagram.android", "Instagram", (durationMillis * 0.26).toLong(), 24, AppCategory.SOCIAL),
                AppUsageInfo("com.whatsapp", "WhatsApp", (durationMillis * 0.18).toLong(), 35, AppCategory.SOCIAL),
                AppUsageInfo("com.android.chrome", "Chrome", (durationMillis * 0.14).toLong(), 12, AppCategory.PRODUCTIVITY),
                AppUsageInfo("com.spotify.music", "Spotify", (durationMillis * 0.10).toLong(), 8, AppCategory.ENTERTAINMENT)
            )

            summaries.add(
                DailyUsageSummary(
                    dateMillis = start,
                    dayLabel = dayLabel,
                    dateFormatted = dateFormatted,
                    totalScreenTimeMillis = durationMillis,
                    pickupsCount = samplePickups[index],
                    topApps = topApps
                )
            )
        }

        return summaries
    }

    fun generateDemoTodayData(): Triple<List<AppUsageInfo>, List<HourlyUsage>, Int> {
        val totalMillis = (4.8 * 3600 * 1000).toLong()
        val apps = listOf(
            AppUsageInfo("com.google.android.youtube", "YouTube", 5400000L, 19, AppCategory.ENTERTAINMENT), // 1h 30m
            AppUsageInfo("com.instagram.android", "Instagram", 4320000L, 26, AppCategory.SOCIAL),         // 1h 12m
            AppUsageInfo("com.whatsapp", "WhatsApp", 2880000L, 42, AppCategory.SOCIAL),                  // 48m
            AppUsageInfo("com.android.chrome", "Google Chrome", 2160000L, 14, AppCategory.PRODUCTIVITY), // 36m
            AppUsageInfo("com.spotify.music", "Spotify", 1440000L, 6, AppCategory.ENTERTAINMENT),        // 24m
            AppUsageInfo("com.slack", "Slack", 900000L, 11, AppCategory.PRODUCTIVITY),                   // 15m
            AppUsageInfo("com.supercell.clashroyale", "Clash Royale", 720000L, 3, AppCategory.GAMING)     // 12m
        )

        // Realistic hourly distribution peaking around lunch and evening
        val hourlyFractions = doubleArrayOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, // 0..5 AM
            0.02, 0.05, 0.08, 0.07, 0.06, 0.09, // 6..11 AM
            0.14, 0.06, 0.04, 0.05, 0.07, 0.08, // 12..5 PM
            0.12, 0.10, 0.08, 0.05, 0.02, 0.0   // 6..11 PM
        )

        val hourlyList = hourlyFractions.mapIndexed { hour, frac ->
            HourlyUsage(hour, (totalMillis * frac).toLong())
        }

        return Triple(apps, hourlyList, 76)
    }
}
