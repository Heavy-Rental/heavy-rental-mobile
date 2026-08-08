package com.heavyrental.network

import com.heavyrental.BuildConfig

/**
 * Read-only REST API backend selection.
 *
 * Source of truth is build-time config from `app/api.properties`
 * (optional override: repo-root `local.properties` key `api.server.target`),
 * injected as [BuildConfig.API_SERVER_TARGET].
 *
 * Edit the properties file, Sync/Rebuild, then Run. There is no in-app toggle.
 */
object ApiEndpointConfig {

    val currentTarget: ApiServerTarget =
        ApiServerTarget.fromName(BuildConfig.API_SERVER_TARGET)

    val currentBaseUrl: String
        get() = currentTarget.baseUrl
}
