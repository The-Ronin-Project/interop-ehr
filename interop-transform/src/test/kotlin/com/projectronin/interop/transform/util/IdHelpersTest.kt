package com.projectronin.interop.transform.util

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdHelpersTest {
    @Test
    fun `creates Identifier from Tenant`() {
        val tenant = Tenant(
            internalId = 1,
            mnemonic = "test",
            name = "Test Tenant",
            batchConfig = null,
            vendor = Epic(
                clientId = "clientId",
                authenticationConfig = AuthenticationConfig(
                    authEndpoint = "authEndpoint",
                    publicKey = "public",
                    privateKey = "private"
                ),
                instanceName = "instanceName",
                serviceEndpoint = "endpoint",
                release = "release",
                ehrUserId = "userId",
                messageType = "messageType",
                practitionerProviderSystem = "providerSystem",
                practitionerUserSystem = "userSystem",
                patientMRNSystem = "mrnSystem",
                patientInternalSystem = "internalSystem"
            )
        )

        val expectedIdentifier =
            Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
        assertEquals(expectedIdentifier, tenant.toFhirIdentifier())
    }
}
