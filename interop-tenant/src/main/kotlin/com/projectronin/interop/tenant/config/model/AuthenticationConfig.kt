package com.projectronin.interop.tenant.config.model

/**
 * Configuration associated to authentication for a tenant.
 * @property authEndpoint The URL at which to connect to authentication services.
 */
interface AuthenticationConfig {
    val authEndpoint: String
}
