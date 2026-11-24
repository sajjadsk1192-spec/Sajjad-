package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DrawResult
import com.example.ui.theme.LocalLuckyColors
import com.example.ui.theme.TicketCodeTextStyle
import com.example.util.DrawScheduleUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ResultsScreen(
    currentUserId: String,
    isAdmin: Boolean,
    configuredPrizeCoins: Long,
    drawResults: List<DrawResult>,
    latestDrawnWinner: DrawResult?,
    isDrawingWinner: Boolean,
    shufflingParticipant: String?,
    onDrawWinner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val luckyColors = LocalLuckyColors.current
    val spotlightWinner = latestDrawnWinner ?: drawResults.firstOrNull()
    val winnerShareCoins = (configuredPrizeCoins * 75L) / 100L
    val houseShareCoins = configuredPrizeCoins - winnerShareCoins

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
            // Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.results_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        color = luckyColors.goldContainer,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, luckyColors.goldAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isAdmin) {
                                stringResource(R.string.results_admin_badge)
                            } else {
                                stringResource(R.string.results_player_badge)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = luckyColors.onGoldContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Hero Card: Admin gets Draw Winner button + animations; Players see 75%/25% read-only info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, luckyColors.goldAccent.copy(alpha = 0.45f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        luckyColors.vaultGradientStart,
                                        luckyColors.vaultGradientEnd
                                    )
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_winner_trophy),
                                contentDescription = stringResource(R.string.results_hero_title),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                luckyColors.vaultGradientStart.copy(alpha = 0.92f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.results_hero_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAdmin) {
                                        stringResource(R.string.results_hero_subtitle_admin)
                                    } else {
                                        stringResource(R.string.results_hero_subtitle_player)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 75% / 25% Split Breakdown Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Winner (75%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.78f)
                                        )
                                        Text(
                                            text = "${DrawScheduleUtil.formatCoins(winnerShareCoins)} Coins",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = luckyColors.goldAccent
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "House (25%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.78f)
                                        )
                                        Text(
                                            text = "${DrawScheduleUtil.formatCoins(houseShareCoins)} Coins",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            if (isAdmin) {
                                AnimatedVisibility(
                                    visible = isDrawingWinner,
                                    enter = fadeIn() + scaleIn(initialScale = 0.92f),
                                    exit = fadeOut()
                                ) {
                                    DrawWheelTickerBanner(
                                        shufflingParticipant = shufflingParticipant ?: "Selecting…"
                                    )
                                }

                                val buttonSpinTransition = rememberInfiniteTransition(label = "draw_btn_spin")
                                val buttonIconAngle by buttonSpinTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 900, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "button_icon_angle"
                                )

                                Button(
                                    onClick = onDrawWinner,
                                    enabled = !isDrawingWinner,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = luckyColors.goldAccent,
                                        contentColor = luckyColors.onGoldAccent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .testTag("draw_winner_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(if (isDrawingWinner) buttonIconAngle else 0f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isDrawingWinner) {
                                            stringResource(R.string.results_drawing_cta)
                                        } else {
                                            stringResource(R.string.results_draw_winner_cta)
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("player_draw_locked_notice"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = luckyColors.goldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.results_player_locked_note),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Latest Winner Spotlight
            if (spotlightWinner != null && !isDrawingWinner) {
                item(key = "spotlight_${spotlightWinner.drawnAtMillis}") {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(
                            initialScale = 0.88f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    ) {
                        SpotlightWinnerCard(
                            winner = spotlightWinner,
                            isCurrentUser = spotlightWinner.winnerUserId == currentUserId
                        )
                    }
                }
            }

            // Past Draw Winners Header
            item {
                Text(
                    text = stringResource(R.string.results_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (drawResults.isEmpty()) {
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
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = stringResource(R.string.results_history_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(drawResults, key = { it.id }) { result ->
                    WinnerHistoryRow(
                        result = result,
                        isCurrentUser = result.winnerUserId == currentUserId
                    )
                }
            }
        }

        // Confetti Celebration Burst Overlay when a new winner is revealed
        if (latestDrawnWinner != null && !isDrawingWinner) {
            WinnerConfettiOverlay(
                triggerKey = latestDrawnWinner.drawnAtMillis,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DrawWheelTickerBanner(
    shufflingParticipant: String
) {
    val luckyColors = LocalLuckyColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "lucky_wheel_transition")
    val wheelRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wheel_rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wheel_pulse"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draw_animation_banner"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, luckyColors.goldAccent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    rotate(degrees = wheelRotation) {
                        val segmentColors = listOf(
                            luckyColors.goldAccent,
                            Color.White.copy(alpha = 0.85f),
                            luckyColors.goldContainer,
                            luckyColors.positive
                        )
                        segmentColors.forEachIndexed { idx, color ->
                            drawArc(
                                color = color,
                                startAngle = idx * 90f,
                                sweepAngle = 68f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    drawCircle(
                        color = luckyColors.goldAccent.copy(alpha = 0.25f),
                        radius = (diameter / 2f) * pulseScale * 0.65f
                    )
                }
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = luckyColors.goldAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "DRAWING LUCKY TICKET…",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                AnimatedContent(
                    targetState = shufflingParticipant,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn(tween(90))) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut(tween(90)))
                    },
                    label = "participant_ticker"
                ) { participantName ->
                    Text(
                        text = participantName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = luckyColors.goldAccent
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val startXFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val sizeDp: Float,
    val rotationSpeed: Float,
    val colorIndex: Int
)

@Composable
private fun WinnerConfettiOverlay(
    triggerKey: Long,
    modifier: Modifier = Modifier
) {
    val luckyColors = LocalLuckyColors.current
    val progress = remember(triggerKey) { Animatable(0f) }

    val particles = remember(triggerKey) {
        val rng = Random(triggerKey)
        List(36) {
            ConfettiParticle(
                startXFraction = 0.15f + rng.nextFloat() * 0.70f,
                velocityX = (rng.nextFloat() - 0.5f) * 2.2f,
                velocityY = -(0.6f + rng.nextFloat() * 1.1f),
                sizeDp = 6f + rng.nextFloat() * 6f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 720f,
                colorIndex = idxColor(it)
            )
        }
    }

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1750, easing = FastOutSlowInEasing)
        )
    }

    val t = progress.value
    if (t < 1f) {
        val palette = listOf(
            luckyColors.goldAccent,
            luckyColors.positive,
            Color.White,
            luckyColors.goldContainer
        )
        Canvas(
            modifier = modifier.testTag("winner_confetti_overlay")
        ) {
            val alpha = (1f - ((t - 0.6f).coerceAtLeast(0f) / 0.4f)).coerceIn(0f, 1f)
            val originY = size.height * 0.34f
            particles.forEach { p ->
                val x = size.width * p.startXFraction + p.velocityX * size.width * 0.28f * t
                val gravity = 1.85f * t * t
                val y = originY + (p.velocityY * t + gravity) * size.height * 0.45f
                val pxSize = p.sizeDp.dp.toPx()
                val angle = p.rotationSpeed * t
                val rad = angle * (PI / 180.0)
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()

                rotate(degrees = angle, pivot = Offset(x, y)) {
                    drawRect(
                        color = palette[p.colorIndex % palette.size].copy(alpha = alpha),
                        topLeft = Offset(x - pxSize / 2f, y - pxSize * 0.35f),
                        size = Size(
                            pxSize * (0.7f + 0.3f * kotlin.math.abs(cosA)),
                            pxSize * (0.45f + 0.25f * kotlin.math.abs(sinA))
                        )
                    )
                }
            }
        }
    }
}

private fun idxColor(index: Int): Int = index % 4

@Composable
private fun SpotlightWinnerCard(
    winner: DrawResult,
    isCurrentUser: Boolean
) {
    val luckyColors = LocalLuckyColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("latest_winner_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, luckyColors.goldAccent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.results_latest_winner_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "#${winner.ticketCode}",
                        style = TicketCodeTextStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (isCurrentUser) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = luckyColors.goldContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stars,
                            contentDescription = null,
                            tint = luckyColors.onGoldContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.results_you_won_banner),
                            style = MaterialTheme.typography.labelMedium,
                            color = luckyColors.onGoldContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = luckyColors.onGoldAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = winner.winnerName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = winner.winnerPhoneMasked,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = winner.prizeUsdFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        color = luckyColors.positive,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${DrawScheduleUtil.formatCoins(winner.prizeCoins)} Coins (75%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Pool: ${DrawScheduleUtil.formatCoins(winner.grossPrizeCoins)} Coins",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "House (25%): ${DrawScheduleUtil.formatCoins(winner.houseCutCoins)} Coins",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WinnerHistoryRow(
    result: DrawResult,
    isCurrentUser: Boolean
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = result.winnerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentUser) {
                        Surface(
                            color = luckyColors.goldContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = luckyColors.onGoldContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "#${result.ticketCode} • House +${DrawScheduleUtil.formatCoins(result.houseCutCoins)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = result.prizeUsdFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = luckyColors.positive
                )
                Text(
                    text = "${DrawScheduleUtil.formatCoins(result.prizeCoins)} (75%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
