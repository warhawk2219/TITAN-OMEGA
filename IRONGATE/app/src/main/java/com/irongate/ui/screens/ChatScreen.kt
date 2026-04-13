package com.irongate.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irongate.model.ChatMessage
import com.irongate.ui.components.*
import com.irongate.ui.theme.IronColors

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSend: (target: String, message: String) -> Unit
) {
    var target by remember { mutableStateOf("GHOST") }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Black)
            .padding(16.dp)
    ) {
        SectionTitle("INTERFACE")

        // Target selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("GHOST", "GIPSY", "BOTH").forEach { t ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (target == t) IronColors.Dark1 else IronColors.Black)
                        .border(1.dp, if (target == t) IronColors.White else IronColors.Dark3)
                        .padding(vertical = 10.dp)
                        .then(Modifier.run {
                            androidx.compose.foundation.clickable { target = t }
                        })
                ) {
                    PixelLabel(
                        t,
                        fontSize = 6.sp,
                        color = if (target == t) IronColors.White else IronColors.Gray1
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Message list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, IronColors.Dark3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .background(IronColors.NearBlack)
                    .padding(12.dp)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = IronColors.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                ),
                cursorBrush = SolidColor(IronColors.White),
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        MonoText("TYPE COMMAND...", fontSize = 12.sp, color = IronColors.Dark4)
                    }
                    inner()
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = {
                        if (input.isNotBlank()) {
                            onSend(target, input.trim())
                            input = ""
                            keyboard?.hide()
                        }
                    }
                )
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(IronColors.Black)
                    .border(1.dp, IronColors.Dark4)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .then(Modifier.run {
                        androidx.compose.foundation.clickable {
                            if (input.isNotBlank()) {
                                onSend(target, input.trim())
                                input = ""
                                keyboard?.hide()
                            }
                        }
                    })
            ) {
                PixelLabel("SEND", fontSize = 7.sp, color = IronColors.White)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.from == "USER"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            PixelLabel(msg.from, fontSize = 6.sp, color = IronColors.Gray1)
            Spacer(Modifier.height(4.dp))
        }

        val bgColor = when {
            isUser -> IronColors.Dark2
            msg.from == "GHOST" -> IronColors.Black
            else -> IronColors.NearBlack
        }
        val borderColor = when {
            isUser -> IronColors.Dark3
            msg.from == "GHOST" -> IronColors.Dark3
            else -> IronColors.Dark2
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bgColor)
                .border(1.dp, borderColor)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            MonoText(msg.text, fontSize = 11.sp, color = IronColors.Gray3)
        }
    }
}
