package com.gipsy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gipsy.ai.AIRouter
import com.gipsy.ai.ChatMessage
import com.gipsy.bridge.BridgeManager
import com.gipsy.data.local.MessageDao
import com.gipsy.data.local.MemoryDao
import com.gipsy.data.local.PreferencesManager
import com.gipsy.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class GipsyUiState(
    val messages: List<Message> = emptyList(),
    val isProcessing: Boolean = false,
    val listenState: GipsyListenState = GipsyListenState.IDLE,
    val bridgeStatus: BridgeStatus = BridgeStatus.DISCONNECTED,
    val ttsEnabled: Boolean = true,
    val activeMode: GipsyMode = GipsyMode.NORMAL,
    val activeProvider: ApiProvider = ApiProvider.GEMINI,
    val showFactoryResetDialog: Boolean = false,
    val factoryResetStep: Int = 0, // 0=hidden, 1=warning, 2=target, 3=confirm, 4=code
    val factoryResetTarget: ResetTarget? = null,
    val factoryResetCodeInput: String = "",
    val factoryResetError: String = "",
    val showDeleteMemoryDialog: Boolean = false,
    val irongateResult: IrongateResult? = null,
    val currentSessionId: String = UUID.randomUUID().toString()
)

@HiltViewModel
class GipsyViewModel @Inject constructor(
    private val aiRouter: AIRouter,
    private val bridgeManager: BridgeManager,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GipsyUiState(
        ttsEnabled = prefs.ttsEnabled,
        activeMode = prefs.activeMode,
        activeProvider = prefs.activeProvider
    ))
    val uiState: StateFlow<GipsyUiState> = _uiState.asStateFlow()

    private val chatHistory = mutableListOf<ChatMessage>()
    private val sessionId = UUID.randomUUID().toString()

    init {
        loadMessages()
        observeBridge()
        observeIrongate()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageDao.getAllMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
                // Sync chat history for AI context
                chatHistory.clear()
                messages.takeLast(20).forEach { msg ->
                    chatHistory.add(ChatMessage(
                        role = if (msg.role == MessageRole.USER) "user" else "assistant",
                        content = msg.content
                    ))
                }
            }
        }
    }

    private fun observeBridge() {
        viewModelScope.launch {
            bridgeManager.status.collect { status ->
                _uiState.update { it.copy(bridgeStatus = status) }
            }
        }
        viewModelScope.launch {
            bridgeManager.incomingMessages.collect { json ->
                json ?: return@collect
                val type = json.get("type")?.asString ?: return@collect
                when (type) {
                    "ghost_execution_complete" -> {
                        val task = json.get("task")?.asString ?: "task"
                        val ghostResponse = "GHOST did it, Cooper. $task — handled."
                        addGipsyMessage(ghostResponse)
                    }
                    "protocol_status" -> {
                        val msg = json.get("message")?.asString ?: return@collect
                        addGipsyMessage(msg)
                    }
                }
            }
        }
    }

    private fun observeIrongate() {
        viewModelScope.launch {
            bridgeManager.irongateResult.collect { result ->
                result ?: return@collect
                _uiState.update { it.copy(irongateResult = result) }
                addGipsyMessage(result.notification)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isProcessing) return

        viewModelScope.launch {
            // Check for protocol commands first
            val protocolCommand = detectProtocol(text)
            if (protocolCommand != null) {
                handleProtocol(protocolCommand, text)
                return@launch
            }

            // Save user message
            val userMessage = Message(
                content = text,
                role = MessageRole.USER,
                sessionId = sessionId
            )
            messageDao.insertMessage(userMessage)

            _uiState.update { it.copy(isProcessing = true, listenState = GipsyListenState.PROCESSING) }

            // Get AI response
            val result = aiRouter.query(chatHistory, text)

            result.fold(
                onSuccess = { response ->
                    val gipsyMessage = Message(
                        content = response,
                        role = MessageRole.GIPSY,
                        sessionId = sessionId
                    )
                    messageDao.insertMessage(gipsyMessage)
                    chatHistory.add(ChatMessage("user", text))
                    chatHistory.add(ChatMessage("assistant", response))
                },
                onFailure = { error ->
                    val errorMsg = "Can't reach any API right now, Cooper. Check your connections."
                    val gipsyMessage = Message(
                        content = errorMsg,
                        role = MessageRole.GIPSY,
                        sessionId = sessionId
                    )
                    messageDao.insertMessage(gipsyMessage)
                }
            )

            _uiState.update { it.copy(isProcessing = false, listenState = GipsyListenState.IDLE) }
        }
    }

    private fun detectProtocol(text: String): String? {
        val upper = text.uppercase().trim()
        val protocols = listOf(
            "DOOMSDAY", "BLACKOUT", "GHOST", "LOCKDOWN", "MORNING", "NIGHT",
            "DRIVE", "FOCUS", "RECON", "PURGE", "SHADOW", "INCOGNITO",
            "BROADCAST", "BRIEFING", "SHUTDOWN", "SOS LITE", "CAMOUFLAGE",
            "PHANTOM", "FORTRESS", "HUNT", "DECOY", "PANIC", "ANTI-THEFT",
            "DEBRIEF", "GUARDIAN", "CLASSIFIED", "INTERCEPTOR", "PHANTOM CALL",
            "SENTINEL", "PREDATOR", "IRONGATE"
        )
        return protocols.firstOrNull { protocol ->
            upper.contains(protocol) &&
            (upper.contains("COMMENCE") || upper.contains("ACTIVATE") ||
             upper.contains("PROTOCOL") || upper == protocol)
        }
    }

    private fun handleProtocol(protocol: String, fullText: String) {
        viewModelScope.launch {
            val response = getProtocolActivationResponse(protocol)
            addGipsyMessage(response, isProtocol = true, protocolName = protocol)

            // Send to GHOST via Bridge
            if (bridgeManager.isConnected()) {
                val payload = com.google.gson.JsonObject().apply {
                    addProperty("protocol", protocol)
                    addProperty("fullCommand", fullText)
                    addProperty("timestamp", System.currentTimeMillis())
                }
                bridgeManager.sendToGhost("protocol_execute", payload)
            } else {
                addGipsyMessage("Bridge is down, Cooper. GHOST is unreachable. Protocol not executed.")
            }
        }
    }

    private fun getProtocolActivationResponse(protocol: String): String {
        return when (protocol.uppercase()) {
            "DOOMSDAY"      -> "Doomsday initiated, Sir. GHOST is acquiring your location and dispatching the distress signal now. Standing by on comms."
            "BLACKOUT"      -> "Blackout initiated, Sir. All networks killed. Going dark."
            "GHOST"         -> "Ghost Protocol active, Sir. Going silent."
            "LOCKDOWN"      -> "Lockdown active, Sir. Apps secured. Whitelist only. Auto-reply enabled."
            "MORNING"       -> "Good morning, Cooper. GHOST has you sorted. Here's your day."
            "NIGHT"         -> "Night Protocol active, Cooper. GHOST has handled the settings. Get some rest. I'll be here if you need anything."
            "DRIVE"         -> "Drive Protocol active, Cooper. Auto-reply on. Music loading. Eyes on the road."
            "FOCUS"         -> "Focus Protocol active, Cooper. I'll hold your calls."
            "RECON"         -> "" // Silent — GIPSY says nothing on activate
            "PURGE"         -> "Purge initiated, Sir."
            "SHADOW"        -> "Shadow active, Sir. Logging in progress."
            "INCOGNITO"     -> "Incognito active, Cooper. You're off the grid socially."
            "BROADCAST"     -> "Broadcasting now, Cooper. Confirm message?"
            "BRIEFING"      -> "Pulling your briefing, Cooper."
            "SHUTDOWN"      -> "Initiating Shutdown sequence, Cooper."
            "SOS LITE"      -> "Location signal sent, Sir. Standing by."
            "CAMOUFLAGE"    -> "" // Goes dark immediately
            "PHANTOM"       -> "Phantom Protocol active, Sir. Stationary detection triggered. Logging position."
            "FORTRESS"      -> "Fortress active, Sir. Maximum security engaged."
            "HUNT"          -> "Hunt active, Cooper. GHOST is monitoring the target."
            "DECOY"         -> "" // Silent
            "ANTI-THEFT"    -> "Anti-Theft active, Sir. Device locked. GHOST is watching."
            "DEBRIEF"       -> "Pulling today's debrief, Cooper."
            "GUARDIAN"      -> "Guardian active, Cooper. GHOST is on battery watch."
            "CLASSIFIED"    -> "" // Goes hidden
            "INTERCEPTOR"   -> "Interceptor active, Cooper. Monitoring all incoming."
            "PHANTOM CALL"  -> "Phantom Call armed, Sir. Trigger word set. Listening."
            "SENTINEL"      -> "Sentinel armed, Sir. Monitoring access points."
            "PREDATOR"      -> "Predator active, Sir. Scanning all processes."
            "IRONGATE"      -> "IRONGATE initiated, Cooper. Running diagnostics on the Bridge."
            else            -> "Protocol acknowledged, Cooper. Passing to GHOST."
        }
    }

    private suspend fun addGipsyMessage(
        content: String,
        isProtocol: Boolean = false,
        protocolName: String? = null
    ) {
        if (content.isBlank()) return
        val message = Message(
            content = content,
            role = MessageRole.GIPSY,
            sessionId = sessionId,
            isProtocolMessage = isProtocol,
            protocolName = protocolName
        )
        messageDao.insertMessage(message)
    }

    // ── TTS TOGGLE ────────────────────────────────────────────
    fun toggleTts() {
        val newValue = !_uiState.value.ttsEnabled
        prefs.ttsEnabled = newValue
        _uiState.update { it.copy(ttsEnabled = newValue) }
    }

    // ── LISTEN STATE ──────────────────────────────────────────
    fun setListenState(state: GipsyListenState) {
        _uiState.update { it.copy(listenState = state) }
    }

    // ── FACTORY RESET ─────────────────────────────────────────
    fun initiateFactoryReset() {
        _uiState.update { it.copy(showFactoryResetDialog = true, factoryResetStep = 1) }
    }

    fun confirmFactoryResetWarning() {
        _uiState.update { it.copy(factoryResetStep = 2) }
    }

    fun selectFactoryResetTarget(target: ResetTarget) {
        _uiState.update { it.copy(factoryResetTarget = target, factoryResetStep = 3) }
    }

    fun confirmFactoryResetStep3() {
        _uiState.update { it.copy(factoryResetStep = 4) }
    }

    fun updateFactoryResetCode(code: String) {
        _uiState.update { it.copy(factoryResetCodeInput = code, factoryResetError = "") }
    }

    fun executeFactoryReset() {
        val state = _uiState.value
        if (state.factoryResetCodeInput.uppercase() != prefs.nuclearCode.uppercase()) {
            _uiState.update { it.copy(factoryResetError = "INVALID CODE. ACCESS DENIED.") }
            return
        }
        // Trigger audio playback via event — handled in UI
        _uiState.update { it.copy(factoryResetStep = 5) } // step 5 = play audio then wipe
    }

    fun performActualWipe() {
        viewModelScope.launch {
            val target = _uiState.value.factoryResetTarget ?: ResetTarget.GIPSY
            when (target) {
                ResetTarget.GIPSY, ResetTarget.BOTH -> {
                    messageDao.deleteAllMessages()
                    memoryDao.deleteAllFacts()
                    prefs.clearAll()
                }
                ResetTarget.GHOST -> {
                    // Send wipe command to GHOST via Bridge
                    bridgeManager.sendToGhost("factory_reset", com.google.gson.JsonObject())
                }
            }
            if (target == ResetTarget.BOTH) {
                bridgeManager.sendToGhost("factory_reset", com.google.gson.JsonObject())
            }
            dismissFactoryReset()
        }
    }

    fun dismissFactoryReset() {
        _uiState.update { it.copy(
            showFactoryResetDialog = false,
            factoryResetStep = 0,
            factoryResetTarget = null,
            factoryResetCodeInput = "",
            factoryResetError = ""
        )}
    }

    // ── DELETE MEMORY ─────────────────────────────────────────
    fun showDeleteMemoryDialog() {
        _uiState.update { it.copy(showDeleteMemoryDialog = true) }
    }

    fun confirmDeleteMemory() {
        viewModelScope.launch {
            messageDao.deleteAllMessages()
            memoryDao.deleteNonPinnedFacts()
            _uiState.update { it.copy(showDeleteMemoryDialog = false) }
        }
    }

    fun dismissDeleteMemory() {
        _uiState.update { it.copy(showDeleteMemoryDialog = false) }
    }

    // ── PROVIDER SWITCH ───────────────────────────────────────
    fun setProvider(provider: ApiProvider) {
        prefs.activeProvider = provider
        _uiState.update { it.copy(activeProvider = provider) }
    }

    // ── IRONGATE ──────────────────────────────────────────────
    fun runIrongate() {
        bridgeManager.requestIrongate()
        addGipsyMessageSync("IRONGATE initiated, Cooper. Running diagnostics on the Bridge.")
    }

    private fun addGipsyMessageSync(content: String) {
        viewModelScope.launch { addGipsyMessage(content) }
    }

    fun clearIrongateResult() {
        _uiState.update { it.copy(irongateResult = null) }
    }
}
