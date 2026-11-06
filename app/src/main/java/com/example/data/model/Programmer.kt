package com.example.data.model

data class Programmer(
    val id: String,
    val name: String,
    val avrdudeParam: String,
    val description: String,
    val usesIcspHeader: Boolean = true,
    val supportedMcus: List<String> = listOf("AVR", "ATmega328P", "ATmega2560", "ATmega32U4", "ATtiny")
)

object SupportedProgrammers {
    val USBTINY_ISP = Programmer(
        id = "usbtiny",
        name = "USBtinyISP",
        avrdudeParam = "-c usbtiny -P usb",
        description = "Hardware ICSP programmer (6-pin / 10-pin SPI). Bypasses bootloader, flashes raw HEX, burns bootloaders.",
        usesIcspHeader = true
    )

    val USBASP = Programmer(
        id = "usbasp",
        name = "USBasp",
        avrdudeParam = "-c usbasp -P usb",
        description = "USB in-circuit programmer for Atmel AVR controllers by Thomas Fischl.",
        usesIcspHeader = true
    )

    val ARDUINO_AS_ISP = Programmer(
        id = "arduino_isp",
        name = "Arduino as ISP",
        avrdudeParam = "-c avrisp -b 19200",
        description = "Uses another Arduino running the 'ArduinoISP' sketch to program the target board.",
        usesIcspHeader = true
    )

    val AVRISP_MKII = Programmer(
        id = "avrispmkii",
        name = "AVRISP mkII",
        avrdudeParam = "-c avrispmkII -P usb",
        description = "Official Atmel AVR In-System Programmer mkII.",
        usesIcspHeader = true
    )

    val DEFAULT_BOOTLOADER = Programmer(
        id = "bootloader",
        name = "Standard USB-Serial (Bootloader)",
        avrdudeParam = "-c arduino / esptool",
        description = "Uploads sketch via standard USB-UART bridge (CH340, CP2102, FTDI) and optiboot bootloader.",
        usesIcspHeader = false
    )

    val allProgrammers: List<Programmer> = listOf(
        USBTINY_ISP,
        USBASP,
        ARDUINO_AS_ISP,
        AVRISP_MKII,
        DEFAULT_BOOTLOADER
    )
}
