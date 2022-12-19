package com.projectronin.interop.tenant.config.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CernerAuthenticationConfigTest {
    @Test
    fun `check getters`() {
        val config = CernerAuthenticationConfig("authEndpoint", "account", "secret")
        assertEquals("authEndpoint", config.authEndpoint)
        assertEquals("account", config.accountId)
        assertEquals("secret", config.secret)
    }

    @Test
    fun `ensure toString is overwritten`() {
        val config = CernerAuthenticationConfig("authEndpoint", "account", "secret")
        assertEquals("CernerAuthenticationConfig", config.toString())
    }
}
