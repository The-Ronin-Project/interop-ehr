package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader
import java.io.IOException
import java.nio.file.Paths
import java.time.LocalTime

class TenantServiceEmbeddedTest {

    @Test
    fun `no tenant found`() {
        val tenantService = TenantServiceEmbedded(DefaultResourceLoader(), "valid_tenants.yaml")
        assertNull(tenantService.getTenantForMnemonic("UNKNOWN"))
    }

    @Test
    fun `tenants load classpath`() {
        val tenantService = TenantServiceEmbedded(DefaultResourceLoader(), "valid_tenants.yaml")
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
    fun `tenants load from filesystem`() {
        val modulePath = Paths.get("").toAbsolutePath().toString()
        val tenantService =
            TenantServiceEmbedded(DefaultResourceLoader(), "file:$modulePath/src/test/resources/valid_tenants.yaml")
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
            TenantServiceEmbedded(DefaultResourceLoader(), "unknown.yaml")
        }
        assertEquals("class path resource [unknown.yaml] cannot be opened because it does not exist", exception.message)
    }
}
