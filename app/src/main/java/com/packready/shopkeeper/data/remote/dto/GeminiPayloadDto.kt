package com.packready.shopkeeper.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String // Base64 encoded image
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float = 0.1f,
    @SerialName("response_mime_type") val responseMimeType: String = "application/json"
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val promptFeedback: GeminiPromptFeedback? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiCandidateContent? = null,
    val finishReason: String? = null
)

@Serializable
data class GeminiCandidateContent(
    val parts: List<GeminiResponsePart>? = null,
    val role: String? = null
)

@Serializable
data class GeminiResponsePart(
    val text: String? = null
)

@Serializable
data class GeminiPromptFeedback(
    val blockReason: String? = null
)
