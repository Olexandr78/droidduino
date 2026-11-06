package com.example.data.ai

import android.util.Log
import com.example.data.model.Board
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiStudioRepo"

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val extractedCode: String? = null
)

class GeminiStudioRepository {

    // Lazy initialization of Gemini model to prevent early class loading issues
    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    /**
     * Streams responses for Studio Bot interactive chat
     */
    fun streamChat(
        userMessage: String,
        currentSketch: String,
        board: Board,
        recentHistory: List<ChatMessage> = emptyList()
    ): Flow<String> = flow {
        val promptBuilder = StringBuilder()
        promptBuilder.append("""
            You are Gemini in DroidDuino Studio (like Studio Bot in Android Studio), a senior embedded systems and Arduino/ESP32 firmware engineer.
            The user is working on an Arduino C/C++ sketch for target board: ${board.name} (MCU: ${board.mcu}, Voltage: ${board.operatingVoltage}, Flash: ${board.flashBytes / 1024}KB, SRAM: ${board.sramBytes / 1024}KB).
            
            Current Sketch in Editor:
            ```cpp
            $currentSketch
            ```
            
            User Question / Request:
            $userMessage
            
            Instructions:
            1. Be concise, direct, and technically accurate.
            2. When providing or modifying Arduino C/C++ code, wrap it in ```cpp ... ``` code blocks.
            3. If explaining pin wiring, give exact board pin names and voltage warnings (e.g. 5V vs 3.3V logic level shifting).
            4. If providing a fix, explain the root cause and provide the clean updated snippet.
        """.trimIndent())

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                generativeModel.generateContentStream(promptBuilder.toString()).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini stream timed out after 60s", e)
            throw IllegalStateException("Studio Bot response timed out. Please check connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini stream failed", e)
            throw e
        }
    }

    /**
     * Fixes compiler errors with smart 1-click patch suggestion
     */
    fun fixCompilerErrors(
        code: String,
        compilerLog: String,
        board: Board
    ): Flow<String> = flow {
        val prompt = """
            You are Studio Bot in DroidDuino IDE. The user's Arduino C/C++ sketch failed compilation for board ${board.name}.
            
            Compiler Output:
            $compilerLog
            
            Broken Sketch Code:
            ```cpp
            $code
            ```
            
            Please:
            1. Clearly explain each error in 1-2 bullet points.
            2. Provide the COMPLETE, corrected, ready-to-run sketch wrapped in ```cpp ... ``` so the user can apply it with 1 tap.
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    accumulated.append(chunk.text.orEmpty())
                    emit(accumulated.toString())
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Fix errors timed out", e)
            throw IllegalStateException("Fix diagnostic timed out.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fix errors failed", e)
            throw e
        }
    }

    /**
     * Explains the current sketch architecture and functions
     */
    fun explainSketch(
        code: String,
        board: Board
    ): Flow<String> = flow {
        val prompt = """
            You are Studio Bot in DroidDuino IDE. Explain this Arduino sketch for board ${board.name}:
            ```cpp
            $code
            ```
            
            Breakdown:
            - **Overview & Objective** (what this sketch accomplishes)
            - **Pin Configuration & Hardware Setup**
            - **Main Loop Architecture** (timing, state management, interrupts)
            - **Key Variables & Libraries**
            - **Potential Improvements or Gotchas** (e.g. blocking delay vs millis)
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    accumulated.append(chunk.text.orEmpty())
                    emit(accumulated.toString())
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Code explanation timed out.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Explain sketch failed", e)
            throw e
        }
    }

    /**
     * Generates a wiring guide and schematic notes for the current sketch
     */
    fun generateWiringGuide(
        code: String,
        board: Board
    ): Flow<String> = flow {
        val prompt = """
            You are Studio Bot in DroidDuino IDE. Based on this Arduino C/C++ sketch and the target board ${board.name}:
            ```cpp
            $code
            ```
            
            Provide a complete, beginner-friendly Pinout & Wiring Connection Table:
            - Device / Sensor Pin -> Board Pin
            - Operating Voltage (VCC, 3.3V or 5V)
            - Pull-up resistors or level shifters required (if any)
            - Breadboard & Power recommendations
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    accumulated.append(chunk.text.orEmpty())
                    emit(accumulated.toString())
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Wiring guide generation timed out.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Generate wiring failed", e)
            throw e
        }
    }

    /**
     * Analyzes serial monitor output stream for crashes, telemetry anomalies, or sensor drift
     */
    fun analyzeSerialLog(
        serialLog: String,
        currentSketch: String,
        board: Board
    ): Flow<String> = flow {
        val prompt = """
            You are Studio Bot in DroidDuino IDE. Analyze this serial monitor output from board ${board.name}:
            
            Serial Output:
            ```text
            $serialLog
            ```
            
            Current Sketch:
            ```cpp
            $currentSketch
            ```
            
            Diagnose:
            - Telemetry health and data trends
            - Any crash reports (e.g. ESP32 Guru Meditation Error, stack trace, watchdog timeout)
            - Baud rate or framing issues (e.g. garbage characters)
            - Suggestions to fix or improve serial output
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    accumulated.append(chunk.text.orEmpty())
                    emit(accumulated.toString())
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Serial log analysis timed out.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Analyze serial log failed", e)
            throw e
        }
    }
}
