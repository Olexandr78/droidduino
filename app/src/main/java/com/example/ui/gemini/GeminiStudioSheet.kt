package com.example.ui.gemini

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.ai.ChatMessage
import com.example.data.auth.AuthState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiStudioSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    sketchName: String,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    streamingText: String,
    errorMessage: String?,
    authState: AuthState = AuthState.Idle,
    onSendMessage: (String) -> Unit,
    onExplainSketch: () -> Unit,
    onFixErrors: () -> Unit,
    onGenerateWiring: () -> Unit,
    onAnalyzeSerial: () -> Unit,
    onApplyCodeToEditor: (String) -> Unit,
    onViewChatFile: () -> Unit,
    onClearChat: () -> Unit,
    onSignInClicked: (Activity) -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IdeBackground,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        GeminiStudioChatView(
            sketchName = sketchName,
            messages = messages,
            isGenerating = isGenerating,
            streamingText = streamingText,
            errorMessage = errorMessage,
            authState = authState,
            onSendMessage = onSendMessage,
            onExplainSketch = onExplainSketch,
            onFixErrors = onFixErrors,
            onGenerateWiring = onGenerateWiring,
            onAnalyzeSerial = onAnalyzeSerial,
            onApplyCodeToEditor = onApplyCodeToEditor,
            onViewChatFile = onViewChatFile,
            onClearChat = onClearChat,
            onSignInClicked = onSignInClicked,
            onClose = onDismiss,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun GeminiStudioChatView(
    sketchName: String,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    streamingText: String,
    errorMessage: String?,
    authState: AuthState = AuthState.Idle,
    onSendMessage: (String) -> Unit,
    onExplainSketch: () -> Unit,
    onFixErrors: () -> Unit,
    onGenerateWiring: () -> Unit,
    onAnalyzeSerial: () -> Unit,
    onApplyCodeToEditor: (String) -> Unit,
    onViewChatFile: () -> Unit,
    onClearChat: () -> Unit,
    onSignInClicked: (Activity) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = modifier
            .background(IdeBackground)
            .padding(top = 4.dp)
    ) {
        // Header: Studio Bot Brand, Chat File Badge & Close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(GeminiGradientStart, GeminiGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Gemini in Studio",
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = GeminiPurple.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "STUDIO BOT",
                            color = GeminiCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Project File: ${sketchName}.chat.md",
                    color = ArduinoTealBright,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // View Chat File Button
            IconButton(
                onClick = onViewChatFile,
                modifier = Modifier.testTag("view_chat_file_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "View Chat File",
                    tint = ArduinoTealBright,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("gemini_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = IdeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Project Chat indicator banner
        Surface(
            color = IdeSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💬 Chat scoped to: ",
                        color = IdeTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = sketchName,
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onViewChatFile,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Export .md", color = ArduinoTealBright, fontSize = 10.sp)
                    }
                    TextButton(
                        onClick = onClearChat,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Clear", color = StatusError, fontSize = 10.sp)
                    }
                }
            }
        }

        // Sign In with Google Prompt Banner (if not authenticated)
        if (authState !is AuthState.Authenticated) {
            Surface(
                color = IdeSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, ArduinoTeal.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔐 Sign in with Google",
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Unlock Gemini AI responses & cloud project chat backup",
                            color = IdeTextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    ElevatedButton(
                        onClick = {
                            if (activity != null) {
                                onSignInClicked(activity)
                            }
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F1F1F)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("gemini_sign_in_button")
                    ) {
                        Text(
                            text = "G Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Quick Prompt Chips
        val quickScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(quickScroll)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionChip("⚡ Explain Sketch", onClick = onExplainSketch)
            QuickActionChip("🛠️ Fix Errors", onClick = onFixErrors)
            QuickActionChip("🔌 Wiring Guide", onClick = onGenerateWiring)
            QuickActionChip("🔌 USBtinyISP ICSP Guide", onClick = {
                onSendMessage("Как подключить программатор USBtinyISP к разъему ICSP (6 пин) платы Arduino и прошить скетч или записать загрузчик через avrdude?")
            })
            QuickActionChip("📊 Analyze Serial", onClick = onAnalyzeSerial)
            QuickActionChip("💡 Add Ultrasonic Sensor", onClick = {
                onSendMessage("Add code for HC-SR04 ultrasonic distance sensor with obstacle threshold beep to this sketch.")
            })
            QuickActionChip("💡 Add ESP32 Wi-Fi", onClick = {
                onSendMessage("Add ESP32 Wi-Fi connection and simple REST endpoint /status to control LED in this sketch.")
            })
            QuickActionChip("🚗 GAZ-24 Tachometer", onClick = {
                onSendMessage("Explain how the GAZ-24 Volga tachometer interrupt code works for ZMZ-402 engine RPM and shift light.")
            })
        }

        if (isGenerating) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = GeminiCyan,
                trackColor = IdeSurfaceVariant
            )
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    message = msg,
                    onApplyCode = onApplyCodeToEditor
                )
            }

            // Live streaming bubble
            if (isGenerating && streamingText.isNotEmpty()) {
                item(key = "streaming_bubble") {
                    StreamingMessageItem(text = streamingText)
                }
            }

            // Error display
            if (errorMessage != null) {
                item(key = "error_bubble") {
                    ErrorMessageItem(errorMessage)
                }
            }
        }

        // Bottom Chat Input Bar
        Surface(
            color = IdeSurface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask Gemini about $sketchName...",
                            color = IdeTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArduinoTeal,
                        unfocusedBorderColor = IdeBorder,
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary,
                        cursorColor = ArduinoTealBright
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gemini_input_field"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isGenerating) ArduinoTeal
                            else IdeSurfaceVariant
                        )
                        .testTag("gemini_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isGenerating) Color.White else IdeTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = IdeSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder)
    ) {
        Text(
            text = label,
            color = IdeTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onApplyCode: (String) -> Unit
) {
    val isUser = message.isUser
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(GeminiGradientStart, GeminiGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isUser) 12.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 12.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) ArduinoTeal.copy(alpha = 0.25f) else IdeSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) ArduinoTeal.copy(alpha = 0.5f) else IdeBorder
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = IdeTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // Extracted Code Action Bar (Apply to Editor / Copy)
                    if (message.extractedCode != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IdeBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generated Arduino Code",
                                        color = ArduinoTealBright,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Arduino Code", message.extractedCode))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = IdeTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = message.extractedCode.lines().take(4).joinToString("\n") + if (message.extractedCode.lines().size > 4) "\n..." else "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = IdeTextSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                ElevatedButton(
                                    onClick = { onApplyCode(message.extractedCode) },
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = ArduinoTeal,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("apply_code_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply Code to Editor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingMessageItem(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(GeminiGradientStart, GeminiGradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = IdeSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeminiCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = text,
                    color = IdeTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ErrorMessageItem(error: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️ $error",
                color = StatusError,
                fontSize = 12.sp
            )
        }
    }
}
