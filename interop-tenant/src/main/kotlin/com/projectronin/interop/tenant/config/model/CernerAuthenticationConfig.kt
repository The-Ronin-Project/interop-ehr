package com.projectronin.interop.tenant.config.model

/**
 * Configuration associated to authentication for a Cerner tenant.
 * @property authEndpoint The URL at which to connect to authentication services.
 * @property accountId
 * @property secret
 */
data class CernerAuthenticationConfig(
    override val authEndpoint: String,
    val accountId: String = "",
    val secret: String = ""
) : AuthenticationConfig {
    override fun toString(): String = this::class.simpleName!!
}
