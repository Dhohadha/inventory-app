package com.packready.shopkeeper.data.remote

import com.packready.shopkeeper.data.remote.dto.MasterProductResponseDto
import com.packready.shopkeeper.data.remote.dto.PaginatedMasterProductsDto
import com.packready.shopkeeper.data.remote.dto.StockAdjustmentRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApiService {

    @GET("api/v1/catalog/barcode/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("auto_ai_fallback") autoAiFallback: Boolean = false
    ): Response<MasterProductResponseDto>

    @POST("api/v1/catalog/extract-ai")
    suspend fun extractProductWithGemini(
        @Body request: com.packready.shopkeeper.data.remote.dto.GeminiExtractRequestDto,
        @retrofit2.http.Header("X-Gemini-Api-Key") apiKey: String? = null
    ): Response<MasterProductResponseDto>


    @GET("api/v1/catalog")
    suspend fun searchCatalog(
        @Query("q") query: String?,
        @Query("category") category: String? = null,
        @Query("brand") brand: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<PaginatedMasterProductsDto>

    @POST("api/v1/stores/{store_id}/inventory/{product_id}/adjust-stock")
    suspend fun adjustStock(
        @Path("store_id") storeId: String,
        @Path("product_id") productId: String,
        @Body request: StockAdjustmentRequestDto
    ): Response<Unit>

    @POST("api/v1/catalog/enroll")
    suspend fun enrollProduct(
        @Body request: com.packready.shopkeeper.data.remote.dto.ProductEnrollRequestDto
    ): Response<Unit>

    @GET("api/v1/health")
    suspend fun checkHealth(): Response<Map<String, String>>
}

