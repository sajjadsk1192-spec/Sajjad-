package com.example

import com.example.data.CoinTransaction
import com.example.data.LuckyDrawRepository
import com.example.data.PaymentMethod
import com.example.data.PaymentOrder
import com.example.data.PaymentStatus
import com.example.data.TransactionType
import com.example.data.UserProfile
import com.example.util.DrawScheduleUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LuckyDrawRepositoryTest {

    @Test
    fun `coin packages compute totalCoins and formattedPrice accurately`() {
        val luckyPack = LuckyDrawRepository.COIN_PACKAGES[1]
        assertEquals(550L, luckyPack.totalCoins)
        assertEquals("$5.00", luckyPack.formattedPrice)

        val royalPack = LuckyDrawRepository.COIN_PACKAGES[3]
        assertEquals(3100L, royalPack.totalCoins)
        assertEquals("$25.00", royalPack.formattedPrice)
    }

    @Test
    fun `enum helpers safely fall back on unknown values`() {
        val unknownTx = CoinTransaction(type = "UNKNOWN_LEGACY_TYPE")
        assertEquals(TransactionType.COIN_PURCHASE, unknownTx.transactionType)

        val unknownOrder = PaymentOrder(paymentMethod = "LEGACY_METHOD", status = "UNKNOWN_STATUS")
        assertEquals(PaymentMethod.GOOGLE_PAY, unknownOrder.methodEnum)
        assertEquals(PaymentStatus.FAILED, unknownOrder.statusEnum)
    }

    @Test
    fun `default user profile starts with 100 welcome coins`() {
        val profile = UserProfile(id = "uid_123", userId = "uid_123", name = "Alex Morgan", email = "alex@example.com")
        assertEquals(100L, profile.coinBalance)
        assertEquals("uid_123", profile.id)
    }

    @Test
    fun `75 percent winner and 25 percent house split sums exactly to gross prize`() {
        val gross = 10_000L
        val winnerShare = (gross * 75L) / 100L
        val houseShare = gross - winnerShare

        assertEquals(7_500L, winnerShare)
        assertEquals(2_500L, houseShare)
        assertEquals(gross, winnerShare + houseShare)
        assertTrue(DrawScheduleUtil.formatUsdFromCoins(winnerShare) == "$75.00")
    }
}
