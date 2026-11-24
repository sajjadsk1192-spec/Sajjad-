package com.example

import com.example.util.DrawScheduleUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DrawScheduleUtilTest {

    private fun timeAt(hour: Int, minute: Int, second: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 24)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `draw is open at 10 AM and counts down to 7 PM`() {
        val state = DrawScheduleUtil.calculateTimerState(timeAt(10, 0, 0))
        assertTrue(state.isDrawOpen)
        assertEquals("09:00:00", state.formattedCountdown)
        assertEquals(0.25f, state.progressFraction, 0.01f)
    }

    @Test
    fun `draw is closed at 8 PM and counts down to 7 AM`() {
        val state = DrawScheduleUtil.calculateTimerState(timeAt(20, 0, 0))
        assertFalse(state.isDrawOpen)
        assertEquals("11:00:00", state.formattedCountdown)
    }

    @Test
    fun `draw is closed at 5 AM and counts down to 7 AM`() {
        val state = DrawScheduleUtil.calculateTimerState(timeAt(5, 30, 0))
        assertFalse(state.isDrawOpen)
        assertEquals("01:30:00", state.formattedCountdown)
    }

    @Test
    fun `coins format to USD correctly`() {
        assertEquals("$1.00", DrawScheduleUtil.formatUsdFromCoins(100))
        assertEquals("$12.50", DrawScheduleUtil.formatUsdFromCoins(1250))
        assertEquals("$500.00", DrawScheduleUtil.formatUsdFromCoins(50_000))
    }
}
