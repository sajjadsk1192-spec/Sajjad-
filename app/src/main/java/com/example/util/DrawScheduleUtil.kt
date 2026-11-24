package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DrawTimerState(
    val isDrawOpen: Boolean,
    val formattedCountdown: String,
    val progressFraction: Float,
    val todayDateKey: String
)

object DrawScheduleUtil {
    const val COINS_PER_USD = 100L
    const val DRAW_ENTRY_COST_COINS = 50L
    const val DAILY_JACKPOT_COINS = 50_000L
    const val OPEN_HOUR = 7
    const val CLOSE_HOUR = 19

    fun calculateTimerState(nowMillis: Long = System.currentTimeMillis()): DrawTimerState {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }

        val todayOpenCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, OPEN_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayCloseCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, CLOSE_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val openMillis = todayOpenCal.timeInMillis
        val closeMillis = todayCloseCal.timeInMillis
        val twelveHoursMillis = 12L * 60L * 60L * 1000L

        val isDrawOpen = nowMillis in openMillis until closeMillis
        val remainingMillis: Long
        val progress: Float

        if (isDrawOpen) {
            remainingMillis = (closeMillis - nowMillis).coerceAtLeast(0L)
            val elapsed = (nowMillis - openMillis).coerceIn(0L, twelveHoursMillis)
            progress = elapsed.toFloat() / twelveHoursMillis.toFloat()
        } else {
            val nextOpenMillis = if (nowMillis < openMillis) {
                openMillis
            } else {
                Calendar.getInstance().apply {
                    timeInMillis = openMillis
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
            }
            remainingMillis = (nextOpenMillis - nowMillis).coerceAtLeast(0L)
            val elapsedSinceClose = (twelveHoursMillis - remainingMillis).coerceIn(0L, twelveHoursMillis)
            progress = elapsedSinceClose.toFloat() / twelveHoursMillis.toFloat()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayDateKey = dateFormat.format(nowCal.time)

        return DrawTimerState(
            isDrawOpen = isDrawOpen,
            formattedCountdown = formatDuration(remainingMillis),
            progressFraction = progress.coerceIn(0f, 1f),
            todayDateKey = todayDateKey
        )
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatCoins(coins: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(coins)
    }

    fun formatUsdFromCoins(coins: Long): String {
        val dollars = coins.toDouble() / COINS_PER_USD.toDouble()
        return String.format(Locale.US, "$%.2f", dollars)
    }

    fun maskPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length >= 4) {
            "••• ••${digits.takeLast(4)}"
        } else {
            "••••"
        }
    }

    fun formatTimestamp(millis: Long): String {
        val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return fmt.format(Date(millis))
    }
}
