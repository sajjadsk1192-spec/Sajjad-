package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CoinPackage
import com.example.data.LuckyDrawRepository
import com.example.data.PaymentMethod
import com.example.data.PaymentOrder
import com.example.data.PaymentStatus
import com.example.ui.PaymentCheckoutState
import com.example.ui.theme.LocalLuckyColors
import com.example.ui.theme.TicketCodeTextStyle
import com.example.util.DrawScheduleUtil
import java.util.Locale

@Composable
fun BuyCoinsScreen(
    currentBalance: Long,
    paymentState: PaymentCheckoutState,
    paymentOrders: List<PaymentOrder>,
    onPayAndBuy: (CoinPackage, PaymentMethod, String) -> Unit,
    onResetPaymentState: () -> Unit,
    onDoneToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val packages = remember { LuckyDrawRepository.COIN_PACKAGES }
    var selectedPackage by remember { mutableStateOf(packages[1]) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.GOOGLE_PAY) }

    // Transient in-memory payment inputs — cleared immediately after masking
    var easypaisaPhone by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val luckyColors = LocalLuckyColors.current
    val isProcessing = paymentState is PaymentCheckoutState.Processing

    fun submitPayment() {
        validationError = null
        val maskedAccount: String = when (selectedMethod) {
            PaymentMethod.GOOGLE_PAY -> "Google Pay Wallet"
            PaymentMethod.EASYPAISA -> {
                val digits = easypaisaPhone.filter { it.isDigit() }
                if (digits.length !in 10..13) {
                    validationError = "Enter a valid 10–13 digit Easypaisa mobile number."
                    return
                }
                val last4 = digits.takeLast(4)
                easypaisaPhone = ""
                "Easypaisa •••• $last4"
            }
            PaymentMethod.CARD -> {
                val digits = cardNumber.filter { it.isDigit() }
                if (digits.length !in 12..19) {
                    validationError = "Enter a 12–19 digit card number."
                    return
                }
                if (!Regex("^(0[1-9]|1[0-2])/?[0-9]{2}$").matches(cardExpiry.trim())) {
                    validationError = "Enter expiry as MM/YY."
                    return
                }
                if (cardCvv.filter { it.isDigit() }.length !in 3..4) {
                    validationError = "Enter a 3 or 4 digit CVV."
                    return
                }
                val last4 = digits.takeLast(4)
                cardNumber = ""
                cardExpiry = ""
                cardCvv = ""
                "Card •••• $last4"
            }
        }
        onPayAndBuy(selectedPackage, selectedMethod, maskedAccount)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.payment_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.payment_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = luckyColors.goldContainer,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, luckyColors.goldAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.payment_test_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = luckyColors.onGoldContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Current Wallet Summary Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, luckyColors.goldAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        luckyColors.vaultGradientStart,
                                        luckyColors.vaultGradientEnd
                                    )
                                )
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = null,
                                tint = luckyColors.goldAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.home_balance_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = "${DrawScheduleUtil.formatCoins(currentBalance)} Coins",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = DrawScheduleUtil.formatUsdFromCoins(currentBalance),
                            style = MaterialTheme.typography.titleMedium,
                            color = luckyColors.goldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Paid Confirmation Card OR Checkout Form
            if (paymentState is PaymentCheckoutState.Success) {
                item {
                    PaymentReceiptConfirmationCard(
                        order = paymentState.order,
                        onBuyMore = onResetPaymentState,
                        onDone = {
                            onResetPaymentState()
                            onDoneToHome()
                        }
                    )
                }
            } else {
                // Step 1: Select Coin Pack
                item {
                    Text(
                        text = stringResource(R.string.payment_select_pack),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        packages.chunked(2).forEach { rowPacks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPacks.forEach { pack ->
                                    val selected = selectedPackage.id == pack.id
                                    CoinPackageCard(
                                        coinPackage = pack,
                                        selected = selected,
                                        enabled = !isProcessing,
                                        onClick = { selectedPackage = pack },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 2: Select Payment Method & Pay
                item {
                    Text(
                        text = stringResource(R.string.payment_select_method),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Payment Methods: Google Pay, Easypaisa, Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PaymentMethodChip(
                                    label = stringResource(R.string.payment_method_gpay),
                                    icon = Icons.Filled.Wallet,
                                    selected = selectedMethod == PaymentMethod.GOOGLE_PAY,
                                    enabled = !isProcessing,
                                    onClick = {
                                        selectedMethod = PaymentMethod.GOOGLE_PAY
                                        validationError = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("method_gpay_chip")
                                )
                                PaymentMethodChip(
                                    label = stringResource(R.string.payment_method_easypaisa),
                                    icon = Icons.Filled.PhoneAndroid,
                                    selected = selectedMethod == PaymentMethod.EASYPAISA,
                                    enabled = !isProcessing,
                                    onClick = {
                                        selectedMethod = PaymentMethod.EASYPAISA
                                        validationError = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("method_easypaisa_chip")
                                )
                            }

                            PaymentMethodChip(
                                label = stringResource(R.string.payment_method_card),
                                icon = Icons.Filled.CreditCard,
                                selected = selectedMethod == PaymentMethod.CARD,
                                enabled = !isProcessing,
                                onClick = {
                                    selectedMethod = PaymentMethod.CARD
                                    validationError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("method_card_chip")
                            )

                            AnimatedVisibility(visible = selectedMethod == PaymentMethod.EASYPAISA) {
                                OutlinedTextField(
                                    value = easypaisaPhone,
                                    onValueChange = { input ->
                                        easypaisaPhone = input.filter { it.isDigit() || it == '+' || it == ' ' }.take(16)
                                        validationError = null
                                    },
                                    label = { Text(stringResource(R.string.payment_easypaisa_label)) },
                                    placeholder = { Text(stringResource(R.string.payment_easypaisa_placeholder)) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Filled.PhoneAndroid, contentDescription = null)
                                    },
                                    singleLine = true,
                                    enabled = !isProcessing,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("payment_easypaisa_input")
                                )
                            }

                            AnimatedVisibility(visible = selectedMethod == PaymentMethod.CARD) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = cardNumber,
                                        onValueChange = { input ->
                                            cardNumber = input.filter { it.isDigit() }.take(19)
                                            validationError = null
                                        },
                                        label = { Text(stringResource(R.string.payment_card_number_label)) },
                                        placeholder = { Text("4242 4242 4242 4242") },
                                        singleLine = true,
                                        enabled = !isProcessing,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("payment_card_number_input")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = cardExpiry,
                                            onValueChange = { input ->
                                                cardExpiry = input.take(5)
                                                validationError = null
                                            },
                                            label = { Text(stringResource(R.string.payment_card_expiry_label)) },
                                            placeholder = { Text("12/28") },
                                            singleLine = true,
                                            enabled = !isProcessing,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("payment_card_expiry_input")
                                        )
                                        OutlinedTextField(
                                            value = cardCvv,
                                            onValueChange = { input ->
                                                cardCvv = input.filter { it.isDigit() }.take(4)
                                                validationError = null
                                            },
                                            label = { Text(stringResource(R.string.payment_card_cvv_label)) },
                                            placeholder = { Text("123") },
                                            singleLine = true,
                                            enabled = !isProcessing,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("payment_card_cvv_input")
                                        )
                                    }
                                }
                            }

                            val displayedError = validationError
                                ?: (paymentState as? PaymentCheckoutState.Error)?.message
                            if (displayedError != null) {
                                Text(
                                    text = displayedError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedPackage.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${DrawScheduleUtil.formatCoins(selectedPackage.totalCoins)} Coins credited after payment",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = selectedPackage.formattedPrice,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = { submitPayment() },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = luckyColors.goldAccent,
                                    contentColor = luckyColors.onGoldAccent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("pay_buy_coins_button")
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = luckyColors.onGoldAccent
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.payment_processing),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.payment_pay_cta,
                                            selectedPackage.formattedPrice,
                                            DrawScheduleUtil.formatCoins(selectedPackage.totalCoins)
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.payment_sandbox_notice),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Payment Receipts History
            item {
                Text(
                    text = stringResource(R.string.payment_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (paymentOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = stringResource(R.string.payment_history_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(paymentOrders, key = { it.orderId }) { order ->
                    PaymentOrderRow(order = order)
                }
            }
        }
    }
}

@Composable
private fun CoinPackageCard(
    coinPackage: CoinPackage,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val luckyColors = LocalLuckyColors.current
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("coin_pack_${coinPackage.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                luckyColors.goldContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) luckyColors.goldAccent else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = coinPackage.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        luckyColors.onGoldContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (coinPackage.badge != null) {
                    Surface(
                        color = luckyColors.goldAccent,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = coinPackage.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = luckyColors.onGoldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "${DrawScheduleUtil.formatCoins(coinPackage.totalCoins)} Coins",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = coinPackage.formattedPrice,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val luckyColors = LocalLuckyColors.current
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = luckyColors.goldContainer,
            selectedLabelColor = luckyColors.onGoldContainer,
            selectedLeadingIconColor = luckyColors.onGoldContainer
        ),
        modifier = modifier.heightIn(min = 48.dp)
    )
}

@Composable
private fun PaymentReceiptConfirmationCard(
    order: PaymentOrder,
    onBuyMore: () -> Unit,
    onDone: () -> Unit
) {
    val luckyColors = LocalLuckyColors.current
    val formattedPaid = String.format(Locale.US, "$%.2f", order.amountCents / 100.0)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_receipt_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, luckyColors.positive)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(luckyColors.positive.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = luckyColors.positive,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.payment_receipt_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.payment_receipt_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            ReceiptDetailRow("Order ID", order.orderId)
            ReceiptDetailRow("Receipt Ref", order.receiptReference ?: "—")
            ReceiptDetailRow("Method", order.maskedAccount)
            ReceiptDetailRow("Amount Paid", formattedPaid)
            ReceiptDetailRow(
                "Coins Credited",
                "+${DrawScheduleUtil.formatCoins(order.coinsPurchased)} Coins"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBuyMore,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Buy More")
                }
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = luckyColors.goldAccent,
                        contentColor = luckyColors.onGoldAccent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("payment_receipt_done_button")
                ) {
                    Text(
                        text = stringResource(R.string.payment_receipt_done),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = TicketCodeTextStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaymentOrderRow(order: PaymentOrder) {
    val luckyColors = LocalLuckyColors.current
    val formattedPrice = String.format(Locale.US, "$%.2f", order.amountCents / 100.0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = order.packageName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = if (order.statusEnum == PaymentStatus.PAID) {
                            luckyColors.positive.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = order.statusEnum.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (order.statusEnum == PaymentStatus.PAID) {
                                luckyColors.positive
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${order.maskedAccount} • ${order.receiptReference ?: order.orderId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${DrawScheduleUtil.formatCoins(order.coinsPurchased)} Coins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = luckyColors.positive
                )
                Text(
                    text = "Paid $formattedPrice",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
