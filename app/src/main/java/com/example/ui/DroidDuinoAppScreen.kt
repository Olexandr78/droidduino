package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.ai.GeminiStudioRepository
import com.example.data.model.Board
import com.example.data.model.SupportedBoards
import com.example.data.serial.UsbSerialManager
import com.example.ui.schematic.SchematicTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppTab(val title: String) { CODE("Код"), SERIAL("Serial"), SCHEME("Схема"), GEMINI("Gemini"), FLASH("Прошивка") }

/** Small launcher shell integrating direct Gemini REST, USB serial and board schematic views. */
@Composable
fun DroidDuinoAppScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serial = remember(context) { UsbSerialManager(context.applicationContext) }
    val gemini = remember { GeminiStudioRepository() }
    val boards = SupportedBoards.allBoards
    var boardIndex by remember { mutableStateOf(0) }
    val board = boards[boardIndex]
    var tab by remember { mutableStateOf(AppTab.CODE) }
    var code by remember { mutableStateOf("""void setup() {
  Serial.begin(115200);
  pinMode(LED_BUILTIN, OUTPUT);
}

void loop() {
  digitalWrite(LED_BUILTIN, !digitalRead(LED_BUILTIN));
  Serial.println("alive");
  delay(500);
}""") }
    var devices by remember { mutableStateOf(emptyList<UsbDevice>()) }
    var connected by remember { mutableStateOf(false) }
    var command by remember { mutableStateOf("") }
    var apiPrompt by remember { mutableStateOf("") }
    var aiText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("USB не подключён") }
    val logs = remember { mutableStateListOf<String>() }

    fun addLog(text: String) {
        logs.add(text)
        while (logs.size > 500) logs.removeAt(0)
    }

    val receiver = remember(serial) {
        serial.permissionReceiver { device, granted ->
            if (device != null && granted) {
                scope.launch {
                    runCatching { serial.open(device, 115200) }
                        .onSuccess { connected = true; status = "Подключено: ${device.deviceName}"; addLog("[USB] ${device.deviceName} подключён") }
                        .onFailure { status = it.message ?: "Ошибка USB"; addLog("[ERROR] $status") }
                }
            } else {
                status = "Нет разрешения на USB"
            }
        }
    }
    DisposableEffect(context, receiver) {
        val filter = IntentFilter(UsbSerialManager.ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") context.registerReceiver(receiver, filter)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            scope.launch { serial.close() }
        }
    }

    LaunchedEffect(tab, connected) {
        if (tab == AppTab.SERIAL && connected) {
            while (true) {
                val incoming = runCatching { serial.read() }.getOrElse {
                    connected = false
                    status = it.message ?: "Serial read failed"
                    ""
                }
                if (incoming.isNotEmpty()) addLog(incoming.trimEnd())
                delay(20)
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DroidDuino", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { boardIndex = (boardIndex + 1) % boards.size }) { Text("Плата: ${board.name} ▾") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppTab.values().forEach { item -> TextButton(onClick = { tab = item }) { Text(if (tab == item) "• ${item.title}" else item.title) } }
                }
            }
            when (tab) {
                AppTab.CODE -> Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Редактор Arduino/C++ · ${board.mcu}")
                    OutlinedTextField(code, { code = it }, Modifier.fillMaxSize(), label = { Text("Sketch") })
                }
                AppTab.SERIAL -> Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { devices = serial.devices(); if (devices.isEmpty()) status = "USB-Serial устройство не найдено. Подключите OTG-адаптер." }) { Text("Найти USB") }
                        if (connected) Button(onClick = { scope.launch { serial.close(); connected = false; status = "Отключено" } }) { Text("Отключить") }
                    }
                    if (!connected) devices.forEach { device ->
                        TextButton(onClick = { if (serial.hasPermission(device)) scope.launch { runCatching { serial.open(device, 115200) }.onSuccess { connected = true; status = "Подключено: ${device.deviceName}" }.onFailure { status = it.message ?: "USB error" } } else serial.requestPermission(device) }) {
                            Text("Подключить ${device.manufacturerName ?: "USB"} (${device.vendorId}:${device.productId})")
                        }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) { items(logs) { line -> Text(line, style = MaterialTheme.typography.bodySmall) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(command, { command = it }, Modifier.weight(1f), label = { Text("Команда") }, singleLine = true)
                        Button(enabled = connected && command.isNotBlank(), onClick = { val text = command; command = ""; scope.launch { runCatching { serial.write(text + "\r\n") }.onSuccess { addLog("> $text") }.onFailure { status = it.message ?: "Write failed" } } }) { Text("Отправить") }
                    }
                    Text("USB Serial · 115200 8N1 · RX/TX идут напрямую с платы; телеметрия не симулируется.", style = MaterialTheme.typography.bodySmall)
                }
                AppTab.SCHEME -> SchematicTab(board)
                AppTab.GEMINI -> Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gemini REST API · ключ задаётся через GEMINI_API_KEY")
                    OutlinedTextField(apiPrompt, { apiPrompt = it }, Modifier.fillMaxWidth(), label = { Text("Вопрос по прошивке") })
                    Button(enabled = !busy && apiPrompt.isNotBlank(), onClick = {
                        busy = true; aiText = ""
                        scope.launch {
                            try {
                                gemini.streamChat(apiPrompt, code, board).collect { aiText = it }
                            } catch (e: Exception) { aiText = e.message ?: "Gemini request failed" }
                            finally { busy = false }
                        }
                    }) { Text(if (busy) "Запрос…" else "Спросить Gemini") }
                    Text(aiText, Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()))
                }
                AppTab.FLASH -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Прошивка через USB", style = MaterialTheme.typography.titleLarge)
                    Text("Реальный Serial уже доступен во вкладке Serial. Прошивка требует настоящего компилятора Arduino и совместимого Android flasher для выбранного MCU; приложение не должно показывать фиктивный успех. Эта сборка пока не содержит такого flasher и не записывает прошивку.")
                    Text("Для безопасной прошивки используйте Arduino IDE/CLI на ПК либо добавьте проверенный Android uploader (avrdude для AVR / esptool для ESP) и передавайте ему собранный HEX/BIN.")
                }
            }
        }
    }
}
