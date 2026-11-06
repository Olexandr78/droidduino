package com.example.data.model

data class Sketch(
    val id: String,
    val name: String,
    val boardId: String,
    val code: String,
    val description: String = "",
    val category: String = "User Projects",
    val updatedAt: Long = System.currentTimeMillis()
)

data class CompilerMessage(
    val line: Int,
    val column: Int,
    val isError: Boolean,
    val message: String
)

data class CompilationResult(
    val success: Boolean,
    val rawLog: String,
    val flashUsedBytes: Long,
    val flashTotalBytes: Long,
    val sramUsedBytes: Long,
    val sramTotalBytes: Long,
    val errors: List<CompilerMessage> = emptyList(),
    val warnings: List<CompilerMessage> = emptyList(),
    val compilationTimeMs: Long = 0L
)
