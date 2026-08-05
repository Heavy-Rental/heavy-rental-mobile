package com.heavyrental.network.dto

import kotlinx.serialization.Serializable

// Mirrors heavy-rental-spring-rest-api's SPEC-auth-login-logout.md §6.

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    // Field name is legacy on the server; the value is the authenticated email.
    val username: String
)

@Serializable
data class MessageResponse(
    val message: String
)
