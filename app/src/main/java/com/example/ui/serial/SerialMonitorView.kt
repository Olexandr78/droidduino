package com.example.ui.serial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TelemetryType
import com.example.ui.theme.ArduinoTeal
import com.example.ui.theme.ArduinoTealBright
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeBorder
import com.example.ui.theme.IdeSurface
import com.example.ui.theme.IdeSurfaceVariant
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun SerialMonitorView(
    logs: List<String>,
    baudRate: Int,
    isSimulating: Boolean,
    telemetryType: TelemetryType,
    autoScroll: Boolean,
    onBaudRateChange: (Int) -> Unit,
    onToggleSimulation: () -> Unit,
    onTelemetryTypeChange: (TelemetryType) -> Unit,
    onClearLogs: () -> Unit,
    onSendCommand: (String) -> Unit,
    onAskGeminiAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var commandInput by remember { mutableStateOf("") }
    val baudRates = listOf(9600, 19200, 38400, 57600, 115200)

    LaunchedEffect(logs.size) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
    ) {
        // Controls Toolbar
        Surface(
            color = IdeSurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Row 1: Baud rate and Play/Pause and Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Baud rate selector
                    val baudScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(baudScroll),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (rate in baudRates) {
                            val isSelected = rate == baudRate
                            Surface(
                                onClick = { onBaudRateChange(rate) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) ArduinoTeal else IdeSurfaceVariant,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, IdeBorder)
                            ) {
                                Text(
                                    text = "$rate baud",
                                    color = if (isSelected) Color.White else IdeTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Stream state toggle
                    IconButton(
                        onClick = onToggleSimulation,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("serial_toggle_sim_button")
                    ) {
                        Icon(
                            imageVector = if (isSimulating) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isSimulating) "Pause" else "Resume",
                            tint = if (isSimulating) StatusWarning else StatusSuccess
                        )
                    }

                    // Clear button
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("serial_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = IdeTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Telemetry presets & Gemini button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presetScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(presetScroll),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (preset in TelemetryType.values()) {
                            val active = preset == telemetryType
                            Surface(
                                onClick = { onTelemetryTypeChange(preset) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) ArduinoTeal.copy(alpha = 0.2f) else IdeSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (active) ArduinoTeal else IdeBorder
                                )
                            ) {
                                Text(
                                    text = preset.title,
                                    color = if (active) ArduinoTealBright else IdeTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Gemini Analyze Output Button
                    ElevatedButton(
                        onClick = onAskGeminiAnalyze,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF1E2638),
                            contentColor = GeminiCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("serial_gemini_analyze_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GeminiCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Terminal Display Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF06090E))
                .padding(8.dp)
        ) {
            if (logs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Serial Monitor Idle",
                        color = IdeTextMuted,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Connected at $baudRate baud. Send commands or start telemetry.",
                        color = IdeTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs) { line ->
                        val textColor = when {
                            line.startsWith(">") -> ArduinoTealBright
                            line.contains("[!]") || line.contains("WARN") || line.contains("ERROR") -> StatusWarning
                            line.contains("[OK]") || line.contains("ONLINE") || line.contains("200 OK") -> StatusSuccess
                            line.contains("Distance:") -> GeminiCyan
                            else -> Color(0xFF88D49E) // Classic phosphor terminal green
                        }

                        Text(
                            text = line,
                            color = textColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Bottom Command Input Bar
        Surface(
            color = IdeSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = {
                        Text(
                            "Enter command (e.g. AT, STATUS, LED_ON)...",
                            color = IdeTextMuted,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArduinoTeal,
                        unfocusedBorderColor = IdeBorder,
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary,
                        focusedContainerColor = IdeBackground,
                        unfocusedContainerColor = IdeBackground
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("serial_command_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            onSendCommand(commandInput)
                            commandInput = ""
                        }
                    },
                    enabled = commandInput.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (commandInput.isNotBlank()) ArduinoTeal else IdeSurfaceVariant)
                        .testTag("serial_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (commandInput.isNotBlank()) Color.White else IdeTextMuted
                    )
                }
            }
        }
    }
}
