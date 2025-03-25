package com.example.lawassist.network

data class GroqRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int,
    val temperature: Double,
    val top_p: Double
)

data class Message(
    val role: String,
    val content: String
)