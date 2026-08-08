package com.heavyrental.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites each request's scheme/host/port to match [ApiEndpointConfig.currentTarget].
 * Path and query are left unchanged so Retrofit relative paths keep working.
 */
class BaseUrlInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = ApiEndpointConfig.currentBaseUrl.toHttpUrl()

        val newUrl = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()

        return chain.proceed(
            request.newBuilder()
                .url(newUrl)
                .build()
        )
    }
}
