package com.heavyrental.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Shared Retrofit / OkHttp stack for the Heavy Rental API.
 *
 * Host selection comes from build-time config (`app/api.properties` →
 * BuildConfig.API_SERVER_TARGET via [ApiEndpointConfig]) and is applied by
 * [BaseUrlInterceptor]. The builder base URL is only a placeholder.
 */
object RetrofitInstance {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlInterceptor())
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .build()

    val api: HeavyRentalApiService by lazy {
        Retrofit.Builder()
            // Placeholder — BaseUrlInterceptor rewrites host/port per ApiEndpointConfig.
            .baseUrl(ApiServerTarget.DEFAULT.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HeavyRentalApiService::class.java)
    }
}
