package com.universalapi.studio.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 与销售端 Web 后台保持一致的品牌色板 */
object Brand {
    val Bg = Color(0xFF080A12)
    val Surface = Color(0xFF0E1220)
    val SurfaceHigh = Color(0xFF161B2E)
    val SurfaceHighest = Color(0xFF1C2238)
    val Input = Color(0xFF0A0D19)
    val Border = Color(0x14FFFFFF)
    val BorderStrong = Color(0x29FFFFFF)

    val Accent = Color(0xFF7C8CFF)
    val AccentSoft = Color(0xFFC9D0FF)
    val Cyan = Color(0xFF36D6E7)
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Danger = Color(0xFFF87171)

    val TextPrimary = Color(0xFFEEF1F8)
    val TextSecondary = Color(0xFFA4ADC4)
    val TextTertiary = Color(0xFF6B7490)

    val Gradient = Brush.linearGradient(
        listOf(Color(0xFF6D7BFF), Color(0xFF8A63FF), Color(0xFFB46BFF)),
    )
    val PageBackground = Brush.verticalGradient(
        listOf(Color(0xFF131A3A), Color(0xFF0A0D1C), Color(0xFF080A12)),
    )
}

/** 玻璃质感卡片：半透明底 + 细描边 */
fun Modifier.glassCard(radius: Int = 18): Modifier = this
    .background(Brand.SurfaceHigh.copy(alpha = 0.72f), RoundedCornerShape(radius.dp))
    .border(1.dp, Brand.Border, RoundedCornerShape(radius.dp))

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C8CFF),
    onPrimary = Color(0xFF0B0F24),
    primaryContainer = Color(0xFF2A3170),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF36D6E7),
    onSecondary = Color(0xFF00252A),
    secondaryContainer = Color(0xFF16323A),
    onSecondaryContainer = Color(0xFFB8F2F8),
    tertiary = Color(0xFFB46BFF),
    background = Brand.Bg,
    onBackground = Brand.TextPrimary,
    surface = Brand.Surface,
    onSurface = Brand.TextPrimary,
    surfaceVariant = Brand.SurfaceHigh,
    onSurfaceVariant = Brand.TextSecondary,
    surfaceTint = Color(0xFF7C8CFF),
    surfaceContainerLowest = Brand.Bg,
    surfaceContainerLow = Brand.Surface,
    surfaceContainer = Brand.SurfaceHigh,
    surfaceContainerHigh = Brand.SurfaceHigh,
    surfaceContainerHighest = Brand.SurfaceHighest,
    outline = Color(0xFF3A4260),
    outlineVariant = Color(0xFF262C45),
    error = Brand.Danger,
    onError = Color(0xFF2A0A0A),
    errorContainer = Color(0xFF4A1D1D),
    onErrorContainer = Color(0xFFFFD9D9),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun UniversalApiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
