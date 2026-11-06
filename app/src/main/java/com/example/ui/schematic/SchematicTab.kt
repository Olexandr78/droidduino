package com.example.ui.schematic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Board
import com.example.data.model.PinType
import com.example.ui.theme.ArduinoTealBright
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeSurface
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary

/** Pinout and bus reference for the currently selected board. */
@Composable
fun SchematicTab(board: Board, modifier: Modifier = Modifier) {
    val buses = listOf(PinType.UART, PinType.I2C, PinType.SPI)
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = IdeSurface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Схема и pinout", color = ArduinoTealBright, style = MaterialTheme.typography.titleLarge)
                    Text(board.name, color = IdeTextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("${board.mcu} · ${board.operatingVoltage} · ${board.clockSpeed}", color = IdeTextSecondary)
                    Text("USB: подключите плату через OTG; RX/TX UART — перекрёстно, общий GND обязателен.", color = IdeTextSecondary)
                }
            }
        }
        items(buses) { type ->
            val pins = board.pins.filter { it.type == type }
            if (pins.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = IdeSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(type.name, color = ArduinoTealBright, style = MaterialTheme.typography.titleMedium)
                        pins.forEach { pin ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${pin.name} · ${pin.label}", color = IdeTextPrimary)
                                Text(pin.extraFunctions.ifBlank { pin.description }, color = IdeTextSecondary)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = IdeSurface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Все контакты", color = ArduinoTealBright, style = MaterialTheme.typography.titleMedium)
                    board.pins.forEach { pin ->
                        Text("${pin.name} (${pin.label}) — ${pin.type}: ${pin.description}", color = IdeTextSecondary)
                    }
                }
            }
        }
        item {
            Text("Проверьте напряжение логики конкретной платы перед подключением датчиков. Для 3.3V GPIO нельзя подавать 5V.", color = IdeTextSecondary)
        }
    }
}
