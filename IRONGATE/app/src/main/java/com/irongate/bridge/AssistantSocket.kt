package com.irongate.bridge

import android.util.Log
import com.irongate.model.AssistantSource
import com.irongate.model.AssistantState
import com.irongate.model.BridgeMessage
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * Manages one side of the bridge — either GHOST (port 8765) or GIPSY (port 8766).
 * Runs a ServerSocket, accepts a single client connection, heartbeats every 10s,
 * marks offline after 30s no-response, buffers up to 100 outgoing messages.
 */
class AssistantSocket(
    private val port: Int,
    private val source: AssistantSource,
    private val onStateChange: (AssistantState) -> Unit,
    private val onMessage: (BridgeMessage) -> Unit,
    private val onLatency: (Long) -> Unit
) {
    companion object {
        private const val TAG = "AssistantSocket"
        private const val HEARTBEAT_INTERVAL_MS = 10_000L
        private const val TIMEOUT_MS = 30_000L
        private const val BUFFER_MAX = 100
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var lastPongMs = 0L
    private val messageBuffer = ArrayDeque<BridgeMessage>()
    var state: AssistantState = AssistantState.OFFLINE
        private set

    fun start() {
        scope.launch { runServer() }
    }

    fun stop() {
        scope.cancel()
        clientSocket?.close()
        serverSocket?.close()
    }

    private suspend fun runServer() {
        while (isActive) {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "$source listening on port $port")
                val client = serverSocket!!.accept()
                handleClient(client)
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "$source server error: ${e.message}")
                    updateState(AssistantState.RECONNECTING)
                    delay(2000)
                }
            }
        }
    }

    private suspend fun handleClient(client: Socket) {
        clientSocket = client
        writer = PrintWriter(BufferedWriter(OutputStreamWriter(client.getOutputStream())), true)
        val reader = BufferedReader(InputStreamReader(client.getInputStream()))

        updateState(AssistantState.ONLINE)
        lastPongMs = System.currentTimeMillis()

        // Deliver buffered messages
        deliverBuffer()

        // Send online status
        sendRaw(BridgeMessage.statusOnline(AssistantSource.BRIDGE))

        val heartbeatJob = scope.launch { heartbeatLoop() }

        try {
            var line: String?
            while (client.isConnected) {
                line = withTimeoutOrNull(1000) {
                    withContext(Dispatchers.IO) { reader.readLine() }
                }
                if (line != null) {
                    lastPongMs = System.currentTimeMillis()
                    val msg = BridgeMessage.fromJson(line)
                    if (msg != null) onMessage(msg)
                }

                // Check timeout
                if (System.currentTimeMillis() - lastPongMs > TIMEOUT_MS) {
                    Log.w(TAG, "$source timeout — marking offline")
                    break
                }
            }
        } catch (e: SocketException) {
            Log.d(TAG, "$source disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "$source read error: ${e.message}")
        } finally {
            heartbeatJob.cancel()
            client.close()
            updateState(AssistantState.OFFLINE)
        }
    }

    private suspend fun heartbeatLoop() {
        while (isActive) {
            delay(HEARTBEAT_INTERVAL_MS)
            val pingStart = System.currentTimeMillis()
            sendRaw(BridgeMessage.ping(AssistantSource.BRIDGE))
            // Latency = time since last pong
            onLatency(System.currentTimeMillis() - lastPongMs)
        }
    }

    fun send(message: BridgeMessage) {
        if (state == AssistantState.ONLINE) {
            val sent = sendRaw(message)
            if (!sent) bufferMessage(message)
        } else {
            bufferMessage(message)
        }
    }

    private fun sendRaw(message: BridgeMessage): Boolean = try {
        writer?.println(message.toJson())
        writer != null
    } catch (e: Exception) {
        Log.e(TAG, "Send error to $source: ${e.message}")
        false
    }

    private fun bufferMessage(message: BridgeMessage) {
        if (messageBuffer.size >= BUFFER_MAX) messageBuffer.removeFirst()
        messageBuffer.addLast(message)
        Log.d(TAG, "Buffered message for $source — queue: ${messageBuffer.size}")
    }

    private fun deliverBuffer() {
        while (messageBuffer.isNotEmpty()) {
            val msg = messageBuffer.removeFirst()
            sendRaw(msg)
        }
    }

    private fun updateState(newState: AssistantState) {
        state = newState
        onStateChange(newState)
    }
}
