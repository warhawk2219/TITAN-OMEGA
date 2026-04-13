package com.ghost.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.ui.theme.*

data class ChatMessage(
    val id: String,
    val text: String,
    val isGhost: Boolean,
    val confidence: Int = 0,
    val timestamp: String = ""
)

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isListening: Boolean,
    isProcessing: Boolean,
    isTtsEnabled: Boolean,
    activeProtocol: String?,
    ghostStatus: String,
    onSendMessage: (String) -> Unit,
    onToggleTts: () -> Unit,
    onMicClick: () -> Unit
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GhostBlack)
    ) {
        // Status bar
        StatusBar(
            ghostStatus = ghostStatus,
            isTtsEnabled = isTtsEnabled,
            onToggleTts = onToggleTts
        )

        // Header
        GhostHeader(
            isListening = isListening,
            isProcessing = isProcessing,
            onMicClick = onMicClick
        )

        // Active protocol bar
        AnimatedVisibility(
            visible = activeProtocol != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            activeProtocol?.let {
                ProtocolBar(protocolName = it)
            }
        }

        // Subbar
        SubBar(isListening = isListening, isProcessing = isProcessing)

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            itemsIndexed(messages) { index, message ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(300)
                    )
                ) {
                    ChatBubble(message = message)
                }
            }

            if (isProcessing) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Input bar
        InputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    onSendMessage(inputText.trim())
                    inputText = ""
                }
            },
            focusRequester = focusRequester
        )
    }
}

@Composable
private fun StatusBar(
    ghostStatus: String,
    isTtsEnabled: Boolean,
    onToggleTts: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostBlack)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "09:41",
            style = GhostTypography.small.copy(color = GhostWhite)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TTS toggle
            GhostToggleButton(
                label = if (isTtsEnabled) "VOX ON" else "VOX OFF",
                isActive = isTtsEnabled,
                onClick = onToggleTts
            )
            Text(
                text = "78%",
                style = GhostTypography.small.copy(color = GhostWhite)
            )
        }
    }
}

@Composable
private fun GhostHeader(
    isListening: Boolean,
    isProcessing: Boolean,
    onMicClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostBlack)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                width = 0.5.dp,
                color = GhostBorder,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu icon
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(1.dp)
                            .background(GhostWhiteDim)
                    )
                }
            }

            Text(
                text = "GHOST",
                style = GhostTypography.display.copy(fontSize = 18.sp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GHOST badge
            GhostBadge(label = "GHOST", hasDot = false)

            // SYS badge
            GhostBadge(label = "SYS", hasDot = true, dotColor = GhostGreen)

            // Mic / voice button
            GhostIconButton(
                isActive = isListening || isProcessing,
                onClick = onMicClick
            )
        }
    }
}

@Composable
private fun GhostBadge(
    label: String,
    hasDot: Boolean,
    dotColor: Color = GhostWhite
) {
    Row(
        modifier = Modifier
            .border(0.5.dp, GhostWhiteDim, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasDot) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(dotColor, shape = RoundedCornerShape(50))
            )
        }
        Text(
            text = label,
            style = GhostTypography.micro.copy(color = GhostWhite)
        )
    }
}

@Composable
private fun GhostIconButton(isActive: Boolean, onClick: () -> Unit) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    Box(
        modifier = Modifier
            .size(28.dp, 24.dp)
            .border(
                0.5.dp,
                if (isActive) GhostWhite else GhostBorder,
                RoundedCornerShape(2.dp)
            )
            .background(
                if (isActive) GhostSurface2 else GhostBlack,
                RoundedCornerShape(2.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Mic icon — three horizontal lines
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(pulse)
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(if (isActive) GhostWhite else GhostWhiteDim)
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(1.dp)
                    .background(if (isActive) GhostWhite else GhostWhiteDim)
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(1.dp)
                    .background(if (isActive) GhostWhite else GhostWhiteDim)
            )
        }
    }
}

@Composable
private fun GhostToggleButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(
                0.5.dp,
                if (isActive) GhostWhite else GhostBorder,
                RoundedCornerShape(2.dp)
            )
            .background(
                if (isActive) GhostSurface2 else GhostBlack,
                RoundedCornerShape(2.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = GhostTypography.micro.copy(
                color = if (isActive) GhostWhite else GhostWhiteDim
            )
        )
    }
}

@Composable
private fun ProtocolBar(protocolName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostSurface)
            .border(
                BorderStroke(0.5.dp, GhostBorder)
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "ACTIVE PROTOCOL",
                style = GhostTypography.micro
            )
            Text(
                text = protocolName.uppercase(),
                style = GhostTypography.small.copy(color = GhostWhite)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pulseAnim = rememberInfiniteTransition(label = "proto_pulse")
            val pulse by pulseAnim.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "proto_pulse_anim"
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .alpha(pulse)
                    .background(GhostWhite, RoundedCornerShape(50))
            )
            Text(
                text = "RUNNING",
                style = GhostTypography.micro.copy(color = GhostWhite)
            )
        }
    }
}

@Composable
private fun SubBar(isListening: Boolean, isProcessing: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostBlack)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .border(0.5.dp, GhostWhiteDim, RoundedCornerShape(50))
            )
            Text(
                text = "WAKE WORD ACTIVE",
                style = GhostTypography.micro
            )
        }

        val statusColor = when {
            isProcessing -> GhostWhite
            isListening -> GhostWhite
            else -> GhostGreen
        }
        val statusText = when {
            isProcessing -> "PROCESSING"
            isListening -> "LISTENING"
            else -> "LISTENING"
        }

        Text(
            text = statusText,
            style = GhostTypography.micro.copy(color = statusColor)
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isGhost) Alignment.Start else Alignment.End
    ) {
        // Sender label
        Text(
            text = if (message.isGhost) "GHOST" else "HARI",
            style = GhostTypography.micro.copy(
                color = GhostWhiteFaint
            ),
            modifier = Modifier.padding(
                start = if (message.isGhost) 2.dp else 0.dp,
                end = if (message.isGhost) 0.dp else 2.dp,
                bottom = 3.dp
            )
        )

        // Bubble
        Box(
            modifier = Modifier
                .let {
                    if (message.isGhost) it.fillMaxWidth(0.92f)
                    else it.fillMaxWidth(0.75f)
                }
                .background(
                    if (message.isGhost) GhostSurface else GhostSurface2,
                    RoundedCornerShape(3.dp)
                )
                .border(
                    0.5.dp,
                    if (message.isGhost) GhostBorder else Color(0xFF2A2A2A),
                    RoundedCornerShape(3.dp)
                )
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    style = GhostTypography.body.copy(
                        color = if (message.isGhost) GhostWhite else Color(0xFFC0C0C0)
                    )
                )

                if (message.isGhost && message.confidence > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(GhostWhiteFaint, RoundedCornerShape(50))
                        )
                        Text(
                            text = "CONF -${message.confidence}%",
                            style = GhostTypography.micro.copy(
                                color = GhostWhiteFaint
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "GHOST",
            style = GhostTypography.micro.copy(color = GhostWhiteFaint),
            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
        )
        Box(
            modifier = Modifier
                .background(GhostSurface, RoundedCornerShape(3.dp))
                .border(0.5.dp, GhostBorder, RoundedCornerShape(3.dp))
                .padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .alpha(alpha)
                            .background(GhostWhiteDim, RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostBlack)
            .border(
                BorderStroke(0.5.dp, GhostBorder),
                RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = "SEND COMMAND...",
                    style = GhostTypography.body.copy(color = GhostWhiteFaint)
                )
            },
            textStyle = GhostTypography.body.copy(color = GhostWhite),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GhostSurface,
                unfocusedContainerColor = GhostSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = GhostWhite
            ),
            shape = RoundedCornerShape(3.dp),
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )

        // Send button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(GhostSurface, RoundedCornerShape(3.dp))
                .border(0.5.dp, GhostWhiteDim, RoundedCornerShape(3.dp))
                .clickable { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "→",
                style = GhostTypography.body.copy(
                    color = GhostWhite,
                    fontSize = 14.sp
                )
            )
        }
    }
}
