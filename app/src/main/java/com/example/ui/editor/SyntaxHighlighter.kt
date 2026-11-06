package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxConstant
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxPreprocessor
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "if", "else", "for", "while", "do", "break", "continue", "return",
        "switch", "case", "default", "goto", "class", "struct", "enum", "union",
        "typedef", "typename", "template", "namespace", "using", "public",
        "private", "protected", "virtual", "override", "final", "sizeof",
        "new", "delete", "this", "try", "catch", "throw", "const", "static",
        "volatile", "extern", "inline", "explicit", "friend", "mutable",
        "IRAM_ATTR", "DRAM_ATTR"
    )

    private val TYPES = setOf(
        "void", "int", "float", "double", "char", "bool", "boolean", "byte",
        "long", "short", "unsigned", "signed", "int8_t", "uint8_t", "int16_t",
        "uint16_t", "int32_t", "uint32_t", "int64_t", "uint64_t", "size_t",
        "String", "IPAddress", "Stream", "Print", "HardwareSerial"
    )

    private val ARDUINO_FUNCS = setOf(
        "setup", "loop", "pinMode", "digitalWrite", "digitalRead",
        "analogRead", "analogWrite", "analogReference", "delay",
        "delayMicroseconds", "millis", "micros", "pulseIn", "pulseInLong",
        "shiftIn", "shiftOut", "tone", "noTone", "attachInterrupt",
        "detachInterrupt", "interrupts", "noInterrupts", "map", "constrain",
        "min", "max", "abs", "round", "random", "randomSeed",
        "begin", "print", "println", "write", "available", "read",
        "flush", "peek", "handleRoot", "handleClient", "softAP", "softAPIP"
    )

    private val CONSTANTS = setOf(
        "HIGH", "LOW", "INPUT", "OUTPUT", "INPUT_PULLUP", "LED_BUILTIN",
        "true", "false", "NULL", "nullptr", "DEC", "HEX", "OCT", "BIN",
        "FALLING", "RISING", "CHANGE", "PI", "HALF_PI", "TWO_PI",
        "DEG_TO_RAD", "RAD_TO_DEG", "EULER"
    )

    fun highlight(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            var i = 0
            val len = code.length

            while (i < len) {
                // Check block comment /* ... */
                if (i + 1 < len && code[i] == '/' && code[i + 1] == '*') {
                    val start = i
                    val end = code.indexOf("*/", start + 2).let { if (it == -1) len else it + 2 }
                    addStyle(SpanStyle(color = SyntaxComment, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, end)
                    i = end
                    continue
                }

                // Check line comment // ...
                if (i + 1 < len && code[i] == '/' && code[i + 1] == '/') {
                    val start = i
                    val end = code.indexOf('\n', start).let { if (it == -1) len else it }
                    addStyle(SpanStyle(color = SyntaxComment, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, end)
                    i = end
                    continue
                }

                // Check string literal "..."
                if (code[i] == '"') {
                    val start = i
                    var end = i + 1
                    while (end < len && code[end] != '"') {
                        if (code[end] == '\\' && end + 1 < len) end += 2
                        else end++
                    }
                    if (end < len) end++ // include closing quote
                    addStyle(SpanStyle(color = SyntaxString), start, end)
                    i = end
                    continue
                }

                // Check char literal '...'
                if (code[i] == '\'') {
                    val start = i
                    var end = i + 1
                    while (end < len && code[end] != '\'') {
                        if (code[end] == '\\' && end + 1 < len) end += 2
                        else end++
                    }
                    if (end < len) end++
                    addStyle(SpanStyle(color = SyntaxString), start, end)
                    i = end
                    continue
                }

                // Check preprocessor #...
                if (code[i] == '#' && (i == 0 || code[i - 1] == '\n' || code.substring(0, i).trim().isEmpty())) {
                    val start = i
                    var end = i + 1
                    while (end < len && (code[end].isLetterOrDigit() || code[end] == '_')) end++
                    addStyle(SpanStyle(color = SyntaxPreprocessor, fontWeight = FontWeight.Bold), start, end)
                    i = end
                    continue
                }

                // Check numbers
                if (code[i].isDigit() && (i == 0 || !code[i - 1].isLetterOrDigit() && code[i - 1] != '_')) {
                    val start = i
                    var end = i + 1
                    while (end < len && (code[end].isLetterOrDigit() || code[end] == '.')) end++
                    addStyle(SpanStyle(color = SyntaxNumber), start, end)
                    i = end
                    continue
                }

                // Check identifiers / words
                if (code[i].isLetter() || code[i] == '_') {
                    val start = i
                    var end = i + 1
                    while (end < len && (code[end].isLetterOrDigit() || code[end] == '_')) end++
                    val word = code.substring(start, end)

                    when {
                        KEYWORDS.contains(word) -> {
                            addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold), start, end)
                        }
                        TYPES.contains(word) -> {
                            addStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.Medium), start, end)
                        }
                        ARDUINO_FUNCS.contains(word) -> {
                            addStyle(SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Medium), start, end)
                        }
                        CONSTANTS.contains(word) -> {
                            addStyle(SpanStyle(color = SyntaxConstant, fontWeight = FontWeight.Bold), start, end)
                        }
                        word == "Serial" || word == "Wire" || word == "WiFi" || word == "server" -> {
                            addStyle(SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Bold), start, end)
                        }
                    }
                    i = end
                    continue
                }

                i++
            }
        }
    }
}
