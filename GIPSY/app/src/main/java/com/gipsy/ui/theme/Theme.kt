package com.gipsy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import com.gipsy.R

// ── COLORS ──────────────────────────────────────────────────
object GipsyColors {
    val Black       = Color(0xFF000000)
    val White       = Color(0xFFFFFFFF)
    val OffWhite    = Color(0xFFE0E0E0)
    val DimWhite    = Color(0xFF9E9E9E)
    val DarkGray    = Color(0xFF1A1A1A)
    val MidGray     = Color(0xFF2C2C2C)
    val BorderGray  = Color(0xFF3A3A3A)
    val Transparent = Color(0x00000000)

    // Status colors (still monochrome)
    val Alert       = Color(0xFFFFFFFF)  // white pulse for alerts
    val Dim         = Color(0xFF555555)  // dimmed state
}

// ── FONT FAMILY (Press Start 2P = Minecraft style) ──────────
val MinecraftFont = FontFamily(
    Font(R.font.press_start_2p, FontWeight.Normal)
)

val GipsyTypography = Typography(
    // Large display — app name, protocol names
    displayLarge = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 4.sp,
        color = GipsyColors.White
    ),
    // Screen titles
    displayMedium = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 3.sp,
        color = GipsyColors.White
    ),
    // Section headers
    displaySmall = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 2.sp,
        color = GipsyColors.White
    ),
    // Body text — chat messages
    bodyLarge = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.sp,
        color = GipsyColors.White
    ),
    bodyMedium = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
        color = GipsyColors.OffWhite
    ),
    bodySmall = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 7.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = GipsyColors.DimWhite
    ),
    // Labels — buttons, tags
    labelLarge = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        letterSpacing = 2.sp,
        color = GipsyColors.White
    ),
    labelMedium = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        letterSpacing = 1.5.sp,
        color = GipsyColors.OffWhite
    ),
    labelSmall = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 7.sp,
        letterSpacing = 1.sp,
        color = GipsyColors.DimWhite
    )
)

// ── COLOR SCHEME ─────────────────────────────────────────────
private val GipsyColorScheme = darkColorScheme(
    primary             = GipsyColors.White,
    onPrimary           = GipsyColors.Black,
    primaryContainer    = GipsyColors.DarkGray,
    onPrimaryContainer  = GipsyColors.White,
    secondary           = GipsyColors.OffWhite,
    onSecondary         = GipsyColors.Black,
    background          = GipsyColors.Black,
    onBackground        = GipsyColors.White,
    surface             = GipsyColors.DarkGray,
    onSurface           = GipsyColors.White,
    surfaceVariant      = GipsyColors.MidGray,
    onSurfaceVariant    = GipsyColors.OffWhite,
    outline             = GipsyColors.BorderGray,
    error               = GipsyColors.White,
    onError             = GipsyColors.Black,
)

// ── THEME ─────────────────────────────────────────────────────
@Composable
fun GipsyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GipsyColorScheme,
        typography = GipsyTypography,
        content = content
    )
}
