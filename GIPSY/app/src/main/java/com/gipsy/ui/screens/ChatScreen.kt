package com.gipsy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gipsy.R
import com.gipsy.data.models.*
import com.gipsy.ui.GipsyViewModel
import com.gipsy.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: GipsyViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Auto-scroll to bottom
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GipsyColors.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── TOP BAR ───────────────────────────────────────
            GipsyTopBar(
                ttsEnabled = uiState.ttsEnabled,
                bridgeStatus = uiState.bridgeStatus,
                activeProvider = uiState.activeProvider,
                listenState = uiState.listenState,
                onTtsToggle = { viewModel.toggleTts() },
                onMicClick = { /* handled by service */ },
                onSettingsClick = onSettingsClick
            )

            // Separator line
            GipsyScanline()

            // ── MESSAGES ──────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item { BootSequence() }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }

                if (uiState.isProcessing) {
                    item { ProcessingIndicator() }
                }
            }

            // ── STATUS BAR ────────────────────────────────────
            GipsyStatusBar(
                bridgeStatus = uiState.bridgeStatus,
                activeMode = uiState.activeMode,
                activeProvider = uiState.activeProvider
            )

            // Separator
            GipsyScanline()

            // ── INPUT BAR ─────────────────────────────────────
            GipsyInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                isProcessing = uiState.isProcessing
            )
        }

        // ── FACTORY RESET DIALOG ──────────────────────────────
        if (uiState.showFactoryResetDialog) {
            FactoryResetDialog(viewModel = viewModel, uiState = uiState)
        }

        // ── DELETE MEMORY DIALOG ──────────────────────────────
        if (uiState.showDeleteMemoryDialog) {
            DeleteMemoryDialog(
                onConfirm = { viewModel.confirmDeleteMemory() },
                onDismiss = { viewModel.dismissDeleteMemory() }
            )
        }

        // ── IRONGATE NOTIFICATION ─────────────────────────────
        uiState.irongateResult?.let { result ->
            IrongateNotification(
                result = result,
                onDismiss = { viewModel.clearIrongateResult() }
            )
        }
    }
}

// ── TOP BAR ───────────────────────────────────────────────────
@Composable
fun GipsyTopBar(
    ttsEnabled: Boolean,
    bridgeStatus: BridgeStatus,
    activeProvider: ApiProvider,
    listenState: GipsyListenState,
    onTtsToggle: () -> Unit,
    onMicClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo + Name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_gipsy_logo),
                contentDescription = "GIPSY",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GIPSY",
                style = MaterialTheme.typography.displaySmall,
                color = GipsyColors.White
            )
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TTS Toggle
            GipsyIconButton(
                onClick = onTtsToggle,
                isActive = ttsEnabled,
                label = if (ttsEnabled) "VOX" else "MUT"
            )

            // Mic button
            GipsyIconButton(
                onClick = onMicClick,
                isActive = listenState != GipsyListenState.IDLE,
                label = "MIC",
                pulsing = listenState == GipsyListenState.LISTENING
            )

            // Settings
            GipsyIconButton(
                onClick = onSettingsClick,
                isActive = false,
                label = "SYS"
            )
        }
    }
}

@Composable
fun GipsyIconButton(
    onClick: () -> Unit,
    isActive: Boolean,
    label: String,
    pulsing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (isActive) GipsyColors.White else GipsyColors.BorderGray,
                shape = RoundedCornerShape(2.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .alpha(if (pulsing) pulseAlpha else 1f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) GipsyColors.White else GipsyColors.DimWhite
        )
    }
}

// ── SCANLINE ──────────────────────────────────────────────────
@Composable
fun GipsyScanline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GipsyColors.BorderGray)
    )
}

// ── MESSAGE BUBBLE ────────────────────────────────────────────
@Composable
fun MessageBubble(message: Message) {
    val isUser = message.role == MessageRole.USER

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(300)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // GIPSY prefix marker
                Text(
                    text = ">",
                    style = MaterialTheme.typography.bodySmall,
                    color = GipsyColors.DimWhite,
                    modifier = Modifier
                        .padding(end = 6.dp, top = 4.dp)
                        .alpha(0.6f)
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .then(
                        if (!isUser) Modifier.border(
                            width = 1.dp,
                            color = GipsyColors.BorderGray,
                            shape = RoundedCornerShape(2.dp)
                        ) else Modifier.border(
                            width = 1.dp,
                            color = GipsyColors.DimWhite,
                            shape = RoundedCornerShape(2.dp)
                        )
                    )
                    .background(
                        color = if (isUser) GipsyColors.DarkGray else GipsyColors.Black,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(10.dp)
            ) {
                if (!isUser && message.isProtocolMessage) {
                    Text(
                        text = "// ${message.protocolName ?: "PROTOCOL"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GipsyColors.DimWhite,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 9.sp),
                    color = if (isUser) GipsyColors.OffWhite else GipsyColors.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 6.sp),
                    color = GipsyColors.DimWhite,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            if (isUser) {
                Text(
                    text = "<",
                    style = MaterialTheme.typography.bodySmall,
                    color = GipsyColors.DimWhite,
                    modifier = Modifier
                        .padding(start = 6.dp, top = 4.dp)
                        .alpha(0.6f)
                )
            }
        }
    }
}

// ── PROCESSING INDICATOR ──────────────────────────────────────
@Composable
fun ProcessingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotCount by infiniteTransition.animateValue(
        initialValue = 1,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotCount"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "> ${"■".repeat(dotCount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = GipsyColors.DimWhite
        )
    }
}

// ── BOOT SEQUENCE ─────────────────────────────────────────────
@Composable
fun BootSequence() {
    var visibleLines by remember { mutableStateOf(0) }
    val lines = listOf(
        "GIPSY v1.0.0",
        "SYSTEM ONLINE",
        "BRIDGE CONNECTING...",
        "AWAITING INPUT, COOPER."
    )

    LaunchedEffect(Unit) {
        lines.indices.forEach { i ->
            kotlinx.coroutines.delay(400L * (i + 1))
            visibleLines = i + 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.take(visibleLines).forEach { line ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + expandVertically(tween(300))
            ) {
                Text(
                    text = "> $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GipsyColors.DimWhite
                )
            }
        }
    }
}

// ── STATUS BAR ────────────────────────────────────────────────
@Composable
fun GipsyStatusBar(
    bridgeStatus: BridgeStatus,
    activeMode: GipsyMode,
    activeProvider: ApiProvider
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "BRIDGE: ${bridgeStatus.name}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
            color = when (bridgeStatus) {
                BridgeStatus.CONNECTED    -> GipsyColors.White
                BridgeStatus.CONNECTING   -> GipsyColors.DimWhite
                else                      -> GipsyColors.DimWhite
            }
        )
        Text(
            text = activeMode.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
            color = GipsyColors.DimWhite
        )
        Text(
            text = activeProvider.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
            color = GipsyColors.DimWhite
        )
    }
}

// ── INPUT BAR ─────────────────────────────────────────────────
@Composable
fun GipsyInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Input field
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .border(1.dp, GipsyColors.BorderGray, RoundedCornerShape(2.dp))
                .padding(10.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = GipsyColors.White,
                fontSize = 9.sp
            ),
            singleLine = false,
            maxLines = 4,
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = "ENTER COMMAND...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 9.sp),
                        color = GipsyColors.DimWhite
                    )
                }
                innerTextField()
            }
        )

        // Send button
        Box(
            modifier = Modifier
                .clickable(enabled = text.isNotBlank() && !isProcessing, onClick = onSend)
                .border(
                    1.dp,
                    if (text.isNotBlank() && !isProcessing) GipsyColors.White else GipsyColors.BorderGray,
                    RoundedCornerShape(2.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TX",
                style = MaterialTheme.typography.labelLarge,
                color = if (text.isNotBlank() && !isProcessing) GipsyColors.White else GipsyColors.DimWhite
            )
        }
    }
}

// ── FACTORY RESET DIALOG ──────────────────────────────────────
@Composable
fun FactoryResetDialog(viewModel: GipsyViewModel, uiState: com.gipsy.ui.GipsyUiState) {
    val context = LocalContext.current

    // Play audio when step reaches 5
    LaunchedEffect(uiState.factoryResetStep) {
        if (uiState.factoryResetStep == 5) {
            playNukeAudio(context) {
                viewModel.performActualWipe()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, GipsyColors.White, RoundedCornerShape(2.dp))
                .background(GipsyColors.Black)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState.factoryResetStep) {

                // Step 1 — Warning
                1 -> {
                    WarningText("WARNING, THIS IS NOT A DRILL. ALL DATA WILL BE NUKED. I REPEAT, ALL DATA WILL BE NUKED, NOT A DRILL, CONFIRMATION REQUESTED, SIR?")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GipsyDialogButton("ABORT") { viewModel.dismissFactoryReset() }
                        GipsyDialogButton("CONFIRM") { viewModel.confirmFactoryResetWarning() }
                    }
                }

                // Step 2 — Target selection
                2 -> {
                    Text("SELECT TARGET", style = MaterialTheme.typography.displaySmall, color = GipsyColors.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GipsyDialogButton("GHOST") { viewModel.selectFactoryResetTarget(ResetTarget.GHOST) }
                        GipsyDialogButton("GIPSY") { viewModel.selectFactoryResetTarget(ResetTarget.GIPSY) }
                        GipsyDialogButton("BOTH")  { viewModel.selectFactoryResetTarget(ResetTarget.BOTH) }
                    }
                }

                // Step 3 — Confirm button
                3 -> {
                    Text(
                        "TARGET: ${uiState.factoryResetTarget?.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GipsyColors.White
                    )
                    Text(
                        "THIS CANNOT BE UNDONE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GipsyColors.DimWhite
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GipsyDialogButton("ABORT") { viewModel.dismissFactoryReset() }
                        GipsyDialogButton("PROCEED") { viewModel.confirmFactoryResetStep3() }
                    }
                }

                // Step 4 — Nuclear code
                4 -> {
                    Text("ENTER NUCLEAR CODE", style = MaterialTheme.typography.displaySmall, color = GipsyColors.White)
                    BasicTextField(
                        value = uiState.factoryResetCodeInput,
                        onValueChange = { viewModel.updateFactoryResetCode(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GipsyColors.White, RoundedCornerShape(2.dp))
                            .padding(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = GipsyColors.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = 4.sp
                        ),
                        singleLine = true
                    )
                    if (uiState.factoryResetError.isNotBlank()) {
                        Text(uiState.factoryResetError, style = MaterialTheme.typography.bodySmall, color = GipsyColors.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GipsyDialogButton("ABORT") { viewModel.dismissFactoryReset() }
                        GipsyDialogButton("EXECUTE") { viewModel.executeFactoryReset() }
                    }
                }

                // Step 5 — Audio playing / wiping
                5 -> {
                    PulsingText("NUKING...")
                }
            }
        }
    }
}

@Composable
fun WarningText(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "warning")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warningAlpha"
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = GipsyColors.White.copy(alpha = alpha),
        textAlign = TextAlign.Center,
        lineHeight = 16.sp
    )
}

@Composable
fun PulsingText(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsingAlpha"
    )
    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium,
        color = GipsyColors.White.copy(alpha = alpha),
        textAlign = TextAlign.Center
    )
}

@Composable
fun GipsyDialogButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(1.dp, GipsyColors.White, RoundedCornerShape(2.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = GipsyColors.White)
    }
}

// ── DELETE MEMORY DIALOG ──────────────────────────────────────
@Composable
fun DeleteMemoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.dp, GipsyColors.White, RoundedCornerShape(2.dp))
                .background(GipsyColors.Black)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("DELETE MEMORY", style = MaterialTheme.typography.displaySmall, color = GipsyColors.White)
            Text(
                "This will delete all conversation memory.\nPinned facts will be preserved.",
                style = MaterialTheme.typography.bodySmall,
                color = GipsyColors.DimWhite,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GipsyDialogButton("ABORT") { onDismiss() }
                GipsyDialogButton("CONFIRM") { onConfirm() }
            }
        }
    }
}

// ── IRONGATE NOTIFICATION ─────────────────────────────────────
@Composable
fun IrongateNotification(result: IrongateResult, onDismiss: () -> Unit) {
    LaunchedEffect(result) {
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .padding(top = 80.dp)
                .fillMaxWidth(0.9f)
                .border(1.dp, GipsyColors.White, RoundedCornerShape(2.dp))
                .background(GipsyColors.Black)
                .padding(12.dp)
                .clickable { onDismiss() }
        ) {
            Text(
                text = result.notification,
                style = MaterialTheme.typography.bodyMedium,
                color = GipsyColors.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── HELPERS ───────────────────────────────────────────────────
fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

fun playNukeAudio(context: android.content.Context, onComplete: () -> Unit) {
    try {
        val mp = android.media.MediaPlayer.create(context, R.raw.nuke_audio)
        mp.setOnCompletionListener {
            it.release()
            onComplete()
        }
        mp.start()
    } catch (e: Exception) {
        onComplete()
    }
}

@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        singleLine = singleLine,
        maxLines = maxLines,
        decorationBox = decorationBox
    )
}
