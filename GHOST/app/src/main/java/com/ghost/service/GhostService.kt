package com.ghost.service

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ghost.R
import com.ghost.bridge.BridgeHandler
import com.ghost.core.*
import com.ghost.overlay.OverlayManager
import com.ghost.protocols.ProtocolEngine
import kotlinx.coroutines.*

class GhostService : Service() {

    private lateinit var wakeWordEngine: WakeWordEngine
    private lateinit var speechEngine: SpeechEngine
    private lateinit var llmEngine: LLMEngine
    private lateinit var ttsEngine: TTSEngine
    private lateinit var actionDispatcher: ActionDispatcher
    private lateinit var protocolEngine: ProtocolEngine
    private lateinit var overlayManager: OverlayManager
    private lateinit var bridgeHandler: BridgeHandler

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeWordTimeout: Job? = null

    companion object {
        const val CHANNEL_ID = "GHOST_SERVICE"
        const val NOTIF_ID = 1
        var isRunning = false
        const val WAKE_TIMEOUT_MS = 5000L
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("STANDBY — LISTENING"))

        overlayManager = OverlayManager(this)
        ttsEngine = TTSEngine(this)
        bridgeHandler = BridgeHandler(this)
        protocolEngine = ProtocolEngine(this)
        actionDispatcher = ActionDispatcher(this, protocolEngine, overlayManager, ttsEngine)

        llmEngine = LLMEngine(this) { response ->
            onLLMResponse(response)
        }

        speechEngine = SpeechEngine(this) { transcript ->
            onSpeechResult(transcript)
        }

        wakeWordEngine = WakeWordEngine(this) {
            onWakeWordDetected()
        }

        wakeWordEngine.start()
        bridgeHandler.connect()

        serviceScope.launch {
            delay(2000)
            ttsEngine.speak("GHOST online. Standing by, Boss.")
        }
    }

    private fun onWakeWordDetected() {
        updateNotification("LISTENING — SPEAK NOW")
        overlayManager.showLogoOverlay(OverlayManager.LogoState.LISTENING)
        wakeWordEngine.pause()
        speechEngine.startListening()

        // 5 second timeout — if owner says nothing
        wakeWordTimeout?.cancel()
        wakeWordTimeout = serviceScope.launch {
            delay(WAKE_TIMEOUT_MS)
            onWakeTimeout()
        }
    }

    private fun onWakeTimeout() {
        ttsEngine.speak("Standing by, Sir.")
        overlayManager.hideLogoOverlay(delay = 500)
        updateNotification("STANDBY — LISTENING")
        speechEngine.stopListening()
        wakeWordEngine.resume()
    }

    private fun onSpeechResult(transcript: String) {
        wakeWordTimeout?.cancel()

        if (transcript.isBlank()) {
            ttsEngine.speak("Standing by, Sir.")
            overlayManager.hideLogoOverlay(500)
            wakeWordEngine.resume()
            return
        }

        updateNotification("PROCESSING")
        overlayManager.showLogoOverlay(OverlayManager.LogoState.PROCESSING)
        llmEngine.process(transcript)
    }

    private fun onLLMResponse(response: GhostResponse) {
        overlayManager.showLogoOverlay(OverlayManager.LogoState.RESPONDING)
        updateNotification("STANDBY — LISTENING")

        // Check if silent mode — text only via overlay
        val currentMode = protocolEngine.getCurrentMode()
        if (currentMode == "silent") {
            overlayManager.showSilentMessage(response.speech)
        } else {
            ttsEngine.speak(response.speech)
        }

        actionDispatcher.dispatch(response.action)

        // Sync to GIPSY via Bridge
        bridgeHandler.sendResponse(response)

        serviceScope.launch {
            delay(3000)
            overlayManager.hideLogoOverlay()
            wakeWordEngine.resume()
        }
    }

    // Called from chat UI when user types
    fun processTextCommand(text: String) {
        updateNotification("PROCESSING")
        llmEngine.process(text)
    }

    // Factory reset with WARHAWK audio
    fun executeFactoryReset(target: String) {
        val mediaPlayer = MediaPlayer.create(this, R.raw.warhawk_audio)
        mediaPlayer.setOnCompletionListener {
            it.release()
            performReset(target)
        }
        mediaPlayer.start()
    }

    private fun performReset(target: String) {
        when (target) {
            "GHOST" -> {
                // Clear all GHOST data
                getSharedPreferences("ghost_prefs", MODE_PRIVATE).edit().clear().apply()
                getDatabasePath("ghost.db").delete()
            }
            "GIPSY" -> {
                bridgeHandler.sendFactoryReset("GIPSY")
            }
            "BOTH" -> {
                getSharedPreferences("ghost_prefs", MODE_PRIVATE).edit().clear().apply()
                getDatabasePath("ghost.db").delete()
                bridgeHandler.sendFactoryReset("GIPSY")
            }
        }
        ttsEngine.speak("GHOST online. Ready for initialization, Sir.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GHOST",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GHOST AI Assistant"
                setShowBadge(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GHOST")
            .setContentText(status)
            .setSmallIcon(R.drawable.ghost_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    fun updateNotification(status: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(status))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        wakeWordEngine.stop()
        speechEngine.stop()
        llmEngine.destroy()
        ttsEngine.shutdown()
        overlayManager.destroyAll()
        bridgeHandler.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
