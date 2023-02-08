package com.projectronin.interop.tenant.config.model.vendor

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.CernerAuthenticationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CernerTest {
    @Test
    fun `check getters`() {
        val authenticationConfig = CernerAuthenticationConfig("authEndpoint", "accountId", "secret")
        val cerner = Cerner(
            "instanceName",
            "clientId",
            authenticationConfig,
            "https://localhost:8080/serviceEndpoint",
            "mrn",
            "practitioner",
            "topic",
            "category",
            "priority"
        )
        assertEquals(VendorType.CERNER, cerner.type)
        assertEquals("clientId", cerner.clientId)
        assertEquals(authenticationConfig, cerner.authenticationConfig)
        assertEquals("https://localhost:8080/serviceEndpoint", cerner.serviceEndpoint)
        assertEquals("mrn", cerner.patientMRNSystem)
        assertEquals("practitioner", cerner.messagePractitioner)
        assertEquals("topic", cerner.messageTopic)
        assertEquals("category", cerner.messageCategory)
        assertEquals("priority", cerner.messagePriority)
    }
}
