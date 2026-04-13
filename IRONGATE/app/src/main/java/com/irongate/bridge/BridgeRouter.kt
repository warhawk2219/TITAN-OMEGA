package com.irongate.bridge

import android.util.Log
import com.irongate.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The core bridge router.
 * Owns both AssistantSockets, routes messages between them,
 * maintains feed log, and exposes state as Flows.
 */
class BridgeRouter {

    companion object {
        private const val TAG = "BridgeRouter"
        const val GHOST_PORT = 8765
        const val GIPSY_PORT = 8766
    }

    private val _status = MutableStateFlow(ConnectionStatus())
    val status: StateFlow<ConnectionStatus> = _status

    private val _feed = MutableStateFlow<List<FeedEntry>>(emptyList())
    val feed: StateFlow<List<FeedEntry>> = _feed

    private var ghostLatency = 0L
    private var gipsyLatency = 0L
    private var msgsRouted = 0
    private val startTime = System.currentTimeMillis()

    // Chat response listeners (set by ViewModel)
    var onChatResponse: ((ChatMessage) -> Unit)? = null

    private val ghostSocket = AssistantSocket(
        port = GHOST_PORT,
        source = AssistantSource.GHOST,
        onStateChange = { state ->
            updateStatus { copy(ghost = state) }
            if (state == AssistantState.OFFLINE) {
                gipsySocket.send(
                    BridgeMessage(
                        type = "status",
                        sender = "BRIDGE",
                        connState = "GHOST has gone offline. Bridge maintaining connection."
                    )
                )
            } else if (state == AssistantState.ONLINE) {
                gipsySocket.send(
                    BridgeMessage(
                        type = "status",
                        sender = "BRIDGE",
                        connState = "GHOST back online."
                    )
                )
            }
        },
        onMessage = { msg -> handleIncoming(msg, AssistantSource.GHOST) },
        onLatency = { ms ->
            ghostLatency = ms
            updateStatus { copy(ghostLatencyMs = ms) }
        }
    )

    private val gipsySocket = AssistantSocket(
        port = GIPSY_PORT,
        source = AssistantSource.GIPSY,
        onStateChange = { state ->
            updateStatus { copy(gipsy = state) }
            if (state == AssistantState.OFFLINE) {
                ghostSocket.send(
                    BridgeMessage(
                        type = "status",
                        sender = "BRIDGE",
                        connState = "GIPSY has gone offline. Bridge maintaining connection."
                    )
                )
            } else if (state == AssistantState.ONLINE) {
                ghostSocket.send(
                    BridgeMessage(
                        type = "status",
                        sender = "BRIDGE",
                        connState = "GIPSY back online."
                    )
                )
            }
        },
        onMessage = { msg -> handleIncoming(msg, AssistantSource.GIPSY) },
        onLatency = { ms ->
            gipsyLatency = ms
            updateStatus { copy(gipsyLatencyMs = ms) }
        }
    )

    fun start() {
        ghostSocket.start()
        gipsySocket.start()
        Log.d(TAG, "BridgeRouter started — GHOST:$GHOST_PORT GIPSY:$GIPSY_PORT")
    }

    fun stop() {
        ghostSocket.stop()
        gipsySocket.stop()
    }

    private fun handleIncoming(msg: BridgeMessage, from: AssistantSource) {
        val target = if (from == AssistantSource.GHOST) AssistantSource.GIPSY else AssistantSource.GHOST
        Log.d(TAG, "Routing ${msg.type} from $from → $target")

        // Route to opposite assistant
        when (target) {
            AssistantSource.GIPSY -> gipsySocket.send(msg)
            AssistantSource.GHOST -> ghostSocket.send(msg)
            else -> {}
        }

        // Send confirm back to sender
        val confirmMsg = BridgeMessage.confirm(target.name, true)
        when (from) {
            AssistantSource.GHOST -> ghostSocket.send(confirmMsg)
            AssistantSource.GIPSY -> gipsySocket.send(confirmMsg)
            else -> {}
        }

        // Check if this is a chat response for INTERFACE
        if (msg.type == "chat_response" && msg.message != null) {
            onChatResponse?.invoke(ChatMessage(from = from.name, text = msg.message))
        }

        // Add to feed
        addFeedEntry(msg, from, target)
        msgsRouted++
        updateStatus { copy(msgsRouted = msgsRouted) }
    }

    fun sendUserMessage(target: String, message: String) {
        val msg = BridgeMessage.userMessage(target, message)
        when (target) {
            "GHOST" -> ghostSocket.send(msg)
            "GIPSY" -> gipsySocket.send(msg)
            "BOTH" -> {
                ghostSocket.send(msg)
                gipsySocket.send(msg)
            }
        }
        addFeedEntry(
            BridgeMessage(type = "command", action = "user_message", target = target, message = message),
            AssistantSource.BRIDGE,
            AssistantSource.valueOf(target.ifEmpty { "GHOST" })
        )
    }

    fun sendCallsign(target: String, displayName: String, wakeWord: String) {
        val msg = BridgeMessage.callsign(target, displayName, wakeWord)
        when (target) {
            "GHOST" -> ghostSocket.send(msg)
            "GIPSY" -> gipsySocket.send(msg)
            "IRONGATE" -> {} // Local only
        }
    }

    fun sendNukeCommand(target: String) {
        val msg = BridgeMessage(
            type = "command",
            action = "factory_reset",
            target = target,
            source = "BRIDGE"
        )
        when (target) {
            "GHOST" -> ghostSocket.send(msg)
            "GIPSY" -> gipsySocket.send(msg)
            "BOTH" -> {
                ghostSocket.send(msg)
                gipsySocket.send(msg)
            }
        }
    }

    fun getProtocolResult(): ProtocolResult {
        val ghostOk = _status.value.ghost == AssistantState.ONLINE
        val gipsyOk = _status.value.gipsy == AssistantState.ONLINE
        return when {
            ghostOk && gipsyOk -> ProtocolResult.NOMINAL
            ghostOk || gipsyOk -> ProtocolResult.CAUTION
            else -> ProtocolResult.BLACKOUT
        }
    }

    fun getUptimeSeconds() = (System.currentTimeMillis() - startTime) / 1000

    private fun addFeedEntry(msg: BridgeMessage, from: AssistantSource, to: AssistantSource) {
        val summary = when (msg.type.lowercase()) {
            "sync" -> "Protocol/Mode ${msg.name ?: ""} ${msg.state ?: ""}"
            "command" -> "action: ${msg.action} · target: ${msg.target ?: ""}"
            "data" -> "category: ${msg.category} · payload received"
            "status" -> msg.connState ?: "Status update"
            "callsign" -> "Callsign update for ${msg.target}"
            "user_message" -> "User → ${msg.target}: ${msg.message?.take(40) ?: ""}"
            else -> msg.type
        }
        val entry = FeedEntry(
            type = msg.type.uppercase(),
            summary = summary,
            route = "${from.name} → ${to.name} · DELIVERED"
        )
        val current = _feed.value.toMutableList()
        current.add(0, entry)
        if (current.size > 200) current.removeLastOrNull()
        _feed.value = current
    }

    private fun updateStatus(transform: ConnectionStatus.() -> ConnectionStatus) {
        _status.value = _status.value.transform().copy(uptimeSeconds = getUptimeSeconds())
    }
}
