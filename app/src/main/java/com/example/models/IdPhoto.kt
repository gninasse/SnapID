package com.example.models

data class IdPhoto(
    val id: String,
    val filePath: String,
    val sheetFilePath: String?,
    val documentId: String,
    val documentName: String,
    val sizeLabel: String,
    val timestamp: Long
)
