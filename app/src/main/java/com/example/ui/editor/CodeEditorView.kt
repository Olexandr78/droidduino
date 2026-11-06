package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArduinoTeal
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeBorder
import com.example.ui.theme.IdeSurface
import com.example.ui.theme.IdeSurfaceVariant
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary

@Composable
fun CodeEditorView(
    code: String,
    onCodeChange: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(code) {
        mutableStateOf(TextFieldValue(text = code, selection = androidx.compose.ui.text.TextRange(code.length)))
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val lines = code.lines()
    val lineCount = maxOf(lines.size, 1)

    // Visual transformation for real-time syntax coloring
    val syntaxVisualTransformation = remember {
        VisualTransformation { text ->
            val annotated = SyntaxHighlighter.highlight(text.text)
            TransformedText(annotated, OffsetMapping.Identity)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
    ) {
        // Quick Symbol & Macro Toolbar above keyboard / editor
        SymbolToolbar(
            onInsertSymbol = { sym ->
                val currentText = textFieldValue.text
                val selStart = textFieldValue.selection.start
                val selEnd = textFieldValue.selection.end
                val newText = currentText.substring(0, selStart) + sym + currentText.substring(selEnd)
                val newSelection = androidx.compose.ui.text.TextRange(selStart + sym.length)
                val newTfv = TextFieldValue(text = newText, selection = newSelection)
                textFieldValue = newTfv
                onCodeChange(newText)
            },
            onUndo = onUndo,
            onRedo = onRedo
        )

        // Main Editor Canvas (Gutter + Text Field)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // Line Number Gutter
            Column(
                modifier = Modifier
                    .background(IdeSurface)
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .width(42.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        color = IdeTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Vertical divider line between gutter and editor
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(IdeBorder)
            )

            // Syntax Highlighted Editable Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
                    .padding(12.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newTfv ->
                        textFieldValue = newTfv
                        if (newTfv.text != code) {
                            onCodeChange(newTfv.text)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_editor"),
                    textStyle = TextStyle(
                        color = IdeTextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    visualTransformation = syntaxVisualTransformation,
                    cursorBrush = SolidColor(ArduinoTeal),
                    keyboardOptions = KeyboardOptions.Default,
                    decorationBox = { innerTextField ->
                        if (code.isEmpty()) {
                            Text(
                                text = "// Enter your Arduino C++ code here...",
                                color = IdeTextMuted,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun SymbolToolbar(
    onInsertSymbol: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val scrollState = rememberScrollState()
    val quickSymbols = listOf(
        "  ", "{", "}", "(", ")", "[", "]", ";", "#", "<", ">", "=", "\"", "'",
        "// ", "Serial.", "digitalWrite(", "pinMode(", "delay(", "millis()", "OUTPUT", "INPUT"
    )

    Surface(
        color = IdeSurfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onUndo,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = IdeTextPrimary
                )
            }

            IconButton(
                onClick = onRedo,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = IdeTextPrimary
                )
            }

            // Quick symbol chips
            for (sym in quickSymbols) {
                val label = if (sym == "  ") "Tab" else sym
                Surface(
                    onClick = { onInsertSymbol(sym) },
                    shape = RoundedCornerShape(6.dp),
                    color = IdeSurface,
                    modifier = Modifier.padding(horizontal = 3.dp)
                ) {
                    Text(
                        text = label,
                        color = ArduinoTeal,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
