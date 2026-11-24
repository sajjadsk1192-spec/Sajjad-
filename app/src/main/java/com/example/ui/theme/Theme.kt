package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class LuckyExtendedColors(
    val goldAccent: Color,
    val onGoldAccent: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val vaultGradientStart: Color,
    val vaultGradientEnd: Color,
    val positive: Color
)

val LocalLuckyColors = staticCompositionLocalOf {
    LuckyExtendedColors(
        goldAccent = GoldBullion,
        onGoldAccent = InkDark,
        goldContainer = GoldSoftContainer,
        onGoldContainer = GoldDeep,
        vaultGradientStart = EmeraldDeep,
        vaultGradientEnd = EmeraldRich,
        positive = PositiveGreen
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8EFE0),
    onPrimaryContainer = Color(0xFF04291C),
    secondary = GoldDeep,
    onSecondary = Color.White,
    secondaryContainer = GoldSoftContainer,
    onSecondaryContainer = Color(0xFF332100),
    tertiary = EmeraldRich,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = InkDark,
    surface = IvorySurface,
    onSurface = InkDark,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineLight,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = GoldBright,
    onPrimary = Color(0xFF2B1D00),
    primaryContainer = EmeraldRich,
    onPrimaryContainer = MintTextLight,
    secondary = GoldBullion,
    onSecondary = Color(0xFF2B1D00),
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = GoldBright,
    tertiary = Color(0xFF6CD9AD),
    onTertiary = Color(0xFF003824),
    background = EmeraldDarkSurface,
    onBackground = MintTextLight,
    surface = EmeraldDarkCard,
    onSurface = MintTextLight,
    surfaceVariant = EmeraldDarkElevated,
    onSurfaceVariant = MintTextMuted,
    outline = OutlineDark,
    error = ErrorRedDark,
    onError = Color(0xFF690005)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DailyLuckyDrawTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        LuckyExtendedColors(
            goldAccent = GoldBright,
            onGoldAccent = Color(0xFF241800),
            goldContainer = GoldDarkContainer,
            onGoldContainer = GoldBright,
            vaultGradientStart = Color(0xFF093927),
            vaultGradientEnd = Color(0xFF135C41),
            positive = PositiveGreenDark
        )
    } else {
        LuckyExtendedColors(
            goldAccent = GoldBullion,
            onGoldAccent = InkDark,
            goldContainer = GoldSoftContainer,
            onGoldContainer = GoldDeep,
            vaultGradientStart = EmeraldDeep,
            vaultGradientEnd = EmeraldRich,
            positive = PositiveGreen
        )
    }

    CompositionLocalProvider(LocalLuckyColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
