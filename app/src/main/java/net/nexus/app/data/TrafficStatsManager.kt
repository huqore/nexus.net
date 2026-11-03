package net.nexus.app.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TrafficStatsManager {

    private const val PREFS_NAME = "nexus_traffic_stats"
    private const val KEY_PREFIX = "traffic_"

    private fun getPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordBytes(context: Context, bytes: Long) {
        if (bytes <= 0) return
        val prefs = getPrefs(context)
        val todayKey = KEY_PREFIX + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val current = prefs.getLong(todayKey, 0L)
        prefs.edit().putLong(todayKey, current + bytes).apply()
    }

    fun getTodayBytes(context: Context): Long {
        val prefs = getPrefs(context)
        val todayKey = KEY_PREFIX + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return prefs.getLong(todayKey, 0L)
    }

    fun getWeekBytes(context: Context): Long {
        val prefs = getPrefs(context)
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        var total = 0L
        for (i in 0 until 7) {
            val key = KEY_PREFIX + sdf.format(cal.time)
            total += prefs.getLong(key, 0L)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return total
    }

    fun getMonthBytes(context: Context): Long {
        val prefs = getPrefs(context)
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        var total = 0L
        for (i in 0 until 30) {
            val key = KEY_PREFIX + sdf.format(cal.time)
            total += prefs.getLong(key, 0L)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return total
    }

    fun formatTraffic(bytes: Long): String {
        return when {
            bytes <= 0L -> "0.0 МБ"
            bytes < 1024L * 1024L -> "%.0f КБ".format(Locale.US, bytes.toDouble() / 1024.0)
            bytes < 1024L * 1024L * 1024L -> "%.1f МБ".format(Locale.US, bytes.toDouble() / (1024.0 * 1024.0))
            else -> "%.2f ГБ".format(Locale.US, bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

