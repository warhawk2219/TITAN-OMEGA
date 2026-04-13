package com.ghost.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.ghost.R
import com.ghost.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen() {
    var logoAlpha by remember { mutableStateOf(0f) }
    var logoScale by remember { mutableStateOf(0.8f) }
    var textAlpha by remember { mutableStateOf(0f) }
    var subTextAlpha by remember { mutableStateOf(0f) }
    var scanLineOffset by remember { mutableStateOf(0f) }
    var bootText by remember { mutableStateOf("") }

    val logoAlphaAnim by animateFloatAsState(
        targetValue = logoAlpha,
        animationSpec = tween(1000, easing = EaseInOut),
        label = "logo_alpha"
    )
    val logoScaleAnim by animateFloatAsState(
        targetValue = logoScale,
        animationSpec = tween(1200, easing = EaseOutBack),
        label = "logo_scale"
    )
    val textAlphaAnim by animateFloatAsState(
        targetValue = textAlpha,
        animationSpec = tween(600),
        label = "text_alpha"
    )
    val subTextAlphaAnim by animateFloatAsState(
        targetValue = subTextAlpha,
        animationSpec = tween(600),
        label = "sub_alpha"
    )

    LaunchedEffect(Unit) {
        delay(200)
        logoAlpha = 1f
        logoScale = 1f
        delay(1000)
        textAlpha = 1f
        delay(400)
        subTextAlpha = 1f
        delay(200)

        val bootLines = listOf(
            "INITIALIZING CORE ENGINE...",
            "LOADING VOSK MODEL...",
            "GEMMA 3 1B READY...",
            "BRIDGE CONNECTION...",
            "ALL SYSTEMS NOMINAL...",
            "GHOST ONLINE."
        )
        for (line in bootLines) {
            bootText = line
            delay(280)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GhostBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Ghost skull logo
            Image(
                painter = painterResource(id = R.drawable.ghost_logo),
                contentDescription = "GHOST",
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScaleAnim)
                    .alpha(logoAlphaAnim)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // GHOST title
            Text(
                text = "GHOST",
                style = GhostTypography.display.copy(fontSize = 28.sp),
                modifier = Modifier.alpha(textAlphaAnim),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = "GENERAL HANDHELD OPERATIVE\nSYSTEM TERMINAL",
                style = GhostTypography.micro.copy(
                    color = GhostWhiteDim,
                    letterSpacing = 0.2.sp
                ),
                modifier = Modifier.alpha(subTextAlphaAnim),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Boot text
            if (bootText.isNotEmpty()) {
                Text(
                    text = bootText,
                    style = GhostTypography.micro.copy(
                        color = if (bootText == "GHOST ONLINE.") GhostWhite else GhostWhiteFaint
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Corner brackets decoration
            Row(
                modifier = Modifier
                    .alpha(subTextAlphaAnim)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(GhostWhiteFaint)
                    )
                }
            }
        }

        // Top left corner bracket
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .alpha(subTextAlphaAnim)
        ) {
            CornerBracket(topLeft = true)
        }

        // Top right corner bracket
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .alpha(subTextAlphaAnim)
        ) {
            CornerBracket(topLeft = false)
        }

        // Version text bottom
        Text(
            text = "v1.0.0 // OFFLINE BUILD",
            style = GhostTypography.micro,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .alpha(subTextAlphaAnim)
        )
    }
}

@Composable
fun CornerBracket(topLeft: Boolean) {
    val size = 16.dp
    val thickness = 1.dp
    Box(modifier = Modifier.size(size)) {
        // Horizontal line
        Box(
            modifier = Modifier
                .width(size)
                .height(thickness)
                .background(GhostWhiteDim)
                .align(if (topLeft) Alignment.TopStart else Alignment.TopEnd)
        )
        // Vertical line
        Box(
            modifier = Modifier
                .width(thickness)
                .height(size)
                .background(GhostWhiteDim)
                .align(if (topLeft) Alignment.TopStart else Alignment.TopEnd)
        )
    }
}
