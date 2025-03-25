package com.example.lawassist

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceToTextParser(
    private val context: Context,
    private val callback: (String) -> Unit,
    private val onListeningStateChanged: ((Boolean) -> Unit)? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening() {
        if (speechRecognizer == null) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                Log.e("VoiceToText", "Speech recognition is not available on this device.")
                callback("Speech recognition is not available on this device.")
                return
            }
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("VoiceToText", "Ready for Speech")
                    onListeningStateChanged?.invoke(true)
                }

                override fun onBeginningOfSpeech() {
                    Log.d("VoiceToText", "Speech Started")
                    onListeningStateChanged?.invoke(true)
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("VoiceToText", "Speech Ended")
                    // Do not change listening state here as we still need to wait for results
                }

                override fun onError(error: Int) {
                    Log.e("VoiceToText", "Error: $error")
                    onListeningStateChanged?.invoke(false)

                    val errorMessage = when(error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    Log.e("VoiceToText", "Specific error: $errorMessage")
                }

                override fun onResults(results: Bundle?) {
                    val matches: ArrayList<String>? = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        Log.d("VoiceToText", "Recognized: $recognizedText")
                        callback(recognizedText) // Send recognized text to UI
                    } else {
                        Log.e("VoiceToText", "No recognition results")
                    }
                    onListeningStateChanged?.invoke(false)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // We could show partial results here if needed
                    val matches: ArrayList<String>? = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        Log.d("VoiceToText", "Partial: ${matches[0]}")
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }

        try {
            speechRecognizer?.startListening(intent)
            onListeningStateChanged?.invoke(true)
        } catch (e: Exception) {
            Log.e("VoiceToText", "Error starting speech recognition: ${e.message}")
            onListeningStateChanged?.invoke(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceToText", "Error stopping recognition: ${e.message}")
        }
        onListeningStateChanged?.invoke(false)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceToText", "Error destroying recognizer: ${e.message}")
        }
        speechRecognizer = null
        onListeningStateChanged?.invoke(false)
    }
}