package com.gipsy.bridge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.gipsy.data.local.PreferencesManager
import com.gipsy.data.models.BridgeStatus
import com.gipsy.data.models.IrongateResult
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

// ── BRIDGE MANAGER ────────────────────────────────────────────
@Singleton
class BridgeManager @Inject constructor(
    private val prefs: PreferencesManager
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _status = MutableStateFlow(BridgeStatus.DISCONNECTED)
    val status: StateFlow<BridgeStatus> = _status

    private val _incomingMessages = MutableStateFlow<JsonObject?>(null)
    val incomingMessages: StateFlow<JsonObject?> = _incomingMessages

    private val _irongateResult = MutableStateFlow<IrongateResult?>(null)
    val irongateResult: StateFlow<IrongateResult?> = _irongateResult

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var heartbeatJob: Job? = null
    private var listenerJob: Job? = null

    fun connect() {
        scope.launch {
            _status.value = BridgeStatus.CONNECTING
            try {
                socket = Socket(prefs.bridgeHost, prefs.bridgePort)
                writer = PrintWriter(OutputStreamWriter(socket!!.getOutputStream()), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                _status.value = BridgeStatus.CONNECTED

                // Identify ourselves to Bridge
                send(JsonObject().apply {
                    addProperty("type", "identify")
                    addProperty("source", "GIPSY")
                    addProperty("version", "1.0.0")
                })

                startHeartbeat()
                startListener()
            } catch (e: Exception) {
                _status.value = BridgeStatus.ERROR
                scheduleReconnect()
            }
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        listenerJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        _status.value = BridgeStatus.DISCONNECTED
    }

    fun send(message: JsonObject) {
        scope.launch {
            try {
                writer?.println(gson.toJson(message))
            } catch (e: Exception) {
                _status.value = BridgeStatus.ERROR
            }
        }
    }

    fun sendToGhost(type: String, payload: JsonObject = JsonObject()) {
        val msg = JsonObject().apply {
            addProperty("type", type)
            addProperty("source", "GIPSY")
            addProperty("target", "GHOST")
            add("payload", payload)
            addProperty("timestamp", System.currentTimeMillis())
        }
        send(msg)
    }

    fun requestIrongate() {
        sendToGhost("irongate_check")
    }

    private fun startHeartbeat() {
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(10_000) // every 10 seconds
                try {
                    send(JsonObject().apply {
                        addProperty("type", "heartbeat")
                        addProperty("source", "GIPSY")
                        addProperty("timestamp", System.currentTimeMillis())
                    })
                } catch (e: Exception) {
                    _status.value = BridgeStatus.ERROR
                    break
                }
            }
        }
    }

    private fun startListener() {
        listenerJob = scope.launch {
            try {
                while (isActive) {
                    val line = reader?.readLine() ?: break
                    try {
                        val json = gson.fromJson(line, JsonObject::class.java)
                        handleIncoming(json)
                    } catch (_: Exception) {}
                }
            } catch (_: SocketException) {
                _status.value = BridgeStatus.DISCONNECTED
                scheduleReconnect()
            }
        }
    }

    private fun handleIncoming(json: JsonObject) {
        val type = json.get("type")?.asString ?: return

        when (type) {
            "irongate_result" -> {
                val status = json.get("status")?.asString ?: "BLACK_OUT"
                _irongateResult.value = when (status) {
                    "ALL_CLEAR"      -> IrongateResult.ALL_CLEAR
                    "SOMETHING_DOWN" -> IrongateResult.SOMETHING_DOWN
                    else             -> IrongateResult.BLACK_OUT
                }
            }
            "callsign_update" -> {
                val target = json.get("target")?.asString ?: return
                val newName = json.get("new_name")?.asString ?: return
                val newWakeWord = json.get("new_wake_word")?.asString ?: return
                when (target) {
                    "GIPSY"     -> { prefs.gipsyCallsign = newName; prefs.gipsyWakeWord = newWakeWord }
                    "GHOST"     -> prefs.ghostCallsign = newName
                    "IRONGATE"  -> prefs.irongateCallsign = newName
                }
            }
            "ghost_execution_complete" -> {
                _incomingMessages.value = json
            }
            "protocol_status" -> {
                _incomingMessages.value = json
            }
            else -> {
                _incomingMessages.value = json
            }
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5_000)
            connect()
        }
    }

    fun isConnected() = _status.value == BridgeStatus.CONNECTED
}
