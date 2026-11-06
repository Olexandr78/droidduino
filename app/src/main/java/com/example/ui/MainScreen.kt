package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.AuthState
import com.example.ui.auth.GoogleAccountDialog
import com.example.ui.boards.BoardPickerDialog
import com.example.ui.boards.BoardPinoutView
import com.example.ui.editor.CodeEditorView
import com.example.ui.examples.ExamplesLibraryDialog
import com.example.ui.gemini.GeminiStudioSheet
import com.example.ui.gemini.ProjectChatFileDialog
import com.example.ui.serial.SerialMonitorView
import com.example.ui.theme.ArduinoTeal
import com.example.ui.theme.ArduinoTealBright
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiGradientEnd
import com.example.ui.theme.GeminiGradientStart
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeBorder
import com.example.ui.theme.IdeSurface
import com.example.ui.theme.IdeSurfaceVariant
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.setupDependencies(context)
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notificationMessage) {
        val msg = uiState.notificationMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val isTablet = maxWidth >= 720.dp
        var tabletSideTool by remember { mutableStateOf(ActiveTab.EDITOR) } // EDITOR means Gemini Studio Bot on side
        var isSideToolExpanded by remember { mutableStateOf(true) }

        if (isTablet) {
            // ================= TABLET / EXPANDED LAYOUT (DUAL-PANE + NAVIGATION RAIL) =================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Left Navigation Rail
                NavigationRail(
                    containerColor = IdeSurface,
                    contentColor = IdeTextSecondary,
                    header = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ArduinoTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("∞", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    },
                    modifier = Modifier.width(72.dp)
                ) {
                    NavigationRailItem(
                        selected = tabletSideTool == ActiveTab.EDITOR && !isSideToolExpanded,
                        onClick = { isSideToolExpanded = false },
                        icon = { Icon(Icons.Default.Code, contentDescription = "Full Editor") },
                        label = { Text("Code", fontSize = 10.sp) }
                    )
                    NavigationRailItem(
                        selected = isSideToolExpanded && tabletSideTool == ActiveTab.EDITOR,
                        onClick = {
                            tabletSideTool = ActiveTab.EDITOR
                            isSideToolExpanded = true
                        },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Studio Bot", tint = GeminiCyan) },
                        label = { Text("Gemini", fontSize = 10.sp) }
                    )
                    NavigationRailItem(
                        selected = isSideToolExpanded && tabletSideTool == ActiveTab.SERIAL_MONITOR,
                        onClick = {
                            tabletSideTool = ActiveTab.SERIAL_MONITOR
                            isSideToolExpanded = true
                        },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = "Serial Monitor") },
                        label = { Text("Serial", fontSize = 10.sp) }
                    )
                    NavigationRailItem(
                        selected = isSideToolExpanded && tabletSideTool == ActiveTab.PINOUT_VIEW,
                        onClick = {
                            tabletSideTool = ActiveTab.PINOUT_VIEW
                            isSideToolExpanded = true
                        },
                        icon = { Icon(Icons.Default.DeveloperBoard, contentDescription = "Pinout") },
                        label = { Text("Pinout", fontSize = 10.sp) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationRailItem(
                        selected = false,
                        onClick = { viewModel.setShowChatFileDialog(true) },
                        icon = { Icon(Icons.Default.Description, contentDescription = "Chat File", tint = ArduinoTealBright) },
                        label = { Text("Chat File", fontSize = 9.sp) }
                    )

                    NavigationRailItem(
                        selected = false,
                        onClick = { viewModel.setShowFlasherDialog(true) },
                        icon = { Icon(Icons.Default.ElectricBolt, contentDescription = "Flash", tint = GeminiCyan) },
                        label = { Text("Flash", fontSize = 10.sp) }
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(IdeBorder)
                ) { }

                // Central Code Workspace Pane
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TopIdeBar(
                        boardName = uiState.selectedBoard.name,
                        isCompiling = uiState.isCompiling,
                        authState = uiState.authState,
                        onOpenBoardPicker = { viewModel.setShowBoardPicker(true) },
                        onCompile = { viewModel.compileSketch() },
                        onOpenFlasher = { viewModel.setShowFlasherDialog(true) },
                        onOpenExamples = { viewModel.setShowExamplesDialog(true) },
                        onOpenAuth = { viewModel.setShowAuthDialog(true) },
                        onOpenChatFile = { viewModel.setShowChatFileDialog(true) },
                        onOpenGemini = {
                            tabletSideTool = ActiveTab.EDITOR
                            isSideToolExpanded = true
                        }
                    )

                    SketchTabsBar(
                        openSketches = uiState.openSketches,
                        activeSketch = uiState.currentSketch,
                        onSelectSketch = { viewModel.loadSketch(it) },
                        onNewSketch = { viewModel.createNewSketch() }
                    )

                    AnimatedVisibility(
                        visible = uiState.compilationResult != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val res = uiState.compilationResult
                        if (res != null) {
                            CompilerBanner(
                                result = res,
                                isExpanded = uiState.isConsoleExpanded,
                                onToggleExpand = { viewModel.toggleConsoleExpanded() },
                                onAskGeminiFix = {
                                    tabletSideTool = ActiveTab.EDITOR
                                    isSideToolExpanded = true
                                    viewModel.askGeminiToFixErrors()
                                }
                            )
                        }
                    }

                    // Code Editor
                    CodeEditorView(
                        code = uiState.currentSketch.code,
                        onCodeChange = { viewModel.updateCode(it) },
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Tablet Right Docked Tool Window (Gemini Bot / Serial Monitor / Pinout side-by-side!)
                AnimatedVisibility(
                    visible = isSideToolExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(IdeBorder)
                        )

                        Surface(
                            color = IdeSurface,
                            modifier = Modifier
                                .width(420.dp)
                                .fillMaxHeight()
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Tool Window Switcher Header
                                Surface(
                                    color = IdeSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            onClick = { tabletSideTool = ActiveTab.EDITOR },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (tabletSideTool == ActiveTab.EDITOR) ArduinoTeal else Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = if (tabletSideTool == ActiveTab.EDITOR) Color.White else GeminiCyan, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Studio Bot", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (tabletSideTool == ActiveTab.EDITOR) Color.White else IdeTextSecondary)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Surface(
                                            onClick = { tabletSideTool = ActiveTab.SERIAL_MONITOR },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (tabletSideTool == ActiveTab.SERIAL_MONITOR) ArduinoTeal else Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Terminal, contentDescription = null, tint = if (tabletSideTool == ActiveTab.SERIAL_MONITOR) Color.White else IdeTextSecondary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Serial", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (tabletSideTool == ActiveTab.SERIAL_MONITOR) Color.White else IdeTextSecondary)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Surface(
                                            onClick = { tabletSideTool = ActiveTab.PINOUT_VIEW },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (tabletSideTool == ActiveTab.PINOUT_VIEW) ArduinoTeal else Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = if (tabletSideTool == ActiveTab.PINOUT_VIEW) Color.White else IdeTextSecondary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pinout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (tabletSideTool == ActiveTab.PINOUT_VIEW) Color.White else IdeTextSecondary)
                                            }
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        IconButton(
                                            onClick = { isSideToolExpanded = false },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Minimize Tool", tint = IdeTextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Tool Window Content
                                Box(modifier = Modifier.weight(1f)) {
                                    when (tabletSideTool) {
                                        ActiveTab.EDITOR -> {
                                            // Gemini Studio Bot docked right beside editor with per-project chat file
                                            com.example.ui.gemini.GeminiStudioChatView(
                                                sketchName = uiState.currentSketch.name,
                                                messages = uiState.aiMessages,
                                                isGenerating = uiState.isAiGenerating,
                                                streamingText = uiState.aiStreamingText,
                                                errorMessage = uiState.aiError,
                                                authState = uiState.authState,
                                                onSendMessage = { viewModel.sendUserPrompt(it) },
                                                onExplainSketch = { viewModel.askGeminiToExplainCode() },
                                                onFixErrors = { viewModel.askGeminiToFixErrors() },
                                                onGenerateWiring = { viewModel.askGeminiForWiring() },
                                                onAnalyzeSerial = { viewModel.askGeminiToAnalyzeSerial() },
                                                onApplyCodeToEditor = { viewModel.applyAiCodeToEditor(it) },
                                                onViewChatFile = { viewModel.setShowChatFileDialog(true) },
                                                onClearChat = { viewModel.clearCurrentProjectChat() },
                                                onSignInClicked = { viewModel.signInWithGoogle(it) },
                                                onClose = { isSideToolExpanded = false },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        ActiveTab.SERIAL_MONITOR -> {
                                            SerialMonitorView(
                                                logs = uiState.serialLogs,
                                                baudRate = uiState.baudRate,
                                                isSimulating = uiState.isSimulatingSerial,
                                                telemetryType = uiState.telemetryType,
                                                autoScroll = uiState.autoScroll,
                                                onBaudRateChange = { viewModel.setBaudRate(it) },
                                                onToggleSimulation = { viewModel.toggleSerialSimulation() },
                                                onTelemetryTypeChange = { viewModel.setTelemetryType(it) },
                                                onClearLogs = { viewModel.clearSerialLogs() },
                                                onSendCommand = { viewModel.sendSerialCommand(it) },
                                                onAskGeminiAnalyze = {
                                                    tabletSideTool = ActiveTab.EDITOR
                                                    viewModel.askGeminiToAnalyzeSerial()
                                                }
                                            )
                                        }
                                        ActiveTab.PINOUT_VIEW -> {
                                            BoardPinoutView(
                                                board = uiState.selectedBoard,
                                                onAskGeminiWiring = {
                                                    tabletSideTool = ActiveTab.EDITOR
                                                    viewModel.askGeminiForWiring()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ================= PHONE / COMPACT LAYOUT =================
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = IdeBackground,
                bottomBar = {
                    Surface(
                        color = IdeSurface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavTabItem(
                                title = "Editor",
                                icon = Icons.Default.Code,
                                isSelected = uiState.activeTab == ActiveTab.EDITOR,
                                onClick = { viewModel.setActiveTab(ActiveTab.EDITOR) }
                            )
                            NavTabItem(
                                title = "Serial",
                                icon = Icons.Default.Terminal,
                                isSelected = uiState.activeTab == ActiveTab.SERIAL_MONITOR,
                                badge = if (uiState.isSimulatingSerial) "ONLINE" else null,
                                onClick = { viewModel.setActiveTab(ActiveTab.SERIAL_MONITOR) }
                            )
                            NavTabItem(
                                title = "Pinout",
                                icon = Icons.Default.DeveloperBoard,
                                isSelected = uiState.activeTab == ActiveTab.PINOUT_VIEW,
                                onClick = { viewModel.setActiveTab(ActiveTab.PINOUT_VIEW) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TopIdeBar(
                        boardName = uiState.selectedBoard.name,
                        isCompiling = uiState.isCompiling,
                        authState = uiState.authState,
                        onOpenBoardPicker = { viewModel.setShowBoardPicker(true) },
                        onCompile = { viewModel.compileSketch() },
                        onOpenFlasher = { viewModel.setShowFlasherDialog(true) },
                        onOpenExamples = { viewModel.setShowExamplesDialog(true) },
                        onOpenAuth = { viewModel.setShowAuthDialog(true) },
                        onOpenChatFile = { viewModel.setShowChatFileDialog(true) },
                        onOpenGemini = { viewModel.openGeminiSheet() }
                    )

                    SketchTabsBar(
                        openSketches = uiState.openSketches,
                        activeSketch = uiState.currentSketch,
                        onSelectSketch = { viewModel.loadSketch(it) },
                        onNewSketch = { viewModel.createNewSketch() }
                    )

                    AnimatedVisibility(
                        visible = uiState.compilationResult != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val res = uiState.compilationResult
                        if (res != null) {
                            CompilerBanner(
                                result = res,
                                isExpanded = uiState.isConsoleExpanded,
                                onToggleExpand = { viewModel.toggleConsoleExpanded() },
                                onAskGeminiFix = { viewModel.askGeminiToFixErrors() }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState.activeTab) {
                            ActiveTab.EDITOR -> {
                                CodeEditorView(
                                    code = uiState.currentSketch.code,
                                    onCodeChange = { viewModel.updateCode(it) },
                                    onUndo = { viewModel.undo() },
                                    onRedo = { viewModel.redo() }
                                )
                            }
                            ActiveTab.SERIAL_MONITOR -> {
                                SerialMonitorView(
                                    logs = uiState.serialLogs,
                                    baudRate = uiState.baudRate,
                                    isSimulating = uiState.isSimulatingSerial,
                                    telemetryType = uiState.telemetryType,
                                    autoScroll = uiState.autoScroll,
                                    onBaudRateChange = { viewModel.setBaudRate(it) },
                                    onToggleSimulation = { viewModel.toggleSerialSimulation() },
                                    onTelemetryTypeChange = { viewModel.setTelemetryType(it) },
                                    onClearLogs = { viewModel.clearSerialLogs() },
                                    onSendCommand = { viewModel.sendSerialCommand(it) },
                                    onAskGeminiAnalyze = { viewModel.askGeminiToAnalyzeSerial() }
                                )
                            }
                            ActiveTab.PINOUT_VIEW -> {
                                BoardPinoutView(
                                    board = uiState.selectedBoard,
                                    onAskGeminiWiring = { viewModel.askGeminiForWiring() }
                                )
                            }
                        }
                    }
                }
            }

            // Gemini Studio Assistant Modal Sheet for Phone
            GeminiStudioSheet(
                isOpen = uiState.isGeminiOpen,
                onDismiss = { viewModel.closeGeminiSheet() },
                sketchName = uiState.currentSketch.name,
                messages = uiState.aiMessages,
                isGenerating = uiState.isAiGenerating,
                streamingText = uiState.aiStreamingText,
                errorMessage = uiState.aiError,
                authState = uiState.authState,
                onSendMessage = { viewModel.sendUserPrompt(it) },
                onExplainSketch = { viewModel.askGeminiToExplainCode() },
                onFixErrors = { viewModel.askGeminiToFixErrors() },
                onGenerateWiring = { viewModel.askGeminiForWiring() },
                onAnalyzeSerial = { viewModel.askGeminiToAnalyzeSerial() },
                onApplyCodeToEditor = { viewModel.applyAiCodeToEditor(it) },
                onViewChatFile = { viewModel.setShowChatFileDialog(true) },
                onClearChat = { viewModel.clearCurrentProjectChat() },
                onSignInClicked = { viewModel.signInWithGoogle(it) }
            )
        }
    }

    // Google Account Dialog
    GoogleAccountDialog(
        isOpen = uiState.showAuthDialog,
        authState = uiState.authState,
        onDismiss = { viewModel.setShowAuthDialog(false) },
        onSignInClicked = { viewModel.signInWithGoogle(it) },
        onSignOutClicked = { viewModel.signOut() }
    )

    // Project Chat File Dialog
    ProjectChatFileDialog(
        isOpen = uiState.showChatFileDialog,
        sketch = uiState.currentSketch,
        board = uiState.selectedBoard,
        messages = uiState.aiMessages,
        onDismiss = { viewModel.setShowChatFileDialog(false) },
        onClearChat = { viewModel.clearCurrentProjectChat() }
    )

    // Examples Library Dialog
    ExamplesLibraryDialog(
        isOpen = uiState.showExamplesDialog,
        onDismiss = { viewModel.setShowExamplesDialog(false) },
        onSelectSketch = { viewModel.loadSketch(it) }
    )

    // Board Selection Dialog
    BoardPickerDialog(
        isOpen = uiState.showBoardPicker,
        currentBoard = uiState.selectedBoard,
        onDismiss = { viewModel.setShowBoardPicker(false) },
        onSelectBoard = { viewModel.selectBoard(it) }
    )

    // Flasher & USBtinyISP Programmer Dialog
    com.example.ui.flasher.FlasherDialog(
        isOpen = uiState.showFlasherDialog,
        board = uiState.selectedBoard,
        selectedProgrammer = uiState.selectedProgrammer,
        flashProgress = uiState.flashProgress,
        isFlashing = uiState.isFlashing,
        onDismiss = { viewModel.setShowFlasherDialog(false) },
        onSelectProgrammer = { viewModel.selectProgrammer(it) },
        onUploadSketch = { viewModel.uploadViaProgrammer() },
        onBurnBootloader = { viewModel.burnBootloaderViaUsbTiny() },
        onAskGeminiWiring = { viewModel.askGeminiAboutUsbTiny() }
    )
}

@Composable
private fun TopIdeBar(
    boardName: String,
    isCompiling: Boolean,
    authState: AuthState,
    onOpenBoardPicker: () -> Unit,
    onCompile: () -> Unit,
    onOpenFlasher: () -> Unit,
    onOpenExamples: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenChatFile: () -> Unit,
    onOpenGemini: () -> Unit
) {
    Surface(
        color = IdeSurface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title & Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ArduinoTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Text("∞", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DroidDuino",
                    color = IdeTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Board Picker Chip
            Surface(
                onClick = onOpenBoardPicker,
                shape = RoundedCornerShape(14.dp),
                color = IdeSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                modifier = Modifier.testTag("board_picker_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = boardName.split(" ").take(2).joinToString(" "),
                        color = ArduinoTealBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Board",
                        tint = IdeTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Compile / Verify Action Button
            IconButton(
                onClick = onCompile,
                enabled = !isCompiling,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isCompiling) IdeSurfaceVariant else ArduinoTeal.copy(alpha = 0.25f))
                    .testTag("compile_button")
            ) {
                if (isCompiling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = ArduinoTealBright,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Compile & Check Syntax",
                        tint = ArduinoTealBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(3.dp))

            // Upload / Flash via Programmer Button
            IconButton(
                onClick = onOpenFlasher,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                    .testTag("open_flasher_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "Flash & Programmer",
                    tint = GeminiCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(3.dp))

            // Chat File Quick Access Button
            IconButton(
                onClick = onOpenChatFile,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_chat_file_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Project Chat File",
                    tint = ArduinoTealBright,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(3.dp))

            // Examples Library Button
            IconButton(
                onClick = onOpenExamples,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("examples_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Sample Sketches",
                    tint = IdeTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(3.dp))

            // Google Account Profile Button
            Surface(
                onClick = onOpenAuth,
                shape = CircleShape,
                color = if (authState is AuthState.Authenticated) ArduinoTeal.copy(alpha = 0.25f) else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                modifier = Modifier
                    .size(34.dp)
                    .testTag("google_auth_top_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (authState is AuthState.Authenticated) {
                        val initial = authState.user.displayName?.firstOrNull()?.uppercase()
                            ?: authState.user.email?.firstOrNull()?.uppercase()
                            ?: "G"
                        Text(
                            text = initial,
                            color = ArduinoTealBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Gemini Studio Bot Button with Gradient Glow
            Surface(
                onClick = onOpenGemini,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(GeminiGradientStart, GeminiGradientEnd)
                        )
                    )
                    .testTag("gemini_top_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Gemini",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SketchTabsBar(
    openSketches: List<com.example.data.model.Sketch>,
    activeSketch: com.example.data.model.Sketch,
    onSelectSketch: (com.example.data.model.Sketch) -> Unit,
    onNewSketch: () -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        color = IdeSurfaceVariant,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (sketch in openSketches) {
                val isActive = sketch.id == activeSketch.id
                Surface(
                    onClick = { onSelectSketch(sketch) },
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                    color = if (isActive) IdeBackground else Color.Transparent,
                    border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, IdeBorder) else null,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${sketch.name}.ino",
                            color = if (isActive) ArduinoTealBright else IdeTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "💬",
                            fontSize = 9.sp
                        )
                    }
                }
            }

            IconButton(
                onClick = onNewSketch,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("new_sketch_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Sketch",
                    tint = IdeTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CompilerBanner(
    result: com.example.data.model.CompilationResult,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAskGeminiFix: () -> Unit
) {
    val isSuccess = result.success
    val bannerColor = if (isSuccess) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f)
    val borderColor = if (isSuccess) StatusSuccess else StatusError

    Surface(
        color = bannerColor,
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSuccess) StatusSuccess else StatusError,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSuccess) "Compilation Finished Successfully" else "Compilation Failed with Errors",
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isSuccess) {
                            "Flash: ${result.flashUsedBytes} / ${result.flashTotalBytes} B | SRAM: ${result.sramUsedBytes} / ${result.sramTotalBytes} B"
                        } else {
                            "${result.errors.size} error(s) found in sketch"
                        },
                        color = IdeTextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (!isSuccess) {
                    ElevatedButton(
                        onClick = onAskGeminiFix,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = GeminiPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("fix_with_gemini_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fix with Gemini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Log",
                        tint = IdeTextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(IdeBackground, RoundedCornerShape(6.dp))
                        .border(1.dp, IdeBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = result.rawLog,
                        color = if (isSuccess) StatusSuccess else StatusError,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ArduinoTeal.copy(alpha = 0.2f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ArduinoTeal.copy(alpha = 0.4f)) else null,
        modifier = Modifier.testTag("nav_tab_${title.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) ArduinoTealBright else IdeTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) ArduinoTealBright else IdeTextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = StatusSuccess
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
