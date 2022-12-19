package com.projectronin.interop.tenant.config.model

/**
 * Configuration associated to authentication for an Epic tenant.
 * @property authEndpoint The URL at which to connect to authentication services.
 * @property publicKey The public key used for authentication.
 * @property privateKey The private key used for authentication.
 */
data class EpicAuthenticationConfig(
    override val authEndpoint: String,
    val publicKey: String,
    val privateKey: String,
) : AuthenticationConfig {
    override fun toString(): String = this::class.simpleName!!
}
