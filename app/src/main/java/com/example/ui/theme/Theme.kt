package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThemeMode

// Brand
val Mint = Color(0xFF2DD4BF)
val MintDeep = Color(0xFF0F766E)
val Indigo = Color(0xFF818CF8)
val Rose = Color(0xFFFB7185)
val Amber = Color(0xFFFBBF24)

private val DarkScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF032B27),
    primaryContainer = Color(0xFF0B3B37),
    onPrimaryContainer = Color(0xFFB2F5EA),
    secondary = Indigo,
    onSecondary = Color(0xFF1B1F4A),
    secondaryContainer = Color(0xFF262B5C),
    onSecondaryContainer = Color(0xFFDDE0FF),
    tertiary = Amber,
    error = Rose,
    onError = Color(0xFF3B0712),
    errorContainer = Color(0xFF4C1320),
    onErrorContainer = Color(0xFFFFD9DE),
    background = Color(0xFF0A0E17),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF0A0E17),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1A2130),
    onSurfaceVariant = Color(0xFF9AA5B8),
    surfaceContainerLowest = Color(0xFF070A11),
    surfaceContainerLow = Color(0xFF0F1420),
    surfaceContainer = Color(0xFF131A28),
    surfaceContainerHigh = Color(0xFF1A2231),
    surfaceContainerHighest = Color(0xFF212A3B),
    outline = Color(0xFF334056),
    outlineVariant = Color(0xFF222C3D),
)

private val LightScheme = lightColorScheme(
    primary = MintDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCF5EE),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF14124A),
    tertiary = Color(0xFFB45309),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E8),
    onErrorContainer = Color(0xFF4C0519),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF6F7FB),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE6E9F0),
    onSurfaceVariant = Color(0xFF5B6475),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFCFE),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F2F7),
    surfaceContainerHighest = Color(0xFFE8EBF2),
    outline = Color(0xFFC5CBD6),
    outlineVariant = Color(0xFFE1E5EC),
)

/** Colors Material 3 has no role for. */
@Immutable
data class ExtraColors(val success: Color, val warning: Color, val danger: Color, val isDark: Boolean)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(success = Mint, warning = Amber, danger = Rose, isDark = true)
}

private val AppTypography = Typography().run {
    val f = FontFamily.Default
    copy(
        displaySmall = TextStyle(fontFamily = f, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, letterSpacing = (-0.5).sp),
        headlineMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = (-0.3).sp),
        headlineSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        labelLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun DetoxTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme: ColorScheme = if (dark) DarkScheme else LightScheme
    val extra = ExtraColors(
        success = scheme.primary,
        warning = if (dark) Amber else Color(0xFFB45309),
        danger = scheme.error,
        isDark = dark,
    )
    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
