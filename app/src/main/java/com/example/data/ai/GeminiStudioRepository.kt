package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.Board
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Direct Gemini Generative Language REST API client (Firebase AI is not used for model calls). */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val extractedCode: String? = null
)

class GeminiStudioRepository(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = "gemini-2.5-flash",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    private suspend fun generate(prompt: String): String = withTimeout(60_000L) {
        require(apiKey.isNotBlank() && !apiKey.contains("your_key", ignoreCase = true)) {
            "Gemini API key is missing. Set GEMINI_API_KEY in .env or local.properties and rebuild."
        }
        withContext(Dispatchers.IO) {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 4096)
                })
            }
            val request = Request.Builder()
                .url(endpoint)
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message")
                    }.getOrNull().orEmpty()
                    throw IllegalStateException("Gemini API returned HTTP ${response.code}: ${message.ifBlank { body.take(400) }}")
                }
                val root = JSONObject(body)
                root.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.let { parts ->
                        (0 until parts.length()).joinToString("") { parts.optJSONObject(it)?.optString("text").orEmpty() }
                    }?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException(root.optJSONObject("promptFeedback")?.optString("blockReason")
                        ?.let { "Gemini did not return text (reason: $it)." } ?: "Gemini returned an empty response.")
            }
        }
    }

    private fun response(prompt: String): Flow<String> = flow { emit(generate(prompt)) }

    fun streamChat(userMessage: String, currentSketch: String, board: Board, recentHistory: List<ChatMessage> = emptyList()): Flow<String> {
        val history = recentHistory.takeLast(8).joinToString("\n") { message ->
            "${if (message.isUser) "User" else "Assistant"}: ${message.text.take(3000)}"
        }
        return response("""You are DroidDuino Studio's embedded firmware assistant. Be concise and technically accurate. For code use ```cpp blocks. Mention voltage hazards.
Target: ${board.name}, MCU ${board.mcu}, logic voltage ${board.operatingVoltage}.
Recent conversation:
$history
Current sketch:
```cpp
$currentSketch
```
User request:
$userMessage""")
    }

    fun fixCompilerErrors(code: String, compilerLog: String, board: Board) = response("""Fix this Arduino sketch for ${board.name} (${board.mcu}). Explain the cause briefly and provide a complete corrected sketch in a ```cpp block.
Compiler output:
$compilerLog
Sketch:
```cpp
$code
```""")

    fun explainSketch(code: String, board: Board) = response("""Explain this Arduino sketch for ${board.name} (${board.mcu}): overview, pin wiring, control flow, and gotchas.
```cpp
$code
```""")

    fun generateWiringGuide(code: String, board: Board) = response("""Create a wiring guide for ${board.name} (${board.mcu}, ${board.operatingVoltage}) based on the sketch. Include a pin mapping table, UART/SPI/I2C pins where relevant, power, and logic-level warnings.
```cpp
$code
```""")

    fun analyzeSerialLog(serialLog: String, currentSketch: String, board: Board) = response("""Analyze actual serial output from ${board.name}. Identify errors, baud/framing issues, and telemetry trends. Do not invent readings.
Serial log:
```text
$serialLog
```
Sketch:
```cpp
$currentSketch
```""")
}
