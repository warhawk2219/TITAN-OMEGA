package com.ghost.core

import android.content.Context
import android.media.*
import org.vosk.Model
import org.vosk.Recognizer
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class WakeWordEngine(
    private val context: Context,
    private val onWakeWord: () -> Unit
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private val isListening = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    // Wake word — dynamically loaded from CALLSIGN settings
    private var wakeWord = "ghost"

    companion object {
        const val SAMPLE_RATE = 16000
        const val BUFFER_SIZE = 4096
        const val MODEL_PATH = "vosk-model-small-en"
    }

    init {
        loadWakeWord()
        loadModel()
    }

    private fun loadWakeWord() {
        val prefs = context.getSharedPreferences("ghost_callsign", Context.MODE_PRIVATE)
        wakeWord = prefs.getString("ghost_wake_word", "ghost")?.lowercase() ?: "ghost"
    }

    fun updateWakeWord(newWord: String) {
        wakeWord = newWord.lowercase()
        // Rebuild recognizer with new grammar
        recognizer?.close()
        model?.let {
            recognizer = Recognizer(it, SAMPLE_RATE.toFloat(),
                "[\"${wakeWord}\", \"[unk]\"]")
        }
    }

    private fun loadModel() {
        thread {
            try {
                val modelDir = File(context.filesDir, MODEL_PATH)
                if (!modelDir.exists()) copyModelFromAssets(modelDir)
                model = Model(modelDir.absolutePath)
                // Grammar-limited — only listens for wake word
                recognizer = Recognizer(model, SAMPLE_RATE.toFloat(),
                    "[\"${wakeWord}\", \"[unk]\"]")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun copyModelFromAssets(destDir: File) {
        destDir.mkdirs()
        context.assets.list(MODEL_PATH)?.forEach { fileName ->
            val inputStream = context.assets.open("$MODEL_PATH/$fileName")
            val outputFile = File(destDir, fileName)
            outputFile.outputStream().use { inputStream.copyTo(it) }
        }
    }

    fun start() {
        if (isListening.get()) return
        isListening.set(true)

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(BUFFER_SIZE)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()

        thread {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isListening.get()) {
                if (isPaused.get()) {
                    Thread.sleep(50)
                    continue
                }
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    recognizer?.let { rec ->
                        if (rec.acceptWaveForm(buffer, bytesRead)) {
                            val result = JSONObject(rec.result)
                            val text = result.optString("text", "")
                            if (text.contains(wakeWord, ignoreCase = true)) {
                                onWakeWord()
                            }
                        }
                    }
                }
            }
        }
    }

    fun pause() { isPaused.set(true) }
    fun resume() { isPaused.set(false) }

    fun stop() {
        isListening.set(false)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recognizer?.close()
        model?.close()
    }
}
