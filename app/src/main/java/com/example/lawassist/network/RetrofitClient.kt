package com.example.lawassist.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.groq.com/openai/v1/"
    private const val API_KEY = "API_KEY" // 🔴 Replace with actual API key

    // Ensure API key is set
    init {
        require(API_KEY.isNotBlank()) { "API Key is missing. Please set the API_KEY in RetrofitClient." }
    }

    // Logging interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Authorization and error-handling interceptor
    private val authInterceptor = Interceptor { chain ->
        try {
            val request: Request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = chain.proceed(request)

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                println("API Error: ${response.code} - ${response.message}\nError Body: $errorBody")
                throw Exception("API Error: ${response.code} - ${response.message}")
            }

            response
        } catch (e: Exception) {
            println("Network Error: ${e.localizedMessage}")
            throw e
        }
    }

    // Configure OkHttpClient with interceptors
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit instance
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
    }
}
