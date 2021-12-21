package com.projectronin.interop.ehr.auth

import java.time.Instant

/**
 * Defines an Authentication for accessing an EHR.
 */
interface Authentication {
    /**
     * The access token issued by the authentication service.
     */
    val accessToken: String

    /**
     * The type of token.
     */
    val tokenType: String

    /**
     * The instant this authentication expires. If not provided, it is assumed that a new authentication should be provided on each request.
     */
    val expiresAt: Instant?

    /**
     * The token used to refresh this authentication.
     */
    val refreshToken: String?

    /**
     * The scope appropriate to this authentication.
     */
    val scope: String?
}
