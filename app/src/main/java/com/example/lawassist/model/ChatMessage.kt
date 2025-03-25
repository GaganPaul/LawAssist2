package com.example.lawassist.model

data class ChatMessage(
    val userId: String = "",
    val role: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
