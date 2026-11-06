package com.example.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Board
import com.example.data.model.BoardPin
import com.example.data.model.PinType
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
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun BoardPinoutView(
    board: Board,
    onAskGeminiWiring: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<PinType?>(null) }
    var selectedPin by remember { mutableStateOf<BoardPin?>(null) }

    val filteredPins = remember(board, selectedFilter) {
        if (selectedFilter == null) board.pins
        else board.pins.filter { it.type == selectedFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Board Overview Specs Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ArduinoTeal.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = ArduinoTealBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = board.name,
                                color = IdeTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "MCU: ${board.mcu} @ ${board.clockSpeed}",
                                color = IdeTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Surface(
                            color = ArduinoTeal.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = board.operatingVoltage,
                                color = ArduinoTealBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Flash and SRAM specs
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SpecMetric(
                            label = "Flash Memory",
                            value = "${board.flashBytes / 1024} KB",
                            modifier = Modifier.weight(1f)
                        )
                        SpecMetric(
                            label = "SRAM Memory",
                            value = "${board.sramBytes / 1024} KB",
                            modifier = Modifier.weight(1f)
                        )
                        SpecMetric(
                            label = "PWM Outputs",
                            value = "${board.pwmPinsCount} pins",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ask Gemini Wiring Button
                    ElevatedButton(
                        onClick = onAskGeminiWiring,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = ArduinoTeal,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ask_gemini_wiring_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GeminiCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Full Schematic & Wiring with Gemini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Selected Pin Detail Banner (if user tapped a pin)
        if (selectedPin != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IdeSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeminiCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Selected: ${selectedPin?.name} (${selectedPin?.label})",
                                color = GeminiCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = selectedPin?.type?.name ?: "",
                                color = IdeTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedPin?.description ?: "",
                            color = IdeTextPrimary,
                            fontSize = 13.sp
                        )
                        if (!selectedPin?.extraFunctions.isNullOrEmpty()) {
                            Text(
                                text = "Protocols: ${selectedPin?.extraFunctions}",
                                color = ArduinoTealBright,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            val filterScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(filterScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipItem(
                    label = "ALL (${board.pins.size})",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                for (type in PinType.values()) {
                    val count = board.pins.count { it.type == type }
                    if (count > 0) {
                        FilterChipItem(
                            label = "${type.name} ($count)",
                            isSelected = selectedFilter == type,
                            onClick = { selectedFilter = type }
                        )
                    }
                }
            }
        }

        // Pin List
        items(filteredPins) { pin ->
            PinCard(
                pin = pin,
                isSelected = selectedPin == pin,
                onClick = { selectedPin = pin }
            )
        }
    }
}

@Composable
private fun SpecMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = IdeTextSecondary, fontSize = 10.sp)
        Text(text = value, color = IdeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ArduinoTeal else IdeSurface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, IdeBorder)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else IdeTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PinCard(
    pin: BoardPin,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typeColor = when (pin.type) {
        PinType.PWM -> StatusWarning
        PinType.ANALOG -> StatusSuccess
        PinType.I2C -> GeminiCyan
        PinType.SPI -> ArduinoTealBright
        PinType.UART -> StatusInfo
        PinType.POWER -> StatusError
        PinType.GND -> Color(0xFF6E7681)
        PinType.DIGITAL -> Color(0xFF79C0FF)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) IdeSurfaceVariant else IdeSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) GeminiCyan else IdeBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pin Name Badge
            Surface(
                color = typeColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.5f)),
                modifier = Modifier.width(62.dp)
            ) {
                Text(
                    text = pin.name,
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pin.label,
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (pin.isPwm) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "~PWM",
                            color = StatusWarning,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = pin.description,
                    color = IdeTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Text(
                text = pin.type.name,
                color = typeColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
