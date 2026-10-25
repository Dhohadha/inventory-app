package com.packready.shopkeeper.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@Serializable
data class CloudinaryUploadResponseDto(
    @SerialName("public_id") val publicId: String? = null,
    @SerialName("secure_url") val secureUrl: String? = null,
    val format: String? = null,
    val bytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null
)

interface UploadApiService {

    @Multipart
    @POST("api/v1/uploads/server-upload")
    suspend fun uploadProductImage(
        @Part file: MultipartBody.Part,
        @Part("folder") folder: RequestBody,
        @Part("barcode") barcode: RequestBody? = null,
        @Part("public_id") publicId: RequestBody? = null
    ): Response<CloudinaryUploadResponseDto>
}
