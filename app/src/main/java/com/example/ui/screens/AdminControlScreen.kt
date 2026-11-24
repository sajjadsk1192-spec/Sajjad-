package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppConfig
import com.example.data.DrawResult
import com.example.data.PaymentOrder
import com.example.data.PaymentStatus
import com.example.data.UserProfile
import com.example.ui.theme.LocalLuckyColors
import com.example.util.DrawScheduleUtil
import java.util.Locale

@Composable
fun AdminControlScreen(
    appConfig: AppConfig,
    players: List<UserProfile>,
    paymentOrders: List<PaymentOrder>,
    drawResults: List<DrawResult>,
    todayDateKey: String,
    isDrawingWinner: Boolean,
    shufflingParticipant: String?,
    onUpdatePrizePool: (Long) -> Unit,
    onTriggerWinnerSelection: (selectedPlayerId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val luckyColors = LocalLuckyColors.current
    val presets = listOf(1_000L, 5_000L, 10_000L, 50_000L)
    var customPrizeText by remember(appConfig.configuredDrawPrizeCoins) {
        mutableStateOf(appConfig.configuredDrawPrizeCoins.toString())
    }
    var prizeError by remember { mutableStateOf<String?>(null) }
    var selectedPlayerIdForDraw by remember { mutableStateOf<String?>(null) }

    val parsedPrize = customPrizeText.toLongOrNull() ?: appConfig.configuredDrawPrizeCoins
    val winnerShare = (parsedPrize * 75L) / 100L
    val houseShare = parsedPrize - winnerShare

    // Aggregate user statistics
    val paidOrders = remember(paymentOrders) {
        paymentOrders.filter { it.statusEnum == PaymentStatus.PAID }
    }
    val totalDepositedCents = remember(paidOrders) {
        paidOrders.sumOf { it.amountCents }
    }
    val totalPlayerCoins = remember(players) {
        players.sumOf { it.coinBalance }
    }
    val activeEntriesToday = remember(players, todayDateKey) {
        players.count { it.lastTicketDate == todayDateKey && !it.todayTicketNumber.isNullOrBlank() }
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.admin_panel_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // House Treasury Card (25% Cut)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("house_treasury_card"),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, luckyColors.goldAccent.copy(alpha = 0.45f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        luckyColors.vaultGradientStart,
                                        luckyColors.vaultGradientEnd
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.admin_house_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(luckyColors.goldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalance,
                                        contentDescription = null,
                                        tint = luckyColors.onGoldAccent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${DrawScheduleUtil.formatCoins(appConfig.houseBalanceCoins)} Coins",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${DrawScheduleUtil.formatUsdFromCoins(appConfig.houseBalanceCoins)} USD • ${appConfig.houseDrawsCount} draws settled",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = luckyColors.goldAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // User Statistics Overview (2x2 Metrics Grid)
            item {
                Text(
                    text = stringResource(R.string.admin_stats_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Column(
                    modifier = Modifier.testTag("admin_user_statistics_grid"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            icon = Icons.Filled.People,
                            label = "Registered Users",
                            value = players.size.toString(),
                            subtitle = "Active profiles",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            icon = Icons.Filled.ConfirmationNumber,
                            label = "Today's Entries",
                            value = activeEntriesToday.toString(),
                            subtitle = "Tickets issued",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            icon = Icons.Filled.MonetizationOn,
                            label = "Player Coins",
                            value = DrawScheduleUtil.formatCoins(totalPlayerCoins),
                            subtitle = DrawScheduleUtil.formatUsdFromCoins(totalPlayerCoins),
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            icon = Icons.Filled.Payments,
                            label = "Paid Deposits",
                            value = String.format(Locale.US, "$%.2f", totalDepositedCents / 100.0),
                            subtitle = "${paidOrders.size} paid orders",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Manual Winner Selection Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_manual_draw_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.5.dp, luckyColors.goldAccent.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.admin_manual_draw_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(R.string.admin_manual_draw_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (players.isNotEmpty()) {
                            Text(
                                text = "Winner Mode",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedPlayerIdForDraw == null,
                                    enabled = !isDrawingWinner,
                                    onClick = { selectedPlayerIdForDraw = null },
                                    label = { Text("Random Draw") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = luckyColors.goldContainer,
                                        selectedLabelColor = luckyColors.onGoldContainer
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                                players.forEach { player ->
                                    FilterChip(
                                        selected = selectedPlayerIdForDraw == player.id,
                                        enabled = !isDrawingWinner,
                                        onClick = { selectedPlayerIdForDraw = player.id },
                                        label = { Text(player.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = luckyColors.goldContainer,
                                            selectedLabelColor = luckyColors.onGoldContainer
                                        ),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isDrawingWinner,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = luckyColors.goldContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Selecting Winner:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = luckyColors.onGoldContainer
                                    )
                                    AnimatedContent(
                                        targetState = shufflingParticipant ?: "…",
                                        transitionSpec = {
                                            (slideInVertically { it / 2 } + fadeIn(tween(90))) togetherWith
                                                (slideOutVertically { -it / 2 } + fadeOut(tween(90)))
                                        },
                                        label = "admin_ticker"
                                    ) { candidate ->
                                        Text(
                                            text = candidate,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = luckyColors.onGoldContainer
                                        )
                                    }
                                }
                            }
                        }

                        val spinTransition = rememberInfiniteTransition(label = "admin_draw_spin")
                        val spinAngle by spinTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 850, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "admin_spin_angle"
                        )

                        Button(
                            onClick = { onTriggerWinnerSelection(selectedPlayerIdForDraw) },
                            enabled = !isDrawingWinner,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = luckyColors.goldAccent,
                                contentColor = luckyColors.onGoldAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("admin_trigger_draw_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(if (isDrawingWinner) spinAngle else 0f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isDrawingWinner -> "Selecting Winner…"
                                    selectedPlayerIdForDraw != null -> "Confirm & Award Selected Winner"
                                    else -> "Trigger Winner Draw"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Prize Pool Configuration Card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.admin_prize_pool_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { preset ->
                                FilterChip(
                                    selected = appConfig.configuredDrawPrizeCoins == preset,
                                    onClick = {
                                        customPrizeText = preset.toString()
                                        prizeError = null
                                        onUpdatePrizePool(preset)
                                    },
                                    label = {
                                        Text(
                                            text = DrawScheduleUtil.formatCoins(preset),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = luckyColors.goldContainer,
                                        selectedLabelColor = luckyColors.onGoldContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            OutlinedTextField(
                                value = customPrizeText,
                                onValueChange = { input ->
                                    customPrizeText = input.filter { it.isDigit() }.take(7)
                                    prizeError = null
                                },
                                label = { Text("Custom Pool (Coins)") },
                                isError = prizeError != null,
                                supportingText = prizeError?.let { err -> { Text(err) } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_prize_input")
                            )
                            Button(
                                onClick = {
                                    val value = customPrizeText.toLongOrNull() ?: 0L
                                    if (value in 100L..1_000_000L) {
                                        onUpdatePrizePool(value)
                                    } else {
                                        prizeError = "100 – 1,000,000 coins"
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .heightIn(min = 52.dp)
                                    .testTag("admin_save_prize_button")
                            ) {
                                Text("Save")
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Winner (75%): ${DrawScheduleUtil.formatCoins(winnerShare)} Coins",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = luckyColors.positive
                                )
                                Text(
                                    text = "House (25%): ${DrawScheduleUtil.formatCoins(houseShare)} Coins",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Per-Player Detailed Statistics List
            item {
                Text(
                    text = stringResource(R.string.admin_players_title, players.size),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (players.isEmpty()) {
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
                                imageVector = Icons.Filled.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = stringResource(R.string.admin_players_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(players, key = { it.id }) { player ->
                    val playerOrders = paidOrders.filter { it.userId == player.id }
                    val playerPaidCents = playerOrders.sumOf { it.amountCents }
                    val playerWins = drawResults.count { it.winnerUserId == player.id }
                    val enteredToday = player.lastTicketDate == todayDateKey &&
                        !player.todayTicketNumber.isNullOrBlank()

                    PlayerStatsRow(
                        player = player,
                        paidCents = playerPaidCents,
                        winsCount = playerWins,
                        enteredToday = enteredToday
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlayerStatsRow(
    player: UserProfile,
    paidCents: Long,
    winsCount: Int,
    enteredToday: Boolean
) {
    val luckyColors = LocalLuckyColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = player.phone.ifBlank { player.email },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${DrawScheduleUtil.formatCoins(player.coinBalance)} Coins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = DrawScheduleUtil.formatUsdFromCoins(player.coinBalance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deposited: ${String.format(Locale.US, "$%.2f", paidCents / 100.0)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = luckyColors.positive
                )
                Text(
                    text = "Wins: $winsCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (enteredToday) {
                        luckyColors.goldContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = if (enteredToday) "#${player.todayTicketNumber}" else "No Ticket",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (enteredToday) {
                            luckyColors.onGoldContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
