package com.example.lawassist.model

data class AIModel(
    val id: String,
    val object_type: String = "model",
    val created: Long = 0,
    val owned_by: String,
    val active: Boolean = true,
    val context_window: Int = 0,
    val public_apps: Any? = null
) {
    companion object {
        val DEFAULT_MODEL = "llama3-70b-8192"
        
        val AVAILABLE_MODELS = listOf(
            AIModel(
                id = "llama3-groq-70b-8192-tool-use-preview",
                owned_by = "Groq",
                context_window = 8192
            ),
            AIModel(
                id = "gemma2-9b-it",
                owned_by = "Google",
                context_window = 8192
            ),
            AIModel(
                id = "llama3-8b-8192",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "llama-3.2-90b-vision-preview",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "llama3-70b-8192",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "llama-3.2-11b-vision-preview",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "llama-3.2-11b-text-preview",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "whisper-large-v3-turbo",
                owned_by = "OpenAI",
                context_window = 448
            ),
            AIModel(
                id = "llava-v1.5-7b-4096-preview",
                owned_by = "Other",
                context_window = 4096
            ),
            AIModel(
                id = "llama-3.1-70b-versatile",
                owned_by = "Meta",
                context_window = 32768
            ),
            AIModel(
                id = "llama-3.2-3b-preview",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "whisper-large-v3",
                owned_by = "OpenAI",
                context_window = 448
            ),
            AIModel(
                id = "llama-guard-3-8b",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "mixtral-8x7b-32768",
                owned_by = "Mistral AI",
                context_window = 32768
            ),
            AIModel(
                id = "gemma-7b-it",
                owned_by = "Google",
                context_window = 8192
            ),
            AIModel(
                id = "distil-whisper-large-v3-en",
                owned_by = "Hugging Face",
                context_window = 448
            ),
            AIModel(
                id = "llama-3.2-1b-preview",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "llama-3.2-90b-text-preview",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "llama3-groq-8b-8192-tool-use-preview",
                owned_by = "Groq",
                context_window = 8192
            ),
            AIModel(
                id = "llama-3.1-8b-instant",
                owned_by = "Meta",
                context_window = 131072
            )
        )
    }
}
