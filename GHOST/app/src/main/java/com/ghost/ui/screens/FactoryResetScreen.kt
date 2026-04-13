package com.ghost.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.ui.theme.*

enum class ResetStep {
    SELECT_TARGET, CONFIRM_1, CONFIRM_2, ENTER_CODE, EXECUTING
}

@Composable
fun FactoryResetScreen(
    onReset: (String) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(ResetStep.SELECT_TARGET) }
    var selectedTarget by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }

    val warningPulse = rememberInfiniteTransition(label = "warning_pulse")
    val warningAlpha by warningPulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GhostBlack),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                fadeIn(tween(400)) togetherWith fadeOut(tween(400))
            },
            label = "reset_step"
        ) { currentStep ->
            when (currentStep) {

                ResetStep.SELECT_TARGET -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Warning text pulsing
                        Text(
                            text = "WARNING, THIS IS NOT A DRILL.\nALL DATA WILL BE NUKED.\nI REPEAT, ALL DATA WILL BE NUKED,\nNOT A DRILL.\nCONFIRMATION REQUESTED, SIR?",
                            style = GhostTypography.body.copy(
                                color = GhostWhite,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.alpha(warningAlpha),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "SELECT TARGET",
                            style = GhostTypography.label,
                            textAlign = TextAlign.Center
                        )

                        // Three target buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("GHOST", "GIPSY", "BOTH").forEach { target ->
                                ResetTargetButton(
                                    label = target,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedTarget = target
                                        step = ResetStep.CONFIRM_1
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        GhostOutlineButton(label = "ABORT", onClick = onCancel)
                    }
                }

                ResetStep.CONFIRM_1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "TARGET: $selectedTarget",
                            style = GhostTypography.title,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Sir, this is your final warning.\nThere is no coming back from this.\nConfirm again to proceed.",
                            style = GhostTypography.body.copy(
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        GhostConfirmButton(
                            label = "CONFIRM",
                            onClick = { step = ResetStep.CONFIRM_2 }
                        )

                        GhostOutlineButton(label = "ABORT", onClick = onCancel)
                    }
                }

                ResetStep.CONFIRM_2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "SECOND CONFIRMATION",
                            style = GhostTypography.title,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Last chance to abort, Sir.\nThis action cannot be undone.\nConfirm to proceed.",
                            style = GhostTypography.body.copy(
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        GhostConfirmButton(
                            label = "CONFIRM AGAIN",
                            onClick = { step = ResetStep.ENTER_CODE }
                        )

                        GhostOutlineButton(label = "ABORT", onClick = onCancel)
                    }
                }

                ResetStep.ENTER_CODE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "ENTER CONFIRMATION CODE",
                            style = GhostTypography.title,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Enter nuclear code to execute, Sir.",
                            style = GhostTypography.small,
                            textAlign = TextAlign.Center
                        )

                        // Code input
                        TextField(
                            value = codeInput,
                            onValueChange = {
                                codeInput = it.uppercase()
                                codeError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "ENTER CODE...",
                                    style = GhostTypography.body.copy(color = GhostWhiteFaint)
                                )
                            },
                            textStyle = GhostTypography.body.copy(
                                color = GhostWhite,
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.3.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = GhostSurface,
                                unfocusedContainerColor = GhostSurface,
                                focusedIndicatorColor = if (codeError) GhostRed else GhostWhite,
                                unfocusedIndicatorColor = GhostBorder,
                                cursorColor = GhostWhite
                            ),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )

                        if (codeError) {
                            Text(
                                text = "INCORRECT CODE. RESET ABORTED.",
                                style = GhostTypography.small.copy(color = GhostRed),
                                textAlign = TextAlign.Center
                            )
                        }

                        // OK button
                        GhostConfirmButton(
                            label = "OK",
                            onClick = {
                                if (codeInput == "WARHAWK") {
                                    step = ResetStep.EXECUTING
                                    onReset(selectedTarget)
                                } else {
                                    codeError = true
                                    codeInput = ""
                                }
                            }
                        )

                        GhostOutlineButton(label = "ABORT", onClick = onCancel)
                    }
                }

                ResetStep.EXECUTING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val execPulse by warningPulse.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "exec_pulse"
                        )

                        Text(
                            text = "CODE ACCEPTED.",
                            style = GhostTypography.title,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "INITIATING WIPE, SIR.",
                            style = GhostTypography.body.copy(
                                color = GhostWhite
                            ),
                            modifier = Modifier.alpha(execPulse),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { i ->
                                val dotPulse by warningPulse.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600, delayMillis = i * 200),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "dot_pulse_$i"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .alpha(dotPulse)
                                        .background(GhostWhite, RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetTargetButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .border(0.5.dp, GhostWhiteDim, RoundedCornerShape(2.dp))
            .background(GhostSurface, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = GhostTypography.small.copy(color = GhostWhite),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GhostConfirmButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GhostWhite, RoundedCornerShape(2.dp))
            .background(GhostSurface2, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = GhostTypography.small.copy(color = GhostWhite),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GhostOutlineButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GhostBorder, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = GhostTypography.small.copy(color = GhostWhiteDim),
            textAlign = TextAlign.Center
        )
    }
}
