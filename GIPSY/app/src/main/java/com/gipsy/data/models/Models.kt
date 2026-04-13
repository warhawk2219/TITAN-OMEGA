package com.gipsy.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── CHAT MESSAGE ──────────────────────────────────────────────
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,
    val isProtocolMessage: Boolean = false,
    val protocolName: String? = null
)

enum class MessageRole {
    USER, GIPSY, SYSTEM
}

// ── MEMORY FACT ───────────────────────────────────────────────
@Entity(tableName = "memory_facts")
data class MemoryFact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

// ── CONVERSATION SESSION ──────────────────────────────────────
@Entity(tableName = "sessions")
data class ConversationSession(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val messageCount: Int = 0
)

// ── PROTOCOL LOG ──────────────────────────────────────────────
@Entity(tableName = "protocol_log")
data class ProtocolLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val protocolName: String,
    val action: String, // ACTIVATED / DEACTIVATED / EXECUTED
    val timestamp: Long = System.currentTimeMillis(),
    val executedBy: String = "OWNER",
    val result: String = ""
)

// ── API PROVIDER ──────────────────────────────────────────────
enum class ApiProvider(val displayName: String) {
    GEMINI("GEMINI"),
    GROQ("GROQ"),
    OPENROUTER("OPENROUTER")
}

// ── BRIDGE MESSAGE ────────────────────────────────────────────
data class BridgeMessage(
    val type: String,
    val payload: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "GIPSY",
    val target: String = "GHOST"
)

// ── BRIDGE STATUS ─────────────────────────────────────────────
enum class BridgeStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR
}

// ── GIPSY STATE ───────────────────────────────────────────────
enum class GipsyListenState {
    IDLE,
    WAKE_WORD_DETECTED,
    LISTENING,
    PROCESSING,
    SPEAKING,
    WAITING_FOR_GHOST
}

// ── ACTIVE MODE ───────────────────────────────────────────────
enum class GipsyMode(val displayName: String) {
    NORMAL("NORMAL"),
    SILENT("SILENT"),
    GHOST("GHOST"),
    COMBAT("COMBAT"),
    GUARDIAN("GUARDIAN"),
    BRIEFING("BRIEFING"),
    INCOGNITO("INCOGNITO"),
    LOCKDOWN("LOCKDOWN"),
    SHADOW("SHADOW"),
    RECON("RECON"),
    CASUAL("CASUAL")
}

// ── PROTOCOL DEFINITION ───────────────────────────────────────
data class Protocol(
    val name: String,
    val type: ProtocolType,
    val callsign: Callsign,
    val ownerOnly: Boolean,
    val triggerPhrases: List<String>,
    val deactivationPhrases: List<String>,
    val activationResponse: String,
    val completionResponse: String,
    val deactivationResponse: String = "",
    val isCustom: Boolean = false
)

enum class ProtocolType { ONE_SHOT, TOGGLE, AUTO_TRIGGER }
enum class Callsign { BOSS, SIR }

// ── IRONGATE RESULT ───────────────────────────────────────────
enum class IrongateResult(val notification: String) {
    ALL_CLEAR("✅ IRONGATE — All clear"),
    SOMETHING_DOWN("⚠️ IRONGATE — Something's down"),
    BLACK_OUT("🔴 IRONGATE — Black out")
}

// ── FACTORY RESET TARGET ─────────────────────────────────────
enum class ResetTarget { GHOST, GIPSY, BOTH }

// ── USER TIER ─────────────────────────────────────────────────
enum class UserTier { OWNER, FAMILY, FRIENDS, STRANGER }
