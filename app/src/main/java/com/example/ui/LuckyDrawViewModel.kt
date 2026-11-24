package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppConfig
import com.example.data.CoinPackage
import com.example.data.CoinTransaction
import com.example.data.DrawResult
import com.example.data.LuckyDrawRepository
import com.example.data.PaymentMethod
import com.example.data.PaymentOrder
import com.example.data.UserProfile
import com.example.util.DrawScheduleUtil
import com.example.util.DrawTimerState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LuckyDrawUiState {
    data object Loading : LuckyDrawUiState
    data class Error(val message: String) : LuckyDrawUiState
    data class Ready(
        val user: UserProfile,
        val isAdmin: Boolean,
        val appConfig: AppConfig,
        val players: List<UserProfile>,
        val transactions: List<CoinTransaction>,
        val paymentOrders: List<PaymentOrder>,
        val drawResults: List<DrawResult>,
        val hasEnteredToday: Boolean,
        val todayDateKey: String
    ) : LuckyDrawUiState
}

sealed interface PaymentCheckoutState {
    data object Idle : PaymentCheckoutState
    data object Processing : PaymentCheckoutState
    data class Success(val order: PaymentOrder) : PaymentCheckoutState
    data class Error(val message: String) : PaymentCheckoutState
}

private data class SessionBundle(
    val config: AppConfig,
    val activeUser: UserProfile?,
    val isAdmin: Boolean,
    val players: List<UserProfile>
)

private data class LedgerBundle(
    val transactions: List<CoinTransaction>,
    val paymentOrders: List<PaymentOrder>,
    val drawResults: List<DrawResult>,
    val timer: DrawTimerState
)

@OptIn(ExperimentalCoroutinesApi::class)
class LuckyDrawViewModel(
    private val repository: LuckyDrawRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _timerState = MutableStateFlow(DrawScheduleUtil.calculateTimerState())
    val timerState: StateFlow<DrawTimerState> = _timerState.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentCheckoutState>(PaymentCheckoutState.Idle)
    val paymentState: StateFlow<PaymentCheckoutState> = _paymentState.asStateFlow()

    private val _isDrawingWinner = MutableStateFlow(false)
    val isDrawingWinner: StateFlow<Boolean> = _isDrawingWinner.asStateFlow()

    private val _shufflingParticipant = MutableStateFlow<String?>(null)
    val shufflingParticipant: StateFlow<String?> = _shufflingParticipant.asStateFlow()

    private val _latestDrawnWinner = MutableStateFlow<DrawResult?>(null)
    val latestDrawnWinner: StateFlow<DrawResult?> = _latestDrawnWinner.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)

    private val isAdminFlow = repository.observeAdminMarker(currentUserId)
        .map { markerExists -> markerExists || repository.isVerifiedOwnerEmail() }

    private val playersFlow = isAdminFlow.flatMapLatest { isAdmin ->
        repository.observeAllPlayers(isAdmin, currentUserId)
    }

    private val ordersFlow = isAdminFlow.flatMapLatest { isAdmin ->
        repository.observePaymentOrders(currentUserId, isAdmin)
    }

    private val sessionFlow = combine(
        repository.observeAppConfig(),
        repository.observeUserProfile(currentUserId),
        isAdminFlow,
        playersFlow
    ) { config, activeUser, isAdmin, players ->
        SessionBundle(config, activeUser, isAdmin, players)
    }

    private val ledgerFlow = combine(
        repository.observeTransactions(currentUserId),
        ordersFlow,
        repository.observeDrawResults(),
        _timerState
    ) { txs, orders, results, timer ->
        LedgerBundle(txs, orders, results, timer)
    }

    val uiState: StateFlow<LuckyDrawUiState> = combine(
        sessionFlow,
        ledgerFlow,
        _profileError
    ) { session, ledger, profileError ->
        val activeUser = session.activeUser
        when {
            profileError != null -> LuckyDrawUiState.Error(profileError)
            activeUser == null -> LuckyDrawUiState.Loading
            else -> {
                val enteredToday = activeUser.lastTicketDate == ledger.timer.todayDateKey &&
                    !activeUser.todayTicketNumber.isNullOrBlank()
                LuckyDrawUiState.Ready(
                    user = activeUser,
                    isAdmin = session.isAdmin,
                    appConfig = session.config,
                    players = session.players,
                    transactions = ledger.transactions,
                    paymentOrders = ledger.paymentOrders,
                    drawResults = ledger.drawResults,
                    hasEnteredToday = enteredToday,
                    todayDateKey = ledger.timer.todayDateKey
                )
            }
        }
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing lucky draw state", rootError)
        emit(
            LuckyDrawUiState.Error(
                rootError.localizedMessage ?: "Unable to sync your wallet with Firestore."
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LuckyDrawUiState.Loading
    )

    init {
        ensureProfileInitialized()
        viewModelScope.launch {
            while (isActive) {
                _timerState.value = DrawScheduleUtil.calculateTimerState()
                delay(1000L)
            }
        }
    }

    fun retrySync() {
        ensureProfileInitialized()
    }

    private fun ensureProfileInitialized() {
        viewModelScope.launch {
            _profileError.value = null
            repository.ensureUserProfile()
                .onFailure { err ->
                    _profileError.value = err.localizedMessage ?: "Failed to initialize wallet."
                }
        }
    }

    fun purchaseCoins(
        coinPackage: CoinPackage,
        method: PaymentMethod,
        maskedAccount: String
    ) {
        if (_paymentState.value is PaymentCheckoutState.Processing) return
        viewModelScope.launch {
            _paymentState.value = PaymentCheckoutState.Processing
            val result = repository.purchaseCoins(
                coinPackage = coinPackage,
                method = method,
                maskedAccount = maskedAccount
            )
            result.onSuccess { order ->
                _paymentState.value = PaymentCheckoutState.Success(order)
                _statusMessage.value =
                    "+${DrawScheduleUtil.formatCoins(order.coinsPurchased)} coins credited after payment!"
            }.onFailure { err ->
                _paymentState.value = PaymentCheckoutState.Error(
                    err.message ?: "Payment failed. Please try again."
                )
            }
        }
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentCheckoutState.Idle
    }

    fun enterTodaysDraw() {
        viewModelScope.launch {
            val dateKey = _timerState.value.todayDateKey
            val result = repository.enterDailyDraw(dateKey)
            result.onSuccess { ticket ->
                _statusMessage.value = "Ticket #$ticket confirmed for today's draw!"
            }.onFailure { error ->
                _statusMessage.value = error.message ?: "Unable to enter draw."
            }
        }
    }

    fun updatePrizePoolCoins(newPrizeCoins: Long) {
        viewModelScope.launch {
            repository.updatePrizePoolCoins(newPrizeCoins)
                .onSuccess {
                    _statusMessage.value =
                        "Draw prize pool updated to ${DrawScheduleUtil.formatCoins(newPrizeCoins)} Coins."
                }
                .onFailure { err ->
                    _statusMessage.value = err.message ?: "Unable to update prize pool."
                }
        }
    }

    fun drawWinnerByAdmin(selectedPlayerId: String? = null) {
        if (_isDrawingWinner.value) return
        val currentState = uiState.value as? LuckyDrawUiState.Ready ?: return
        if (!currentState.isAdmin) {
            _statusMessage.value = "Only Admin can draw a winner."
            return
        }

        viewModelScope.launch {
            _isDrawingWinner.value = true
            val candidates = if (currentState.players.isNotEmpty()) {
                currentState.players.map { it.name.ifBlank { it.email } }
            } else {
                LuckyDrawRepository.DEMO_PARTICIPANTS.map { it.first }
            }

            repeat(14) { idx ->
                _shufflingParticipant.value = candidates[idx % candidates.size]
                delay(110L)
            }

            val result = repository.drawWinnerByAdmin(
                selectedPlayerId = selectedPlayerId,
                playersSnapshot = currentState.players
            )
            _shufflingParticipant.value = null
            _isDrawingWinner.value = false

            result.onSuccess { winner ->
                _latestDrawnWinner.value = winner
                _statusMessage.value =
                    "${winner.winnerName} won ${DrawScheduleUtil.formatCoins(winner.prizeCoins)} Coins (75%) • House +${
                        DrawScheduleUtil.formatCoins(winner.houseCutCoins)
                    } Coins (25%)"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Draw failed."
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private companion object {
        const val TAG = "LuckyDrawVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
