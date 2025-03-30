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
                id = "llama-3.2-3b-preview",
                owned_by = "Meta",
                context_window = 8192
            ),
            AIModel(
                id = "llama-3.2-1b-preview",
                owned_by = "Meta",
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
