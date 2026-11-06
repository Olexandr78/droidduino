package com.example.data.model

object SampleSketches {
    val BLINK = Sketch(
        id = "sample_blink",
        name = "01.Blink",
        boardId = "uno",
        category = "01. Basics",
        description = "Standard LED blinking sketch with 1-second interval.",
        code = """// Standard Blink Sketch
// Turns on LED for 1 second, then off for 1 second repeatedly.

const int ledPin = LED_BUILTIN; // Pin 13 on Arduino Uno

void setup() {
  pinMode(ledPin, OUTPUT);
  Serial.begin(9600);
  Serial.println(F("[System] DroidDuino Blink started"));
}

void loop() {
  digitalWrite(ledPin, HIGH);   // turn the LED on
  Serial.println(F("LED: ON"));
  delay(1000);                  // wait for a second
  
  digitalWrite(ledPin, LOW);    // turn the LED off
  Serial.println(F("LED: OFF"));
  delay(1000);                  // wait for a second
}
""".trimIndent()
    )

    val ULTRASONIC = Sketch(
        id = "sample_ultrasonic",
        name = "02.HC-SR04_Radar",
        boardId = "uno",
        category = "02. Sensors",
        description = "HC-SR04 ultrasonic distance sensor with serial telemetry.",
        code = """// HC-SR04 Ultrasonic Distance Sensor
// VCC -> 5V, GND -> GND, TRIG -> Pin 9, ECHO -> Pin 10

const int trigPin = 9;
const int echoPin = 10;
const int buzzerPin = 6;

void setup() {
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
  pinMode(buzzerPin, OUTPUT);
  Serial.begin(115200);
  Serial.println(F("--- HC-SR04 Radar Telemetry Online ---"));
}

void loop() {
  // Clear trigger pin
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  
  // Send 10 microsecond pulse
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  
  // Read echo travel time
  long duration = pulseIn(echoPin, HIGH);
  
  // Speed of sound = 340 m/s = 0.034 cm/microsecond
  float distanceCm = duration * 0.034 / 2;
  
  Serial.print(F("Distance: "));
  Serial.print(distanceCm, 1);
  Serial.println(F(" cm"));
  
  // Beep if obstacle is closer than 15 cm
  if (distanceCm > 0 && distanceCm < 15.0) {
    digitalWrite(buzzerPin, HIGH);
  } else {
    digitalWrite(buzzerPin, LOW);
  }
  
  delay(100);
}
""".trimIndent()
    )

    val ESP32_WEB_SERVER = Sketch(
        id = "sample_esp32_server",
        name = "03.ESP32_WiFi_Server",
        boardId = "esp32",
        category = "03. Wireless & IoT",
        description = "ESP32 Wi-Fi Access Point & Web Server to control GPIO outputs.",
        code = """// ESP32 Wi-Fi Web Server
// Creates an Access Point and hosts an interactive HTML dashboard.

#include <WiFi.h>
#include <WebServer.h>

const char* ssid = "DroidDuino-ESP32";
const char* password = "retro_tech_rules";

WebServer server(80);
const int ledPin = 2; // Onboard LED on ESP32 DevKit

void handleRoot() {
  String html = "<!DOCTYPE html><html><head>";
  html += "<meta name='viewport' content='width=device-width, initial-scale=1'>";
  html += "<title>ESP32 DroidDuino</title>";
  html += "<style>body{background:#0D1117;color:#F0F6FC;font-family:sans-serif;text-align:center;padding:20px;}";
  html += ".btn{background:#00979D;color:#fff;padding:15px 30px;font-size:18px;border:none;border-radius:8px;text-decoration:none;margin:10px;display:inline-block;}</style>";
  html += "</head><body><h1>ESP32 Hardware Controller</h1>";
  html += "<p>Studio Bot & ESP32 DevKit V1 Online</p>";
  html += "<p><a class='btn' href='/led/on'>LED ON</a> <a class='btn' href='/led/off'>LED OFF</a></p>";
  html += "</body></html>";
  server.send(200, "text/html", html);
}

void handleLedOn() {
  digitalWrite(ledPin, HIGH);
  Serial.println("[ESP32] GPIO 2 set HIGH");
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleLedOff() {
  digitalWrite(ledPin, LOW);
  Serial.println("[ESP32] GPIO 2 set LOW");
  server.sendHeader("Location", "/");
  server.send(303);
}

void setup() {
  Serial.begin(115200);
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);
  
  Serial.println(F("[WiFi] Starting Access Point..."));
  WiFi.softAP(ssid, password);
  
  IPAddress IP = WiFi.softAPIP();
  Serial.print(F("[WiFi] AP IP address: "));
  Serial.println(IP);
  
  server.on("/", handleRoot);
  server.on("/led/on", handleLedOn);
  server.on("/led/off", handleLedOff);
  server.begin();
  Serial.println(F("[HTTP] Web Server started"));
}

void loop() {
  server.handleClient();
  delay(2); // Yield to FreeRTOS watchdog
}
""".trimIndent()
    )

    val GAZ24_TACHOMETER = Sketch(
        id = "sample_gaz24",
        name = "04.GAZ24_RPM_Tachometer",
        boardId = "uno",
        category = "04. Automotive & Retro",
        description = "Engine RPM meter for classic Volga GAZ-24 (ZMZ-402 4-cylinder 4-stroke).",
        code = """// GAZ-24-10 Volga Engine RPM Gauge & Shift Light
// Configured for ZMZ-402 (4 cylinders, 4-stroke).
// Ignition pulse input via optocoupler PC817 -> Pin D2 (INT0)
// 4 pulses per 2 crankshaft revolutions = 2 pulses per revolution.

const int sensorPin = 2;       // Hardware Interrupt INT0
const int shiftLightPin = 13;  // Warning indicator
const int redlineRpm = 4500;   // ZMZ-402 safe max RPM

volatile unsigned long pulseCount = 0;
unsigned long lastCalcTime = 0;
unsigned int currentRpm = 0;

void IRAM_ATTR countPulse() {
  pulseCount++;
}

void setup() {
  pinMode(sensorPin, INPUT_PULLUP);
  pinMode(shiftLightPin, OUTPUT);
  attachInterrupt(digitalPinToInterrupt(sensorPin), countPulse, FALLING);
  
  Serial.begin(115200);
  Serial.println(F("========================================"));
  Serial.println(F(" GAZ-24 Volga ZMZ-402 Tachometer V1.0 "));
  Serial.println(F(" Studio Bot Engine Diagnostics Online  "));
  Serial.println(F("========================================"));
}

void loop() {
  unsigned long now = millis();
  
  // Calculate RPM every 250 milliseconds
  if (now - lastCalcTime >= 250) {
    noInterrupts();
    unsigned long pulses = pulseCount;
    pulseCount = 0;
    interrupts();
    
    unsigned long timeElapsedMs = now - lastCalcTime;
    lastCalcTime = now;
    
    // (pulses / 2) * (60000 / timeElapsedMs)
    currentRpm = (pulses * 30000UL) / timeElapsedMs;
    
    Serial.print(F("RPM: "));
    Serial.print(currentRpm);
    
    if (currentRpm >= redlineRpm) {
      digitalWrite(shiftLightPin, HIGH);
      Serial.println(F(" [!] REDLINE WARNING [!]"));
    } else {
      digitalWrite(shiftLightPin, LOW);
      Serial.println(F(" [OK]"));
    }
  }
}
""".trimIndent()
    )

    val OLED_DASHBOARD = Sketch(
        id = "sample_oled",
        name = "05.OLED_SSD1306",
        boardId = "esp32",
        category = "02. Sensors",
        description = "I2C 128x64 OLED graphic meter with Wire library.",
        code = """// I2C 128x64 OLED Display (SSD1306)
// SDA -> GPIO 21, SCL -> GPIO 22, VCC -> 3.3V, GND -> GND

#include <Wire.h>

#define OLED_ADDR 0x3C
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64

int sensorValue = 0;

void setup() {
  Serial.begin(115200);
  Wire.begin(21, 22); // ESP32 default SDA, SCL
  Serial.println(F("[I2C] Scanning bus at 0x3C..."));
  
  // Send display initialization commands
  Wire.beginTransmission(OLED_ADDR);
  Wire.write(0x00); // Command stream
  Wire.write(0xAF); // Display ON
  byte error = Wire.endTransmission();
  
  if (error == 0) {
    Serial.println(F("[OLED] Display SSD1306 acknowledged!"));
  } else {
    Serial.println(F("[OLED] Error: Display not responding. Check wiring."));
  }
}

void loop() {
  // Read analog telemetry (e.g., potentiometer or temperature)
  sensorValue = analogRead(34);
  
  Serial.print(F("[Gauge] Raw ADC: "));
  Serial.print(sensorValue);
  Serial.print(F(" | Bar: ["));
  
  int bars = map(sensorValue, 0, 4095, 0, 20);
  for (int i = 0; i < 20; i++) {
    if (i < bars) Serial.print(F("#"));
    else Serial.print(F("."));
  }
  Serial.println(F("]"));
  
  delay(200);
}
""".trimIndent()
    )

    val SERVO_SWEEP = Sketch(
        id = "sample_servo",
        name = "06.Servo_Sweep",
        boardId = "uno",
        category = "01. Basics",
        description = "Smooth servo motor sweep 0° to 180° with PWM pin D9.",
        code = """// Servo Motor Sweep Control
// Signal -> Pin 9, VCC -> 5V, GND -> GND

#include <Servo.h>

Servo myServo;
int pos = 0;

void setup() {
  myServo.attach(9); // Attaches the servo on pin 9 to the servo object
  Serial.begin(9600);
  Serial.println(F("[Servo] DroidDuino Sweep Initialized on Pin 9"));
}

void loop() {
  // Sweep from 0 degrees to 180 degrees
  for (pos = 0; pos <= 180; pos += 5) {
    myServo.write(pos);
    Serial.print(F("Angle: "));
    Serial.println(pos);
    delay(15);
  }
  
  // Sweep back from 180 degrees to 0 degrees
  for (pos = 180; pos >= 0; pos -= 5) {
    myServo.write(pos);
    Serial.print(F("Angle: "));
    Serial.println(pos);
    delay(15);
  }
}
""".trimIndent()
    )

    val samplesList: List<Sketch> = listOf(
        BLINK,
        ULTRASONIC,
        ESP32_WEB_SERVER,
        GAZ24_TACHOMETER,
        OLED_DASHBOARD,
        SERVO_SWEEP
    )
}
