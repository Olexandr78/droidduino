package com.example

import com.example.data.compiler.SketchCompiler
import com.example.data.model.SupportedBoards
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SketchCompilerTest {

    @Test
    fun testValidSketchCompilesSuccessfully() {
        val validSketch = """
            void setup() {
                Serial.begin(9600);
            }
            void loop() {
                Serial.println("Hello");
                delay(1000);
            }
        """.trimIndent()

        val result = SketchCompiler.compile(validSketch, SupportedBoards.ARDUINO_UNO)
        assertTrue(result.success)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.flashUsedBytes > 0)
    }

    @Test
    fun testMissingSetupFailsCompilation() {
        val brokenSketch = """
            void loop() {
                delay(1000);
            }
        """.trimIndent()

        val result = SketchCompiler.compile(brokenSketch, SupportedBoards.ARDUINO_UNO)
        assertFalse(result.success)
        assertTrue(result.errors.any { it.message.contains("setup()") })
    }

    @Test
    fun testMismatchedBracesFailsCompilation() {
        val brokenSketch = """
            void setup() {
                pinMode(13, OUTPUT);
            
            void loop() {
                digitalWrite(13, HIGH);
            }
        """.trimIndent()

        val result = SketchCompiler.compile(brokenSketch, SupportedBoards.ARDUINO_UNO)
        assertFalse(result.success)
        assertTrue(result.errors.any { it.message.contains("expected '}'") })
    }
}
