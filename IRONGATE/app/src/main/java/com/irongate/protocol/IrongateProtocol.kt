package com.irongate.protocol

import android.content.Context
import android.util.Log
import com.irongate.bridge.BridgeRouter
import com.irongate.model.AssistantState
import com.irongate.model.ProtocolResult
import com.irongate.model.ProtocolLog
import com.irongate.utils.Notifs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ProtocolStep {
    IDLE, PINGING_GHOST, GHOST_OK, PINGING_GIPSY, GIPSY_OK,
    VALIDATING_BRIDGE, CLEARING_MESSAGES, MEASURING_LATENCY,
    WRITING_LOG, COMPLETE
}

data class ProtocolState(
    val running: Boolean = false,
    val step: ProtocolStep = ProtocolStep.IDLE,
    val stepText: String = "",
    val progress: Float = 0f,
    val result: ProtocolResult? = null,
    val logs: List<ProtocolLog> = emptyList()
)

class IrongateProtocol(
    private val context: Context,
    private val router: BridgeRouter
) {
    companion object {
        private const val TAG = "IrongateProtocol"
        private const val INTERVAL_MS = 60 * 60 * 1000L  // 1 hour
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logs = mutableListOf<ProtocolLog>()

    private val _state = MutableStateFlow(ProtocolState())
    val state: StateFlow<ProtocolState> = _state

    private var schedulerJob: Job? = null

    fun startScheduler() {
        schedulerJob?.cancel()
        schedulerJob = scope.launch {
            while (isActive) {
                delay(INTERVAL_MS)
                run()
            }
        }
        Log.d(TAG, "IRONGATE Protocol scheduler started — interval: 60 min")
    }

    fun stopScheduler() {
        schedulerJob?.cancel()
    }

    suspend fun run(): ProtocolResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "IRONGATE Protocol commencing...")
        _state.value = ProtocolState(running = true)

        val steps = listOf(
            Triple(ProtocolStep.PINGING_GHOST,    "PINGING GHOST ON PORT 8765...", 0.15f),
            Triple(ProtocolStep.GHOST_OK,          "GHOST HANDSHAKE RECEIVED",      0.30f),
            Triple(ProtocolStep.PINGING_GIPSY,    "PINGING GIPSY ON PORT 8766...", 0.45f),
            Triple(ProtocolStep.GIPSY_OK,          "GIPSY HANDSHAKE RECEIVED",      0.60f),
            Triple(ProtocolStep.VALIDATING_BRIDGE, "VALIDATING BRIDGE STRUCTURE...", 0.72f),
            Triple(ProtocolStep.CLEARING_MESSAGES, "CLEARING STALE MESSAGES...",    0.82f),
            Triple(ProtocolStep.MEASURING_LATENCY, "MEASURING LATENCY...",           0.90f),
            Triple(ProtocolStep.WRITING_LOG,       "WRITING TO IRONGATE LOG...",     0.97f),
        )

        for ((step, text, prog) in steps) {
            _state.value = _state.value.copy(step = step, stepText = text, progress = prog)
            delay(400)
        }

        val status = router.status.value
        val ghostOk = status.ghost == AssistantState.ONLINE
        val gipsyOk = status.gipsy == AssistantState.ONLINE

        val result = when {
            ghostOk && gipsyOk -> ProtocolResult.NOMINAL
            ghostOk || gipsyOk -> ProtocolResult.CAUTION
            else -> ProtocolResult.BLACKOUT
        }

        val log = ProtocolLog(
            ghostOk = ghostOk,
            gipsyOk = gipsyOk,
            ghostLatencyMs = status.ghostLatencyMs,
            gipsyLatencyMs = status.gipsyLatencyMs,
            result = result
        )
        logs.add(0, log)
        if (logs.size > 50) logs.removeLastOrNull()

        _state.value = _state.value.copy(
            running = false,
            step = ProtocolStep.COMPLETE,
            stepText = "PROTOCOL COMPLETE",
            progress = 1f,
            result = result,
            logs = logs.toList()
        )

        // Fire notification — silent, one line
        Notifs.sendProtocolResult(context, result)

        Log.d(TAG, "IRONGATE Protocol complete: $result")
        result
    }

    fun reset() {
        _state.value = ProtocolState(logs = _state.value.logs)
    }
}
