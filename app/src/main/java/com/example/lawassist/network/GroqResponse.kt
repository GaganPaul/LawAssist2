package com.example.lawassist.network

import com.google.gson.annotations.SerializedName

data class GroqResponse(
    val id: String?,
    val objectname: String?,
    val created: Long?,
    val model: String?,
    val choices: List<Choice>?,
    val usage: Usage?
)

data class Choice(
    val index: Int?,
    val message: ChoiceMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class ChoiceMessage(
    val role: String?,
    val content: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    @SerializedName("total_tokens")
    val totalTokens: Int?
)