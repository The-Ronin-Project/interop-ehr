package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalTime

class TenantServiceEmbeddedTest {

    @Test
    fun `no tenant found`() {
        val tenantService = TenantServiceEmbedded("valid_tenants.yaml")
        assertNull(tenantService.getTenantForMnemonic("UNKNOWN"))
    }

    @Test
    fun `tenants load from yaml`() {
        val tenantService = TenantServiceEmbedded("valid_tenants.yaml")
        assertNotNull(tenantService.getTenantForMnemonic("PSJ"))

        val tenant = tenantService.getTenantForMnemonic("SAND_AO")
        assertNotNull(tenant)
        assertEquals("SAND_AO", tenant!!.mnemonic)

        val batchConfig = tenant.batchConfig
        assertNotNull(batchConfig)
        assertEquals(LocalTime.of(22, 0, 0), batchConfig!!.availableStart)
        assertEquals(LocalTime.of(6, 0, 0), batchConfig.availableEnd)

        val vendor = tenant.vendor
        assertEquals(VendorType.EPIC, vendor.type)
        assertEquals("101", vendor.clientId)
        assertEquals("https://example.com", vendor.serviceEndpoint)

        val authenticationConfig = vendor.authenticationConfig
        assertEquals("pubkey", authenticationConfig.publicKey)
        // We will not assert the proper key is here, just that it was loaded
        assertNotNull(authenticationConfig.privateKey)

        // Cast to epic to ensure epic specific field are populated
        val epicVendor = vendor as Epic
        assertEquals("1.0", epicVendor.release)
        assertEquals("1", epicVendor.ehrUserId)
        assertEquals("Message Report", epicVendor.messageType)
    }

    @Test
    fun `missing file`() {
        val exception = assertThrows(IOException::class.java) {
            TenantServiceEmbedded("unknown.yaml")
        }
        assertEquals("Clients configuration file unknown.yaml not found.", exception.message)
    }
}
