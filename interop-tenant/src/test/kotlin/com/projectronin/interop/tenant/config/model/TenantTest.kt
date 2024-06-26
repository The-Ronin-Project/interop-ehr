package com.projectronin.interop.tenant.config.model

import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.time.ZoneId

class TenantTest {
    @Test
    fun `check getters`() {
        val batchConfig = BatchConfig(LocalTime.of(20, 0), LocalTime.of(6, 30))
        val authenticationConfig = EpicAuthenticationConfig("authEndpoint", "public", "private")
        val timezone = ZoneId.of("America/Chicago")
        val epic =
            Epic(
                "clientId",
                authenticationConfig,
                "https://localhost:8080/",
                "Epic Sandbox",
                "21.10",
                "RoninUser",
                "Ronin Message",
                "urn:oid:1.2.840.114350.1.13.0.1.7.2.836982",
                "urn:oid:1.2.840.114350.1.13.0.1.7.2.697780",
                "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
                "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.15",
                "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.8",
                "MRN",
                "urn:epic:apporchard.curprod",
                "urn:oid:1.2.840.114350.1.13.297.3.7.2.686980",
            )
        val tenant =
            Tenant(
                1,
                "mnemonic",
                "Memorial National Eastern Masonic Oncology Naturopathic Institute, Consolidated",
                timezone,
                batchConfig,
                epic,
                true,
            )
        assertEquals(1, tenant.internalId)
        assertEquals("mnemonic", tenant.mnemonic)
        assertEquals("Memorial National Eastern Masonic Oncology Naturopathic Institute, Consolidated", tenant.name)
        assertEquals(timezone, tenant.timezone)
        assertEquals(batchConfig, tenant.batchConfig)
        assertEquals(epic, tenant.vendor)
        assertTrue(tenant.monitoredIndicator!!)
    }
}
