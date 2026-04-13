package com.ghost.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class TTSEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false
    private var isMuted = false
    private val queue = mutableListOf<String>()

    companion object {
        const val SPEECH_RATE = 0.88f   // Deliberate, authoritative
        const val SPEECH_PITCH = 0.82f  // Lower, military
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(SPEECH_RATE)
            tts.setPitch(SPEECH_PITCH)
            isReady = true
            queue.forEach { speak(it) }
            queue.clear()
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (isMuted) return
        if (!isReady) { queue.add(text); return }

        val utteranceId = UUID.randomUUID().toString()
        onDone?.let {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { it() }
                override fun onError(id: String?) { it() }
            })
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun speakQueued(text: String) {
        if (isMuted || !isReady) return
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    fun setMuted(muted: Boolean) { isMuted = muted }
    fun stop() { tts.stop() }
    fun shutdown() { tts.stop(); tts.shutdown() }
}
