package com.example.lawassist.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://api.groq.com/openai/v1/"
    private const val API_KEY = "ADD_YOUR_API_KEY_HERE" // 🔴 Replace with actual API key

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
                
                // We don't throw here to allow Retrofit to handle the error response
                // This ensures the error body is properly parsed
            }

            response
        } catch (e: Exception) {
            println("Network Error: ${e.localizedMessage}")
            throw e
        }
    }

    // Configure OkHttpClient with interceptors and timeouts
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
