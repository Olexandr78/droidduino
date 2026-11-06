package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ChatMessage
import com.example.data.ai.GeminiStudioRepository
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthState
import com.example.data.compiler.SketchCompiler
import com.example.data.firestore.ProjectRepository
import com.example.data.model.Board
import com.example.data.model.CompilationResult
import com.example.data.model.SampleSketches
import com.example.data.model.Sketch
import com.example.data.model.SupportedBoards
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Stack

private const val TAG = "MainViewModel"

enum class ActiveTab {
    EDITOR,
    SERIAL_MONITOR,
    PINOUT_VIEW
}

enum class TelemetryType(val title: String) {
    HC_SR04("Ultrasonic Distance"),
    GAZ24_RPM("GAZ-24 Engine RPM"),
    ESP32_WIFI("ESP32 HTTP Logs"),
    ANALOG_ADC("Analog ADC Stream")
}

data class IdeUiState(
    val currentSketch: Sketch,
    val openSketches: List<Sketch>,
    val selectedBoard: Board,
    val activeTab: ActiveTab = ActiveTab.EDITOR,
    val isCompiling: Boolean = false,
    val compilationResult: CompilationResult? = null,
    val isConsoleExpanded: Boolean = false,

    // Serial Monitor State
    val isSerialConnected: Boolean = true,
    val baudRate: Int = 115200,
    val serialLogs: List<String> = emptyList(),
    val isSimulatingSerial: Boolean = true,
    val telemetryType: TelemetryType = TelemetryType.HC_SR04,
    val autoScroll: Boolean = true,
    val showTimestamps: Boolean = true,

    // Gemini Studio Bot State
    val isGeminiOpen: Boolean = false,
    val isAiGenerating: Boolean = false,
    val aiStreamingText: String = "",
    val aiMessages: List<ChatMessage> = emptyList(),
    val aiError: String? = null,

    // Per-project chat storage: maps sketch.id -> List<ChatMessage>
    val projectChats: Map<String, List<ChatMessage>> = emptyMap(),

    // Google Auth State
    val authState: AuthState = AuthState.Idle,
    val showAuthDialog: Boolean = false,
    val showChatFileDialog: Boolean = false,

    // Flasher & Programmer State (USBtinyISP, USBasp, Bootloader)
    val selectedProgrammer: com.example.data.model.Programmer = com.example.data.model.SupportedProgrammers.USBTINY_ISP,
    val showFlasherDialog: Boolean = false,
    val isFlashing: Boolean = false,
    val flashProgress: com.example.data.flasher.FlashProgress? = null,

    // Dialogs
    val showExamplesDialog: Boolean = false,
    val showBoardPicker: Boolean = false,
    val notificationMessage: String? = null
)

class MainViewModel(
    private val geminiRepo: GeminiStudioRepository = GeminiStudioRepository(),
    private var authManager: AuthManager? = null,
    private var projectRepo: ProjectRepository? = null
) : ViewModel() {

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var telemetryJob: Job? = null
    private var aiJob: Job? = null

    private val initialSketch = SampleSketches.BLINK

    private val initialBlinkChat = listOf(
        ChatMessage(
            isUser = false,
            text = "👋 Welcome to **DroidDuino Studio**! I am **Gemini Studio Bot**.\n\nActive Project: **01.Blink** (`01.Blink.chat.md`)\n\nI can:\n- ⚡ **Explain** sketch architecture & registers\n- 🛠️ **Diagnose and Fix** compiler & syntax errors\n- 🔌 **Generate Wiring Pinouts** for sensors & modules\n- 💡 **Create Custom Sketches** for ESP32, Arduino, and retro automotive projects (GAZ-24!)\n- 📊 **Analyze Serial Monitor** telemetry logs\n\n📌 *Every project in DroidDuino Studio has its own dedicated chat file!*"
        )
    )

    private val _uiState = MutableStateFlow(
        IdeUiState(
            currentSketch = initialSketch,
            openSketches = listOf(initialSketch),
            selectedBoard = SupportedBoards.ARDUINO_UNO,
            aiMessages = initialBlinkChat,
            projectChats = mapOf(initialSketch.id to initialBlinkChat)
        )
    )
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    init {
        startTelemetrySimulation()
    }

    /**
     * Initializes AuthManager and ProjectRepository if not injected via factory
     */
    fun setupDependencies(context: Context) {
        if (authManager == null) {
            val appCtx = context.applicationContext
            val auth = AuthManager(appCtx)
            val repo = ProjectRepository(appCtx)
            authManager = auth
            projectRepo = repo

            viewModelScope.launch {
                auth.authState.collect { state ->
                    _uiState.update { it.copy(authState = state) }
                }
            }
        }
    }

    // ================= GOOGLE AUTH =================

    fun setShowAuthDialog(show: Boolean) {
        _uiState.update { it.copy(showAuthDialog = show) }
    }

    fun setShowChatFileDialog(show: Boolean) {
        _uiState.update { it.copy(showChatFileDialog = show) }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            val manager = authManager ?: AuthManager(activity.applicationContext).also { authManager = it }
            val result = manager.signInWithGoogle(activity)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        showAuthDialog = false,
                        notificationMessage = "Signed in as ${user.displayName ?: user.email}!"
                    )
                }
                // Sync current sketch to cloud
                projectRepo?.saveProject(user.uid, _uiState.value.currentSketch)
            }.onFailure { err ->
                Log.w(TAG, "Sign in failed: ${err.message}")
            }
        }
    }

    fun signOut() {
        authManager?.signOut()
        _uiState.update {
            it.copy(
                showAuthDialog = false,
                notificationMessage = "Signed out of Google account"
            )
        }
    }

    // ================= CODE EDITING & TABS =================

    fun updateCode(newCode: String) {
        val currentCode = _uiState.value.currentSketch.code
        if (newCode != currentCode) {
            undoStack.push(currentCode)
            redoStack.clear()
            _uiState.update { state ->
                val updatedSketch = state.currentSketch.copy(code = newCode, updatedAt = System.currentTimeMillis())
                state.copy(
                    currentSketch = updatedSketch,
                    openSketches = state.openSketches.map { if (it.id == updatedSketch.id) updatedSketch else it }
                )
            }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prevCode = undoStack.pop()
            redoStack.push(_uiState.value.currentSketch.code)
            _uiState.update { state ->
                val updated = state.currentSketch.copy(code = prevCode)
                state.copy(currentSketch = updated)
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextCode = redoStack.pop()
            undoStack.push(_uiState.value.currentSketch.code)
            _uiState.update { state ->
                val updated = state.currentSketch.copy(code = nextCode)
                state.copy(currentSketch = updated)
            }
        }
    }

    fun selectBoard(board: Board) {
        _uiState.update { state ->
            val updatedSketch = state.currentSketch.copy(boardId = board.id)
            state.copy(
                selectedBoard = board,
                currentSketch = updatedSketch,
                showBoardPicker = false,
                notificationMessage = "Target board changed to ${board.name}"
            )
        }
    }

    fun loadSketch(sketch: Sketch) {
        val board = SupportedBoards.allBoards.find { it.id == sketch.boardId } ?: _uiState.value.selectedBoard
        undoStack.clear()
        redoStack.clear()

        // Get or initialize per-project chat file for this sketch
        val currentChats = _uiState.value.projectChats
        val sketchChat = currentChats[sketch.id] ?: run {
            val welcomeText = when {
                sketch.id.contains("gaz24") ->
                    "👋 Gemini Studio Bot online for **GAZ-24 Volga Tachometer**! Ask me about pulse interrupt formulas, optical PC817 isolation, or ZMZ-402 redline shifts."
                sketch.id.contains("esp32") ->
                    "👋 Gemini Studio Bot online for **ESP32 Wi-Fi Server**! Ask me about Access Point configuration, HTML web forms, or FreeRTOS watchdog handling."
                sketch.id.contains("ultrasonic") ->
                    "👋 Gemini Studio Bot online for **HC-SR04 Ultrasonic Radar**! Ask me about speed-of-sound timing, trigger pulses, or buzzer PWM alerts."
                else ->
                    "👋 Gemini Studio Bot online for project **${sketch.name}** (`${sketch.name}.chat.md`). How can I help you build this firmware?"
            }
            listOf(ChatMessage(isUser = false, text = welcomeText))
        }

        val updatedChats = currentChats + (sketch.id to sketchChat)

        _uiState.update { state ->
            val existingOpen = state.openSketches.find { it.id == sketch.id }
            val newOpenList = if (existingOpen == null) state.openSketches + sketch else state.openSketches
            state.copy(
                currentSketch = sketch,
                openSketches = newOpenList,
                selectedBoard = board,
                showExamplesDialog = false,
                aiMessages = sketchChat,
                projectChats = updatedChats,
                notificationMessage = "Loaded ${sketch.name}"
            )
        }
    }

    fun createNewSketch(name: String = "Sketch_${System.currentTimeMillis() % 10000}") {
        val newId = "user_${System.currentTimeMillis()}"
        val newSketch = Sketch(
            id = newId,
            name = name,
            boardId = _uiState.value.selectedBoard.id,
            code = """// DroidDuino Studio Sketch
// Target: ${_uiState.value.selectedBoard.name}

void setup() {
  Serial.begin(115200);
  pinMode(LED_BUILTIN, OUTPUT);
}

void loop() {
  digitalWrite(LED_BUILTIN, HIGH);
  delay(500);
  digitalWrite(LED_BUILTIN, LOW);
  delay(500);
}
""".trimIndent()
        )
        loadSketch(newSketch)
    }

    fun applyAiCodeToEditor(code: String) {
        updateCode(code)
        _uiState.update { it.copy(notificationMessage = "Applied AI fix to editor!", isGeminiOpen = false) }
    }

    fun clearCurrentProjectChat() {
        val sketchId = _uiState.value.currentSketch.id
        val freshGreeting = listOf(
            ChatMessage(
                isUser = false,
                text = "💬 Chat history for project **${_uiState.value.currentSketch.name}** cleared. Ask me anything about this sketch!"
            )
        )
        _uiState.update { state ->
            state.copy(
                aiMessages = freshGreeting,
                projectChats = state.projectChats + (sketchId to freshGreeting),
                notificationMessage = "Cleared chat for ${_uiState.value.currentSketch.name}"
            )
        }
    }

    // ================= COMPILATION =================

    fun compileSketch() {
        val sketch = _uiState.value.currentSketch
        val board = _uiState.value.selectedBoard

        _uiState.update {
            it.copy(
                isCompiling = true,
                compilationResult = null,
                isConsoleExpanded = false
            )
        }

        viewModelScope.launch {
            val result = SketchCompiler.compile(sketch.code, board)
            _uiState.update {
                it.copy(
                    isCompiling = false,
                    compilationResult = result,
                    isConsoleExpanded = !result.success,
                    notificationMessage = if (result.success) "✓ Compilation successful!" else "⚠️ Build errors detected"
                )
            }
        }
    }

    fun toggleConsoleExpanded() {
        _uiState.update { it.copy(isConsoleExpanded = !it.isConsoleExpanded) }
    }

    // ================= GEMINI STUDIO BOT & PROJECT CHAT =================

    fun openGeminiSheet() {
        _uiState.update { it.copy(isGeminiOpen = true) }
    }

    fun closeGeminiSheet() {
        _uiState.update { it.copy(isGeminiOpen = false) }
    }

    private fun appendMessageToProject(userMsg: ChatMessage) {
        val sketchId = _uiState.value.currentSketch.id
        _uiState.update { state ->
            val updatedMessages = state.aiMessages + userMsg
            state.copy(
                aiMessages = updatedMessages,
                projectChats = state.projectChats + (sketchId to updatedMessages)
            )
        }

        // Persist to Firestore if authenticated
        val user = authManager?.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    projectRepo?.saveChatMessage(user.uid, sketchId, userMsg)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to persist chat message to Firestore: ${e.message}")
                }
            }
        }
    }

    fun sendUserPrompt(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isEmpty() || _uiState.value.isAiGenerating) return

        val userMsg = ChatMessage(isUser = true, text = trimmed)
        appendMessageToProject(userMsg)

        _uiState.update {
            it.copy(
                isAiGenerating = true,
                aiStreamingText = "",
                aiError = null
            )
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            try {
                geminiRepo.streamChat(
                    userMessage = trimmed,
                    currentSketch = _uiState.value.currentSketch.code,
                    board = _uiState.value.selectedBoard,
                    recentHistory = _uiState.value.aiMessages
                ).collect { chunk ->
                    _uiState.update { it.copy(aiStreamingText = chunk) }
                }

                // Finalize streaming response
                val finalText = _uiState.value.aiStreamingText
                val extracted = extractCodeFromMarkdown(finalText)
                val aiMsg = ChatMessage(isUser = false, text = finalText, extractedCode = extracted)

                _uiState.update { it.copy(isAiGenerating = false, aiStreamingText = "") }
                appendMessageToProject(aiMsg)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiError = e.localizedMessage ?: "Generation failed. Please retry."
                    )
                }
            }
        }
    }

    fun askGeminiToFixErrors() {
        val result = _uiState.value.compilationResult ?: return
        openGeminiSheet()
        val userPrompt = "Explain and fix this compilation error in my sketch:\n${result.rawLog}"
        val userMsg = ChatMessage(isUser = true, text = userPrompt)
        appendMessageToProject(userMsg)

        _uiState.update {
            it.copy(
                isAiGenerating = true,
                aiStreamingText = "",
                aiError = null
            )
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            try {
                geminiRepo.fixCompilerErrors(
                    code = _uiState.value.currentSketch.code,
                    compilerLog = result.rawLog,
                    board = _uiState.value.selectedBoard
                ).collect { chunk ->
                    _uiState.update { it.copy(aiStreamingText = chunk) }
                }

                val finalText = _uiState.value.aiStreamingText
                val extracted = extractCodeFromMarkdown(finalText)
                val aiMsg = ChatMessage(isUser = false, text = finalText, extractedCode = extracted)
                _uiState.update { it.copy(isAiGenerating = false, aiStreamingText = "") }
                appendMessageToProject(aiMsg)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiError = e.localizedMessage ?: "Failed to generate fix."
                    )
                }
            }
        }
    }

    fun askGeminiToExplainCode() {
        openGeminiSheet()
        val userMsg = ChatMessage(isUser = true, text = "⚡ Explain how this sketch works step-by-step.")
        appendMessageToProject(userMsg)

        _uiState.update {
            it.copy(
                isAiGenerating = true,
                aiStreamingText = "",
                aiError = null
            )
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            try {
                geminiRepo.explainSketch(
                    code = _uiState.value.currentSketch.code,
                    board = _uiState.value.selectedBoard
                ).collect { chunk ->
                    _uiState.update { it.copy(aiStreamingText = chunk) }
                }

                val finalText = _uiState.value.aiStreamingText
                val aiMsg = ChatMessage(isUser = false, text = finalText)
                _uiState.update { it.copy(isAiGenerating = false, aiStreamingText = "") }
                appendMessageToProject(aiMsg)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiError = e.localizedMessage ?: "Failed to explain sketch."
                    )
                }
            }
        }
    }

    fun askGeminiForWiring() {
        openGeminiSheet()
        val userMsg = ChatMessage(isUser = true, text = "🔌 Generate a hardware wiring diagram & pinout table for this sketch.")
        appendMessageToProject(userMsg)

        _uiState.update {
            it.copy(
                isAiGenerating = true,
                aiStreamingText = "",
                aiError = null
            )
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            try {
                geminiRepo.generateWiringGuide(
                    code = _uiState.value.currentSketch.code,
                    board = _uiState.value.selectedBoard
                ).collect { chunk ->
                    _uiState.update { it.copy(aiStreamingText = chunk) }
                }

                val finalText = _uiState.value.aiStreamingText
                val aiMsg = ChatMessage(isUser = false, text = finalText)
                _uiState.update { it.copy(isAiGenerating = false, aiStreamingText = "") }
                appendMessageToProject(aiMsg)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiError = e.localizedMessage ?: "Failed to generate wiring."
                    )
                }
            }
        }
    }

    fun askGeminiToAnalyzeSerial() {
        val logs = _uiState.value.serialLogs.takeLast(25).joinToString("\n")
        openGeminiSheet()
        val userMsg = ChatMessage(isUser = true, text = "📊 Analyze my current Serial Monitor logs for errors and telemetry trends.")
        appendMessageToProject(userMsg)

        _uiState.update {
            it.copy(
                isAiGenerating = true,
                aiStreamingText = "",
                aiError = null
            )
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            try {
                geminiRepo.analyzeSerialLog(
                    serialLog = logs,
                    currentSketch = _uiState.value.currentSketch.code,
                    board = _uiState.value.selectedBoard
                ).collect { chunk ->
                    _uiState.update { it.copy(aiStreamingText = chunk) }
                }

                val finalText = _uiState.value.aiStreamingText
                val aiMsg = ChatMessage(isUser = false, text = finalText)
                _uiState.update { it.copy(isAiGenerating = false, aiStreamingText = "") }
                appendMessageToProject(aiMsg)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiError = e.localizedMessage ?: "Failed to analyze serial logs."
                    )
                }
            }
        }
    }

    fun askGeminiAboutUsbTiny() {
        _uiState.update { it.copy(showFlasherDialog = false) }
        openGeminiSheet()
        val userMsg = ChatMessage(
            isUser = true,
            text = "🔌 Как подключить программатор USBtinyISP к плате ${_uiState.value.selectedBoard.name} через 6-пиновый разъем ICSP и как прошить скетч или загрузчик без ошибок?"
        )
        appendMessageToProject(userMsg)

        _uiState.update {
            it.copy(
                isAiGenerating = true,
                aiStreamingText = "",
                aiError = null
            )
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            try {
                geminiRepo.streamChat(
                    userMessage = userMsg.text,
                    currentSketch = _uiState.value.currentSketch.code,
                    board = _uiState.value.selectedBoard,
                    recentHistory = _uiState.value.aiMessages
                ).collect { chunk ->
                    _uiState.update { it.copy(aiStreamingText = chunk) }
                }

                val finalText = _uiState.value.aiStreamingText
                val aiMsg = ChatMessage(isUser = false, text = finalText)
                _uiState.update { it.copy(isAiGenerating = false, aiStreamingText = "") }
                appendMessageToProject(aiMsg)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiError = e.localizedMessage ?: "Failed to get USBtinyISP guide."
                    )
                }
            }
        }
    }

    private fun extractCodeFromMarkdown(markdown: String): String? {
        val cppBlock = markdown.indexOf("```cpp")
        val altBlock = if (cppBlock == -1) markdown.indexOf("```c") else cppBlock
        val codeStart = if (altBlock == -1) markdown.indexOf("```") else altBlock
        if (codeStart != -1) {
            val firstLineEnd = markdown.indexOf('\n', codeStart)
            if (firstLineEnd != -1) {
                val blockEnd = markdown.indexOf("```", firstLineEnd)
                if (blockEnd != -1) {
                    return markdown.substring(firstLineEnd + 1, blockEnd).trim()
                }
            }
        }
        return null
    }

    // ================= SERIAL MONITOR =================

    fun setActiveTab(tab: ActiveTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setBaudRate(baud: Int) {
        _uiState.update { it.copy(baudRate = baud) }
        appendSerialLog("[Serial] Baud rate configured to $baud bps")
    }

    fun toggleSerialSimulation() {
        val newSim = !_uiState.value.isSimulatingSerial
        _uiState.update { it.copy(isSimulatingSerial = newSim) }
        if (newSim) startTelemetrySimulation() else telemetryJob?.cancel()
    }

    fun setTelemetryType(type: TelemetryType) {
        _uiState.update { it.copy(telemetryType = type) }
        appendSerialLog("[Telemetry] Switched mode to ${type.title}")
    }

    fun clearSerialLogs() {
        _uiState.update { it.copy(serialLogs = emptyList()) }
    }

    fun sendSerialCommand(command: String, ending: String = "\r\n") {
        if (command.isBlank()) return
        appendSerialLog("> $command")
        if (command.equals("AT", ignoreCase = true)) {
            appendSerialLog("OK")
        } else if (command.startsWith("LED_ON", ignoreCase = true)) {
            appendSerialLog("[Hardware] Pin 13 / GPIO2 set HIGH")
        } else if (command.startsWith("LED_OFF", ignoreCase = true)) {
            appendSerialLog("[Hardware] Pin 13 / GPIO2 set LOW")
        } else if (command.equals("STATUS", ignoreCase = true)) {
            appendSerialLog("[Hardware] Board: ${_uiState.value.selectedBoard.name} | UpTime: ${System.currentTimeMillis() / 1000}s | Free RAM: ${_uiState.value.selectedBoard.sramBytes}B")
        } else {
            appendSerialLog("[Echo] Received: $command")
        }
    }

    private fun appendSerialLog(text: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val formatted = if (_uiState.value.showTimestamps) "[$timestamp] $text" else text
        _uiState.update { state ->
            val updated = (state.serialLogs + formatted).takeLast(200)
            state.copy(serialLogs = updated)
        }
    }

    private fun startTelemetrySimulation() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            var counter = 0
            while (true) {
                delay(800)
                if (_uiState.value.isSimulatingSerial && _uiState.value.isSerialConnected) {
                    val log = when (_uiState.value.telemetryType) {
                        TelemetryType.HC_SR04 -> {
                            val dist = 10.0 + (Math.sin(counter * 0.3) * 15.0).coerceAtLeast(1.0)
                            val alert = if (dist < 15.0) " [BUZZER ACTIVE]" else ""
                            String.format(Locale.US, "Distance: %.1f cm%s", dist, alert)
                        }
                        TelemetryType.GAZ24_RPM -> {
                            val baseRpm = 850 + (Math.sin(counter * 0.2) * 2200).coerceAtLeast(0.0).toInt()
                            val shift = if (baseRpm > 4200) " [SHIFT LIGHT ON!]" else ""
                            "GAZ-24 ZMZ-402 RPM: $baseRpm$shift | Oil: 3.2 bar"
                        }
                        TelemetryType.ESP32_WIFI -> {
                            val ip = "192.168.4.1"
                            "[HTTP] 200 OK GET /status from 192.168.4.2 (Free heap: 284KB, RSSI: -54dBm)"
                        }
                        TelemetryType.ANALOG_ADC -> {
                            val raw = (512 + Math.sin(counter * 0.5) * 400).toInt()
                            val v = raw * (5.0 / 1023.0)
                            String.format(Locale.US, "ADC Channel A0: %4d (%.2f V)", raw, v)
                        }
                    }
                    appendSerialLog(log)
                    counter++
                }
            }
        }
    }

    // ================= FLASHER & PROGRAMMERS =================

    fun setShowFlasherDialog(show: Boolean) {
        _uiState.update { it.copy(showFlasherDialog = show) }
    }

    fun selectProgrammer(programmer: com.example.data.model.Programmer) {
        _uiState.update { it.copy(selectedProgrammer = programmer) }
    }

    fun uploadViaProgrammer() {
        val board = _uiState.value.selectedBoard
        val programmer = _uiState.value.selectedProgrammer
        val codeLength = _uiState.value.currentSketch.code.length

        _uiState.update { it.copy(isFlashing = true, flashProgress = null) }

        viewModelScope.launch {
            com.example.data.flasher.AvrFlasher.flashSketch(
                board = board,
                programmer = programmer,
                codeLength = codeLength
            ).collect { progress ->
                _uiState.update {
                    it.copy(
                        flashProgress = progress,
                        isFlashing = !progress.isComplete,
                        notificationMessage = if (progress.isComplete) {
                            if (progress.isSuccess) "✓ Flash succeeded via ${programmer.name}!"
                            else "Upload failed: ${progress.currentStep}"
                        } else null
                    )
                }
            }
        }
    }

    fun burnBootloaderViaUsbTiny() {
        val board = _uiState.value.selectedBoard
        _uiState.update { it.copy(isFlashing = true, flashProgress = null) }

        viewModelScope.launch {
            com.example.data.flasher.AvrFlasher.burnBootloaderViaUsbTiny(board = board).collect { progress ->
                _uiState.update {
                    it.copy(
                        flashProgress = progress,
                        isFlashing = !progress.isComplete,
                        notificationMessage = if (progress.isComplete) {
                            if (progress.isSuccess) "Bootloader successfully burned via USBtinyISP!"
                            else "Bootloader burn failed."
                        } else null
                    )
                }
            }
        }
    }

    // ================= DIALOGS =================

    fun setShowExamplesDialog(show: Boolean) {
        _uiState.update { it.copy(showExamplesDialog = show) }
    }

    fun setShowBoardPicker(show: Boolean) {
        _uiState.update { it.copy(showBoardPicker = show) }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }
}
