package com.projectronin.interop.tenant.config.model.vendor

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CernerTest {
    @Test
    fun `check getters`() {
        val authenticationConfig = AuthenticationConfig("authEndpoint", "public", "private", "accountId", "secret")
        val cerner = Cerner(
            "instanceName",
            "clientId",
            authenticationConfig,
            "https://localhost:8080/serviceEndpoint",
            "mrn"
        )
        assertEquals(VendorType.CERNER, cerner.type)
        assertEquals("clientId", cerner.clientId)
        assertEquals(authenticationConfig, cerner.authenticationConfig)
        assertEquals("https://localhost:8080/serviceEndpoint", cerner.serviceEndpoint)
    }
}
