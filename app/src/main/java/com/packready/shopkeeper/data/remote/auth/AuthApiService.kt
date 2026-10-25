package com.packready.shopkeeper.data.remote.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class GoogleAuthRequestDto(
    @SerialName("id_token") val idToken: String
)

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class UserResponseDto(
    val id: String,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("full_name") val fullName: String,
    val role: String = "SHOPKEEPER",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_verified") val isVerified: Boolean = false
)

@Serializable
data class StoreDto(
    val id: String,
    val name: String,
    @SerialName("shopkeeper_email") val shopkeeperEmail: String,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val gstin: String? = null,
    @SerialName("upi_vpa") val upiVpa: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class StoreProfileCheckResponseDto(
    @SerialName("is_registered") val isRegistered: Boolean,
    val message: String? = null,
    @SerialName("contact_email") val contactEmail: String = "dhohadhanaryana@gmail.com",
    val store: StoreDto? = null
)

interface AuthApiService {
    @POST("api/v1/auth/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleAuthRequestDto
    ): Response<TokenResponseDto>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<UserResponseDto>

    @GET("api/v1/users/stores/by-email/{email}")
    suspend fun checkStoreRegistration(
        @retrofit2.http.Path("email") email: String
    ): Response<StoreProfileCheckResponseDto>
}
