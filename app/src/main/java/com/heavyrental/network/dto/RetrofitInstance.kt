package com.heavyrental.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory


object RetrofitInstance {

    // A toggle to switch between Mockoon and the real Spring Boot backend
    // true  = Mockoon (npm run mock:mockoon)
    // false = real Spring Boot backend
    private const val USE_MOCK_SERVER = false
    private const val MOCK_BASE_URL = "http://10.0.2.2:8081/"
    private const val REAL_BASE_URL = "http://10.0.2.2:8080/"
    private val BASE_URL = if (USE_MOCK_SERVER) MOCK_BASE_URL else REAL_BASE_URL

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .build()

    val api: HeavyRentalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HeavyRentalApiService::class.java)
    }
}

