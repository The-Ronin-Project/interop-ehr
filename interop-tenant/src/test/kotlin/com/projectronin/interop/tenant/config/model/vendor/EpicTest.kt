package com.projectronin.interop.tenant.config.model.vendor

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicTest {
    @Test
    fun `check getters`() {
        val authenticationConfig = AuthenticationConfig("public", "private")
        val epic =
            Epic("clientId", authenticationConfig, "https://localhost:8080/", "21.10", "RoninUser", "Ronin Message")
        assertEquals(VendorType.EPIC, epic.type)
        assertEquals("clientId", epic.clientId)
        assertEquals(authenticationConfig, epic.authenticationConfig)
        assertEquals("https://localhost:8080/", epic.serviceEndpoint)
        assertEquals("21.10", epic.release)
        assertEquals("RoninUser", epic.ehrUserId)
        assertEquals("Ronin Message", epic.messageType)
    }
}
