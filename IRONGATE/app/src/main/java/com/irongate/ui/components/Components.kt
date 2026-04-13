package com.irongate.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irongate.model.AssistantState
import com.irongate.model.ProtocolResult
import com.irongate.ui.theme.IronColors

// ─── Pixel Label ─────────────────────────────────────────────────────────────
@Composable
fun PixelLabel(
    text: String,
    fontSize: TextUnit = 7.sp,
    color: Color = IronColors.Gray1,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        color = color,
        letterSpacing = 3.sp,
        modifier = modifier
    )
}

// ─── Mono Text ───────────────────────────────────────────────────────────────
@Composable
fun MonoText(
    text: String,
    fontSize: TextUnit = 11.sp,
    color: Color = IronColors.Gray2,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        color = color,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

// ─── Status Dot ──────────────────────────────────────────────────────────────
@Composable
fun StatusDot(state: AssistantState, size: Dp = 8.dp) {
    val color = when (state) {
        AssistantState.ONLINE -> IronColors.Online
        AssistantState.RECONNECTING -> IronColors.Warn
        AssistantState.OFFLINE -> IronColors.Gray1
    }
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == AssistantState.ONLINE) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .background(color.copy(alpha = if (state == AssistantState.ONLINE) alpha else 1f))
    )
}

// ─── Connection Card ─────────────────────────────────────────────────────────
@Composable
fun ConnectionCard(
    name: String,
    state: AssistantState,
    port: Int,
    latencyMs: Long,
    modifier: Modifier = Modifier
) {
    val borderColor = when (state) {
        AssistantState.ONLINE -> IronColors.Dark3
        AssistantState.RECONNECTING -> IronColors.Warn.copy(alpha = 0.4f)
        AssistantState.OFFLINE -> IronColors.Dark2
    }

    // Scan line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "scan"
    )

    Box(
        modifier = modifier
            .background(IronColors.NearBlack)
            .border(1.dp, borderColor)
            .padding(14.dp)
    ) {
        Column {
            PixelLabel(name, fontSize = 7.sp, color = IronColors.Gray2)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(state, 7.dp)
                Spacer(Modifier.width(8.dp))
                val statusText = when (state) {
                    AssistantState.ONLINE -> "ONLINE"
                    AssistantState.RECONNECTING -> "RECONNECTING"
                    AssistantState.OFFLINE -> "OFFLINE"
                }
                val statusColor = when (state) {
                    AssistantState.ONLINE -> IronColors.Online
                    AssistantState.RECONNECTING -> IronColors.Warn
                    AssistantState.OFFLINE -> IronColors.Gray1
                }
                MonoText(statusText, fontSize = 10.sp, color = statusColor)
            }
            Spacer(Modifier.height(6.dp))
            MonoText("PORT $port", fontSize = 9.sp, color = IronColors.Gray1)
            Spacer(Modifier.height(4.dp))
            MonoText(
                if (state == AssistantState.ONLINE) "LATENCY: ${latencyMs}ms" else "LATENCY: ---",
                fontSize = 9.sp, color = IronColors.Gray2
            )
        }
    }
}

// ─── Protocol Badge ──────────────────────────────────────────────────────────
@Composable
fun ProtocolBadge(result: ProtocolResult?) {
    val (text, color) = when (result) {
        ProtocolResult.NOMINAL -> "NOMINAL" to IronColors.Online
        ProtocolResult.CAUTION -> "CAUTION" to IronColors.Warn
        ProtocolResult.BLACKOUT -> "BLACKOUT" to IronColors.Danger
        null -> "PENDING" to IronColors.Gray1
    }
    Box(
        modifier = Modifier
            .border(1.dp, color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        MonoText(text, fontSize = 9.sp, color = color)
    }
}

// ─── Progress Bar ────────────────────────────────────────────────────────────
@Composable
fun IronProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animProg by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(IronColors.Dark2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animProg)
                .background(IronColors.White)
        )
    }
}

// ─── Iron Button ─────────────────────────────────────────────────────────────
@Composable
fun IronButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = IronColors.Dark4,
    textColor: Color = IronColors.White,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.3f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(IronColors.Black)
            .border(1.dp, borderColor.copy(alpha = alpha))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        PixelLabel(text, fontSize = 7.sp, color = textColor.copy(alpha = alpha))
    }
}

// ─── Section Divider ─────────────────────────────────────────────────────────
@Composable
fun SectionTitle(text: String, color: Color = IronColors.Gray1) {
    Column {
        PixelLabel(text, color = color)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IronColors.Dark2)
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ─── Data Row ────────────────────────────────────────────────────────────────
@Composable
fun DataRow(label: String, value: String, valueColor: Color = IronColors.Gray3) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MonoText(label, fontSize = 10.sp, color = IronColors.Gray2)
        MonoText(value, fontSize = 10.sp, color = valueColor)
    }
}

// ─── Feed Tag ────────────────────────────────────────────────────────────────
@Composable
fun FeedTag(type: String) {
    val color = when (type.uppercase()) {
        "SYNC" -> IronColors.Gray2
        "COMMAND" -> IronColors.Gray3
        "DATA" -> IronColors.Gray1
        "STATUS" -> IronColors.Gray1
        "USER_MESSAGE" -> IronColors.White
        else -> IronColors.Gray1
    }
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        PixelLabel(type, fontSize = 6.sp, color = color)
    }
}
