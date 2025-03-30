package com.example.lawassist.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.lawassist.VoiceToTextParser

class VoiceViewModel : ViewModel() {
    private var voiceToTextParser: VoiceToTextParser? = null
    var onTextReceived: ((String) -> Unit)? = null // Callback to send text to UI
    var isListening = mutableStateOf(false)  // Observable listening state
    
    // Add voice input result state
    val voiceInputResult = mutableStateOf("")

    fun startVoiceRecognition(context: Context) {
        try {
            // Stop any ongoing recognition first
            stopVoiceRecognition()

            Log.d("VoiceViewModel", "Starting voice recognition")

            voiceToTextParser = VoiceToTextParser(
                context,
                { recognizedText ->
                    Log.d("VoiceViewModel", "Received text: $recognizedText")
                    if (recognizedText.isNotBlank()) {
                        voiceInputResult.value = recognizedText
                        onTextReceived?.invoke(recognizedText) // This will now update the TextField
                    }
                },
                { listening ->
                    Log.d("VoiceViewModel", "Listening state changed: $listening")
                    isListening.value = listening
                }
            )
            voiceToTextParser?.startListening()
        } catch (e: Exception) {
            Log.e("VoiceViewModel", "Error starting voice recognition: ${e.message}")
            isListening.value = false
            // Reset the parser to avoid memory leaks
            voiceToTextParser = null
        }
    }

    fun stopVoiceRecognition() {
        try {
            Log.d("VoiceViewModel", "Stopping voice recognition")
            voiceToTextParser?.stopListening()
            voiceToTextParser?.destroy()
            voiceToTextParser = null
            isListening.value = false
        } catch (e: Exception) {
            Log.e("VoiceViewModel", "Error stopping voice recognition: ${e.message}")
            // Ensure we reset the state even if there's an error
            isListening.value = false
            voiceToTextParser = null
        }
    }
    
    // Add method to clear voice input result
    fun clearVoiceInputResult() {
        voiceInputResult.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        stopVoiceRecognition()
    }
}