package com.gipsy.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.gipsy.data.models.ApiProvider
import com.gipsy.data.models.GipsyMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "gipsy_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── API KEYS ──────────────────────────────────────────────
    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API, value).apply()

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_API, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ_API, value).apply()

    var openRouterApiKey: String
        get() = prefs.getString(KEY_OPENROUTER_API, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENROUTER_API, value).apply()

    var activeProvider: ApiProvider
        get() = ApiProvider.valueOf(
            prefs.getString(KEY_ACTIVE_PROVIDER, ApiProvider.GEMINI.name) ?: ApiProvider.GEMINI.name
        )
        set(value) = prefs.edit().putString(KEY_ACTIVE_PROVIDER, value.name).apply()

    // ── SETTINGS ──────────────────────────────────────────────
    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var activeMode: GipsyMode
        get() = GipsyMode.valueOf(
            prefs.getString(KEY_ACTIVE_MODE, GipsyMode.NORMAL.name) ?: GipsyMode.NORMAL.name
        )
        set(value) = prefs.edit().putString(KEY_ACTIVE_MODE, value.name).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD, value).apply()

    // ── CALLSIGN / CUSTOM NAMES ───────────────────────────────
    var gipsyCallsign: String
        get() = prefs.getString(KEY_GIPSY_CALLSIGN, "GIPSY") ?: "GIPSY"
        set(value) = prefs.edit().putString(KEY_GIPSY_CALLSIGN, value).apply()

    var ghostCallsign: String
        get() = prefs.getString(KEY_GHOST_CALLSIGN, "GHOST") ?: "GHOST"
        set(value) = prefs.edit().putString(KEY_GHOST_CALLSIGN, value).apply()

    var irongateCallsign: String
        get() = prefs.getString(KEY_IRONGATE_CALLSIGN, "IRONGATE") ?: "IRONGATE"
        set(value) = prefs.edit().putString(KEY_IRONGATE_CALLSIGN, value).apply()

    var gipsyWakeWord: String
        get() = prefs.getString(KEY_GIPSY_WAKE_WORD, "GIPSY") ?: "GIPSY"
        set(value) = prefs.edit().putString(KEY_GIPSY_WAKE_WORD, value).apply()

    // ── NUCLEAR CODE ─────────────────────────────────────────
    var nuclearCode: String
        get() = prefs.getString(KEY_NUCLEAR_CODE, "WARHAWK") ?: "WARHAWK"
        set(value) = prefs.edit().putString(KEY_NUCLEAR_CODE, value).apply()

    // ── PANIC WORD ────────────────────────────────────────────
    var panicWord: String
        get() = prefs.getString(KEY_PANIC_WORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PANIC_WORD, value).apply()

    // ── BRIDGE ────────────────────────────────────────────────
    var bridgeHost: String
        get() = prefs.getString(KEY_BRIDGE_HOST, "localhost") ?: "localhost"
        set(value) = prefs.edit().putString(KEY_BRIDGE_HOST, value).apply()

    var bridgePort: Int
        get() = prefs.getInt(KEY_BRIDGE_PORT, 8766)
        set(value) = prefs.edit().putInt(KEY_BRIDGE_PORT, value).apply()

    // ── CLEAR ALL ─────────────────────────────────────────────
    fun clearAll() {
        prefs.edit().clear().apply()
        // Restore nuclear code default
        nuclearCode = "WARHAWK"
    }

    companion object {
        private const val KEY_GEMINI_API        = "gemini_api_key"
        private const val KEY_GROQ_API          = "groq_api_key"
        private const val KEY_OPENROUTER_API    = "openrouter_api_key"
        private const val KEY_ACTIVE_PROVIDER   = "active_provider"
        private const val KEY_TTS_ENABLED       = "tts_enabled"
        private const val KEY_ACTIVE_MODE       = "active_mode"
        private const val KEY_WAKE_WORD         = "wake_word_enabled"
        private const val KEY_GIPSY_CALLSIGN    = "gipsy_callsign"
        private const val KEY_GHOST_CALLSIGN    = "ghost_callsign"
        private const val KEY_IRONGATE_CALLSIGN = "irongate_callsign"
        private const val KEY_GIPSY_WAKE_WORD   = "gipsy_wake_word"
        private const val KEY_NUCLEAR_CODE      = "nuclear_code"
        private const val KEY_PANIC_WORD        = "panic_word"
        private const val KEY_BRIDGE_HOST       = "bridge_host"
        private const val KEY_BRIDGE_PORT       = "bridge_port"
    }
}
