package com.example.lawassist.viewmodel

import com.example.lawassist.auth.FirebaseAuthService
import com.example.lawassist.model.ChatMessage
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.example.lawassist.repository.LawRepository
import com.example.lawassist.service.FirestoreService
import com.example.lawassist.repository.GroqRepository
import com.example.lawassist.database.LawEntity
import com.example.lawassist.settings.SettingsManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class LawViewModel(private val lawRepository: LawRepository) : ViewModel() {

    private val _aiResponse = mutableStateOf("")
    val aiResponse: MutableState<String> = _aiResponse

    private val _lawsList = mutableStateOf<List<LawEntity>>(emptyList())
    val lawsList: MutableState<List<LawEntity>> = _lawsList
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: MutableState<Boolean> = _isLoading

    private val groqRepository = GroqRepository()
    private val firestoreService = FirestoreService()
    private val authService = FirebaseAuthService()

    // Store the current user's ID
    var currentUserId: String? = authService.getCurrentUserId()

    // Session Management
    fun checkUserSession() {
        currentUserId = authService.getCurrentUserId()
        println("Current User ID: $currentUserId")
    }

    // AI Chat Handling
    suspend fun queryGroqLlama(prompt: String, modelId: String? = null): String {
        // Always refresh the user ID before making a query
        checkUserSession()
        
        if (prompt.isBlank()) {
            return "I couldn't understand your message. Please try again with a more detailed question."
        }

        _isLoading.value = true
        
        try {
            // Query the AI with the selected model or default
            val response = withContext(Dispatchers.IO) {
                // Ensure we're using a valid model ID
                val actualModelId = if (!modelId.isNullOrBlank()) {
                    modelId
                } else {
                    "llama3-70b-8192" // Fallback to a known working model
                }
                
                println("Using model ID: $actualModelId")
                groqRepository.queryGroqLlama(prompt, actualModelId)
            }

            if (response.isNotBlank()) {
                _aiResponse.value = response
                return response
            } else {
                val errorMessage = "I couldn't generate a response. Please try rephrasing your question."
                _aiResponse.value = errorMessage
                return errorMessage
            }
        } catch (e: Exception) {
            val errorMessage = "Error: ${e.message ?: "Unknown error occurred"}"
            println(errorMessage)
            _aiResponse.value = "Sorry, I encountered an error. Please try again later."
            return "Sorry, I encountered an error. Please try again later."
        } finally {
            _isLoading.value = false
        }
    }

    fun resetResponseState() {
        _aiResponse.value = ""
    }
}
