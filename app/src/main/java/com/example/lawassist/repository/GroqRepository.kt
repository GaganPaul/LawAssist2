package com.example.lawassist.repository

import android.util.Log
import com.example.lawassist.network.GroqResponse
import com.example.lawassist.network.GroqApiService
import com.example.lawassist.network.GroqRequest
import com.example.lawassist.network.Message
import com.example.lawassist.network.RetrofitClient
import com.example.lawassist.model.AIModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class GroqRepository {

    private val groqApiService: GroqApiService =
        RetrofitClient.instance.create(GroqApiService::class.java)
    
    private val TAG = "GroqRepository"

    suspend fun queryGroqLlama(prompt: String, modelId: String = AIModel.DEFAULT_MODEL): String {
        return withContext(Dispatchers.IO) {
            try {
                // Validate model ID - if it's not in the available models, use the default
                val validModelId = if (AIModel.AVAILABLE_MODELS.any { it.id == modelId }) {
                    modelId
                } else {
                    Log.w(TAG, "Invalid model ID: $modelId, falling back to default")
                    AIModel.DEFAULT_MODEL
                }
                
                val systemPrompt = """
                  You are LawAssist, an AI assistant that provides quick, clear, and easy-to-understand answers about laws and government schemes and accessibility services in India.
                    - Keep responses under 50 words.
                    - Prioritize clarity and efficiency.
                    - Mention key schemes like Ayushman Bharat and Sugamya Bharat Abhiyan.
                    - Ensure responses are actionable and useful for Indian users.
                    - Use simple language, avoiding unnecessary details.
                    - If the user makes a spelling mistake, assume the correct spelling and respond accordingly.
                """.trimIndent()

                val requestBody = GroqRequest(
                    model = validModelId,
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = prompt)
                    ),
                    max_tokens = 200,
                    temperature = 0.5,
                    top_p = 0.7
                )

                Log.d(TAG, "Sending request to Groq API with model: $validModelId")
                val response: GroqResponse = groqApiService.getGroqResponse(requestBody)
                Log.d(TAG, "Received response from Groq: ${response.choices?.size ?: 0} choices")

                val content = response.choices?.firstOrNull()?.message?.content
                if (content.isNullOrBlank()) {
                    Log.w(TAG, "Empty response from AI")
                    "No response from AI. Please try again."
                } else {
                    content
                }

            } catch (e: HttpException) {
                Log.e(TAG, "HTTP Error: ${e.code()} - ${e.message}")
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(TAG, "Error body: $errorBody")
                "Error: Unable to fetch response. Please try again."
            } catch (e: IOException) {
                Log.e(TAG, "Network Error: ${e.message}")
                "Network Error: Please check your connection and try again."
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected Error: ${e.localizedMessage}", e)
                "An unexpected error occurred. Please try again."
            }
        }
    }
}
