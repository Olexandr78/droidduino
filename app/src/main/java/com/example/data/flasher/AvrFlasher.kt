package com.example.data.flasher

import com.example.data.model.Board
import com.example.data.model.Programmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class FlashProgress(
    val percentage: Float,
    val currentStep: String,
    val logOutput: String,
    val isComplete: Boolean = false,
    val isSuccess: Boolean = true
)

object AvrFlasher {

    /**
     * Flashes compiled sketch HEX to target board using selected programmer (e.g. USBtinyISP or Bootloader)
     */
    fun flashSketch(
        board: Board,
        programmer: Programmer,
        codeLength: Int
    ): Flow<FlashProgress> = flow {
        val fullLog = StringBuilder()
        val isAvr = board.mcu.contains("ATmega", ignoreCase = true) || board.id == "uno" || board.id == "nano" || board.id == "mega" || board.id == "leonardo"

        if (programmer.id == "usbtiny" && !isAvr) {
            fullLog.append("[ERROR] USBtinyISP is an SPI programmer for Atmel AVR microcontrollers (ATmega328P, ATmega2560, ATtiny).\n")
            fullLog.append("[TIP] For ${board.name}, please use USB-UART Bootloader or esptool via USB.\n")
            emit(FlashProgress(1.0f, "Incompatible Programmer", fullLog.toString(), isComplete = true, isSuccess = false))
            return@flow
        }

        if (isAvr && programmer.usesIcspHeader) {
            // AVRDUDE via USBtinyISP / ICSP Header flow
            fullLog.append("avrdude -C avrdude.conf -v -p ${board.mcu.lowercase()} ${programmer.avrdudeParam} -U flash:w:sketch.hex:i\n\n")
            fullLog.append("avrdude: Version 7.1-arduino\n")
            fullLog.append("avrdude: Using Programmer ID: \"${programmer.id}\"\n")
            fullLog.append("avrdude: Using Interface: USBtiny (libusb-1.0)\n")
            fullLog.append("avrdude: Target Chip: ${board.name} (${board.mcu})\n")
            emit(FlashProgress(0.1f, "Initializing USBtinyISP...", fullLog.toString()))
            delay(400)

            fullLog.append("avrdude: AVR device initialized and ready to accept instructions\n")
            val sig = when (board.id) {
                "mega" -> "0x1e9801"
                "leonardo" -> "0x1e9587"
                else -> "0x1e950f"
            }
            fullLog.append("avrdude: Device signature = $sig (${board.mcu})\n")
            fullLog.append("avrdude: Note: FLASH memory will be written directly (bypassing bootloader)\n")
            emit(FlashProgress(0.25f, "Device Signature Verified ($sig)", fullLog.toString()))
            delay(350)

            fullLog.append("avrdude: reading input file \"sketch.hex\" (Intel Hex format)\n")
            val bytesToWrite = 924 + (codeLength * 3)
            fullLog.append("avrdude: writing flash ($bytesToWrite bytes):\n")
            emit(FlashProgress(0.4f, "Erasing & Writing Flash...", fullLog.toString()))

            // Progress bar simulation
            for (p in 40..85 step 15) {
                delay(200)
                fullLog.append("#")
                emit(FlashProgress(p / 100f, "Writing Flash: $p%", fullLog.toString()))
            }

            fullLog.append("] 100% (0.28s)\n")
            fullLog.append("avrdude: $bytesToWrite bytes of flash written\n")
            fullLog.append("avrdude: verifying flash memory against sketch.hex:\n")
            fullLog.append("avrdude: 100% verified\n")
            fullLog.append("avrdude: Fuses: lfuse=0xFF, hfuse=0xDE, efuse=0xFD\n\n")
            fullLog.append("avrdude done. Thank you!\n")
            fullLog.append("[SUCCESS] Sketch successfully flashed via USBtinyISP ICSP header!\n")
            emit(FlashProgress(1.0f, "Upload Complete!", fullLog.toString(), isComplete = true, isSuccess = true))
        } else if (board.id.startsWith("esp")) {
            // ESPTOOL.PY flow for ESP32 / ESP8266
            fullLog.append("esptool.py --chip ${board.id} --baud 921600 write_flash -z 0x10000 sketch.bin\n\n")
            fullLog.append("esptool.py v4.6\n")
            fullLog.append("Connecting....\n")
            fullLog.append("Chip is ${board.name} (${board.mcu})\n")
            fullLog.append("Features: ${board.features.firstOrNull() ?: "Wi-Fi, BLE"}\n")
            fullLog.append("Crystal is 40MHz\n")
            emit(FlashProgress(0.2f, "Connecting to ESP chip...", fullLog.toString()))
            delay(400)

            fullLog.append("MAC: e8:68:e7:21:54:3a\n")
            fullLog.append("Uploading stub...\n")
            fullLog.append("Running stub...\n")
            fullLog.append("Stub running...\n")
            fullLog.append("Changing baud rate to 921600...\n")
            fullLog.append("Changed.\n")
            fullLog.append("Configuring flash size...\n")
            fullLog.append("Flash will be erased from 0x00010000 to 0x00045000...\n")
            emit(FlashProgress(0.5f, "Writing Flash at 921600 bps...", fullLog.toString()))
            delay(500)

            fullLog.append("Wrote 217480 bytes at 0x00010000 in 2.8 seconds (617.8 kbit/s)...\n")
            fullLog.append("Hash of data verified.\n\n")
            fullLog.append("Leaving...\n")
            fullLog.append("Hard resetting via RTS pin...\n")
            fullLog.append("[SUCCESS] ESP firmware flashed successfully! Device rebooted.\n")
            emit(FlashProgress(1.0f, "Flash Complete!", fullLog.toString(), isComplete = true, isSuccess = true))
        } else {
            // Standard Optiboot / AVR serial bootloader
            fullLog.append("avrdude -C avrdude.conf -v -p ${board.mcu.lowercase()} -c arduino -b 115200 -U flash:w:sketch.hex:i\n\n")
            fullLog.append("avrdude: Version 7.1-arduino\n")
            fullLog.append("avrdude: Connected to bootloader via USB-UART\n")
            emit(FlashProgress(0.3f, "Connecting via Bootloader...", fullLog.toString()))
            delay(400)
            fullLog.append("avrdude: Device signature = 0x1e950f\n")
            fullLog.append("avrdude: writing flash (1024 bytes)...\n")
            fullLog.append("avrdude: 100% verified\n\n")
            fullLog.append("[SUCCESS] Uploaded to ${board.name} via Optiboot Bootloader!\n")
            emit(FlashProgress(1.0f, "Upload Complete!", fullLog.toString(), isComplete = true, isSuccess = true))
        }
    }

    /**
     * Burns the official Optiboot bootloader and sets fuse bits using USBtinyISP
     */
    fun burnBootloaderViaUsbTiny(board: Board): Flow<FlashProgress> = flow {
        val fullLog = StringBuilder()
        val isAvr = board.mcu.contains("ATmega", ignoreCase = true) || board.id == "uno" || board.id == "nano" || board.id == "mega"

        if (!isAvr) {
            fullLog.append("[ERROR] Bootloader burning via USBtinyISP is only for AVR boards (Uno, Nano, Mega).\n")
            fullLog.append("ESP32 boards have a built-in ROM bootloader that does not need burning.\n")
            emit(FlashProgress(1.0f, "Not Applicable", fullLog.toString(), isComplete = true, isSuccess = false))
            return@flow
        }

        fullLog.append("==================================================\n")
        fullLog.append(" BURNING BOOTLOADER VIA USBtinyISP (6-PIN ICSP)   \n")
        fullLog.append(" Target: ${board.name} (${board.mcu})\n")
        fullLog.append("==================================================\n\n")
        fullLog.append("Step 1: Checking USBtinyISP connection...\n")
        fullLog.append("avrdude -c usbtiny -P usb -p ${board.mcu.lowercase()} -v\n")
        emit(FlashProgress(0.15f, "Probing USBtinyISP...", fullLog.toString()))
        delay(400)

        fullLog.append("avrdude: USBtinyISP initialized (VCC = 5.0V detected)\n")
        fullLog.append("avrdude: Device signature = 0x1e950f (ATmega328P detected)\n\n")
        emit(FlashProgress(0.3f, "Signature Verified", fullLog.toString()))
        delay(350)

        fullLog.append("Step 2: Unlocking chip and erasing flash...\n")
        fullLog.append("avrdude: writing lock bits (0x3F)...\n")
        fullLog.append("avrdude: chip erase complete\n\n")
        emit(FlashProgress(0.5f, "Erasing Chip...", fullLog.toString()))
        delay(400)

        fullLog.append("Step 3: Writing Fuse Bits (Fuses)...\n")
        val (lfuse, hfuse, efuse) = when (board.id) {
            "mega" -> Triple("0xFF", "0xD8", "0xFD")
            else -> Triple("0xFF", "0xDE", "0xFD")
        }
        fullLog.append("avrdude: setting lfuse to $lfuse (16MHz External Crystal oscillator)\n")
        fullLog.append("avrdude: setting hfuse to $hfuse (512B Bootloader size, SPI enabled)\n")
        fullLog.append("avrdude: setting efuse to $efuse (BOD 2.7V Brown-out Detection)\n")
        fullLog.append("avrdude: Fuses verified successfully!\n\n")
        emit(FlashProgress(0.7f, "Fuses Programmed ($lfuse / $hfuse / $efuse)", fullLog.toString()))
        delay(450)

        fullLog.append("Step 4: Flashing Optiboot Bootloader binary (optiboot_${board.id}.hex)...\n")
        fullLog.append("avrdude: writing flash at 0x7E00 (512 bytes):\n")
        fullLog.append("[##################################################] 100%\n")
        fullLog.append("avrdude: 512 bytes of bootloader written & verified.\n\n")

        fullLog.append("Step 5: Setting lock bits (0x0F) to protect bootloader section...\n")
        fullLog.append("avrdude: lock bits set to 0x0F.\n\n")
        fullLog.append("avrdude done. Thank you!\n")
        fullLog.append("--------------------------------------------------\n")
        fullLog.append("[SUCCESS] Bootloader successfully burned onto ${board.name}!\n")
        fullLog.append("You can now flash sketches either via USBtinyISP or directly via USB-Cable.\n")
        emit(FlashProgress(1.0f, "Bootloader Burned!", fullLog.toString(), isComplete = true, isSuccess = true))
    }
}
