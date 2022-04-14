package com.projectronin.interop.tenant.config.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthenticationConfigTest {
    @Test
    fun `check getters`() {
        val config = AuthenticationConfig("authEndpoint", "publicKey", "privateKey")
        assertEquals("authEndpoint", config.authEndpoint)
        assertEquals("publicKey", config.publicKey)
        assertEquals("privateKey", config.privateKey)
    }

    @Test
    fun `ensure toString is overwritten`() {
        val config = AuthenticationConfig("authEndpoint", "publicKey", "privateKey")
        assertEquals("AuthenticationConfig", config.toString())
    }
}
