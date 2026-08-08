package com.heavyrental.network

/**
 * Selectable REST API backends for local development.
 *
 * Host machine:
 * - Mockoon / Prism: `localhost:8081`
 * - Spring Boot:     `localhost:8080`
 *
 * Android emulator uses `10.0.2.2` as the alias for the host loopback.
 * Physical devices need the host LAN IP (not covered by these fixed URLs).
 */
enum class ApiServerTarget(
    val displayName: String,
    /** Trailing slash required by Retrofit [retrofit2.Retrofit.Builder.baseUrl]. */
    val baseUrl: String,
) {
    MOCKOON(
        displayName = "Mock API (Mockoon)",
        baseUrl = "http://10.0.2.2:8081/",
    ),
    SPRING_BOOT(
        displayName = "Spring Boot API",
        baseUrl = "http://10.0.2.2:8080/",
    );

    companion object {
        val DEFAULT = MOCKOON

        fun fromName(name: String?): ApiServerTarget =
            entries.find { it.name == name } ?: DEFAULT
    }
}
