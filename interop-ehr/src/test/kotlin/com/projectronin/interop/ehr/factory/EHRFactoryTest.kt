package com.projectronin.interop.ehr.factory

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

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
                AuthenticationConfig("authEndpoint", "public", "private"),
                "endpoint",
                "Epic Sandbox",
                "release",
                "userId",
                "message",
                "providerSystem",
                "userSystem",
                "mrnSystem",
                "internalSystem"
            )
        val tenant = Tenant(1, "TENANT", null, vendor)

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
                AuthenticationConfig("authEndpoint", "public", "private"),
                "endpoint",
                "Epic Sandbox",
                "release",
                "userId",
                "message",
                "providerSystem",
                "userSystem",
                "mrnSystem",
                "internalSystem"
            )
        val tenant = Tenant(1, "TENANT", null, vendor)

        val exception = assertThrows(IllegalStateException::class.java) {
            ehrFactory.getVendorFactory(tenant)
        }
        assertEquals("No VendorFactory registered for EPIC", exception.message)
    }
}
