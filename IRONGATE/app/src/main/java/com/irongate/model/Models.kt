package com.irongate.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// ─── Connection State ────────────────────────────────────────────────────────

enum class AssistantState { ONLINE, OFFLINE, RECONNECTING }

data class ConnectionStatus(
    val ghost: AssistantState = AssistantState.OFFLINE,
    val gipsy: AssistantState = AssistantState.OFFLINE,
    val ghostLatencyMs: Long = 0L,
    val gipsyLatencyMs: Long = 0L,
    val uptimeSeconds: Long = 0L,
    val msgsRouted: Int = 0,
    val msgsBuffered: Int = 0,
    val lastProtocolResult: ProtocolResult? = null
)

// ─── Message Types ───────────────────────────────────────────────────────────

enum class MessageType { SYNC, CONFIRM, DATA, COMMAND, STATUS, CALLSIGN, USER_MESSAGE }
enum class AssistantSource { GHOST, GIPSY, BRIDGE }
enum class ProtocolResult { NOMINAL, CAUTION, BLACKOUT }

data class BridgeMessage(
    val type: String,
    val source: String = "BRIDGE",
    val timestamp: String = java.time.Instant.now().toString(),
    // SYNC fields
    val category: String? = null,
    val name: String? = null,
    val state: String? = null,
    // CONFIRM fields
    val status: String? = null,
    // DATA fields
    val payload: Map<String, Any>? = null,
    // COMMAND fields
    val action: String? = null,
    val target: String? = null,
    val params: Map<String, Any>? = null,
    // STATUS fields
    val sender: String? = null,
    @SerializedName("state") val connState: String? = null,
    // CALLSIGN fields
    val displayName: String? = null,
    val wakeWord: String? = null,
    // USER_MESSAGE fields
    val message: String? = null
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): BridgeMessage? = try {
            Gson().fromJson(json, BridgeMessage::class.java)
        } catch (e: Exception) { null }

        fun ping(source: AssistantSource) = BridgeMessage(
            type = MessageType.STATUS.name.lowercase(),
            sender = source.name,
            connState = "ping",
            source = source.name
        )

        fun statusOnline(source: AssistantSource) = BridgeMessage(
            type = MessageType.STATUS.name.lowercase(),
            sender = source.name,
            connState = "online",
            source = source.name
        )

        fun callsign(target: String, displayName: String, wakeWord: String) = BridgeMessage(
            type = MessageType.CALLSIGN.name.lowercase(),
            target = target,
            displayName = displayName,
            wakeWord = wakeWord,
            source = AssistantSource.BRIDGE.name
        )

        fun userMessage(target: String, message: String) = BridgeMessage(
            type = MessageType.USER_MESSAGE.name.lowercase(),
            target = target,
            message = message,
            source = AssistantSource.BRIDGE.name
        )

        fun confirm(recipient: String, delivered: Boolean) = BridgeMessage(
            type = MessageType.CONFIRM.name.lowercase(),
            target = recipient,
            status = if (delivered) "synced" else "failed",
            source = AssistantSource.BRIDGE.name
        )
    }
}

// ─── Feed Log Entry ──────────────────────────────────────────────────────────

data class FeedEntry(
    val id: Long = System.currentTimeMillis(),
    val type: String,
    val summary: String,
    val route: String,
    val time: String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        .format(java.util.Date())
)

// ─── Chat Message ────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val from: String,   // "USER", "GHOST", "GIPSY"
    val text: String
)

// ─── Protocol Log ────────────────────────────────────────────────────────────

data class ProtocolLog(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        .format(java.util.Date()),
    val ghostOk: Boolean,
    val gipsyOk: Boolean,
    val ghostLatencyMs: Long,
    val gipsyLatencyMs: Long,
    val result: ProtocolResult,
    val notes: String = ""
)

// ─── Callsign Settings ───────────────────────────────────────────────────────

data class CallsignConfig(
    val ghostName: String = "GHOST",
    val ghostWake: String = "Hey Ghost",
    val gipsyName: String = "GIPSY",
    val gipsyWake: String = "Hey Gipsy",
    val irongateName: String = "IRONGATE",
    val irongateWake: String = "Hey Irongate"
)
