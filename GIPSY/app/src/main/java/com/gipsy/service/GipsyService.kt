package com.gipsy.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.gipsy.R
import com.gipsy.bridge.BridgeManager
import com.gipsy.data.local.PreferencesManager
import com.gipsy.data.models.GipsyListenState
import com.gipsy.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class GipsyService : Service() {

    @Inject lateinit var prefs: PreferencesManager
    @Inject lateinit var bridgeManager: BridgeManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var listenTimeoutJob: Job? = null
    private var irongateJob: Job? = null

    val listenState = MutableStateFlow(GipsyListenState.IDLE)
    val pendingSpeak = MutableStateFlow<String?>(null)

    companion object {
        const val CHANNEL_ID = "gipsy_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SPEAK = "com.gipsy.SPEAK"
        const val EXTRA_TEXT = "text"
        var instance: GipsyService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        initTts()
        bridgeManager.connect()
        startIrongateScheduler()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SPEAK -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return START_STICKY
                if (prefs.ttsEnabled) speak(text)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── TTS ───────────────────────────────────────────────────
    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                // Deep, flat, slightly slow — TARS-like
                tts?.setSpeechRate(0.85f)
                tts?.setPitch(0.75f)
            }
        }
    }

    fun speak(text: String) {
        if (!prefs.ttsEnabled) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gipsy_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    // ── WAKE WORD DETECTION ───────────────────────────────────
    fun startListeningForWakeWord() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(WakeWordListener())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        speechRecognizer?.startListening(intent)
    }

    fun startListeningForCommand() {
        setListenState(GipsyListenState.LISTENING)

        // 5 second timeout
        listenTimeoutJob?.cancel()
        listenTimeoutJob = scope.launch {
            delay(5000)
            if (listenState.value == GipsyListenState.LISTENING) {
                setListenState(GipsyListenState.IDLE)
                updateFloatingLogo(GipsyListenState.IDLE)
                startListeningForWakeWord()
            }
        }
    }

    private fun setListenState(state: GipsyListenState) {
        listenState.value = state
        updateFloatingLogo(state)
    }

    private fun updateFloatingLogo(state: GipsyListenState) {
        val intent = Intent(this, FloatingLogoService::class.java).apply {
            action = if (state == GipsyListenState.IDLE)
                FloatingLogoService.ACTION_HIDE
            else
                FloatingLogoService.ACTION_STATE
            putExtra(FloatingLogoService.EXTRA_STATE, state.name)
        }
        startService(intent)
    }

    // ── IRONGATE SCHEDULER ────────────────────────────────────
    private fun startIrongateScheduler() {
        irongateJob = scope.launch {
            while (isActive) {
                delay(60 * 60 * 1000L) // every 1 hour
                bridgeManager.requestIrongate()
            }
        }
    }

    // ── WAKE WORD LISTENER ────────────────────────────────────
    inner class WakeWordListener : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val wakeWord = prefs.gipsyWakeWord.uppercase()
            val panicWord = prefs.panicWord.uppercase()

            val detected = matches?.any { result ->
                result.uppercase().contains(wakeWord)
            } ?: false

            val panicDetected = panicWord.isNotBlank() && matches?.any { result ->
                result.uppercase().trim() == panicWord
            } ?: false

            when {
                panicDetected -> {
                    // Silent immediate panic execution
                    bridgeManager.sendToGhost("protocol_execute",
                        com.google.gson.JsonObject().apply {
                            addProperty("protocol", "PANIC")
                            addProperty("silent", true)
                        }
                    )
                    startListeningForWakeWord()
                }
                detected -> {
                    setListenState(GipsyListenState.WAKE_WORD_DETECTED)
                    startListeningForCommand()
                }
                else -> startListeningForWakeWord()
            }
        }

        override fun onError(error: Int) {
            scope.launch {
                delay(1000)
                startListeningForWakeWord()
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // ── NOTIFICATION ──────────────────────────────────────────
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GIPSY")
            .setContentText("ONLINE")
            .setSmallIcon(R.drawable.ic_gipsy_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GIPSY Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        tts?.shutdown()
        speechRecognizer?.destroy()
        bridgeManager.disconnect()
        instance = null
    }
}
