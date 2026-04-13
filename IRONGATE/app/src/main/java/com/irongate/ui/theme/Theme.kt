package com.irongate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.irongate.R

// ─── Colors ──────────────────────────────────────────────────────────────────
object IronColors {
    val Black = Color(0xFF000000)
    val NearBlack = Color(0xFF0A0A0A)
    val Dark1 = Color(0xFF111111)
    val Dark2 = Color(0xFF1A1A1A)
    val Dark3 = Color(0xFF222222)
    val Dark4 = Color(0xFF333333)
    val Gray1 = Color(0xFF666666)
    val Gray2 = Color(0xFFAAAAAA)
    val Gray3 = Color(0xFFCCCCCC)
    val White = Color(0xFFFFFFFF)
    val Online = Color(0xFF44FF88)
    val Warn = Color(0xFFFFAA00)
    val Danger = Color(0xFFFF4444)
    val DangerDark = Color(0xFF330000)
}

val IrongateColorScheme = darkColorScheme(
    primary = IronColors.White,
    onPrimary = IronColors.Black,
    background = IronColors.Black,
    onBackground = IronColors.White,
    surface = IronColors.NearBlack,
    onSurface = IronColors.White,
    surfaceVariant = IronColors.Dark1,
    onSurfaceVariant = IronColors.Gray2,
    outline = IronColors.Dark3,
    secondary = IronColors.Gray2,
    onSecondary = IronColors.Black,
    error = IronColors.Danger,
    onError = IronColors.White
)

// ─── Typography ───────────────────────────────────────────────────────────────
// Pixel font for headers/labels, Share Tech Mono for body/data
object IronType {
    // We use programmatic font loading; pixel font is included as asset
    val Pixel = FontFamily.Monospace   // Replaced by actual font in res/font
    val Mono = FontFamily.Monospace
}

@Composable
fun IrongateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IrongateColorScheme,
        content = content
    )
}
