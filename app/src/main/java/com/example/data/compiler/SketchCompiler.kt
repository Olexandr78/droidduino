package com.example.data.compiler

import com.example.data.model.Board
import com.example.data.model.CompilationResult
import com.example.data.model.CompilerMessage
import java.util.Stack

object SketchCompiler {

    fun compile(code: String, board: Board): CompilationResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<CompilerMessage>()
        val warnings = mutableListOf<CompilerMessage>()
        val lines = code.lines()

        // 1. Check for empty code
        if (code.trim().isEmpty()) {
            errors.add(CompilerMessage(1, 1, true, "Empty sketch: code must define setup() and loop()"))
            return CompilationResult(
                success = false,
                rawLog = "avr-g++: fatal error: no input files provided\ncompilation terminated.",
                flashUsedBytes = 0,
                flashTotalBytes = board.flashBytes,
                sramUsedBytes = 0,
                sramTotalBytes = board.sramBytes,
                errors = errors,
                compilationTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // 2. Check for required setup() and loop() functions
        val hasSetup = lines.any { it.contains("void setup") }
        val hasLoop = lines.any { it.contains("void loop") }

        if (!hasSetup) {
            errors.add(CompilerMessage(1, 1, true, "undefined reference to 'setup()'. Every Arduino sketch requires void setup()."))
        }
        if (!hasLoop) {
            errors.add(CompilerMessage(lines.size, 1, true, "undefined reference to 'loop()'. Every Arduino sketch requires void loop()."))
        }

        // 3. Bracket and brace validation
        val braceStack = Stack<Pair<Char, Int>>() // char to line index
        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val stripped = line.split("//").first() // ignore comments
            for (char in stripped) {
                when (char) {
                    '{', '(', '[' -> braceStack.push(char to lineNum)
                    '}' -> {
                        if (braceStack.isEmpty() || braceStack.peek().first != '{') {
                            errors.add(CompilerMessage(lineNum, 1, true, "mismatched or unexpected '}' token"))
                        } else {
                            braceStack.pop()
                        }
                    }
                    ')' -> {
                        if (braceStack.isEmpty() || braceStack.peek().first != '(') {
                            errors.add(CompilerMessage(lineNum, 1, true, "mismatched or unexpected ')' token"))
                        } else {
                            braceStack.pop()
                        }
                    }
                    ']' -> {
                        if (braceStack.isEmpty() || braceStack.peek().first != '[') {
                            errors.add(CompilerMessage(lineNum, 1, true, "mismatched or unexpected ']' token"))
                        } else {
                            braceStack.pop()
                        }
                    }
                }
            }
        }

        while (braceStack.isNotEmpty()) {
            val (openChar, openLine) = braceStack.pop()
            val expected = when (openChar) {
                '{' -> "'}'"
                '(' -> "')'"
                '[' -> "']'"
                else -> "closing token"
            }
            errors.add(CompilerMessage(openLine, 1, true, "expected $expected to match '$openChar' opened at line $openLine"))
        }

        // 4. Line by line syntax heuristics
        var inBlockComment = false
        for ((idx, rawLine) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = rawLine.trim()

            if (trimmed.contains("/*")) inBlockComment = true
            if (trimmed.contains("*/")) inBlockComment = false
            if (inBlockComment || trimmed.startsWith("//") || trimmed.isEmpty()) continue

            // Semicolon check on statement lines
            val codePart = trimmed.split("//").first().trim()
            val isDirective = codePart.startsWith("#")
            val isFunctionHeader = codePart.endsWith("{") || codePart.contains("void ") || codePart.contains("int ") && codePart.contains("(") && codePart.contains(")")
            val isControlBlock = codePart.startsWith("if") || codePart.startsWith("else") || codePart.startsWith("for") || codePart.startsWith("while") || codePart.startsWith("switch") || codePart.startsWith("case") || codePart.startsWith("default:")

            if (!isDirective && !isFunctionHeader && !isControlBlock && codePart.isNotEmpty()) {
                if (!codePart.endsWith(";") && !codePart.endsWith("{") && !codePart.endsWith("}") && !codePart.endsWith(":")) {
                    errors.add(CompilerMessage(lineNum, codePart.length, true, "expected ';' before end of line"))
                }
            }

            // Case sensitivity check for common Arduino APIs
            if (codePart.contains("digitalwrite(")) {
                errors.add(CompilerMessage(lineNum, 1, true, "'digitalwrite' was not declared in this scope; did you mean 'digitalWrite'?"))
            }
            if (codePart.contains("pinmode(")) {
                errors.add(CompilerMessage(lineNum, 1, true, "'pinmode' was not declared in this scope; did you mean 'pinMode'?"))
            }
            if (codePart.contains("digitalread(")) {
                errors.add(CompilerMessage(lineNum, 1, true, "'digitalread' was not declared in this scope; did you mean 'digitalRead'?"))
            }
            if (codePart.contains("serial.println") || codePart.contains("serial.print")) {
                errors.add(CompilerMessage(lineNum, 1, true, "'serial' was not declared in this scope; did you mean 'Serial'?"))
            }

            // Warning checks
            if (codePart.contains("delay(") && lines.subList(0, idx).any { it.contains("attachInterrupt") }) {
                warnings.add(CompilerMessage(lineNum, 1, false, "Using delay() inside or near interrupt code may lock the CPU. Prefer millis() timers."))
            }
            if (codePart.contains("Serial.print") && !code.contains("Serial.begin")) {
                warnings.add(CompilerMessage(lineNum, 1, false, "Using Serial output without Serial.begin(baudRate) in setup()."))
            }
        }

        val success = errors.isEmpty()
        val compTime = System.currentTimeMillis() - startTime + (150..350).random()

        // Calculate simulated flash & SRAM usage
        val baseFlash = when (board.id) {
            "esp32" -> 220000L + (lines.size * 35L)
            "mega" -> 1600L + (lines.size * 22L)
            "pico" -> 45000L + (lines.size * 28L)
            else -> 924L + (lines.size * 18L)
        }
        val flashUsed = if (success) minOf(baseFlash, board.flashBytes) else 0L

        val baseSram = when (board.id) {
            "esp32" -> 14200L + (lines.size * 8L)
            "mega" -> 350L + (lines.size * 5L)
            else -> 120L + (lines.size * 4L)
        }
        val sramUsed = if (success) minOf(baseSram, board.sramBytes) else 0L

        val logBuilder = StringBuilder()
        logBuilder.append("Target Board: ${board.name} (${board.mcu})\n")
        logBuilder.append("Building sketch in /tmp/droidduino_build_${board.id}...\n")
        logBuilder.append("avr-g++ -c -g -Os -w -std=gnu++17 -fno-exceptions -ffunction-sections -fdata-sections -flto ")
        logBuilder.append("-mmcu=${board.mcu.lowercase()} -DF_CPU=${board.clockSpeed.replace(" ", "")} ")
        logBuilder.append("-DARDUINO=10819 -DARDUINO_${board.id.uppercase()} sketch.ino -o sketch.ino.o\n\n")

        if (warnings.isNotEmpty()) {
            for (w in warnings) {
                logBuilder.append("sketch.ino:${w.line}:${w.column}: warning: ${w.message}\n")
            }
            logBuilder.append("\n")
        }

        if (success) {
            val flashPct = (flashUsed * 100) / board.flashBytes
            val sramPct = (sramUsed * 100) / board.sramBytes

            logBuilder.append("Linking everything together...\n")
            logBuilder.append("avr-gcc -w -Os -g -flto -fuse-linker-plugin -Wl,--gc-sections -o sketch.elf sketch.ino.o\n")
            logBuilder.append("Creating HEX binary...\n")
            logBuilder.append("avr-objcopy -O ihex -R .eeprom sketch.elf sketch.hex\n\n")
            logBuilder.append("Sketch uses $flashUsed bytes ($flashPct%) of program storage space. Maximum is ${board.flashBytes} bytes.\n")
            logBuilder.append("Global variables use $sramUsed bytes ($sramPct%) of dynamic memory, leaving ${board.sramBytes - sramUsed} bytes for local variables. Maximum is ${board.sramBytes} bytes.\n\n")
            logBuilder.append("[SUCCESS] Sketch compiled in ${compTime}ms. Ready to upload to ${board.name}!")
        } else {
            for (e in errors) {
                logBuilder.append("sketch.ino:${e.line}:${e.column}: error: ${e.message}\n")
            }
            logBuilder.append("\n[ERROR] Compilation failed with ${errors.size} error(s).\n")
            logBuilder.append("[TIP] Tap 'Ask Gemini to Fix' below to have Studio Bot diagnose and fix your code automatically!")
        }

        return CompilationResult(
            success = success,
            rawLog = logBuilder.toString(),
            flashUsedBytes = flashUsed,
            flashTotalBytes = board.flashBytes,
            sramUsedBytes = sramUsed,
            sramTotalBytes = board.sramBytes,
            errors = errors,
            warnings = warnings,
            compilationTimeMs = compTime
        )
    }
}
