package com.ghost.bridge

import android.content.Context
import android.util.Log
import com.ghost.core.GhostResponse
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.Socket
import java.net.ConnectException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class BridgeHandler(private val context: Context) {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isConnected = AtomicBoolean(false)
    private val messageQueue = ConcurrentLinkedQueue<String>()
    private var reconnectDelay = 1000L

    companion object {
        const val TAG = "GHOST_BRIDGE"
        const val HOST = "localhost"
        const val PORT = 8765
        const val HEARTBEAT_INTERVAL = 10000L
        const val MAX_RECONNECT_DELAY = 30000L
    }

    fun connect() {
        scope.launch {
            attemptConnection()
        }
    }

    private suspend fun attemptConnection() {
        while (true) {
            try {
                Log.d(TAG, "Connecting to Bridge on $HOST:$PORT")
                socket = Socket(HOST, PORT)
                writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket!!.outputStream)), true)
                reader = BufferedReader(InputStreamReader(socket!!.inputStream))
                isConnected.set(true)
                reconnectDelay = 1000L

                Log.d(TAG, "Bridge connected")
                deliverQueuedMessages()
                startHeartbeat()
                listenForMessages()

            } catch (e: ConnectException) {
                Log.w(TAG, "Bridge not available. Retrying in ${reconnectDelay}ms")
                isConnected.set(false)
                delay(reconnectDelay)
                reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
            } catch (e: Exception) {
                Log.e(TAG, "Bridge error: ${e.message}")
                isConnected.set(false)
                delay(reconnectDelay)
            }
        }
    }

    private fun startHeartbeat() {
        scope.launch {
            while (isConnected.get()) {
                send(JSONObject().apply {
                    put("type", "status")
                    put("sender", "GHOST")
                    put("state", "online")
                    put("timestamp", System.currentTimeMillis())
                }.toString())
                delay(HEARTBEAT_INTERVAL)
            }
        }
    }

    private suspend fun listenForMessages() {
        try {
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                line?.let { processIncoming(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bridge connection lost: ${e.message}")
            isConnected.set(false)
        }
    }

    private fun processIncoming(raw: String) {
        try {
            val json = JSONObject(raw)
            when (json.optString("type")) {
                "sync" -> {
                    val name = json.optString("name")
                    val state = json.optString("state")
                    val category = json.optString("category")
                    Log.d(TAG, "Sync received: $category/$name → $state")
                    // Forward to GhostService via broadcast
                    val intent = android.content.Intent("com.ghost.BRIDGE_SYNC").apply {
                        putExtra("category", category)
                        putExtra("name", name)
                        putExtra("state", state)
                    }
                    context.sendBroadcast(intent)
                }
                "command" -> {
                    val action = json.optString("action")
                    val target = json.optString("target")
                    Log.d(TAG, "Command from GIPSY: $action → $target")
                    val intent = android.content.Intent("com.ghost.BRIDGE_COMMAND").apply {
                        putExtra("action", action)
                        putExtra("target", target)
                        putExtra("params", json.optJSONObject("params")?.toString() ?: "{}")
                    }
                    context.sendBroadcast(intent)
                }
                "status" -> {
                    Log.d(TAG, "Status: ${json.optString("sender")} → ${json.optString("state")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse incoming: $raw")
        }
    }

    fun syncProtocol(name: String, state: String) {
        val msg = JSONObject().apply {
            put("type", "sync")
            put("category", "protocol")
            put("name", name)
            put("state", state)
            put("source", "GHOST")
            put("timestamp", System.currentTimeMillis())
        }.toString()
        send(msg)
    }

    fun syncMode(name: String, state: String) {
        val msg = JSONObject().apply {
            put("type", "sync")
            put("category", "mode")
            put("name", name)
            put("state", state)
            put("source", "GHOST")
            put("timestamp", System.currentTimeMillis())
        }.toString()
        send(msg)
    }

    fun sendResponse(response: GhostResponse) {
        val msg = JSONObject().apply {
            put("type", "data")
            put("category", "response")
            put("payload", JSONObject().apply {
                put("speech", response.speech)
                put("action_type", response.action.type)
                put("confidence", response.confidence)
            })
            put("source", "GHOST")
            put("timestamp", System.currentTimeMillis())
        }.toString()
        send(msg)
    }

    fun sendIrongateResult(result: String) {
        val msg = JSONObject().apply {
            put("type", "data")
            put("category", "irongate")
            put("payload", JSONObject().apply {
                put("result", result)
                put("timestamp", System.currentTimeMillis())
            })
            put("source", "GHOST")
        }.toString()
        send(msg)

        // Post notification
        postIrongateNotification(result)
    }

    fun sendFactoryReset(target: String) {
        val msg = JSONObject().apply {
            put("type", "command")
            put("action", "factory_reset")
            put("target", target)
            put("source", "GHOST")
            put("timestamp", System.currentTimeMillis())
        }.toString()
        send(msg)
    }

    private fun postIrongateNotification(result: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        val notif = androidx.core.app.NotificationCompat.Builder(context, "IRONGATE")
            .setContentTitle("IRONGATE COMPLETE")
            .setContentText(result)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(result == "ALL CLEAR")
            .build()
        nm.notify(999, notif)
    }

    private fun send(message: String) {
        if (!isConnected.get()) {
            messageQueue.add(message)
            return
        }
        scope.launch {
            try {
                writer?.println(message)
            } catch (e: Exception) {
                Log.e(TAG, "Send failed: ${e.message}")
                messageQueue.add(message)
                isConnected.set(false)
            }
        }
    }

    private fun deliverQueuedMessages() {
        while (messageQueue.isNotEmpty()) {
            val msg = messageQueue.poll() ?: break
            writer?.println(msg)
        }
    }

    fun isConnected() = isConnected.get()

    fun disconnect() {
        scope.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {}
        isConnected.set(false)
    }
}
