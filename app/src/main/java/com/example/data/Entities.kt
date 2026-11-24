package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType {
    WELCOME,
    COIN_PURCHASE,
    DRAW_ENTRY,
    PRIZE_WIN
}

enum class PaymentStatus {
    PROCESSING,
    PAID,
    FAILED
}

enum class PaymentMethod(val displayLabel: String) {
    GOOGLE_PAY("Google Pay"),
    EASYPAISA("Easypaisa"),
    CARD("Card")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val coinBalance: Long = 100L,
    val todayTicketNumber: String? = null,
    val lastTicketDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "config",
    val configuredDrawPrizeCoins: Long = 10_000L,
    val houseBalanceCoins: Long = 0L,
    val houseDrawsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CoinTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val coinDelta: Long = 0L,
    val type: String = TransactionType.COIN_PURCHASE.name,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val transactionType: TransactionType
        get() = TransactionType.entries.firstOrNull { it.name == type } ?: TransactionType.COIN_PURCHASE
}

@Keep
@IgnoreExtraProperties
data class PaymentOrder(
    @DocumentId val orderId: String = "",
    val userId: String = "",
    val packageName: String = "",
    val coinsPurchased: Long = 0L,
    val amountCents: Long = 0L,
    val paymentMethod: String = PaymentMethod.GOOGLE_PAY.name,
    val maskedAccount: String = "",
    val status: String = PaymentStatus.PROCESSING.name,
    val receiptReference: String? = null,
    val paidAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val methodEnum: PaymentMethod
        get() = PaymentMethod.entries.firstOrNull { it.name == paymentMethod } ?: PaymentMethod.GOOGLE_PAY

    val statusEnum: PaymentStatus
        get() = PaymentStatus.entries.firstOrNull { it.name == status } ?: PaymentStatus.FAILED
}

@Keep
@IgnoreExtraProperties
data class DrawResult(
    @DocumentId val id: String = "",
    val winnerUserId: String? = null,
    val winnerName: String = "",
    val winnerPhoneMasked: String = "",
    val ticketCode: String = "",
    val grossPrizeCoins: Long = 0L,
    val prizeCoins: Long = 0L,
    val houseCutCoins: Long = 0L,
    val prizeUsdFormatted: String = "",
    val visibility: String = "PUBLIC",
    val drawnAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
