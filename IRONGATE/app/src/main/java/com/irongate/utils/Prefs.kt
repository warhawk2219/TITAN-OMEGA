package com.irongate.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.irongate.model.CallsignConfig

object Prefs {

    private const val PREF_FILE = "irongate_secure_prefs"
    private const val KEY_OPENROUTER_API = "openrouter_api_key"
    private const val KEY_CALLSIGN = "callsign_config"
    private const val KEY_PROTOCOL_LOGS = "protocol_logs"

    private fun getPrefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    fun saveApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_OPENROUTER_API, key).apply()
    }

    fun getApiKey(context: Context): String =
        getPrefs(context).getString(KEY_OPENROUTER_API, "") ?: ""

    fun hasApiKey(context: Context): Boolean = getApiKey(context).isNotEmpty()

    fun saveCallsign(context: Context, config: CallsignConfig) {
        getPrefs(context).edit().putString(KEY_CALLSIGN, Gson().toJson(config)).apply()
    }

    fun getCallsign(context: Context): CallsignConfig = try {
        val json = getPrefs(context).getString(KEY_CALLSIGN, null)
        if (json != null) Gson().fromJson(json, CallsignConfig::class.java)
        else CallsignConfig()
    } catch (e: Exception) { CallsignConfig() }
}
