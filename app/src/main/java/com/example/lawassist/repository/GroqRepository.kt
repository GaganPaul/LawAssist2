package com.example.lawassist.repository

import com.example.lawassist.network.GroqResponse
import com.example.lawassist.network.GroqApiService
import com.example.lawassist.network.GroqRequest
import com.example.lawassist.network.Message
import com.example.lawassist.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class GroqRepository {

    private val groqApiService: GroqApiService =
        RetrofitClient.instance.create(GroqApiService::class.java)

    suspend fun queryGroqLlama(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = """
                You are LawAssist, an AI providing clear and concise legal information on Indian laws and schemes like Ayushman Bharat and Sugamya Bharat Abhiyan. 
                Keep responses brief and actionable, using simple language.
                """.trimIndent()

                val requestBody = GroqRequest(
                    model = "llama3-8b-8192",
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = prompt)
                    ),
                    max_tokens = 300,
                    temperature = 0.5,
                    top_p = 0.7
                )

                println("Sending request to Groq API")
                val response: GroqResponse = groqApiService.getGroqResponse(requestBody)
                println("Received response from Groq: ${response.choices?.size ?: 0} choices")

                response.choices?.firstOrNull()?.message?.content ?: "No response from AI."

            } catch (e: HttpException) {
                println("HTTP Error: ${e.code()} - ${e.message}")
                val errorBody = e.response()?.errorBody()?.string()
                println("Error body: $errorBody")
                "Error: Unable to fetch response. (${e.code()}) Please try again."
            } catch (e: IOException) {
                println("Network Error: ${e.message}")
                "Network Error: Please check your connection and try again."
            } catch (e: Exception) {
                println("Unexpected Error: ${e.localizedMessage}")
                e.printStackTrace()
                "An unexpected error occurred. Please try again."
            }
        }
    }
}
