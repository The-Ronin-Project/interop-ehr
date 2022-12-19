package com.projectronin.interop.ehr.factory

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.EpicAuthenticationConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class EHRFactoryTest {
    @Test
    fun `vendor factory found for tenant`() {
        val epicVendorFactory = mockk<VendorFactory> {
            every { vendorType } returns VendorType.EPIC
        }
        val ehrFactory = EHRFactory(listOf(epicVendorFactory))
        val vendor =
            Epic(
                "clientId",
                EpicAuthenticationConfig("authEndpoint", "public", "private"),
                "endpoint",
                "Epic Sandbox",
                "release",
                "userId",
                "message",
                "providerSystem",
                "userSystem",
                "mrnSystem",
                "internalSystem",
                "csnSystem",
                "patientMRNTypeText",
                "urn:epic:apporchard.curprod",
                "urn:oid:1.2.840.114350.1.13.297.3.7.2.686980"
            )
        val tenant = Tenant(1, "TENANT", "Test Tenant", ZoneOffset.UTC, null, vendor)

        val vendorFactory = ehrFactory.getVendorFactory(tenant)
        assertEquals(epicVendorFactory, vendorFactory)
    }

    @Test
    fun `vendor factory not found for tenant should throw IllegalStateException`() {
        // Today we have to set this up with no factories since we only currently support 1 VendorType.
        val ehrFactory = EHRFactory(listOf())
        val vendor =
            Epic(
                "clientId",
                EpicAuthenticationConfig("authEndpoint", "public", "private"),
                "endpoint",
                "Epic Sandbox",
                "release",
                "userId",
                "message",
                "providerSystem",
                "userSystem",
                "mrnSystem",
                "internalSystem",
                "csnSystem",
                "patientMRNTypeText",
                "urn:epic:apporchard.curprod",
                "urn:oid:1.2.840.114350.1.13.297.3.7.2.686980"
            )
        val tenant = Tenant(1, "TENANT", "Test Tenant", ZoneOffset.UTC, null, vendor)

        val exception = assertThrows(IllegalStateException::class.java) {
            ehrFactory.getVendorFactory(tenant)
        }
        assertEquals("No VendorFactory registered for EPIC", exception.message)
    }
}
