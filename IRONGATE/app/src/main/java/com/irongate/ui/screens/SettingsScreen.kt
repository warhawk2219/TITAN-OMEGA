package com.irongate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irongate.model.CallsignConfig
import com.irongate.ui.components.*
import com.irongate.ui.theme.IronColors

@Composable
fun SettingsScreen(
    callsign: CallsignConfig,
    onSaveApiKey: (String) -> Unit,
    onSaveCallsign: (target: String, name: String, wake: String) -> Unit,
    getApiKey: () -> String
) {
    val scroll = rememberScrollState()
    var apiKey by remember { mutableStateOf(getApiKey()) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var callsignTarget by remember { mutableStateOf("GHOST") }
    var callsignName by remember { mutableStateOf(callsign.ghostName) }
    var callsignWake by remember { mutableStateOf(callsign.ghostWake) }
    var savedMsg by remember { mutableStateOf("") }

    LaunchedEffect(callsignTarget) {
        callsignName = when (callsignTarget) {
            "GHOST" -> callsign.ghostName
            "GIPSY" -> callsign.gipsyName
            "IRONGATE" -> callsign.irongateName
            else -> ""
        }
        callsignWake = when (callsignTarget) {
            "GHOST" -> callsign.ghostWake
            "GIPSY" -> callsign.gipsyWake
            "IRONGATE" -> callsign.irongateWake
            else -> ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Black)
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        SectionTitle("SETTINGS")

        // ─── API KEY ──────────────────────────────────────────────────────
        PixelLabel("OPENROUTER API KEY", fontSize = 6.sp, color = IronColors.Gray1)
        Spacer(Modifier.height(10.dp))

        IronInputField(
            value = apiKey,
            onValueChange = { apiKey = it },
            placeholder = "sk-or-...",
            visualTransformation = if (apiKeyVisible) VisualTransformation.None
                                   else PasswordVisualTransformation()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IronButton(
                text = if (apiKeyVisible) "HIDE" else "SHOW",
                onClick = { apiKeyVisible = !apiKeyVisible },
                modifier = Modifier.weight(1f)
            )
            IronButton(
                text = "SAVE KEY",
                onClick = {
                    onSaveApiKey(apiKey)
                    savedMsg = "API KEY SAVED"
                },
                modifier = Modifier.weight(1f)
            )
        }

        IronDivider()

        // ─── CALLSIGN ─────────────────────────────────────────────────────
        PixelLabel("CALLSIGN MODULE", fontSize = 6.sp, color = IronColors.Gray1)
        Spacer(Modifier.height(10.dp))

        // Target selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("GHOST", "GIPSY", "IRONGATE").forEach { t ->
                Box(
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (callsignTarget == t) IronColors.Dark1 else IronColors.Black)
                        .border(1.dp, if (callsignTarget == t) IronColors.White else IronColors.Dark3)
                        .padding(vertical = 10.dp)
                        .then(Modifier.run {
                            androidx.compose.foundation.clickable { callsignTarget = t }
                        })
                ) {
                    PixelLabel(
                        t, fontSize = 6.sp,
                        color = if (callsignTarget == t) IronColors.White else IronColors.Gray1
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PixelLabel("CUSTOM DISPLAY NAME", fontSize = 6.sp, color = IronColors.Gray1)
        Spacer(Modifier.height(8.dp))
        IronInputField(
            value = callsignName,
            onValueChange = { callsignName = it },
            placeholder = "e.g. TITAN"
        )

        Spacer(Modifier.height(12.dp))

        PixelLabel("CUSTOM WAKE WORD", fontSize = 6.sp, color = IronColors.Gray1)
        Spacer(Modifier.height(8.dp))
        IronInputField(
            value = callsignWake,
            onValueChange = { callsignWake = it },
            placeholder = "e.g. Hey Titan"
        )

        Spacer(Modifier.height(12.dp))

        IronButton(
            text = "SAVE CALLSIGN",
            onClick = {
                if (callsignName.isNotBlank() && callsignWake.isNotBlank()) {
                    onSaveCallsign(callsignTarget, callsignName, callsignWake)
                    savedMsg = "CALLSIGN UPDATED: $callsignTarget → $callsignName"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = callsignName.isNotBlank() && callsignWake.isNotBlank()
        )

        if (savedMsg.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            MonoText(savedMsg, fontSize = 9.sp, color = IronColors.Online)
        }

        IronDivider()

        // ─── SYSTEM INFO ──────────────────────────────────────────────────
        PixelLabel("SYSTEM INFO", fontSize = 6.sp, color = IronColors.Gray1)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "PORT 8765" to "GHOST SOCKET",
                "PORT 8766" to "GIPSY SOCKET",
                "HEARTBEAT" to "10 SECONDS",
                "TIMEOUT" to "30 SECONDS",
                "BUFFER" to "100 MSG MAX",
                "PROTOCOL" to "EVERY 60 MIN",
                "STORAGE" to "SESSION ONLY",
                "NETWORK" to "LOCALHOST ONLY"
            ).forEach { (k, v) ->
                DataRow(k, v)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun IronInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .background(IronColors.NearBlack)
            .border(1.dp, IronColors.Dark3)
            .padding(12.dp),
        textStyle = TextStyle(
            color = IronColors.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        ),
        cursorBrush = SolidColor(IronColors.White),
        visualTransformation = visualTransformation,
        decorationBox = { inner ->
            if (value.isEmpty()) {
                MonoText(placeholder, fontSize = 12.sp, color = IronColors.Dark4)
            }
            inner()
        },
        singleLine = true
    )
}

@Composable
fun IronDivider() {
    Spacer(Modifier.height(20.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(IronColors.Dark2))
    Spacer(Modifier.height(20.dp))
}
