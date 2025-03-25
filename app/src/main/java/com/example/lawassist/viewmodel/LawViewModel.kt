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
import kotlinx.coroutines.launch
import androidx.compose.runtime.MutableState


class LawViewModel(private val lawRepository: LawRepository) : ViewModel() {

    private val _aiResponse = mutableStateOf("")
    val aiResponse: MutableState<String> = _aiResponse

    private val _lawsList = mutableStateOf<List<LawEntity>>(emptyList())
    val lawsList: MutableState<List<LawEntity>> = _lawsList

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
/*
    fun registerUser(email: String, password: String, name: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = authService.registerUser(email, password, name)
            if (result.isSuccess) {
                currentUserId = result.getOrNull()
                checkUserSession()
                clearAIResponse() // Clear response on new registration
                onResult(true, "Registration successful")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun loginUser(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = authService.loginUser(email, password)
            if (result.isSuccess) {
                currentUserId = result.getOrNull()
                checkUserSession()
                clearAIResponse() // Clear response on login
                onResult(true, "Login successful")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

 */
/*
    fun logoutUser(onResult: () -> Unit) {
        authService.logoutUser()
        currentUserId = null
        checkUserSession()
        clearAIResponse() // Clear response on logout
        onResult()
    }
*/
    // AI Chat Handling
    fun queryGroqLlama(prompt: String) {
        if (currentUserId.isNullOrBlank()) {
            println("Error: currentUserId is null or empty. Cannot save chat.")
            return
        }

        if (prompt.isBlank()) {
            println("Error: Cannot send an empty prompt.")
            return
        }

        viewModelScope.launch {
            val response = groqRepository.queryGroqLlama(prompt)

            if (response.isNotBlank()) {
                aiResponse.value = response
            } else {
                aiResponse.value = "No response from AI."
            }

            try {
                println("Storing User Message: $prompt")
                firestoreService.saveChatMessage(currentUserId!!, prompt, "user")

                if (response.isNotBlank()) {
                    println("Storing AI Response: $response")
                    firestoreService.saveChatMessage(currentUserId!!, response, "ai")
                }
            } catch (e: Exception) {
                println("Error saving chat messages: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    fun resetResponseState() {
        _aiResponse.value = ""
    }
}

