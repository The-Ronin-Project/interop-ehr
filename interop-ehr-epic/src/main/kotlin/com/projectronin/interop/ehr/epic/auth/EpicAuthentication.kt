package com.projectronin.interop.ehr.epic.auth

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.common.auth.Authentication
import java.time.Instant

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class EpicAuthentication(
    override val accessToken: String,
    override val tokenType: String,
    private val expiresIn: Long,
    override val scope: String
) : Authentication {
    override val expiresAt: Instant? = Instant.now().plusSeconds(expiresIn)

    // Refresh token is not supported by Epic's authentication.
    override val refreshToken: String? = null
}
