package com.ghost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ghost.R

// Pure black and white — zero color anywhere
val GhostBlack = Color(0xFF000000)
val GhostWhite = Color(0xFFE8E8E8)
val GhostWhiteDim = Color(0xFF888888)
val GhostWhiteFaint = Color(0xFF333333)
val GhostBorder = Color(0xFF1E1E1E)
val GhostSurface = Color(0xFF0A0A0A)
val GhostSurface2 = Color(0xFF0F0F0F)
val GhostGreen = Color(0xFF4CFF90) // Only for SYS indicator
val GhostRed = Color(0xFFFF4444)   // Only for critical alerts

val GhostColorScheme = darkColorScheme(
    primary = GhostWhite,
    onPrimary = GhostBlack,
    secondary = GhostWhiteDim,
    onSecondary = GhostBlack,
    background = GhostBlack,
    onBackground = GhostWhite,
    surface = GhostSurface,
    onSurface = GhostWhite,
    outline = GhostBorder,
    error = GhostRed,
)

// Minecraft-style pixel font
val MinecraftFont = FontFamily(
    Font(R.font.minecraft_regular, FontWeight.Normal),
    Font(R.font.minecraft_bold, FontWeight.Bold)
)

object GhostTypography {
    val display = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = GhostWhite,
        letterSpacing = 0.2.sp
    )
    val title = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = GhostWhite,
        letterSpacing = 0.15.sp
    )
    val body = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        color = GhostWhite,
        letterSpacing = 0.08.sp,
        lineHeight = 18.sp
    )
    val small = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        color = GhostWhiteDim,
        letterSpacing = 0.12.sp
    )
    val micro = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 6.sp,
        color = GhostWhiteFaint,
        letterSpacing = 0.1.sp
    )
    val label = TextStyle(
        fontFamily = MinecraftFont,
        fontWeight = FontWeight.Normal,
        fontSize = 7.sp,
        color = GhostWhiteDim,
        letterSpacing = 0.14.sp
    )
}

@Composable
fun GhostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GhostColorScheme,
        content = content
    )
}
