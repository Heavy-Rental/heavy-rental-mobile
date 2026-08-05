package com.heavyrental.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the current session's access-token Bearer to every request except the
 * three auth endpoints, which carry their own interim/access Bearer explicitly
 * (see AuthRepository — getBearerToken needs no auth, login needs the interim
 * token, logout needs the access token passed as an explicit @Header).
 */
class AuthInterceptor : Interceptor {

    private val authPaths = setOf(
        "/api/auth/getBearerToken",
        "/api/auth/login",
        "/api/auth/logout"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = TokenSession.accessToken

        val authorized = if (request.url.encodedPath !in authPaths && token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(authorized)
    }
}
