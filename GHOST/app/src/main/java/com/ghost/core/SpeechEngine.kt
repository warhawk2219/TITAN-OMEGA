package com.ghost.core

import android.content.Context
import android.media.*
import org.vosk.Model
import org.vosk.Recognizer
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class SpeechEngine(
    private val context: Context,
    private val onResult: (String) -> Unit
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private val isListening = AtomicBoolean(false)

    companion object {
        const val SAMPLE_RATE = 16000
        const val BUFFER_SIZE = 4096
        const val MODEL_PATH = "vosk-model-small-en"
        const val SILENCE_THRESHOLD = 300
        const val SILENCE_TIMEOUT_MS = 2000L
    }

    init { loadModel() }

    private fun loadModel() {
        thread {
            try {
                val modelDir = File(context.filesDir, MODEL_PATH)
                if (!modelDir.exists()) {
                    modelDir.mkdirs()
                    context.assets.list(MODEL_PATH)?.forEach { fileName ->
                        val inputStream = context.assets.open("$MODEL_PATH/$fileName")
                        File(modelDir, fileName).outputStream().use { inputStream.copyTo(it) }
                    }
                }
                model = Model(modelDir.absolutePath)
                recognizer = Recognizer(model, SAMPLE_RATE.toFloat())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startListening() {
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
            var lastSoundTime = System.currentTimeMillis()
            val transcript = StringBuilder()

            while (isListening.get()) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    val energy = buffer.take(bytesRead)
                        .sumOf { b -> (b.toInt() * b.toInt()).toLong() } / bytesRead
                    if (energy > SILENCE_THRESHOLD) lastSoundTime = System.currentTimeMillis()

                    recognizer?.let { rec ->
                        if (rec.acceptWaveForm(buffer, bytesRead)) {
                            val text = JSONObject(rec.result).optString("text", "")
                            if (text.isNotBlank()) transcript.append(" $text")
                        }
                    }

                    if (System.currentTimeMillis() - lastSoundTime > SILENCE_TIMEOUT_MS) {
                        val finalText = JSONObject(recognizer?.finalResult ?: "{}").optString("text", "")
                        if (finalText.isNotBlank()) transcript.append(" $finalText")
                        val result = transcript.toString().trim()
                        stopListening()
                        onResult(result)
                        return@thread
                    }
                }
            }
        }
    }

    fun stopListening() {
        isListening.set(false)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun stop() {
        stopListening()
        recognizer?.close()
        model?.close()
    }
}
