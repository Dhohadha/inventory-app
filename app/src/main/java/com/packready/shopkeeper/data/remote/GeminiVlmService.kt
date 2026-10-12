package com.packready.shopkeeper.data.remote

import com.packready.shopkeeper.data.remote.dto.GeminiRequest
import com.packready.shopkeeper.data.remote.dto.GeminiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiVlmService {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
