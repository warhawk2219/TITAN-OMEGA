package com.irongate.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.irongate.model.ProtocolResult
import com.irongate.protocol.ProtocolState
import com.irongate.protocol.ProtocolStep
import com.irongate.ui.components.*
import com.irongate.ui.theme.IronColors

@Composable
fun ProtocolOverlay(
    state: ProtocolState,
    onDismiss: () -> Unit
) {
    if (!state.running && state.step != ProtocolStep.COMPLETE) return

    Dialog(
        onDismissRequest = { if (!state.running) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IronColors.Black.copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(32.dp)
            ) {

                // Title
                PixelLabel(
                    "IRONGATE PROTOCOL",
                    fontSize = 10.sp,
                    color = IronColors.White
                )

                Spacer(Modifier.height(4.dp))

                // Animated step text
                AnimatedContent(
                    targetState = state.stepText,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "stepText"
                ) { text ->
                    MonoText(text, fontSize = 11.sp, color = IronColors.Gray2)
                }

                // Progress bar
                IronProgressBar(
                    progress = state.progress,
                    modifier = Modifier.width(280.dp)
                )

                // Step indicators
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.width(280.dp)
                ) {
                    val checkSteps = listOf(
                        ProtocolStep.GHOST_OK to "GHOST HANDSHAKE",
                        ProtocolStep.GIPSY_OK to "GIPSY HANDSHAKE",
                        ProtocolStep.VALIDATING_BRIDGE to "BRIDGE STRUCTURE",
                        ProtocolStep.CLEARING_MESSAGES to "MESSAGE QUEUE",
                        ProtocolStep.MEASURING_LATENCY to "LATENCY CHECK",
                        ProtocolStep.WRITING_LOG to "LOG WRITTEN"
                    )

                    checkSteps.forEach { (checkStep, label) ->
                        val done = state.step.ordinal > checkStep.ordinal ||
                                state.step == ProtocolStep.COMPLETE
                        val active = state.step == checkStep

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.alpha(if (done || active) 1f else 0.3f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        when {
                                            done -> IronColors.Online
                                            active -> IronColors.White
                                            else -> IronColors.Dark3
                                        }
                                    )
                            )
                            MonoText(
                                label,
                                fontSize = 9.sp,
                                color = when {
                                    done -> IronColors.Online
                                    active -> IronColors.White
                                    else -> IronColors.Gray1
                                }
                            )
                            if (done) {
                                Spacer(Modifier.weight(1f))
                                MonoText("OK", fontSize = 9.sp, color = IronColors.Online)
                            }
                        }
                    }
                }

                // Result
                AnimatedVisibility(
                    visible = state.step == ProtocolStep.COMPLETE && state.result != null,
                    enter = fadeIn() + scaleIn()
                ) {
                    state.result?.let { result ->
                        val (text, color) = when (result) {
                            ProtocolResult.NOMINAL -> "ALL CLEAR." to IronColors.Online
                            ProtocolResult.CAUTION -> "SOMETHING'S DOWN." to IronColors.Warn
                            ProtocolResult.BLACKOUT -> "BLACKOUT." to IronColors.Danger
                        }
                        Box(
                            modifier = Modifier
                                .border(1.dp, color)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            PixelLabel(text, fontSize = 9.sp, color = color)
                        }
                    }
                }

                // Dismiss button
                AnimatedVisibility(
                    visible = state.step == ProtocolStep.COMPLETE,
                    enter = fadeIn(tween(500))
                ) {
                    IronButton(
                        text = "DISMISS",
                        onClick = onDismiss,
                        borderColor = IronColors.Dark4
                    )
                }
            }
        }
    }
}
