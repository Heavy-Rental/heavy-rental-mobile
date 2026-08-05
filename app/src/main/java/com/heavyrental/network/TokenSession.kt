package com.heavyrental.network

/**
 * Holds the current auth session in memory only — no persistence in v1
 * (see specification/product/01-login.md "Known gaps"). Session is lost on
 * process death, same as the rest of the app's in-memory state.
 */
object TokenSession {

    /** Single-use interim JWT between GET /getBearerToken and POST /login. */
    @Volatile
    var interimToken: String? = null

    /** Session JWT returned by POST /login; used for all subsequent calls until logout. */
    @Volatile
    var accessToken: String? = null

    val isLoggedIn: Boolean get() = accessToken != null

    fun clear() {
        interimToken = null
        accessToken = null
    }
}
