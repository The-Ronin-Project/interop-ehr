package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.TenantDAO
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime

class TenantServiceTest {
    private lateinit var tenantDAO: TenantDAO
    private lateinit var service: TenantService

    @BeforeEach
    fun setup() {
        tenantDAO = mockk()

        service = TenantService(tenantDAO)
    }

    @Test
    fun `no tenant found`() {
        every { tenantDAO.getTenantForMnemonic("UNKNOWN") }.returns(null)

        val tenant = service.getTenantForMnemonic("UNKNOWN")
        assertNull(tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("UNKNOWN")
        }
        confirmVerified(tenantDAO)
    }

    @Test
    fun `no ehr tenant found`() {
        val ehrDO = mockk<EhrDO> {
            every { id } returns 1
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "clientId"
            every { publicKey } returns "publicKey"
            every { privateKey } returns "privateKey"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO
        every { tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC) } returns null

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertNull(tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC)
        }
    }

    @Test
    fun `epic tenant found`() {
        val ehrDO = mockk<EhrDO> {
            every { id } returns 1
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "clientId"
            every { publicKey } returns "publicKey"
            every { privateKey } returns "privateKey"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        val epicTenantDO = mockk<EpicTenantDO> {
            every { tenantId } returns 1
            every { release } returns "release"
            every { serviceEndpoint } returns "http://localhost/"
            every { ehrUserId } returns "ehr user"
            every { messageType } returns "message type"
        }
        every { tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC) } returns epicTenantDO

        val expectedTenant = Tenant(
            mnemonic = "Tenant1",
            batchConfig = null,
            vendor = Epic(
                clientId = "clientId",
                authenticationConfig = AuthenticationConfig(
                    publicKey = "publicKey",
                    privateKey = "privateKey"
                ),
                serviceEndpoint = "http://localhost/",
                release = "release",
                ehrUserId = "ehr user",
                messageType = "message type"
            )
        )

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(expectedTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC)
        }
    }

    @Test
    fun `tenant has no batch config if no batch start`() {
        val ehrDO = mockk<EhrDO> {
            every { id } returns 1
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "clientId"
            every { publicKey } returns "publicKey"
            every { privateKey } returns "privateKey"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns LocalTime.of(6, 0)
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        val epicTenantDO = mockk<EpicTenantDO> {
            every { tenantId } returns 1
            every { release } returns "release"
            every { serviceEndpoint } returns "http://localhost/"
            every { ehrUserId } returns "ehr user"
            every { messageType } returns "message type"
        }
        every { tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC) } returns epicTenantDO

        val expectedTenant = Tenant(
            mnemonic = "Tenant1",
            batchConfig = null,
            vendor = Epic(
                clientId = "clientId",
                authenticationConfig = AuthenticationConfig(
                    publicKey = "publicKey",
                    privateKey = "privateKey"
                ),
                serviceEndpoint = "http://localhost/",
                release = "release",
                ehrUserId = "ehr user",
                messageType = "message type"
            )
        )

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(expectedTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC)
        }
    }

    @Test
    fun `tenant has no batch config if no batch end`() {
        val ehrDO = mockk<EhrDO> {
            every { id } returns 1
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "clientId"
            every { publicKey } returns "publicKey"
            every { privateKey } returns "privateKey"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns LocalTime.of(20, 0)
            every { availableBatchEnd } returns null
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        val epicTenantDO = mockk<EpicTenantDO> {
            every { tenantId } returns 1
            every { release } returns "release"
            every { serviceEndpoint } returns "http://localhost/"
            every { ehrUserId } returns "ehr user"
            every { messageType } returns "message type"
        }
        every { tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC) } returns epicTenantDO

        val expectedTenant = Tenant(
            mnemonic = "Tenant1",
            batchConfig = null,
            vendor = Epic(
                clientId = "clientId",
                authenticationConfig = AuthenticationConfig(
                    publicKey = "publicKey",
                    privateKey = "privateKey"
                ),
                serviceEndpoint = "http://localhost/",
                release = "release",
                ehrUserId = "ehr user",
                messageType = "message type"
            )
        )

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(expectedTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC)
        }
    }

    @Test
    fun `tenant with batch config`() {
        val ehrDO = mockk<EhrDO> {
            every { id } returns 1
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "clientId"
            every { publicKey } returns "publicKey"
            every { privateKey } returns "privateKey"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns LocalTime.of(20, 0)
            every { availableBatchEnd } returns LocalTime.of(6, 0)
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        val epicTenantDO = mockk<EpicTenantDO> {
            every { tenantId } returns 1
            every { release } returns "release"
            every { serviceEndpoint } returns "http://localhost/"
            every { ehrUserId } returns "ehr user"
            every { messageType } returns "message type"
        }
        every { tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC) } returns epicTenantDO

        val expectedTenant = Tenant(
            mnemonic = "Tenant1",
            batchConfig = BatchConfig(
                availableStart = LocalTime.of(20, 0),
                availableEnd = LocalTime.of(6, 0)
            ),
            vendor = Epic(
                clientId = "clientId",
                authenticationConfig = AuthenticationConfig(
                    publicKey = "publicKey",
                    privateKey = "privateKey"
                ),
                serviceEndpoint = "http://localhost/",
                release = "release",
                ehrUserId = "ehr user",
                messageType = "message type"
            )
        )

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(expectedTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            tenantDAO.getEHRTenant<EpicTenantDO>(1, VendorType.EPIC)
        }
    }
}
