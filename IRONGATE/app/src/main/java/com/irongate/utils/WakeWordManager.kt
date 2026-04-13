package com.irongate.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages wake word state. If wake word is detected and no command
 * follows within 5 seconds, auto-dismisses silently.
 */
class WakeWordManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timeoutJob: Job? = null

    private val _awake = MutableStateFlow(false)
    val awake: StateFlow<Boolean> = _awake

    fun onWakeWordDetected() {
        _awake.value = true
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(5_000L)
            // No command received in 5 seconds — stand down silently
            _awake.value = false
        }
    }

    fun onCommandReceived() {
        timeoutJob?.cancel()
        _awake.value = false
    }

    fun dismiss() {
        timeoutJob?.cancel()
        _awake.value = false
    }

    fun destroy() {
        scope.cancel()
    }
}
