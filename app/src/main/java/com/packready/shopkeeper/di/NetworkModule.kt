package com.packready.shopkeeper.di

import com.packready.shopkeeper.data.remote.GeminiVlmService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: com.packready.shopkeeper.data.remote.AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu")

        val failoverInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val url = request.url
            if (url.host == "13.239.252.110" || url.host == "10.0.2.2" || url.host == "127.0.0.1" || url.host == "localhost" || url.host == "10.94.108.35") {
                val candidateHosts = if (isEmulator) {
                    listOf("13.239.252.110", "10.0.2.2", "127.0.0.1")
                } else {
                    listOf("13.239.252.110", "127.0.0.1", "10.0.2.2")
                }
                var lastException: java.io.IOException? = null
                for (host in candidateHosts) {
                    val newUrl = url.newBuilder().host(host).build()
                    val newRequest = request.newBuilder().url(newUrl).build()
                    try {
                        return@Interceptor chain.proceed(newRequest)
                    } catch (e: java.io.IOException) {
                        lastException = e
                    }
                }
                throw lastException ?: java.io.IOException("Unable to connect to PackReady server on $candidateHosts")
            }
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(failoverInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiVlmService(okHttpClient: OkHttpClient, json: Json): GeminiVlmService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GeminiVlmService::class.java)
    }

    private const val PACKREADY_SERVER_BASE_URL = "http://13.239.252.110/"

    @Provides
    @Singleton
    fun provideAuthApiService(okHttpClient: OkHttpClient, json: Json): com.packready.shopkeeper.data.remote.auth.AuthApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(PACKREADY_SERVER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(com.packready.shopkeeper.data.remote.auth.AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCatalogApiService(okHttpClient: OkHttpClient, json: Json): com.packready.shopkeeper.data.remote.CatalogApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(PACKREADY_SERVER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(com.packready.shopkeeper.data.remote.CatalogApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUploadApiService(okHttpClient: OkHttpClient, json: Json): com.packready.shopkeeper.data.remote.UploadApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(PACKREADY_SERVER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(com.packready.shopkeeper.data.remote.UploadApiService::class.java)
    }
}
