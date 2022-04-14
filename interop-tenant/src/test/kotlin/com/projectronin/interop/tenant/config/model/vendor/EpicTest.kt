package com.projectronin.interop.tenant.config.model.vendor

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicTest {
    @Test
    fun `check getters`() {
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

        assertEquals(VendorType.EPIC, epic.type)
        assertEquals("clientId", epic.clientId)
        assertEquals(authenticationConfig, epic.authenticationConfig)
        assertEquals("https://localhost:8080/", epic.serviceEndpoint)
        assertEquals("21.10", epic.release)
        assertEquals("RoninUser", epic.ehrUserId)
        assertEquals("Ronin Message", epic.messageType)
        assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.2.836982", epic.practitionerProviderSystem)
        assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.2.697780", epic.practitionerUserSystem)
        assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14", epic.mrnSystem)
    }
}
