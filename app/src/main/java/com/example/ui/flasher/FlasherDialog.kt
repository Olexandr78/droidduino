package com.example.ui.flasher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.flasher.FlashProgress
import com.example.data.model.Board
import com.example.data.model.Programmer
import com.example.data.model.SupportedProgrammers
import com.example.ui.theme.ArduinoTeal
import com.example.ui.theme.ArduinoTealBright
import com.example.ui.theme.GeminiCyan
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
fun FlasherDialog(
    isOpen: Boolean,
    board: Board,
    selectedProgrammer: Programmer,
    flashProgress: FlashProgress?,
    isFlashing: Boolean,
    onDismiss: () -> Unit,
    onSelectProgrammer: (Programmer) -> Unit,
    onUploadSketch: () -> Unit,
    onBurnBootloader: () -> Unit,
    onAskGeminiWiring: () -> Unit
) {
    if (!isOpen) return

    val isAvr = board.mcu.contains("ATmega", ignoreCase = true) || board.id == "uno" || board.id == "nano" || board.id == "mega" || board.id == "leonardo"

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = IdeSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = null,
                        tint = ArduinoTealBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Flash & Programmer (Прошивка)",
                            color = IdeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: ${board.name} (${board.mcu})",
                            color = ArduinoTealBright,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Programmer Selector Chips
                Text(
                    text = "SELECT PROGRAMMER:",
                    color = IdeTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val progScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(progScroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (prog in SupportedProgrammers.allProgrammers) {
                        val isSel = prog.id == selectedProgrammer.id
                        Surface(
                            onClick = { onSelectProgrammer(prog) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) ArduinoTeal else IdeSurfaceVariant,
                            border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, IdeBorder)
                        ) {
                            Text(
                                text = prog.name,
                                color = if (isSel) Color.White else IdeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // USBtinyISP / ICSP 6-Pin Wiring Guide Card (if programmer is ICSP)
                if (selectedProgrammer.usesIcspHeader && isAvr) {
                    IcspWiringCard(onAskGemini = onAskGeminiWiring)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = onUploadSketch,
                        enabled = !isFlashing,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = ArduinoTeal,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_via_programmer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload via ${selectedProgrammer.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Burn Bootloader button for AVR
                    if (isAvr && selectedProgrammer.usesIcspHeader) {
                        ElevatedButton(
                            onClick = onBurnBootloader,
                            enabled = !isFlashing,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("burn_bootloader_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Burn Bootloader", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Flashing Progress Indicator & Log
                if (isFlashing || flashProgress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val progress = flashProgress?.percentage ?: 0f
                    val step = flashProgress?.currentStep ?: "Flashing in progress..."
                    val isDone = flashProgress?.isComplete ?: false
                    val isSuccess = flashProgress?.isSuccess ?: true

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = step,
                            color = if (isDone && !isSuccess) StatusError else ArduinoTealBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = IdeTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isDone && !isSuccess) StatusError else ArduinoTealBright,
                        trackColor = IdeSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Flasher Log Output Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .background(Color(0xFF06090E), RoundedCornerShape(6.dp))
                            .border(1.dp, IdeBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = flashProgress?.logOutput ?: "Initializing programmer...",
                            color = if (isDone && !isSuccess) StatusError else Color(0xFF88D49E),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = IdeTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun IcspWiringCard(onAskGemini: () -> Unit) {
    Surface(
        color = IdeSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ArduinoTeal.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔌 6-Pin ICSP Connection (USBtinyISP):",
                    color = ArduinoTealBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    onClick = onAskGemini,
                    shape = RoundedCornerShape(12.dp),
                    color = GeminiPurple.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GeminiCyan, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Help", color = GeminiCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pin 1: MISO  (D12)", color = Color(0xFFFFD54F), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("Pin 3: SCK   (D13)", color = Color(0xFF81C784), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("Pin 5: RESET (RST)", color = Color(0xFFFF8A65), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pin 2: VCC (+5V)", color = Color(0xFFE57373), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("Pin 4: MOSI (D11)", color = Color(0xFF64B5F6), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("Pin 6: GND  (0V)", color = Color(0xFFB0BEC5), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
