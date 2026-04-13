package com.irongate.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irongate.model.*
import com.irongate.protocol.ProtocolState
import com.irongate.service.BridgeService
import com.irongate.utils.Prefs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private var bridgeService: BridgeService? = null
    private var bound = false

    // Expose service flows directly to UI
    val connectionStatus: StateFlow<ConnectionStatus> get() =
        bridgeService?.router?.status ?: MutableStateFlow(ConnectionStatus())

    private val _protocolState = MutableStateFlow(ProtocolState())
    val protocolState: StateFlow<ProtocolState> = _protocolState

    private val _feed = MutableStateFlow<List<FeedEntry>>(emptyList())
    val feed: StateFlow<List<FeedEntry>> = _feed

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(from = "GHOST", text = "GHOST online. Bridge connection stable. Standing by, Boss."),
            ChatMessage(from = "GIPSY", text = "All systems nominal, Cooper. IRONGATE running. You're clear.")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _callsign = MutableStateFlow(CallsignConfig())
    val callsign: StateFlow<CallsignConfig> = _callsign

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BridgeService.BridgeBinder
            bridgeService = binder.getService()
            bound = true

            // Forward service flows
            viewModelScope.launch {
                bridgeService!!.router.feed.collect { _feed.value = it }
            }
            viewModelScope.launch {
                bridgeService!!.protocol.state.collect { _protocolState.value = it }
            }

            // Register chat response handler
            bridgeService!!.router.onChatResponse = { msg ->
                val current = _chatMessages.value.toMutableList()
                current.add(msg)
                _chatMessages.value = current
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            bridgeService = null
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, BridgeService::class.java).apply {
            action = BridgeService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Load saved callsign
        _callsign.value = Prefs.getCallsign(context)
    }

    fun unbindService(context: Context) {
        if (bound) {
            context.unbindService(connection)
            bound = false
        }
    }

    fun commenceProtocol() {
        viewModelScope.launch {
            bridgeService?.protocol?.run()
        }
    }

    fun resetProtocol() {
        bridgeService?.protocol?.reset()
    }

    fun sendChat(target: String, message: String) {
        val current = _chatMessages.value.toMutableList()
        current.add(ChatMessage(from = "USER", text = message))
        _chatMessages.value = current
        bridgeService?.router?.sendUserMessage(target, message)
    }

    fun saveCallsign(context: Context, target: String, name: String, wake: String) {
        val current = _callsign.value
        val updated = when (target) {
            "GHOST" -> current.copy(ghostName = name, ghostWake = wake)
            "GIPSY" -> current.copy(gipsyName = name, gipsyWake = wake)
            "IRONGATE" -> current.copy(irongateName = name, irongateWake = wake)
            else -> current
        }
        _callsign.value = updated
        Prefs.saveCallsign(context, updated)
        bridgeService?.router?.sendCallsign(target, name, wake)
    }

    fun saveApiKey(context: Context, key: String) {
        Prefs.saveApiKey(context, key)
    }

    fun executeNuke(target: String) {
        bridgeService?.router?.sendNukeCommand(target)
    }

    fun getApiKey(context: Context) = Prefs.getApiKey(context)
}
