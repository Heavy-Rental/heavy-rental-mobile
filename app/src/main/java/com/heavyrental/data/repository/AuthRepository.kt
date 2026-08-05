package com.heavyrental.data.repository

import com.heavyrental.network.RetrofitInstance
import com.heavyrental.network.TokenSession
import com.heavyrental.network.dto.LoginRequest
import com.heavyrental.network.dto.LoginResponse

/**
 * Interim → access Bearer login/logout, per
 * heavy-rental-spring-rest-api/specification/SPEC-auth-login-logout.md §2:
 *
 *   1. GET  /api/auth/getBearerToken  → interim JWT (ROLE_INTERIM, single-use)
 *   2. POST /api/auth/login           → access JWT (ROLE_USER/ADMIN), interim is denylisted server-side
 *   3. POST /api/auth/logout          → access JWT is denylisted server-side
 *
 * Tokens are held in TokenSession only — no persistence in v1.
 */
class AuthRepository {

    private val api = RetrofitInstance.api

    /**
     * @throws retrofit2.HttpException 400 (blank email/password), 401 (invalid credentials
     *   or bad/expired/reused interim), 403 (an access token was used as the interim Bearer)
     */
    suspend fun login(email: String, password: String): LoginResponse {
        val interim = api.getBearerToken().string().trim()
        TokenSession.interimToken = interim

        val response = api.login(
            interimBearer = "Bearer $interim",
            request = LoginRequest(email = email.trim(), password = password)
        )

        // Interim is single-use (denylisted by the server on successful login).
        TokenSession.interimToken = null
        TokenSession.accessToken = response.accessToken
        return response
    }

    /** Best-effort revoke: session is cleared locally even if the server call fails. */
    suspend fun logout() {
        val access = TokenSession.accessToken ?: return
        try {
            api.logout("Bearer $access")
        } finally {
            TokenSession.clear()
        }
    }
}
