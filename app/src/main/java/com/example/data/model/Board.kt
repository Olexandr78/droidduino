package com.example.data.model

enum class PinType {
    DIGITAL,
    ANALOG,
    PWM,
    POWER,
    GND,
    I2C,
    SPI,
    UART
}

data class BoardPin(
    val name: String,
    val label: String,
    val type: PinType,
    val description: String,
    val isPwm: Boolean = false,
    val extraFunctions: String = ""
)

data class Board(
    val id: String,
    val name: String,
    val mcu: String,
    val clockSpeed: String,
    val flashBytes: Long,
    val sramBytes: Long,
    val operatingVoltage: String,
    val digitalPinsCount: Int,
    val analogPinsCount: Int,
    val pwmPinsCount: Int,
    val features: List<String>,
    val pins: List<BoardPin>
)

object SupportedBoards {
    val ARDUINO_UNO = Board(
        id = "uno",
        name = "Arduino Uno R3",
        mcu = "ATmega328P",
        clockSpeed = "16 MHz",
        flashBytes = 32L * 1024L,
        sramBytes = 2L * 1024L,
        operatingVoltage = "5V",
        digitalPinsCount = 14,
        analogPinsCount = 6,
        pwmPinsCount = 6,
        features = listOf("USB-B Interface", "ICSP Header", "Standard Shield Form Factor", "10-bit ADC"),
        pins = listOf(
            BoardPin("D0", "RX0", PinType.UART, "Serial Receive pin", extraFunctions = "USART RX"),
            BoardPin("D1", "TX0", PinType.UART, "Serial Transmit pin", extraFunctions = "USART TX"),
            BoardPin("D2", "INT0", PinType.DIGITAL, "Digital I/O, External Interrupt 0"),
            BoardPin("D3", "~PWM", PinType.PWM, "Digital I/O with 8-bit Timer PWM, INT1", isPwm = true),
            BoardPin("D4", "D4", PinType.DIGITAL, "Digital I/O"),
            BoardPin("D5", "~PWM", PinType.PWM, "Digital I/O with 8-bit Timer PWM", isPwm = true),
            BoardPin("D6", "~PWM", PinType.PWM, "Digital I/O with 8-bit Timer PWM", isPwm = true),
            BoardPin("D7", "D7", PinType.DIGITAL, "Digital I/O"),
            BoardPin("D8", "D8", PinType.DIGITAL, "Digital I/O"),
            BoardPin("D9", "~PWM", PinType.PWM, "Digital I/O with 16-bit Timer PWM", isPwm = true),
            BoardPin("D10", "~SS", PinType.SPI, "SPI Slave Select / PWM", isPwm = true, extraFunctions = "SPI SS"),
            BoardPin("D11", "~MOSI", PinType.SPI, "SPI Master Out / PWM", isPwm = true, extraFunctions = "SPI MOSI"),
            BoardPin("D12", "MISO", PinType.SPI, "SPI Master In", extraFunctions = "SPI MISO"),
            BoardPin("D13", "SCK", PinType.SPI, "SPI Clock & Builtin LED (LED_BUILTIN)", extraFunctions = "SPI SCK / LED"),
            BoardPin("A0", "A0", PinType.ANALOG, "Analog Input 0 (10-bit ADC)"),
            BoardPin("A1", "A1", PinType.ANALOG, "Analog Input 1 (10-bit ADC)"),
            BoardPin("A2", "A2", PinType.ANALOG, "Analog Input 2 (10-bit ADC)"),
            BoardPin("A3", "A3", PinType.ANALOG, "Analog Input 3 (10-bit ADC)"),
            BoardPin("A4", "SDA", PinType.I2C, "Analog Input 4 & I2C Data", extraFunctions = "I2C SDA"),
            BoardPin("A5", "SCL", PinType.I2C, "Analog Input 5 & I2C Clock", extraFunctions = "I2C SCL"),
            BoardPin("5V", "5V", PinType.POWER, "Regulated 5 Volt Power output"),
            BoardPin("3.3V", "3.3V", PinType.POWER, "Regulated 3.3 Volt Power output (50mA max)"),
            BoardPin("GND", "GND", PinType.GND, "Ground Reference (0V)"),
            BoardPin("VIN", "VIN", PinType.POWER, "External input voltage (7-12V)")
        )
    )

    val ARDUINO_NANO = Board(
        id = "nano",
        name = "Arduino Nano",
        mcu = "ATmega328P",
        clockSpeed = "16 MHz",
        flashBytes = 32L * 1024L,
        sramBytes = 2L * 1024L,
        operatingVoltage = "5V",
        digitalPinsCount = 14,
        analogPinsCount = 8,
        pwmPinsCount = 6,
        features = listOf("Breadboard Friendly", "Mini-USB / Type-C", "8 Analog Inputs (A0-A7)"),
        pins = ARDUINO_UNO.pins + listOf(
            BoardPin("A6", "A6", PinType.ANALOG, "Analog Input only (no digital)"),
            BoardPin("A7", "A7", PinType.ANALOG, "Analog Input only (no digital)")
        )
    )

    val ESP32_DEVKIT = Board(
        id = "esp32",
        name = "ESP32 DevKit V1",
        mcu = "Tensilica Xtensa Dual-Core 32-bit LX6",
        clockSpeed = "240 MHz",
        flashBytes = 4L * 1024L * 1024L,
        sramBytes = 520L * 1024L,
        operatingVoltage = "3.3V (5V via USB)",
        digitalPinsCount = 28,
        analogPinsCount = 16,
        pwmPinsCount = 16,
        features = listOf("Wi-Fi 802.11 b/g/n", "Bluetooth 4.2 BLE", "FreeRTOS", "Capacitive Touch Pins", "DAC / ADC"),
        pins = listOf(
            BoardPin("GPIO2", "LED", PinType.PWM, "Onboard Blue LED & Bootstrap pin", isPwm = true),
            BoardPin("GPIO4", "D4", PinType.DIGITAL, "ADC2_CH0, Touch 0", isPwm = true),
            BoardPin("GPIO5", "CS", PinType.SPI, "VSPI Chip Select", isPwm = true),
            BoardPin("GPIO12", "MTDI", PinType.DIGITAL, "Touch 5, Boot config (caution at boot)"),
            BoardPin("GPIO13", "MTCK", PinType.DIGITAL, "Touch 4, ADC2_CH4"),
            BoardPin("GPIO14", "MTMS", PinType.DIGITAL, "Touch 6, ADC2_CH6"),
            BoardPin("GPIO15", "MTDO", PinType.DIGITAL, "Touch 3, Boot debug output"),
            BoardPin("GPIO18", "SCK", PinType.SPI, "VSPI Clock", isPwm = true),
            BoardPin("GPIO19", "MISO", PinType.SPI, "VSPI Master In", isPwm = true),
            BoardPin("GPIO21", "SDA", PinType.I2C, "Default I2C Data wire"),
            BoardPin("GPIO22", "SCL", PinType.I2C, "Default I2C Clock wire"),
            BoardPin("GPIO23", "MOSI", PinType.SPI, "VSPI Master Out", isPwm = true),
            BoardPin("GPIO25", "DAC1", PinType.ANALOG, "8-bit Digital-to-Analog Converter 1"),
            BoardPin("GPIO26", "DAC2", PinType.ANALOG, "8-bit Digital-to-Analog Converter 2"),
            BoardPin("GPIO32", "ADC4", PinType.ANALOG, "ADC1_CH4 (usable with Wi-Fi on)"),
            BoardPin("GPIO33", "ADC5", PinType.ANALOG, "ADC1_CH5 (usable with Wi-Fi on)"),
            BoardPin("GPIO34", "IN34", PinType.ANALOG, "Input ONLY (no pullup/pulldown)"),
            BoardPin("GPIO35", "IN35", PinType.ANALOG, "Input ONLY (no pullup/pulldown)"),
            BoardPin("GPIO36", "VP", PinType.ANALOG, "Sensor VP (ADC1_CH0, Input ONLY)"),
            BoardPin("GPIO39", "VN", PinType.ANALOG, "Sensor VN (ADC1_CH3, Input ONLY)"),
            BoardPin("3V3", "3.3V", PinType.POWER, "3.3V Power Out"),
            BoardPin("VIN", "5V", PinType.POWER, "5V Power In / USB Power"),
            BoardPin("GND", "GND", PinType.GND, "Ground (0V)")
        )
    )

    val ARDUINO_MEGA = Board(
        id = "mega",
        name = "Arduino Mega 2560",
        mcu = "ATmega2560",
        clockSpeed = "16 MHz",
        flashBytes = 256L * 1024L,
        sramBytes = 8L * 1024L,
        operatingVoltage = "5V",
        digitalPinsCount = 54,
        analogPinsCount = 16,
        pwmPinsCount = 15,
        features = listOf("4 Hardware Serial Ports", "16 Analog Inputs", "54 Digital I/O", "Ideal for 3D Printers (Marlin) & Robotics"),
        pins = ARDUINO_UNO.pins + listOf(
            BoardPin("TX1/RX1", "Serial1", PinType.UART, "Hardware Serial Port 1 (Pins 18, 19)"),
            BoardPin("TX2/RX2", "Serial2", PinType.UART, "Hardware Serial Port 2 (Pins 16, 17)"),
            BoardPin("TX3/RX3", "Serial3", PinType.UART, "Hardware Serial Port 3 (Pins 14, 15)"),
            BoardPin("D20/SDA", "SDA", PinType.I2C, "Dedicated I2C Data"),
            BoardPin("D21/SCL", "SCL", PinType.I2C, "Dedicated I2C Clock")
        )
    )

    val RASPBERRY_PI_PICO = Board(
        id = "pico",
        name = "Raspberry Pi Pico (RP2040)",
        mcu = "RP2040 Dual ARM Cortex-M0+",
        clockSpeed = "133 MHz",
        flashBytes = 2L * 1024L * 1024L,
        sramBytes = 264L * 1024L,
        operatingVoltage = "3.3V",
        digitalPinsCount = 26,
        analogPinsCount = 3,
        pwmPinsCount = 16,
        features = listOf("Programmable I/O (PIO)", "Dual-Core ARM", "High-Speed USB 1.1", "Native C++/MicroPython"),
        pins = listOf(
            BoardPin("GP0", "TX0", PinType.UART, "UART0 TX / I2C0 SDA"),
            BoardPin("GP1", "RX0", PinType.UART, "UART0 RX / I2C0 SCL"),
            BoardPin("GP4", "SDA", PinType.I2C, "I2C0 SDA"),
            BoardPin("GP5", "SCL", PinType.I2C, "I2C0 SCL"),
            BoardPin("GP16", "MISO", PinType.SPI, "SPI0 RX"),
            BoardPin("GP17", "CS", PinType.SPI, "SPI0 CS"),
            BoardPin("GP18", "SCK", PinType.SPI, "SPI0 SCK"),
            BoardPin("GP19", "MOSI", PinType.SPI, "SPI0 TX"),
            BoardPin("GP25", "LED", PinType.DIGITAL, "Onboard green LED"),
            BoardPin("GP26", "A0", PinType.ANALOG, "ADC0 (12-bit)"),
            BoardPin("GP27", "A1", PinType.ANALOG, "ADC1 (12-bit)"),
            BoardPin("GP28", "A2", PinType.ANALOG, "ADC2 (12-bit)"),
            BoardPin("3V3", "3.3V", PinType.POWER, "3.3V Power Out"),
            BoardPin("GND", "GND", PinType.GND, "Ground Reference")
        )
    )

    val ESP8266_NODEMCU = Board(
        id = "esp8266",
        name = "NodeMCU V3 (ESP8266)",
        mcu = "Tensilica L106 32-bit",
        clockSpeed = "80 / 160 MHz",
        flashBytes = 4L * 1024L * 1024L,
        sramBytes = 80L * 1024L,
        operatingVoltage = "3.3V (5V via USB)",
        digitalPinsCount = 11,
        analogPinsCount = 1,
        pwmPinsCount = 10,
        features = listOf("Wi-Fi 802.11 b/g/n", "Integrated TCP/IP stack", "Breadboard Friendly", "Low Cost IoT"),
        pins = listOf(
            BoardPin("D0", "GPIO16", PinType.DIGITAL, "WAKE pin for deep sleep recovery"),
            BoardPin("D1", "GPIO5", PinType.I2C, "I2C SCL (Clock)", extraFunctions = "I2C SCL"),
            BoardPin("D2", "GPIO4", PinType.I2C, "I2C SDA (Data)", extraFunctions = "I2C SDA"),
            BoardPin("D3", "GPIO0", PinType.DIGITAL, "FLASH button / Bootloader mode at boot"),
            BoardPin("D4", "GPIO2", PinType.PWM, "TX1 & Onboard Blue LED (inverted)", isPwm = true),
            BoardPin("D5", "GPIO14", PinType.SPI, "HSPI SCK Clock", isPwm = true, extraFunctions = "SPI SCK"),
            BoardPin("D6", "GPIO12", PinType.SPI, "HSPI MISO", isPwm = true, extraFunctions = "SPI MISO"),
            BoardPin("D7", "GPIO13", PinType.SPI, "HSPI MOSI / RXD2", isPwm = true, extraFunctions = "SPI MOSI"),
            BoardPin("D8", "GPIO15", PinType.SPI, "HSPI CS (Must be LOW at boot)", isPwm = true, extraFunctions = "SPI CS"),
            BoardPin("RX", "GPIO3", PinType.UART, "UART0 RX (Serial Receive)"),
            BoardPin("TX", "GPIO1", PinType.UART, "UART0 TX (Serial Transmit)"),
            BoardPin("A0", "ADC0", PinType.ANALOG, "Analog Input (1.0V max input range!)"),
            BoardPin("3V3", "3.3V", PinType.POWER, "3.3V Regulated Output"),
            BoardPin("VIN", "5V", PinType.POWER, "External 5V Power Input"),
            BoardPin("GND", "GND", PinType.GND, "Ground (0V)")
        )
    )

    val STM32_BLUE_PILL = Board(
        id = "stm32",
        name = "STM32 Blue Pill",
        mcu = "STM32F103C8T6 ARM Cortex-M3",
        clockSpeed = "72 MHz",
        flashBytes = 64L * 1024L,
        sramBytes = 20L * 1024L,
        operatingVoltage = "3.3V (5V tolerant pins)",
        digitalPinsCount = 32,
        analogPinsCount = 10,
        pwmPinsCount = 15,
        features = listOf("32-bit ARM Cortex-M3", "USB Bootloader support", "2x I2C, 2x SPI, 3x USART", "SWD Debug Header"),
        pins = listOf(
            BoardPin("PA0", "WKUP", PinType.PWM, "ADC1_IN0, PWM Timer 2 Ch1", isPwm = true),
            BoardPin("PA1", "PA1", PinType.PWM, "ADC1_IN1, PWM Timer 2 Ch2", isPwm = true),
            BoardPin("PA2", "TX2", PinType.UART, "USART2 TX, ADC1_IN2"),
            BoardPin("PA3", "RX2", PinType.UART, "USART2 RX, ADC1_IN3"),
            BoardPin("PA4", "NSS1", PinType.SPI, "SPI1 NSS / ADC1_IN4"),
            BoardPin("PA5", "SCK1", PinType.SPI, "SPI1 SCK / ADC1_IN5"),
            BoardPin("PA6", "MISO1", PinType.SPI, "SPI1 MISO / PWM", isPwm = true),
            BoardPin("PA7", "MOSI1", PinType.SPI, "SPI1 MOSI / PWM", isPwm = true),
            BoardPin("PA9", "TX1", PinType.UART, "USART1 TX (5V Tolerant)"),
            BoardPin("PA10", "RX1", PinType.UART, "USART1 RX (5V Tolerant)"),
            BoardPin("PB6", "SCL1", PinType.I2C, "I2C1 SCL (5V Tolerant)"),
            BoardPin("PB7", "SDA1", PinType.I2C, "I2C1 SDA (5V Tolerant)"),
            BoardPin("PC13", "LED", PinType.DIGITAL, "Onboard Green LED (Active LOW)"),
            BoardPin("3V3", "3.3V", PinType.POWER, "3.3V Power Out"),
            BoardPin("5V", "5V", PinType.POWER, "5V USB Power Input/Output"),
            BoardPin("GND", "GND", PinType.GND, "Ground Reference (0V)")
        )
    )

    val ARDUINO_LEONARDO = Board(
        id = "leonardo",
        name = "Arduino Leonardo / Pro Micro",
        mcu = "ATmega32U4",
        clockSpeed = "16 MHz",
        flashBytes = 32L * 1024L,
        sramBytes = 2560L,
        operatingVoltage = "5V",
        digitalPinsCount = 20,
        analogPinsCount = 12,
        pwmPinsCount = 7,
        features = listOf("Native USB CDC/HID", "Emulates Keyboard & Mouse", "No FTDI/CH340 chip needed", "Ideal for custom Gamepads & Macro-pads"),
        pins = listOf(
            BoardPin("D0", "RX", PinType.UART, "Hardware Serial1 RX"),
            BoardPin("D1", "TX", PinType.UART, "Hardware Serial1 TX"),
            BoardPin("D2", "SDA", PinType.I2C, "Dedicated I2C Data (INT1)"),
            BoardPin("D3", "SCL", PinType.I2C, "Dedicated I2C Clock (INT0, ~PWM)", isPwm = true),
            BoardPin("D4", "A6", PinType.ANALOG, "Digital I/O & Analog Input 6"),
            BoardPin("D5", "~PWM", PinType.PWM, "Digital I/O & Timer PWM", isPwm = true),
            BoardPin("D6", "~A7", PinType.PWM, "Digital I/O, Analog Input 7 & PWM", isPwm = true),
            BoardPin("D7", "INT4", PinType.DIGITAL, "Digital I/O, External Interrupt 4"),
            BoardPin("D8", "A8", PinType.ANALOG, "Digital I/O & Analog Input 8"),
            BoardPin("D9", "~A9", PinType.PWM, "Digital I/O, Analog Input 9 & PWM", isPwm = true),
            BoardPin("D10", "~A10", PinType.PWM, "Digital I/O, Analog Input 10 & PWM", isPwm = true),
            BoardPin("D11", "~PWM", PinType.PWM, "Digital I/O with PWM", isPwm = true),
            BoardPin("D12", "A11", PinType.ANALOG, "Digital I/O & Analog Input 11"),
            BoardPin("D13", "~LED", PinType.PWM, "Built-in LED & PWM output", isPwm = true),
            BoardPin("A0", "A0", PinType.ANALOG, "10-bit Analog Input 0"),
            BoardPin("A1", "A1", PinType.ANALOG, "10-bit Analog Input 1"),
            BoardPin("A2", "A2", PinType.ANALOG, "10-bit Analog Input 2"),
            BoardPin("A3", "A3", PinType.ANALOG, "10-bit Analog Input 3"),
            BoardPin("5V", "5V", PinType.POWER, "Regulated 5V Power"),
            BoardPin("3.3V", "3.3V", PinType.POWER, "3.3V Output"),
            BoardPin("GND", "GND", PinType.GND, "Ground Reference")
        )
    )

    val ESP32_S3 = Board(
        id = "esp32_s3",
        name = "ESP32-S3 DevKit",
        mcu = "Xtensa LX7 Dual-Core 32-bit with Vector AI",
        clockSpeed = "240 MHz",
        flashBytes = 8L * 1024L * 1024L,
        sramBytes = 512L * 1024L,
        operatingVoltage = "3.3V (5V via USB)",
        digitalPinsCount = 36,
        analogPinsCount = 20,
        pwmPinsCount = 16,
        features = listOf("Vector Instructions for AI / NN", "Native USB OTG (Full Speed)", "Bluetooth 5 (LE) & Mesh", "8MB Octal Flash + 2MB PSRAM"),
        pins = listOf(
            BoardPin("GPIO0", "BOOT", PinType.DIGITAL, "Boot mode button & strapping pin"),
            BoardPin("GPIO1", "ADC1_0", PinType.ANALOG, "ADC1 Channel 0, Touch 1"),
            BoardPin("GPIO2", "ADC1_1", PinType.ANALOG, "ADC1 Channel 1, Touch 2"),
            BoardPin("GPIO4", "SDA", PinType.I2C, "Default I2C SDA data pin"),
            BoardPin("GPIO5", "SCL", PinType.I2C, "Default I2C SCL clock pin"),
            BoardPin("GPIO10", "MOSI", PinType.SPI, "FSPI MOSI Data out", isPwm = true),
            BoardPin("GPIO11", "MISO", PinType.SPI, "FSPI MISO Data in", isPwm = true),
            BoardPin("GPIO12", "SCK", PinType.SPI, "FSPI SCK Clock", isPwm = true),
            BoardPin("GPIO13", "CS", PinType.SPI, "FSPI Chip Select", isPwm = true),
            BoardPin("GPIO19", "USB D-", PinType.UART, "Native USB OTG D- (Direct USB CDC)"),
            BoardPin("GPIO20", "USB D+", PinType.UART, "Native USB OTG D+ (Direct USB CDC)"),
            BoardPin("GPIO38", "RGB_LED", PinType.PWM, "Onboard Addressable WS2812 RGB LED", isPwm = true),
            BoardPin("GPIO43", "TX0", PinType.UART, "UART0 TX (Debug Serial)"),
            BoardPin("GPIO44", "RX0", PinType.UART, "UART0 RX (Debug Serial)"),
            BoardPin("3V3", "3.3V", PinType.POWER, "3.3V Regulated Output"),
            BoardPin("5V", "5V", PinType.POWER, "5V USB Power"),
            BoardPin("GND", "GND", PinType.GND, "Ground Reference")
        )
    )

    val ESP32_C3 = Board(
        id = "esp32_c3",
        name = "ESP32-C3 SuperMini",
        mcu = "RISC-V 32-bit Single-Core RV32IMC",
        clockSpeed = "160 MHz",
        flashBytes = 4L * 1024L * 1024L,
        sramBytes = 400L * 1024L,
        operatingVoltage = "3.3V",
        digitalPinsCount = 15,
        analogPinsCount = 6,
        pwmPinsCount = 6,
        features = listOf("Open-Source RISC-V Architecture", "Wi-Fi 4 + Bluetooth 5 (LE)", "Ultra-Low Power Deep Sleep", "Thumb-sized Form Factor"),
        pins = listOf(
            BoardPin("GPIO0", "ADC1_CH0", PinType.ANALOG, "Analog ADC1 Ch0 / XTAL"),
            BoardPin("GPIO1", "ADC1_CH1", PinType.ANALOG, "Analog ADC1 Ch1"),
            BoardPin("GPIO2", "LED", PinType.PWM, "Onboard Blue LED / Strapping pin", isPwm = true),
            BoardPin("GPIO3", "ADC1_CH3", PinType.ANALOG, "Analog ADC1 Ch3"),
            BoardPin("GPIO4", "FSPI_HD", PinType.SPI, "SPI Hold"),
            BoardPin("GPIO5", "FSPI_WP", PinType.SPI, "SPI Write Protect"),
            BoardPin("GPIO6", "FSPI_CLK", PinType.SPI, "SPI Clock"),
            BoardPin("GPIO7", "FSPI_Q", PinType.SPI, "SPI MISO"),
            BoardPin("GPIO8", "SDA", PinType.I2C, "Default I2C Data wire"),
            BoardPin("GPIO9", "SCL", PinType.I2C, "Default I2C Clock wire / BOOT button"),
            BoardPin("GPIO10", "FSPI_CS", PinType.SPI, "SPI Chip Select"),
            BoardPin("GPIO20", "RX", PinType.UART, "UART0 RX"),
            BoardPin("GPIO21", "TX", PinType.UART, "UART0 TX"),
            BoardPin("3V3", "3.3V", PinType.POWER, "3.3V Power Out"),
            BoardPin("5V", "5V", PinType.POWER, "5V USB Input"),
            BoardPin("GND", "GND", PinType.GND, "Ground Reference")
        )
    )

    val ESP32_CAM = Board(
        id = "esp32_cam",
        name = "ESP32-CAM (AI-Thinker)",
        mcu = "Xtensa Dual-Core 32-bit LX6",
        clockSpeed = "240 MHz",
        flashBytes = 4L * 1024L * 1024L,
        sramBytes = 520L * 1024L,
        operatingVoltage = "5V Power (3.3V logic)",
        digitalPinsCount = 16,
        analogPinsCount = 7,
        pwmPinsCount = 8,
        features = listOf("OV2640 2MP Camera Sensor", "MicroSD TF Card Slot", "Ultra-Bright Flash LED", "4MB External PSRAM"),
        pins = listOf(
            BoardPin("GPIO0", "BOOT", PinType.DIGITAL, "Connect to GND to flash, leave open to run"),
            BoardPin("GPIO1", "U0TXD", PinType.UART, "UART0 TX for FTDI / USB-UART"),
            BoardPin("GPIO3", "U0RXD", PinType.UART, "UART0 RX for FTDI / USB-UART"),
            BoardPin("GPIO4", "FLASH_LED", PinType.PWM, "High-power Flashlight LED & MicroSD D1", isPwm = true),
            BoardPin("GPIO12", "SD_D2", PinType.SPI, "MicroSD Data 2"),
            BoardPin("GPIO13", "SD_D3", PinType.SPI, "MicroSD Data 3"),
            BoardPin("GPIO14", "SD_CLK", PinType.SPI, "MicroSD Clock"),
            BoardPin("GPIO15", "SD_CMD", PinType.SPI, "MicroSD Command"),
            BoardPin("GPIO16", "U2RXD", PinType.UART, "UART2 RX / Camera PCLK"),
            BoardPin("GPIO33", "RED_LED", PinType.DIGITAL, "Small red indicator LED on back"),
            BoardPin("5V", "5V_IN", PinType.POWER, "Main 5V Power Input (needs min 2A for Wi-Fi/Cam)"),
            BoardPin("3V3", "3.3V", PinType.POWER, "3.3V Output"),
            BoardPin("GND", "GND", PinType.GND, "Ground (0V)")
        )
    )

    val WEMOS_D1_MINI = Board(
        id = "d1_mini",
        name = "Wemos D1 Mini (ESP8266)",
        mcu = "ESP8266 ESP-12F Tensilica L106",
        clockSpeed = "80 / 160 MHz",
        flashBytes = 4L * 1024L * 1024L,
        sramBytes = 80L * 1024L,
        operatingVoltage = "3.3V (5V via Micro-USB)",
        digitalPinsCount = 11,
        analogPinsCount = 1,
        pwmPinsCount = 10,
        features = listOf("Mini Breadboard Layout", "Integrated Wi-Fi 802.11 b/g/n", "Micro-USB + CH340", "Full Arduino IDE support"),
        pins = listOf(
            BoardPin("D0", "GPIO16", PinType.DIGITAL, "User I/O, Wake from Sleep"),
            BoardPin("D1", "GPIO5", PinType.I2C, "I2C SCL Clock", extraFunctions = "I2C SCL"),
            BoardPin("D2", "GPIO4", PinType.I2C, "I2C SDA Data", extraFunctions = "I2C SDA"),
            BoardPin("D3", "GPIO0", PinType.DIGITAL, "10k Pull-up, Flash mode if LOW"),
            BoardPin("D4", "GPIO2", PinType.PWM, "10k Pull-up, Built-in Blue LED (Active LOW)", isPwm = true),
            BoardPin("D5", "GPIO14", PinType.SPI, "SPI SCK Clock", isPwm = true, extraFunctions = "SPI SCK"),
            BoardPin("D6", "GPIO12", PinType.SPI, "SPI MISO Data In", isPwm = true, extraFunctions = "SPI MISO"),
            BoardPin("D7", "GPIO13", PinType.SPI, "SPI MOSI Data Out", isPwm = true, extraFunctions = "SPI MOSI"),
            BoardPin("D8", "GPIO15", PinType.SPI, "10k Pull-down, SPI SS (Must be LOW at boot)", isPwm = true, extraFunctions = "SPI SS"),
            BoardPin("RX", "GPIO3", PinType.UART, "UART0 RX (Serial)"),
            BoardPin("TX", "GPIO1", PinType.UART, "UART0 TX (Serial)"),
            BoardPin("A0", "ADC0", PinType.ANALOG, "Analog input 0 (Max 3.2V with internal divider)"),
            BoardPin("5V", "5V", PinType.POWER, "5V USB Power"),
            BoardPin("3V3", "3.3V", PinType.POWER, "3.3V Regulated Output"),
            BoardPin("GND", "GND", PinType.GND, "Ground Reference (0V)")
        )
    )

    val allBoards: List<Board> = listOf(
        ARDUINO_UNO,
        ESP32_DEVKIT,
        ESP32_S3,
        ESP32_C3,
        ESP32_CAM,
        ESP8266_NODEMCU,
        WEMOS_D1_MINI,
        ARDUINO_NANO,
        ARDUINO_MEGA,
        RASPBERRY_PI_PICO,
        STM32_BLUE_PILL,
        ARDUINO_LEONARDO
    )
}
