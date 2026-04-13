package com.gipsy.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gipsy.data.local.PreferencesManager
import com.gipsy.data.models.ApiProvider
import com.gipsy.ui.GipsyViewModel
import com.gipsy.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── SETTINGS VIEWMODEL ────────────────────────────────────────
@HiltViewModel
class SettingsViewModel @Inject constructor(
    val prefs: PreferencesManager
) : ViewModel() {

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun saveApiKeys(gemini: String, groq: String, openRouter: String) {
        if (gemini.isNotBlank()) prefs.geminiApiKey = gemini
        if (groq.isNotBlank()) prefs.groqApiKey = groq
        if (openRouter.isNotBlank()) prefs.openRouterApiKey = openRouter
        _saved.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _saved.value = false
        }
    }

    fun saveCallsigns(gipsyName: String, ghostName: String, irongateName: String,
                      gipsyWake: String) {
        if (gipsyName.isNotBlank()) prefs.gipsyCallsign = gipsyName.uppercase()
        if (ghostName.isNotBlank()) prefs.ghostCallsign = ghostName.uppercase()
        if (irongateName.isNotBlank()) prefs.irongateCallsign = irongateName.uppercase()
        if (gipsyWake.isNotBlank()) prefs.gipsyWakeWord = gipsyWake.uppercase()
        _saved.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _saved.value = false
        }
    }

    fun saveNuclearCode(code: String) {
        if (code.isNotBlank()) prefs.nuclearCode = code.uppercase()
    }

    fun savePanicWord(word: String) {
        if (word.isNotBlank()) prefs.panicWord = word.uppercase()
    }
}

// ── SETTINGS SCREEN ───────────────────────────────────────────
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: GipsyViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saved by settingsVm.saved.collectAsState()

    var geminiKey by remember { mutableStateOf("") }
    var groqKey by remember { mutableStateOf("") }
    var openRouterKey by remember { mutableStateOf("") }
    var showKeys by remember { mutableStateOf(false) }

    // Callsign fields
    var gipsyName by remember { mutableStateOf(settingsVm.prefs.gipsyCallsign) }
    var ghostName by remember { mutableStateOf(settingsVm.prefs.ghostCallsign) }
    var irongateName by remember { mutableStateOf(settingsVm.prefs.irongateCallsign) }
    var gipsyWake by remember { mutableStateOf(settingsVm.prefs.gipsyWakeWord) }
    var selectedCallsignTarget by remember { mutableStateOf("GIPSY") }

    // Security
    var nuclearCode by remember { mutableStateOf("") }
    var panicWord by remember { mutableStateOf("") }
    var showNuclear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GipsyColors.Black)
            .verticalScroll(rememberScrollState())
    ) {
        // ── HEADER ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .border(1.dp, GipsyColors.BorderGray, RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("< BACK", style = MaterialTheme.typography.labelSmall, color = GipsyColors.DimWhite)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("SYSTEM CONFIG", style = MaterialTheme.typography.displaySmall, color = GipsyColors.White)
        }

        GipsyScanline()

        // ── API PROVIDER ──────────────────────────────────────
        SettingsSection(title = "AI BACKEND") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ApiProvider.values().forEach { provider ->
                    val isActive = uiState.activeProvider == provider
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setProvider(provider) }
                            .border(
                                1.dp,
                                if (isActive) GipsyColors.White else GipsyColors.BorderGray,
                                RoundedCornerShape(2.dp)
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            provider.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) GipsyColors.White else GipsyColors.DimWhite
                        )
                    }
                }
            }
        }

        GipsyScanline()

        // ── API KEYS ──────────────────────────────────────────
        SettingsSection(title = "API KEYS") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showKeys = !showKeys }
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (showKeys) "[ HIDE KEYS ]" else "[ SHOW KEYS ]",
                    style = MaterialTheme.typography.labelSmall,
                    color = GipsyColors.DimWhite
                )
            }

            AnimatedVisibility(visible = showKeys) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsKeyField("GEMINI API KEY", geminiKey, { geminiKey = it },
                        hint = if (settingsVm.prefs.geminiApiKey.isNotBlank()) "••••••••••••" else "ENTER KEY")
                    SettingsKeyField("GROQ API KEY", groqKey, { groqKey = it },
                        hint = if (settingsVm.prefs.groqApiKey.isNotBlank()) "••••••••••••" else "ENTER KEY")
                    SettingsKeyField("OPENROUTER API KEY", openRouterKey, { openRouterKey = it },
                        hint = if (settingsVm.prefs.openRouterApiKey.isNotBlank()) "••••••••••••" else "ENTER KEY")

                    GipsyDialogButton("SAVE KEYS") {
                        settingsVm.saveApiKeys(geminiKey, groqKey, openRouterKey)
                        geminiKey = ""; groqKey = ""; openRouterKey = ""
                    }
                }
            }
        }

        GipsyScanline()

        // ── CALLSIGN MODULE ───────────────────────────────────
        SettingsSection(title = "CALLSIGN") {
            Text(
                "RENAME SYSTEMS AND WAKE WORDS",
                style = MaterialTheme.typography.bodySmall,
                color = GipsyColors.DimWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Target selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("GIPSY", "GHOST", "IRONGATE").forEach { target ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCallsignTarget = target }
                            .border(
                                1.dp,
                                if (selectedCallsignTarget == target) GipsyColors.White else GipsyColors.BorderGray,
                                RoundedCornerShape(2.dp)
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            target,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = if (selectedCallsignTarget == target) GipsyColors.White else GipsyColors.DimWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedCallsignTarget) {
                "GIPSY" -> {
                    SettingsKeyField("CUSTOM NAME", gipsyName, { gipsyName = it }, hint = "GIPSY")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsKeyField("WAKE WORD", gipsyWake, { gipsyWake = it }, hint = "GIPSY")
                }
                "GHOST" -> {
                    SettingsKeyField("CUSTOM NAME", ghostName, { ghostName = it }, hint = "GHOST")
                }
                "IRONGATE" -> {
                    SettingsKeyField("CUSTOM NAME", irongateName, { irongateName = it }, hint = "IRONGATE")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            GipsyDialogButton("SAVE CALLSIGN") {
                settingsVm.saveCallsigns(gipsyName, ghostName, irongateName, gipsyWake)
            }
        }

        GipsyScanline()

        // ── SECURITY ──────────────────────────────────────────
        SettingsSection(title = "SECURITY") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNuclear = !showNuclear }
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    if (showNuclear) "[ HIDE ]" else "[ CHANGE NUCLEAR CODE ]",
                    style = MaterialTheme.typography.labelSmall,
                    color = GipsyColors.DimWhite
                )
            }

            AnimatedVisibility(visible = showNuclear) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsKeyField("NEW NUCLEAR CODE", nuclearCode, { nuclearCode = it },
                        hint = "WARHAWK", masked = true)
                    GipsyDialogButton("SAVE CODE") {
                        settingsVm.saveNuclearCode(nuclearCode)
                        nuclearCode = ""
                        showNuclear = false
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            SettingsKeyField("PANIC WORD", panicWord, { panicWord = it },
                hint = if (settingsVm.prefs.panicWord.isNotBlank()) "SET" else "NOT SET",
                masked = true)
            Spacer(modifier = Modifier.height(8.dp))
            GipsyDialogButton("SAVE PANIC WORD") {
                settingsVm.savePanicWord(panicWord)
                panicWord = ""
            }
        }

        GipsyScanline()

        // ── MEMORY ────────────────────────────────────────────
        SettingsSection(title = "MEMORY") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showDeleteMemoryDialog() }
                        .border(1.dp, GipsyColors.BorderGray, RoundedCornerShape(2.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "DELETE MEMORY",
                        style = MaterialTheme.typography.labelMedium,
                        color = GipsyColors.DimWhite
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.initiateFactoryReset() }
                        .border(1.dp, GipsyColors.White, RoundedCornerShape(2.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "FACTORY RESET",
                        style = MaterialTheme.typography.labelMedium,
                        color = GipsyColors.White
                    )
                }
            }
        }

        GipsyScanline()

        // ── IRONGATE ──────────────────────────────────────────
        SettingsSection(title = "DIAGNOSTICS") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.runIrongate() }
                    .border(1.dp, GipsyColors.BorderGray, RoundedCornerShape(2.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "RUN IRONGATE",
                    style = MaterialTheme.typography.labelMedium,
                    color = GipsyColors.DimWhite
                )
            }
        }

        // Save confirmation
        AnimatedVisibility(
            visible = saved,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SAVED.",
                    style = MaterialTheme.typography.displaySmall,
                    color = GipsyColors.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── SETTINGS HELPERS ──────────────────────────────────────────
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "// $title",
            style = MaterialTheme.typography.labelSmall,
            color = GipsyColors.DimWhite,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}

@Composable
fun SettingsKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String = "",
    masked: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            color = GipsyColors.DimWhite
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GipsyColors.BorderGray, RoundedCornerShape(2.dp))
                .padding(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = GipsyColors.White),
            singleLine = true,
            visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(hint, style = MaterialTheme.typography.bodyMedium, color = GipsyColors.DimWhite)
                }
                innerTextField()
            }
        )
    }
}
