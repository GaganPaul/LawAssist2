package com.example.lawassist.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service to handle text-to-speech functionality
 */
class TextToSpeechService(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private val _isSpeaking = mutableStateOf(false)
    val isSpeaking: Boolean
        get() = _isSpeaking.value
    
    private val isInitialized = AtomicBoolean(false)
    private val params = HashMap<String, String>()
    
    init {
        initTTS()
    }
    
    private fun initTTS() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale.US)
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported")
                    } else {
                        Log.i("TTS", "TextToSpeech initialized successfully")
                        isInitialized.set(true)
                        
                        // Set up progress listener
                        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                                Log.d("TTS", "Started speaking: $utteranceId")
                            }
                            
                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                                Log.d("TTS", "Finished speaking: $utteranceId")
                            }
                            
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                                Log.e("TTS", "Error speaking: $utteranceId")
                            }
                        })
                    }
                } else {
                    Log.e("TTS", "TextToSpeech initialization failed with status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("TTS", "Error initializing TextToSpeech: ${e.message}")
        }
    }
    
    /**
     * Speak the given text
     */
    fun speak(text: String) {
        try {
            if (!isInitialized.get()) {
                Log.w("TTS", "TextToSpeech not initialized yet, retrying...")
                initTTS()
                android.os.Handler().postDelayed({ speak(text) }, 500)
                return
            }
            
            // Stop any ongoing speech
            stop()
            
            val utteranceId = UUID.randomUUID().toString()
            
            // Set speech rate and pitch
            textToSpeech?.setSpeechRate(0.9f)
            textToSpeech?.setPitch(1.0f)
            
            // Create params with utterance ID
            params.clear()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
            
            // Speak the text
            val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
            
            if (result == TextToSpeech.ERROR) {
                Log.e("TTS", "Error speaking text")
                _isSpeaking.value = false
            } else {
                Log.d("TTS", "Speaking text with utterance ID: $utteranceId")
                _isSpeaking.value = true
            }
        } catch (e: Exception) {
            Log.e("TTS", "Error in speak method: ${e.message}")
            _isSpeaking.value = false
        }
    }
    
    /**
     * Stop speaking
     */
    fun stop() {
        try {
            if (textToSpeech?.isSpeaking == true) {
                textToSpeech?.stop()
                _isSpeaking.value = false
            }
        } catch (e: Exception) {
            Log.e("TTS", "Error stopping speech: ${e.message}")
        }
    }
    
    /**
     * Shutdown TTS engine
     */
    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            _isSpeaking.value = false
            isInitialized.set(false)
        } catch (e: Exception) {
            Log.e("TTS", "Error shutting down TTS: ${e.message}")
        }
    }
}
