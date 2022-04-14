package com.projectronin.interop.tenant.config.model

import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class TenantTest {
    @Test
    fun `check getters`() {
        val batchConfig = BatchConfig(LocalTime.of(20, 0), LocalTime.of(6, 30))
        val authenticationConfig = AuthenticationConfig("authEndpoint", "public", "private")
        val epic =
            Epic(
                "clientId",
                authenticationConfig,
                "https://localhost:8080/",
                "21.10",
                "RoninUser",
                "Ronin Message",
                "urn:oid:1.2.840.114350.1.13.0.1.7.2.836982",
                "urn:oid:1.2.840.114350.1.13.0.1.7.2.697780",
                "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"
            )
        val tenant = Tenant(1, "mnemonic", batchConfig, epic)
        assertEquals(1, tenant.internalId)
        assertEquals("mnemonic", tenant.mnemonic)
        assertEquals(batchConfig, tenant.batchConfig)
        assertEquals(epic, tenant.vendor)
    }
}
