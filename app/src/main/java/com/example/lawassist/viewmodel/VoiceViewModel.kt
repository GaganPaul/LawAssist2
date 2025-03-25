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

    fun startVoiceRecognition(context: Context) {
        // Stop any ongoing recognition first
        stopVoiceRecognition()

        Log.d("VoiceViewModel", "Starting voice recognition")

        voiceToTextParser = VoiceToTextParser(
            context,
            { recognizedText ->
                Log.d("VoiceViewModel", "Received text: $recognizedText")
                onTextReceived?.invoke(recognizedText) // This will now update the TextField
            },
            { listening ->
                Log.d("VoiceViewModel", "Listening state changed: $listening")
                isListening.value = listening
            }
        )
        voiceToTextParser?.startListening()
    }

    fun stopVoiceRecognition() {
        Log.d("VoiceViewModel", "Stopping voice recognition")
        voiceToTextParser?.stopListening()
        voiceToTextParser?.destroy()
        voiceToTextParser = null
        isListening.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopVoiceRecognition()
    }
}