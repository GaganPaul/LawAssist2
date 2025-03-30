package com.example.lawassist.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.lawassist.model.AIModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager class for handling app settings and preferences
 */
class SettingsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Font size settings
    private val _fontSize = MutableStateFlow(getFontSize())
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()
    
    // AI model settings
    private val _selectedModel = MutableStateFlow(getSelectedModel())
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()
    
    // Initialize with default values if needed
    init {
        if (!prefs.contains(KEY_FONT_SIZE)) {
            setFontSize(DEFAULT_FONT_SIZE)
        }
        
        if (!prefs.contains(KEY_SELECTED_MODEL)) {
            setSelectedModel(AIModel.DEFAULT_MODEL)
        }
    }
    
    // Font size methods
    fun getFontSize(): Float {
        return prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
    }
    
    fun setFontSize(size: Float) {
        val clampedSize = size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        prefs.edit().putFloat(KEY_FONT_SIZE, clampedSize).apply()
        _fontSize.value = clampedSize
    }
    
    fun increaseFontSize() {
        val currentSize = getFontSize()
        if (currentSize < MAX_FONT_SIZE) {
            setFontSize(currentSize + FONT_SIZE_STEP)
        }
    }
    
    fun decreaseFontSize() {
        val currentSize = getFontSize()
        if (currentSize > MIN_FONT_SIZE) {
            setFontSize(currentSize - FONT_SIZE_STEP)
        }
    }
    
    // Toggle between small and large font size
    fun toggleFontSize() {
        val currentSize = getFontSize()
        if (currentSize > DEFAULT_FONT_SIZE) {
            setFontSize(DEFAULT_FONT_SIZE)
        } else {
            setFontSize(MAX_FONT_SIZE)
        }
    }
    
    // Check if using large font size
    fun isLargeFontSize(): Boolean {
        return getFontSize() > DEFAULT_FONT_SIZE
    }
    
    // AI model methods
    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, AIModel.DEFAULT_MODEL) ?: AIModel.DEFAULT_MODEL
    }
    
    fun setSelectedModel(modelId: String) {
        // Verify the model exists in available models
        val validModel = if (AIModel.AVAILABLE_MODELS.any { it.id == modelId }) {
            modelId
        } else {
            AIModel.DEFAULT_MODEL
        }
        
        prefs.edit().putString(KEY_SELECTED_MODEL, validModel).apply()
        _selectedModel.value = validModel
    }
    
    // Toggle between default models
    fun toggleModel() {
        val currentModel = getSelectedModel()
        if (isGroqLlamaModel()) {
            setSelectedModel("llama3-70b-8192")
        } else {
            setSelectedModel("llama3-groq-70b-8192-tool-use-preview")
        }
    }
    
    // Check if using Groq Llama model
    fun isGroqLlamaModel(): Boolean {
        return getSelectedModel().contains("groq")
    }
    
    // Clear all settings
    fun clearAllSettings() {
        prefs.edit().clear().apply()
        setFontSize(DEFAULT_FONT_SIZE)
        setSelectedModel(AIModel.DEFAULT_MODEL)
    }
    
    companion object {
        private const val PREFS_NAME = "law_assist_settings"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_SELECTED_MODEL = "selected_model"
        
        const val MIN_FONT_SIZE = 0.8f
        const val DEFAULT_FONT_SIZE = 1.0f
        const val MAX_FONT_SIZE = 1.5f
        const val FONT_SIZE_STEP = 0.1f
        
        // Singleton instance
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
