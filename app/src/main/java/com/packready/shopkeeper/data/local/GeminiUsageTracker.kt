package com.packready.shopkeeper.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class GeminiUsageStats(
    val currentRpm: Int,
    val maxRpm: Int = 15,
    val currentRpd: Int,
    val maxRpd: Int = 500,
    val secondsUntilNextSlot: Int,
    val isBlocked: Boolean,
    val blockReason: String?,
    val hasApiKey: Boolean,
    val maskedApiKey: String
) {
    val rpmRemaining: Int get() = (maxRpm - currentRpm).coerceAtLeast(0)
    val rpdRemaining: Int get() = (maxRpd - currentRpd).coerceAtLeast(0)
}

@Singleton
class GeminiUsageTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gemini_rate_tracker", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "gemini_custom_api_key"
        private const val KEY_TIMESTAMPS = "gemini_req_timestamps"
        private const val KEY_DAILY_DATE = "gemini_daily_date"
        private const val KEY_DAILY_COUNT = "gemini_daily_count"

        const val MAX_RPM = 15
        const val SAFETY_RPM = 14
        const val MAX_RPD = 500
        const val SAFETY_RPD = 495
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "")?.trim() ?: ""
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    @Synchronized
    fun getUsageStats(): GeminiUsageStats {
        val apiKey = getApiKey()
        val now = System.currentTimeMillis()

        // 1. Sliding window (last 60,000 ms)
        val rawSet = prefs.getStringSet(KEY_TIMESTAMPS, emptySet()) ?: emptySet()
        val validTimestamps = rawSet
            .mapNotNull { it.toLongOrNull() }
            .filter { (now - it) < 60_000L }
            .sorted()

        // 2. Daily reset check
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val savedDate = prefs.getString(KEY_DAILY_DATE, "")
        var dailyCount = prefs.getInt(KEY_DAILY_COUNT, 0)
        if (savedDate != todayStr) {
            dailyCount = 0
            prefs.edit()
                .putString(KEY_DAILY_DATE, todayStr)
                .putInt(KEY_DAILY_COUNT, 0)
                .apply()
        }

        val currentRpm = validTimestamps.size
        var secondsUntilNextSlot = 0
        if (currentRpm >= SAFETY_RPM && validTimestamps.isNotEmpty()) {
            val oldest = validTimestamps.first()
            val elapsed = now - oldest
            secondsUntilNextSlot = (((60_000L - elapsed) / 1000).toInt()).coerceIn(1, 60)
        }

        var isBlocked = false
        var blockReason: String? = null

        if (apiKey.isBlank()) {
          isBlocked = true
          blockReason = "Configure your personal Gemini API key to use AI extraction"
        } else if (currentRpm >= SAFETY_RPM) {
            isBlocked = true
            blockReason = "Rate Limit Safety: $currentRpm/$MAX_RPM RPM reached. Please wait ${secondsUntilNextSlot}s."
        } else if (dailyCount >= SAFETY_RPD) {
            isBlocked = true
            blockReason = "Daily Quota Safety: $dailyCount/$MAX_RPD RPD reached. Resets tomorrow."
        }

        val masked = if (apiKey.isNotBlank()) {
            if (apiKey.length > 8) {
                "${apiKey.substring(0, 4)}••••${apiKey.substring(apiKey.length - 4)}"
            } else {
                "••••••••"
            }
        } else {
            "Not Set"
        }

        return GeminiUsageStats(
            currentRpm = currentRpm,
            maxRpm = MAX_RPM,
            currentRpd = dailyCount,
            maxRpd = MAX_RPD,
            secondsUntilNextSlot = secondsUntilNextSlot,
            isBlocked = isBlocked,
            blockReason = blockReason,
            hasApiKey = apiKey.isNotBlank(),
            maskedApiKey = masked
        )
    }

    @Synchronized
    fun recordRequest() {
        val now = System.currentTimeMillis()

        // 1. Sliding window timestamps
        val rawSet = prefs.getStringSet(KEY_TIMESTAMPS, emptySet()) ?: emptySet()
        val validTimestamps = rawSet
            .mapNotNull { it.toLongOrNull() }
            .filter { (now - it) < 60_000L }
            .toMutableList()
        validTimestamps.add(now)

        // 2. Daily count
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val savedDate = prefs.getString(KEY_DAILY_DATE, "")
        var dailyCount = prefs.getInt(KEY_DAILY_COUNT, 0)
        if (savedDate != todayStr) {
            dailyCount = 1
        } else {
            dailyCount += 1
        }

        prefs.edit()
            .putStringSet(KEY_TIMESTAMPS, validTimestamps.map { it.toString() }.toSet())
            .putString(KEY_DAILY_DATE, todayStr)
            .putInt(KEY_DAILY_COUNT, dailyCount)
            .apply()
    }
}
