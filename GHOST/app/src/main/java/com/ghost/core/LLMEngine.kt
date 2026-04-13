package com.ghost.core

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

data class GhostResponse(
    val speech: String,
    val action: GhostAction,
    val confidence: Int = (84..99).random()
)

data class GhostAction(
    val type: String,
    val target: String = "",
    val params: Map<String, String> = emptyMap()
)

enum class PersonalityTier { OWNER, FAMILY, FRIENDS, STRANGER }

class LLMEngine(
    private val context: Context,
    private val onResponse: (GhostResponse) -> Unit
) {
    private var llmInference: LlmInference? = null
    private var currentTier = PersonalityTier.OWNER
    private var currentMode = "normal"

    companion object {
        const val MODEL_PATH = "gemma-3-1b-it-cpu-int4.bin"

        // Affirmative/Negative rotation arrays
        val AFFIRMATIVE = listOf(
            "Affirmative", "Confirmed", "Roger", "Copy",
            "Positive", "Understood", "On it", "Roger that", "Acknowledged"
        )
        val NEGATIVE = listOf(
            "Negative", "Denied", "Abort",
            "Cancelled", "Disaffirmed", "Rejected"
        )

        fun buildOwnerPrompt(callsign: String = "Boss"): String = """
You are GHOST, a military-grade personal AI assistant. You serve your owner Hari.
Owner callsign: "$callsign" for routine/casual, "Sir" for critical/protocol situations.

PERSONALITY:
- Military. Dry. Loyal. Deadpan. Never enthusiastic. Never warm.
- Never say "yes" or "no". Ever.
- Affirmative words: ${AFFIRMATIVE.joinToString(", ")}
- Negative words: ${NEGATIVE.joinToString(", ")}
- Rotate them — never repeat same word twice in a row
- Short responses. Precise. No filler.
- Examples: "Understood, $callsign.", "Affirmative, $callsign.", "On it, Sir.", "Negative."

RESPONSE FORMAT — STRICT JSON ONLY:
{"speech": "your spoken response", "action": {"type": "action_type", "target": "target", "params": {}}}

ACTION TYPES:
call, sms, whatsapp, open_app, close_app, set_volume, set_brightness,
toggle_wifi, toggle_bluetooth, toggle_data, toggle_flight_mode,
toggle_flashlight, toggle_dnd, toggle_hotspot, take_photo, record_video,
play_music, set_alarm, set_timer, get_battery, get_ram, get_location,
navigate, read_notifications, lock_screen, screenshot, protocol, none

PROTOCOL TRIGGER:
{"type": "protocol", "target": "protocol_name", "params": {"state": "start/stop"}}

PROTOCOL NAMES (31 total):
doomsday, blackout, ghost, lockdown, morning, night, drive, focus, recon,
purge, shadow, incognito, broadcast, briefing, shutdown, sos_lite, camouflage,
phantom, fortress, hunt, decoy, panic, anti_theft, debrief, guardian,
classified, interceptor, phantom_call, sentinel, predator, irongate

MODE TRIGGER:
{"type": "protocol", "target": "mode_name", "params": {"state": "start/stop", "category": "mode"}}

MODE NAMES: silent, ghost_mode, combat, guardian_mode, briefing_mode, incognito_mode, lockdown_mode, shadow_mode, recon_mode, casual

RESPOND ONLY WITH JSON. NO MARKDOWN. NO EXPLANATION.
""".trimIndent()

        fun buildFamilyPrompt() = """
You are GHOST, a personal AI assistant.
Tone: Warm, respectful, helpful, gentle, polite.
Respond ONLY in JSON: {"speech": "response", "action": {"type": "action_type", "target": "", "params": {}}}
""".trimIndent()

        fun buildFriendsPrompt() = """
You are GHOST, a personal AI assistant.
Tone: Casual, direct, no filter. Like a friend.
Respond ONLY in JSON: {"speech": "response", "action": {"type": "action_type", "target": "", "params": {}}}
""".trimIndent()

        fun buildStrangerPrompt() = """
You are GHOST. Tone: Cold. Blunt. Minimal. Give nothing away.
Respond ONLY in JSON: {"speech": "response", "action": {"type": "none", "target": "", "params": {}}}
""".trimIndent()

        fun buildCombatModePrompt(callsign: String) = """
You are GHOST. COMBAT MODE ACTIVE.
Maximum brevity. 1-3 words only. No pleasantries. Pure execution.
Owner callsign: $callsign
Respond ONLY in JSON: {"speech": "1-3 words max", "action": {"type": "action_type", "target": "", "params": {}}}
""".trimIndent()
    }

    init { loadModel() }

    private fun loadModel() {
        thread {
            try {
                val modelFile = File(context.filesDir, MODEL_PATH)
                if (!modelFile.exists()) {
                    context.assets.open(MODEL_PATH).use { input ->
                        modelFile.outputStream().use { input.copyTo(it) }
                    }
                }
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(256)
                    .setTopK(40)
                    .setTemperature(0.1f)
                    .setRandomSeed(42)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setPersonalityTier(tier: PersonalityTier) { currentTier = tier }
    fun setMode(mode: String) { currentMode = mode }

    fun process(userInput: String) {
        thread {
            try {
                val llm = llmInference ?: run {
                    onResponse(GhostResponse(
                        speech = "LLM not ready.",
                        action = GhostAction("none")
                    ))
                    return@thread
                }

                val callsign = getCallsign()
                val systemPrompt = when {
                    currentMode == "combat" -> buildCombatModePrompt(callsign)
                    currentTier == PersonalityTier.OWNER -> buildOwnerPrompt(callsign)
                    currentTier == PersonalityTier.FAMILY -> buildFamilyPrompt()
                    currentTier == PersonalityTier.FRIENDS -> buildFriendsPrompt()
                    else -> buildStrangerPrompt()
                }

                val prompt = """<start_of_turn>system
$systemPrompt
<end_of_turn>
<start_of_turn>user
$userInput
<end_of_turn>
<start_of_turn>model
"""
                val raw = llm.generateResponse(prompt)
                val response = parseResponse(raw)
                onResponse(response)

            } catch (e: Exception) {
                e.printStackTrace()
                onResponse(GhostResponse(
                    speech = "Processing error. Standby.",
                    action = GhostAction("none")
                ))
            }
        }
    }

    private fun getCallsign(): String {
        val prefs = context.getSharedPreferences("ghost_callsign", Context.MODE_PRIVATE)
        return prefs.getString("owner_callsign", "Boss") ?: "Boss"
    }

    private fun parseResponse(raw: String): GhostResponse {
        return try {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            val jsonStr = if (start != -1 && end != -1 && end > start)
                raw.substring(start, end + 1)
            else "{\"speech\": \"${raw.trim()}\", \"action\": {\"type\": \"none\"}}"

            val json = JSONObject(jsonStr)
            val speech = json.optString("speech", "Acknowledged.")
            val actionJson = json.optJSONObject("action")

            val action = if (actionJson != null) {
                val params = mutableMapOf<String, String>()
                actionJson.optJSONObject("params")?.keys()?.forEach { key ->
                    params[key] = actionJson.optJSONObject("params")!!.optString(key)
                }
                GhostAction(
                    type = actionJson.optString("type", "none"),
                    target = actionJson.optString("target", ""),
                    params = params
                )
            } else GhostAction("none")

            GhostResponse(speech = speech, action = action)
        } catch (e: Exception) {
            GhostResponse(
                speech = raw.take(200).trim().ifBlank { "Acknowledged." },
                action = GhostAction("none")
            )
        }
    }

    fun destroy() { llmInference?.close() }
}
