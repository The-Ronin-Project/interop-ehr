package com.projectronin.interop.ehr.cerner.auth

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.common.auth.Authentication
import java.time.Instant

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CernerAuthentication(
    override val accessToken: String,
    override val tokenType: String,
    private val expiresIn: Long,
    override val scope: String?,
    override val refreshToken: String?,
) : Authentication {
    override val expiresAt: Instant? = Instant.now().plusSeconds(expiresIn)

    override fun toString(): String = this::class.simpleName!!
}
