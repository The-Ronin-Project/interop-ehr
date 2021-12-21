package com.projectronin.interop.tenant.config.model

import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class TenantTest {
    @Test
    fun `check getters`() {
        val batchConfig = BatchConfig(LocalTime.of(20, 0), LocalTime.of(6, 30))
        val authenticationConfig = AuthenticationConfig("public", "private")
        val epic =
            Epic("clientId", authenticationConfig, "https://localhost:8080/", "21.10", "RoninUser", "Ronin Message")
        val tenant = Tenant("mnemonic", batchConfig, epic)
        assertEquals("mnemonic", tenant.mnemonic)
        assertEquals(batchConfig, tenant.batchConfig)
        assertEquals(epic, tenant.vendor)
    }
}
