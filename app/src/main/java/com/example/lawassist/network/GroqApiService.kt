package com.example.lawassist.network

import com.example.lawassist.network.GroqResponse
import com.example.lawassist.network.GroqRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GroqApiService {

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun getGroqResponse(@Body requestBody: GroqRequest): GroqResponse
}
