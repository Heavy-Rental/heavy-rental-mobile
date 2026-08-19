package com.heavyrental.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Base64

/**
 * Reads claims out of an access JWT without verifying its signature.
 *
 * Verification is unnecessary here: the token was just handed to us by our own
 * POST /api/auth/login (or /api/auth/google) call over TLS. We only need the
 * `roles` claim, which the backend does not repeat in the login response body
 * (see AuthDtos.LoginResponse).
 *
 * Every failure mode — malformed token, bad base64, bad JSON, missing claim —
 * yields no roles, so callers fail closed and treat the account as non-staff.
 */
object JwtClaims {

    private val ADMIN_ROLES = setOf("ROLE_ADMIN", "ROLE_DRIVER")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Roles from the token's `roles` claim, or empty if it can't be read. */
    fun rolesOf(jwt: String): List<String> {
        val segments = jwt.split('.')
        if (segments.size != 3) return emptyList()

        val payload = try {
            val decoded = Base64.getUrlDecoder().decode(padBase64(segments[1]))
            json.parseToJsonElement(String(decoded, Charsets.UTF_8)) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return when (val roles = payload["roles"]) {
            // Expected shape, e.g. ["ROLE_ADMIN"].
            is JsonArray -> roles.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
            // Tolerate a single role emitted as a bare string.
            is JsonPrimitive -> if (roles.isString) listOf(roles.content) else emptyList()
            else -> emptyList()
        }
    }

    /** True only for staff accounts — ROLE_USER and anything unrecognised are rejected. */
    fun isStaff(jwt: String): Boolean = rolesOf(jwt).any { it in ADMIN_ROLES }

    /** JWT segments are base64url without padding; getUrlDecoder() requires it. */
    private fun padBase64(segment: String): String = when (segment.length % 4) {
        2 -> "$segment=="
        3 -> "$segment="
        else -> segment
    }
}
