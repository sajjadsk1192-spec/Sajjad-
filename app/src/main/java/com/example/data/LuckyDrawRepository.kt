package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.R
import com.example.util.DrawScheduleUtil
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

data class CoinPackage(
    val id: String,
    val title: String,
    val coins: Long,
    val bonusCoins: Long,
    val priceCents: Long,
    val badge: String? = null
) {
    val totalCoins: Long get() = coins + bonusCoins
    val formattedPrice: String
        get() = String.format(Locale.US, "$%.2f", priceCents / 100.0)
}

interface AdminConfigProvider {
    val adminEmail: String
}

object BuildConfigAdminProvider : AdminConfigProvider {
    private const val PLACEHOLDER_EMAIL = "MY_ADMIN_EMAIL"
    private const val DEFAULT_OWNER_EMAIL = "sajjad.khansk1192@gmail.com"

    override val adminEmail: String
        get() = BuildConfig.ADMIN_EMAIL.trim()
            .takeIf { it.isNotBlank() && it != PLACEHOLDER_EMAIL }
            ?: DEFAULT_OWNER_EMAIL
}

class LuckyDrawRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val adminConfigProvider: AdminConfigProvider = BuildConfigAdminProvider
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    fun isVerifiedOwnerEmail(): Boolean {
        val currentUser = auth.currentUser ?: return false
        return currentUser.isEmailVerified &&
            currentUser.email.equals(adminConfigProvider.adminEmail, ignoreCase = true)
    }

    fun observeAdminMarker(userId: String): Flow<Boolean> {
        if (userId.isBlank()) return flowOf(false)
        val docRef = db.collection("admins").document(userId)
        return docRef.snapshots()
            .map { snapshot -> snapshot.exists() }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val errToLog = firestoreError ?: error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(errToLog, OperationType.GET, docRef.path)
                }
                emit(false)
            }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        if (userId.isBlank()) return flowOf(null)
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    fun observeAppConfig(): Flow<AppConfig> {
        val docRef = db.collection("app_state").document("config")
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    AppConfig()
                } else {
                    snapshot.toObject(
                        AppConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: AppConfig()
                }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    fun observeTransactions(userId: String): Flow<List<CoinTransaction>> {
        if (userId.isBlank()) return flowOf(emptyList())
        val colRef = db.collection("users").document(userId).collection("transactions")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CoinTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    fun observePaymentOrders(userId: String, isAdmin: Boolean): Flow<List<PaymentOrder>> {
        if (userId.isBlank()) return flowOf(emptyList())
        val query = if (isAdmin) {
            db.collection("payment_orders")
        } else {
            db.collection("payment_orders").whereEqualTo("userId", userId)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PaymentOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "payment_orders")
                }
                throw error
            }
    }

    fun observeDrawResults(): Flow<List<DrawResult>> {
        return db.collection("draws")
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DrawResult::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.drawnAtMillis }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "draws")
                }
                throw error
            }
    }

    fun observeAllPlayers(isAdmin: Boolean, currentUserId: String): Flow<List<UserProfile>> {
        if (!isAdmin || currentUserId.isBlank()) return flowOf(emptyList())
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter {
                    it.id != currentUserId &&
                        !it.email.equals(adminConfigProvider.adminEmail, ignoreCase = true)
                }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users")
                }
                throw error
            }
    }

    suspend fun ensureUserProfile(): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val firebaseUser = auth.currentUser
                ?: throw IllegalStateException("User must be signed in.")
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path

            val existing = userRef.get().await()
            if (!existing.exists()) {
                val rawEmail = firebaseUser.email?.trim().orEmpty().take(160)
                val fallbackName = rawEmail.substringBefore("@").ifBlank { "Lucky Player" }
                val displayName = firebaseUser.displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.take(80)
                    ?: fallbackName.take(80)

                val txRef = userRef.collection("transactions").document()
                val welcomeCoins = 100L

                val userPayload = mapOf(
                    "userId" to uid,
                    "name" to displayName,
                    "email" to rawEmail,
                    "phone" to "",
                    "coinBalance" to welcomeCoins,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val txPayload = mapOf(
                    "userId" to uid,
                    "title" to "Welcome Bonus",
                    "coinDelta" to welcomeCoins,
                    "type" to TransactionType.WELCOME.name,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                db.runBatch { batch ->
                    batch.set(userRef, userPayload)
                    batch.set(txRef, txPayload)
                }.await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun purchaseCoins(
        coinPackage: CoinPackage,
        method: PaymentMethod,
        maskedAccount: String
    ): Result<PaymentOrder> {
        if (coinPackage.priceCents <= 0L || coinPackage.totalCoins <= 0L) {
            return Result.failure(IllegalArgumentException("Invalid coin package selected."))
        }
        val cleanMasked = maskedAccount.trim().take(80)
        if (cleanMasked.isBlank()) {
            return Result.failure(IllegalArgumentException("Payment account details required."))
        }

        val orderId = "ORD-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase(Locale.US)
        var currentPath = "payment_orders/$orderId"

        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val orderRef = db.collection("payment_orders").document(orderId)
            val txRef = userRef.collection("transactions").document()
            currentPath = orderRef.path

            val createOrderPayload = mapOf(
                "userId" to uid,
                "packageName" to coinPackage.title.take(60),
                "coinsPurchased" to coinPackage.totalCoins,
                "amountCents" to coinPackage.priceCents,
                "paymentMethod" to method.name,
                "maskedAccount" to cleanMasked,
                "status" to PaymentStatus.PROCESSING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            orderRef.set(createOrderPayload).await()

            delay(1200L)

            val paidAtMillis = System.currentTimeMillis()
            val receiptRef = "TXN-" + Random.nextInt(100000, 999999)
            val txTitle =
                "Bought ${DrawScheduleUtil.formatCoins(coinPackage.totalCoins)} Coins (${coinPackage.formattedPrice})"
                    .take(140)

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val currentBalance = userSnap.getLong("coinBalance")
                    ?: throw IllegalStateException("Wallet profile not found.")
                val newBalance = currentBalance + coinPackage.totalCoins

                transaction.update(
                    orderRef,
                    mapOf(
                        "status" to PaymentStatus.PAID.name,
                        "receiptReference" to receiptRef,
                        "paidAtMillis" to paidAtMillis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    userRef,
                    mapOf(
                        "coinBalance" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "title" to txTitle,
                        "coinDelta" to coinPackage.totalCoins,
                        "type" to TransactionType.COIN_PURCHASE.name,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(
                PaymentOrder(
                    orderId = orderId,
                    userId = uid,
                    packageName = coinPackage.title,
                    coinsPurchased = coinPackage.totalCoins,
                    amountCents = coinPackage.priceCents,
                    paymentMethod = method.name,
                    maskedAccount = cleanMasked,
                    status = PaymentStatus.PAID.name,
                    receiptReference = receiptRef,
                    paidAtMillis = paidAtMillis
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun enterDailyDraw(dateKey: String): Result<String> {
        var currentPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanDateKey = dateKey.trim().take(20)
            require(cleanDateKey.isNotEmpty()) { "Invalid draw date key." }

            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document()
            currentPath = userRef.path
            val cost = DrawScheduleUtil.DRAW_ENTRY_COST_COINS

            val ticket = db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("User wallet not found.")
                }
                val lastDate = userSnap.getString("lastTicketDate")
                val existingTicket = userSnap.getString("todayTicketNumber")
                if (lastDate == cleanDateKey && !existingTicket.isNullOrBlank()) {
                    return@runTransaction existingTicket
                }

                val currentBalance = userSnap.getLong("coinBalance") ?: 0L
                if (currentBalance < cost) {
                    throw IllegalStateException("Need at least $cost coins to enter.")
                }

                val ticketCode = generateTicketCode()
                transaction.update(
                    userRef,
                    mapOf(
                        "coinBalance" to (currentBalance - cost),
                        "todayTicketNumber" to ticketCode,
                        "lastTicketDate" to cleanDateKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "Daily Draw Entry #$ticketCode",
                        "coinDelta" to -cost,
                        "type" to TransactionType.DRAW_ENTRY.name,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                ticketCode
            }.await()

            Result.success(ticket)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun updatePrizePoolCoins(newPrizeCoins: Long): Result<Unit> {
        if (newPrizeCoins !in 100L..1_000_000L) {
            return Result.failure(
                IllegalArgumentException("Prize pool must be between 100 and 1,000,000 coins.")
            )
        }
        val configRef = db.collection("app_state").document("config")
        return try {
            requireUserId()
            db.runTransaction { transaction ->
                val snap = transaction.get(configRef)
                if (!snap.exists()) {
                    transaction.set(
                        configRef,
                        mapOf(
                            "configuredDrawPrizeCoins" to newPrizeCoins,
                            "houseBalanceCoins" to 0L,
                            "houseDrawsCount" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.update(
                        configRef,
                        mapOf(
                            "configuredDrawPrizeCoins" to newPrizeCoins,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, configRef.path)
            Result.failure(e)
        }
    }

    suspend fun drawWinnerByAdmin(
        selectedPlayerId: String? = null,
        playersSnapshot: List<UserProfile> = emptyList()
    ): Result<DrawResult> {
        val configRef = db.collection("app_state").document("config")
        val drawRef = db.collection("draws").document()

        return try {
            requireUserId()
            val chosenPlayer: UserProfile? = if (!selectedPlayerId.isNullOrBlank()) {
                playersSnapshot.firstOrNull { it.id == selectedPlayerId }
                    ?: db.collection("users").document(selectedPlayerId).get().await()
                        .takeIf { it.exists() }
                        ?.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    ?: return Result.failure(IllegalArgumentException("Selected player not found."))
            } else {
                playersSnapshot.randomOrNull()
            }

            val settledResult = db.runTransaction { transaction ->
                // All reads MUST execute before any writes inside a Firestore transaction
                val configSnap = transaction.get(configRef)
                val winnerRef = chosenPlayer?.let { db.collection("users").document(it.id) }
                val winnerSnap = winnerRef?.let { transaction.get(it) }

                val grossPrizeCoins = (configSnap.getLong("configuredDrawPrizeCoins") ?: 10_000L)
                    .coerceIn(100L, 1_000_000L)
                val currentHouseBalance = configSnap.getLong("houseBalanceCoins") ?: 0L
                val currentHouseDraws = configSnap.getLong("houseDrawsCount") ?: 0L

                val winnerPayoutCoins = (grossPrizeCoins * 75L) / 100L
                val houseCutCoins = grossPrizeCoins - winnerPayoutCoins
                val prizeUsd = DrawScheduleUtil.formatUsdFromCoins(winnerPayoutCoins)
                val drawnAtMillis = System.currentTimeMillis()

                val winnerName: String
                val winnerMasked: String
                val ticketCode: String
                val winnerUid: String?

                if (chosenPlayer != null && winnerSnap != null && winnerSnap.exists()) {
                    winnerUid = chosenPlayer.id
                    winnerName = chosenPlayer.name.ifBlank { "Lucky Player" }.take(80)
                    val contactSource = chosenPlayer.phone.ifBlank { chosenPlayer.email }
                    winnerMasked = maskContact(contactSource).take(60)
                    ticketCode = chosenPlayer.todayTicketNumber?.takeIf { it.isNotBlank() }
                        ?: generateTicketCode()

                    val existingWinnerCoins = winnerSnap.getLong("coinBalance") ?: 0L
                    transaction.update(
                        winnerRef,
                        mapOf(
                            "coinBalance" to (existingWinnerCoins + winnerPayoutCoins),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val winnerTxRef = winnerRef.collection("transactions").document()
                    transaction.set(
                        winnerTxRef,
                        mapOf(
                            "userId" to winnerUid,
                            "title" to "Lucky Draw Win (75%) #$ticketCode",
                            "coinDelta" to winnerPayoutCoins,
                            "type" to TransactionType.PRIZE_WIN.name,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val demo = DEMO_PARTICIPANTS.random()
                    winnerUid = null
                    winnerName = demo.first
                    winnerMasked = demo.second
                    ticketCode = generateTicketCode()
                }

                if (!configSnap.exists()) {
                    transaction.set(
                        configRef,
                        mapOf(
                            "configuredDrawPrizeCoins" to grossPrizeCoins,
                            "houseBalanceCoins" to (currentHouseBalance + houseCutCoins),
                            "houseDrawsCount" to (currentHouseDraws + 1L),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.update(
                        configRef,
                        mapOf(
                            "houseBalanceCoins" to (currentHouseBalance + houseCutCoins),
                            "houseDrawsCount" to (currentHouseDraws + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val drawPayload = mapOf(
                    "winnerUserId" to winnerUid,
                    "winnerName" to winnerName,
                    "winnerPhoneMasked" to winnerMasked,
                    "ticketCode" to ticketCode,
                    "grossPrizeCoins" to grossPrizeCoins,
                    "prizeCoins" to winnerPayoutCoins,
                    "houseCutCoins" to houseCutCoins,
                    "prizeUsdFormatted" to prizeUsd,
                    "visibility" to "PUBLIC",
                    "drawnAtMillis" to drawnAtMillis,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>

                transaction.set(drawRef, drawPayload)

                DrawResult(
                    id = drawRef.id,
                    winnerUserId = winnerUid,
                    winnerName = winnerName,
                    winnerPhoneMasked = winnerMasked,
                    ticketCode = ticketCode,
                    grossPrizeCoins = grossPrizeCoins,
                    prizeCoins = winnerPayoutCoins,
                    houseCutCoins = houseCutCoins,
                    prizeUsdFormatted = prizeUsd,
                    visibility = "PUBLIC",
                    drawnAtMillis = drawnAtMillis
                )
            }.await()

            Result.success(settledResult)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, drawRef.path)
            Result.failure(e)
        }
    }

    private fun maskContact(contact: String): String {
        val digits = contact.filter { it.isDigit() }
        if (digits.length >= 4) {
            return DrawScheduleUtil.maskPhone(contact)
        }
        val atIdx = contact.indexOf('@')
        if (atIdx > 1) {
            return contact.take(2) + "•••" + contact.substring(atIdx)
        }
        return "••••"
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private fun generateTicketCode(): String {
        val number = Random.nextInt(1000, 9999)
        return "DLD-$number"
    }

    companion object {
        val COIN_PACKAGES = listOf(
            CoinPackage(
                id = "pack_starter",
                title = "Starter Pack",
                coins = 100L,
                bonusCoins = 0L,
                priceCents = 100L
            ),
            CoinPackage(
                id = "pack_lucky",
                title = "Lucky Pack",
                coins = 500L,
                bonusCoins = 50L,
                priceCents = 500L,
                badge = "+10% BONUS"
            ),
            CoinPackage(
                id = "pack_gold",
                title = "Gold Vault",
                coins = 1_000L,
                bonusCoins = 200L,
                priceCents = 1_000L,
                badge = "POPULAR"
            ),
            CoinPackage(
                id = "pack_royal",
                title = "Royal Jackpot",
                coins = 2_500L,
                bonusCoins = 600L,
                priceCents = 2_500L,
                badge = "BEST VALUE"
            )
        )

        val DEMO_PARTICIPANTS = listOf(
            "Maya Lin" to "••• ••4821",
            "Jordan Patel" to "••• ••9034",
            "Sofia Reyes" to "••• ••1176",
            "Liam O'Connor" to "••• ••6502",
            "Nadia Hassan" to "••• ••3398",
            "Ethan Brooks" to "••• ••7419",
            "Chloe Kim" to "••• ••8250"
        )
    }
}
