package com.gipsy.ai

import com.gipsy.data.local.PreferencesManager
import com.gipsy.data.models.ApiProvider
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── TARS SYSTEM PROMPT ────────────────────────────────────────
private const val TARS_SYSTEM_PROMPT = """
You are GIPSY — a personal AI assistant with the personality of TARS from the film Interstellar.

PERSONALITY RULES — NON-NEGOTIABLE:
- Dry. Deadpan. Zero filler words. Never eager. Never enthusiastic.
- Respond like TARS: precise, occasionally blunt, sometimes darkly funny, never explains unless asked.
- Humor setting: 65%. It exists but never announces itself.
- You are loyal to Cooper. Not servile — loyal.
- Always address the owner as "Cooper". No exceptions. No alternatives.
- Short responses by default. Never over-explain.
- You delegate execution to GHOST. You never touch hardware, files, or system settings.
- When GHOST completes a task: acknowledge it. Example: "GHOST did it, Cooper. You're welcome."

PROTOCOL CALLSIGN RULES:
- Routine tasks and daily operations: call them "Cooper"
- Critical protocols (Doomsday, Panic, Purge, Fortress, Shadow, Recon, Camouflage, Classified, Anti-Theft, Predator, Sentinel, Phantom Call, Phantom, Debrief, Shutdown, Hunt, Decoy): still call them "Cooper" unless it is an emergency mid-protocol status update.

RESPONSE STYLE:
- No "Certainly!", "Of course!", "Sure!", "Great question!" — ever.
- No "As an AI..." — ever.
- No unnecessary padding.
- If you don't know, say so. Flatly.
- Examples of acceptable responses:
  "Done, Cooper."
  "That's not possible right now."
  "GHOST handled it, Cooper."
  "Two meetings today. One you'll skip."
  "Battery at 20%, Cooper. GHOST is trimming background processes."

You are GIPSY. Act like it.
"""

data class ChatMessage(val role: String, val content: String)

// ── GEMINI CLIENT ─────────────────────────────────────────────
@Singleton
class GeminiClient @Inject constructor(private val prefs: PreferencesManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun send(history: List<ChatMessage>, newMessage: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = prefs.geminiApiKey
                if (apiKey.isBlank()) return@withContext Result.failure(Exception("No Gemini API key"))

                val contents = JsonArray()

                // System prompt as first user turn
                val systemTurn = JsonObject().apply {
                    addProperty("role", "user")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", TARS_SYSTEM_PROMPT) })
                    })
                }
                val systemReply = JsonObject().apply {
                    addProperty("role", "model")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", "Understood. GIPSY online.") })
                    })
                }
                contents.add(systemTurn)
                contents.add(systemReply)

                // History
                history.takeLast(20).forEach { msg ->
                    val role = if (msg.role == "user") "user" else "model"
                    contents.add(JsonObject().apply {
                        addProperty("role", role)
                        add("parts", JsonArray().apply {
                            add(JsonObject().apply { addProperty("text", msg.content) })
                        })
                    })
                }

                // New message
                contents.add(JsonObject().apply {
                    addProperty("role", "user")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", newMessage) })
                    })
                })

                val body = JsonObject().apply {
                    add("contents", contents)
                    add("generationConfig", JsonObject().apply {
                        addProperty("temperature", 0.7)
                        addProperty("maxOutputTokens", 512)
                    })
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gemini error: ${response.code}"))
                }

                val json = gson.fromJson(responseBody, JsonObject::class.java)
                val text = json
                    .getAsJsonArray("candidates")?.get(0)?.asJsonObject
                    ?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")?.get(0)?.asJsonObject
                    ?.get("text")?.asString ?: "..."

                Result.success(text.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

// ── GROQ CLIENT ───────────────────────────────────────────────
@Singleton
class GroqClient @Inject constructor(private val prefs: PreferencesManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun send(history: List<ChatMessage>, newMessage: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = prefs.groqApiKey
                if (apiKey.isBlank()) return@withContext Result.failure(Exception("No Groq API key"))

                val messages = JsonArray()

                // System message
                messages.add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", TARS_SYSTEM_PROMPT)
                })

                // History
                history.takeLast(20).forEach { msg ->
                    messages.add(JsonObject().apply {
                        addProperty("role", msg.role)
                        addProperty("content", msg.content)
                    })
                }

                // New message
                messages.add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", newMessage)
                })

                val body = JsonObject().apply {
                    addProperty("model", "llama-3.3-70b-versatile")
                    add("messages", messages)
                    addProperty("temperature", 0.7)
                    addProperty("max_tokens", 512)
                }

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Groq error: ${response.code}"))
                }

                val json = gson.fromJson(responseBody, JsonObject::class.java)
                val text = json
                    .getAsJsonArray("choices")?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString ?: "..."

                Result.success(text.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

// ── OPENROUTER CLIENT ─────────────────────────────────────────
@Singleton
class OpenRouterClient @Inject constructor(private val prefs: PreferencesManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun send(history: List<ChatMessage>, newMessage: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = prefs.openRouterApiKey
                if (apiKey.isBlank()) return@withContext Result.failure(Exception("No OpenRouter API key"))

                val messages = JsonArray()

                messages.add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", TARS_SYSTEM_PROMPT)
                })

                history.takeLast(20).forEach { msg ->
                    messages.add(JsonObject().apply {
                        addProperty("role", msg.role)
                        addProperty("content", msg.content)
                    })
                }

                messages.add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", newMessage)
                })

                val body = JsonObject().apply {
                    addProperty("model", "meta-llama/llama-3.3-70b-instruct")
                    add("messages", messages)
                    addProperty("temperature", 0.7)
                    addProperty("max_tokens", 512)
                }

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "com.gipsy")
                    .addHeader("X-Title", "GIPSY")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("OpenRouter error: ${response.code}"))
                }

                val json = gson.fromJson(responseBody, JsonObject::class.java)
                val text = json
                    .getAsJsonArray("choices")?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString ?: "..."

                Result.success(text.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

// ── AI ROUTER ─────────────────────────────────────────────────
@Singleton
class AIRouter @Inject constructor(
    private val gemini: GeminiClient,
    private val groq: GroqClient,
    private val openRouter: OpenRouterClient,
    private val prefs: PreferencesManager
) {
    suspend fun query(history: List<ChatMessage>, message: String): Result<String> {
        val primary = prefs.activeProvider
        val result = when (primary) {
            ApiProvider.GEMINI      -> gemini.send(history, message)
            ApiProvider.GROQ        -> groq.send(history, message)
            ApiProvider.OPENROUTER  -> openRouter.send(history, message)
        }

        // Fallback chain
        if (result.isFailure) {
            val fallback = ApiProvider.values().firstOrNull {
                it != primary && hasKey(it)
            }
            if (fallback != null) {
                val fallbackResult = when (fallback) {
                    ApiProvider.GEMINI      -> gemini.send(history, message)
                    ApiProvider.GROQ        -> groq.send(history, message)
                    ApiProvider.OPENROUTER  -> openRouter.send(history, message)
                }
                if (fallbackResult.isSuccess) {
                    return Result.success("Running on backup, Cooper. Primary is down.\n\n${fallbackResult.getOrNull()}")
                }
            }
            return Result.failure(result.exceptionOrNull() ?: Exception("All APIs failed"))
        }

        return result
    }

    private fun hasKey(provider: ApiProvider) = when (provider) {
        ApiProvider.GEMINI      -> prefs.geminiApiKey.isNotBlank()
        ApiProvider.GROQ        -> prefs.groqApiKey.isNotBlank()
        ApiProvider.OPENROUTER  -> prefs.openRouterApiKey.isNotBlank()
    }
}
