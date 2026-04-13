package com.irongate.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irongate.ui.components.*
import com.irongate.ui.theme.IronColors
import kotlinx.coroutines.delay

enum class NukeStep { IDLE, LAYER1, LAYER2, LAYER3, EXECUTING, COMPLETE }

@Composable
fun NukeScreen(
    onExecuteNuke: (target: String) -> Unit,
    onPlayAudio: () -> Unit
) {
    var step by remember { mutableStateOf(NukeStep.IDLE) }
    var nukeTarget by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var confirmClicked by remember { mutableStateOf(false) }
    var nukeProgress by remember { mutableStateOf(0f) }
    var nukeStatusText by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    // Nuke execution animation
    LaunchedEffect(step) {
        if (step == NukeStep.EXECUTING) {
            onPlayAudio()
            // Wait for audio (approx 8 seconds — actual audio duration)
            delay(8000L)
            nukeStatusText = "CODE ACCEPTED. INITIATING WIPE SIR."
            // Animate progress
            var prog = 0f
            while (prog < 1f) {
                prog += 0.02f
                nukeProgress = prog
                delay(80L)
            }
            delay(500L)
            nukeStatusText = "WIPE COMPLETE."
            delay(1500L)
            nukeStatusText = "GHOST ONLINE. READY FOR INITIALIZATION SIR."
            onExecuteNuke(nukeTarget)
            step = NukeStep.COMPLETE
        }
    }

    // Flicker animation for warning text
    val infiniteTransition = rememberInfiniteTransition(label = "flicker")
    val warningAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "warningAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Black)
    ) {
        when (step) {

            // ─── NUKE EXECUTING / COMPLETE ────────────────────────────────
            NukeStep.EXECUTING, NukeStep.COMPLETE -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PixelLabel(
                        if (step == NukeStep.EXECUTING) "INITIATING WIPE" else "COMPLETE",
                        fontSize = 10.sp,
                        color = IronColors.Danger
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .height(2.dp)
                            .background(IronColors.DangerDark)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(nukeProgress)
                                .background(IronColors.Danger)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    MonoText(
                        nukeStatusText,
                        fontSize = 10.sp,
                        color = if (step == NukeStep.COMPLETE) IronColors.Online else IronColors.Danger
                    )
                }
            }

            // ─── NORMAL NUKE FLOW ─────────────────────────────────────────
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(16.dp)
                ) {
                    SectionTitle("NUCLEAR AUTHORIZATION", color = IronColors.Danger)

                    // WARNING
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IronColors.NearBlack)
                            .border(1.dp, IronColors.DangerDark)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PixelLabel(
                                "WARNING — THIS IS NOT A DRILL",
                                fontSize = 7.sp,
                                color = IronColors.Danger.copy(alpha = warningAlpha)
                            )
                            Spacer(Modifier.height(8.dp))
                            PixelLabel(
                                "ALL DATA WILL BE NUKED",
                                fontSize = 7.sp,
                                color = IronColors.Danger.copy(alpha = warningAlpha)
                            )
                            Spacer(Modifier.height(8.dp))
                            PixelLabel(
                                "CONFIRMATION REQUIRED SIR",
                                fontSize = 7.sp,
                                color = IronColors.Danger
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // LAYER 1.5 — TARGET SELECTION
                    PixelLabel("SELECT TARGET SYSTEM", fontSize = 6.sp, color = IronColors.Gray1)
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("GHOST", "GIPSY", "BOTH").forEach { t ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(IronColors.Black)
                                    .border(
                                        1.dp,
                                        if (nukeTarget == t) IronColors.Danger else IronColors.Dark2
                                    )
                                    .padding(vertical = 12.dp)
                                    .then(Modifier.run {
                                        androidx.compose.foundation.clickable {
                                            nukeTarget = t
                                            step = NukeStep.LAYER1
                                            confirmClicked = false
                                            codeInput = ""
                                        }
                                    })
                            ) {
                                PixelLabel(
                                    t, fontSize = 6.sp,
                                    color = if (nukeTarget == t) IronColors.Danger else IronColors.Gray1
                                )
                            }
                        }
                    }

                    if (nukeTarget.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))

                        // LAYER 2 — CONFIRM BUTTON
                        PixelLabel("LAYER 2 — CONFIRM SELECTION", fontSize = 6.sp, color = IronColors.Gray1)
                        Spacer(Modifier.height(10.dp))

                        IronButton(
                            text = if (confirmClicked) "CONFIRMED ✓" else "CONFIRM TARGET: $nukeTarget",
                            onClick = {
                                confirmClicked = true
                                step = NukeStep.LAYER2
                            },
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (confirmClicked) IronColors.Online else IronColors.Dark4,
                            textColor = if (confirmClicked) IronColors.Online else IronColors.White
                        )

                        if (confirmClicked) {
                            Spacer(Modifier.height(20.dp))

                            // LAYER 3 — NUCLEAR CODE
                            PixelLabel("LAYER 3 — ENTER NUCLEAR CODE", fontSize = 6.sp, color = IronColors.Gray1)
                            Spacer(Modifier.height(10.dp))

                            BasicTextField(
                                value = codeInput,
                                onValueChange = { if (it.length <= 8) codeInput = it.uppercase() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(IronColors.Black)
                                    .border(
                                        1.dp,
                                        if (codeInput == "WARHAWK") IronColors.Online else IronColors.Dark3
                                    )
                                    .padding(16.dp),
                                textStyle = TextStyle(
                                    color = if (codeInput == "WARHAWK") IronColors.Online else IronColors.White,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 8.sp,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(IronColors.Danger),
                                visualTransformation = PasswordVisualTransformation(),
                                decorationBox = { inner ->
                                    if (codeInput.isEmpty()) {
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            MonoText("· · · · · · · ·", fontSize = 18.sp, color = IronColors.Dark3)
                                        }
                                    }
                                    inner()
                                },
                                singleLine = true
                            )

                            Spacer(Modifier.height(12.dp))

                            // EXECUTE BUTTON
                            val codeOk = codeInput == "WARHAWK"
                            IronButton(
                                text = "INITIATE WIPE",
                                onClick = {
                                    if (codeOk) {
                                        step = NukeStep.EXECUTING
                                        nukeProgress = 0f
                                        nukeStatusText = "AWAITING AUDIO..."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = if (codeOk) IronColors.Danger else IronColors.Dark2,
                                textColor = if (codeOk) IronColors.Danger else IronColors.Gray1,
                                enabled = codeOk
                            )

                            if (!codeOk && codeInput.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                MonoText("INVALID CODE. ACCESS DENIED.", fontSize = 9.sp, color = IronColors.Danger)
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}
