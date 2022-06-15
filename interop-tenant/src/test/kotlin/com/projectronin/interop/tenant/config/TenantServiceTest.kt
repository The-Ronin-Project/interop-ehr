package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.EhrDAO
import com.projectronin.interop.tenant.config.data.EpicTenantDAO
import com.projectronin.interop.tenant.config.data.TenantDAO
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import com.projectronin.interop.tenant.config.exception.NoEHRFoundException
import com.projectronin.interop.tenant.config.exception.NoTenantFoundException
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
import org.junit.jupiter.api.assertThrows
import org.ktorm.database.Database
import java.time.LocalTime

class TenantServiceTest {
    private lateinit var database: Database
    private lateinit var tenantDAO: TenantDAO
    private lateinit var ehrDAO: EhrDAO
    private lateinit var epicTenantDAO: EpicTenantDAO
    private lateinit var service: TenantService
    private lateinit var ehrTenantDAOFactory: EHRTenantDAOFactory

    private val standardEHRDO1 = mockk<EhrDO> {
        every { id } returns 1
        every { instanceName } returns "Epic Sandbox"
        every { vendorType } returns VendorType.EPIC
        every { clientId } returns "clientId"
        every { publicKey } returns "publicKey"
        every { privateKey } returns "privateKey"
    }
    private val standardEHRDO2 = mockk<EhrDO> {
        every { id } returns 2
        every { instanceName } returns "Epic Sandbox Instance 2"
        every { vendorType } returns VendorType.EPIC
        every { clientId } returns "clientId2"
        every { publicKey } returns "publicKey2"
        every { privateKey } returns "privateKey2"
    }
    private val standardEpicTenantDO = mockk<EpicTenantDO> {
        every { tenantId } returns 1
        every { release } returns "release"
        every { serviceEndpoint } returns "http://localhost/"
        every { authEndpoint } returns "http://localhost/oauth2/token"
        every { ehrUserId } returns "ehr user"
        every { messageType } returns "message type"
        every { practitionerProviderSystem } returns "practitionerSystemExample"
        every { practitionerUserSystem } returns "userSystemExample"
        every { patientMRNSystem } returns "mrnSystemExample"
        every { patientInternalSystem } returns "internalSystemExample"
        every { hsi } returns null
    }
    private val standardEpicTenantDO2 = mockk<EpicTenantDO> {
        every { tenantId } returns 2
        every { release } returns "release2"
        every { serviceEndpoint } returns "http://otherhost/"
        every { authEndpoint } returns "http://otherhost/oauth2/token"
        every { ehrUserId } returns "ehr user2"
        every { messageType } returns "message type2"
        every { practitionerProviderSystem } returns "practitionerSystemExample2"
        every { practitionerUserSystem } returns "userSystemExample2"
        every { mrnSystem } returns "mrnSystemExample2"
        every { hsi } returns null
    }
    private val standardTenantDO = mockk<TenantDO> {
        every { id } returns 1
        every { mnemonic } returns "Tenant1"
        every { ehr } returns standardEHRDO1
        every { availableBatchStart } returns null
        every { availableBatchEnd } returns null
    }

    private val standardTenantDO2 = mockk<TenantDO> {
        every { id } returns 2
        every { mnemonic } returns "Tenant2"
        every { ehr } returns standardEHRDO2
        every { availableBatchStart } returns null
        every { availableBatchEnd } returns null
    }

    private val standardTenant = Tenant(
        internalId = 1,
        mnemonic = "Tenant1",
        batchConfig = null,
        vendor = Epic(
            clientId = "clientId",
            instanceName = "Epic Sandbox",
            authenticationConfig = AuthenticationConfig(
                authEndpoint = "http://localhost/oauth2/token",
                publicKey = "publicKey",
                privateKey = "privateKey"
            ),
            serviceEndpoint = "http://localhost/",
            release = "release",
            ehrUserId = "ehr user",
            messageType = "message type",
            practitionerProviderSystem = "practitionerSystemExample",
            practitionerUserSystem = "userSystemExample",
            patientMRNSystem = "mrnSystemExample",
            patientInternalSystem = "internalSystemExample"
        )
    )

    private val standardTenant2 = Tenant(
        internalId = 2,
        mnemonic = "Tenant2",
        batchConfig = null,
        vendor = Epic(
            clientId = "clientId2",
            instanceName = "Epic Sandbox Instance 2",
            authenticationConfig = AuthenticationConfig(
                authEndpoint = "http://otherhost/oauth2/token",
                publicKey = "publicKey2",
                privateKey = "privateKey2"
            ),
            serviceEndpoint = "http://otherhost/",
            release = "release2",
            ehrUserId = "ehr user2",
            messageType = "message type2",
            practitionerProviderSystem = "practitionerSystemExample2",
            practitionerUserSystem = "userSystemExample2",
            mrnSystem = "mrnSystemExample2"
        )
    )

    @BeforeEach
    fun setup() {
        tenantDAO = mockk()
        ehrDAO = mockk()
        epicTenantDAO = mockk()
        database = mockk()
        ehrTenantDAOFactory = mockk<EHRTenantDAOFactory> {
            every { getEHRTenantDAO(any()) } returns epicTenantDAO
        }
        service = TenantService(tenantDAO, ehrDAO, ehrTenantDAOFactory)
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
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns standardTenantDO
        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns null

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertNull(tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            epicTenantDAO.getByTenantMnemonic("Tenant1")
        }
    }

    @Test
    fun `epic tenant found`() {
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns standardTenantDO

        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns standardEpicTenantDO

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(standardTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            epicTenantDAO.getByTenantMnemonic("Tenant1")
        }
    }

    @Test
    fun `tenant has no batch config if no batch start`() {
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns standardEHRDO1
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns LocalTime.of(6, 0)
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns standardEpicTenantDO

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(standardTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            epicTenantDAO.getByTenantMnemonic("Tenant1")
        }
    }

    @Test
    fun `tenant has no batch config if no batch end`() {
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns standardEHRDO1
            every { availableBatchStart } returns LocalTime.of(20, 0)
            every { availableBatchEnd } returns null
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns standardEpicTenantDO

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(standardTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            epicTenantDAO.getByTenantMnemonic("Tenant1")
        }
    }

    @Test
    fun `tenant with batch config`() {
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns standardEHRDO1
            every { availableBatchStart } returns LocalTime.of(20, 0)
            every { availableBatchEnd } returns LocalTime.of(6, 0)
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns standardEpicTenantDO

        val expectedTenant = Tenant(
            internalId = 1,
            mnemonic = "Tenant1",
            batchConfig = BatchConfig(
                availableStart = LocalTime.of(20, 0),
                availableEnd = LocalTime.of(6, 0)
            ),
            vendor = Epic(
                clientId = "clientId",
                instanceName = "Epic Sandbox",
                authenticationConfig = AuthenticationConfig(
                    authEndpoint = "http://localhost/oauth2/token",
                    publicKey = "publicKey",
                    privateKey = "privateKey"
                ),
                serviceEndpoint = "http://localhost/",
                release = "release",
                ehrUserId = "ehr user",
                messageType = "message type",
                practitionerProviderSystem = "practitionerSystemExample",
                practitionerUserSystem = "userSystemExample",
                patientMRNSystem = "mrnSystemExample",
                patientInternalSystem = "internalSystemExample"
            )
        )

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(expectedTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic(standardTenant.mnemonic)
            epicTenantDAO.getByTenantMnemonic("Tenant1")
        }
    }

    @Test
    fun `epic tenant with hsi found`() {
        every { tenantDAO.getTenantForMnemonic(standardTenant.mnemonic) } returns standardTenantDO

        val epicTenantDO = mockk<EpicTenantDO> {
            every { tenantId } returns 1
            every { release } returns "release"
            every { serviceEndpoint } returns "http://localhost/"
            every { authEndpoint } returns "http://localhost/oauth2/token"
            every { ehrUserId } returns "ehr user"
            every { messageType } returns "message type"
            every { practitionerProviderSystem } returns "practitionerSystemExample"
            every { practitionerUserSystem } returns "userSystemExample"
            every { patientMRNSystem } returns "mrnSystemExample"
            every { patientInternalSystem } returns "internalSystemExample"
            every { hsi } returns "urn:epic:apporchard.curprod"
        }
        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns epicTenantDO

        val expectedTenant = Tenant(
            internalId = 1,
            mnemonic = "Tenant1",
            batchConfig = null,
            vendor = Epic(
                clientId = "clientId",
                instanceName = "Epic Sandbox",
                authenticationConfig = AuthenticationConfig(
                    authEndpoint = "http://localhost/oauth2/token",
                    publicKey = "publicKey",
                    privateKey = "privateKey"
                ),
                serviceEndpoint = "http://localhost/",
                release = "release",
                ehrUserId = "ehr user",
                messageType = "message type",
                practitionerProviderSystem = "practitionerSystemExample",
                practitionerUserSystem = "userSystemExample",
                patientMRNSystem = "mrnSystemExample",
                patientInternalSystem = "internalSystemExample",
                hsi = "urn:epic:apporchard.curprod"
            )
        )

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(expectedTenant, tenant)

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            epicTenantDAO.getByTenantMnemonic("Tenant1")
        }
    }

    @Test
    fun `getAll none found`() {
        every { tenantDAO.getAllTenants() } returns emptyList()

        val tenants = service.getAllTenants()
        assertEquals(0, tenants.size)
    }

    @Test
    fun `getAll finds 'em`() {
        every { tenantDAO.getAllTenants() } returns listOf(standardTenantDO)
        every { epicTenantDAO.getAll() } returns listOf(standardEpicTenantDO)

        val tenants = service.getAllTenants()
        assertEquals(1, tenants.size)
        assertEquals(standardTenant, tenants.first())
    }

    @Test
    fun `getAll returns multiple tenants with multiple instances`() {
        every { tenantDAO.getAllTenants() } returns listOf(standardTenantDO, standardTenantDO2)
        every { epicTenantDAO.getAll() } returns listOf(standardEpicTenantDO, standardEpicTenantDO2)

        val tenants = service.getAllTenants()
        assertEquals(2, tenants.size)
        assertEquals(listOf(standardTenant, standardTenant2), tenants)
    }

    @Test
    fun `insert ok`() {
        every { ehrDAO.getByInstance("Epic Sandbox") } returns standardEHRDO1
        every { tenantDAO.insertTenant(any()) } returns standardTenantDO
        every { epicTenantDAO.insert(any()) } returns standardEpicTenantDO

        val tenant = service.insertTenant(standardTenant)
        assertEquals(standardTenant, tenant)
    }

    @Test
    fun `insert ok with batch`() {
        val newTenant = Tenant(
            internalId = 1,
            mnemonic = "Tenant1",
            batchConfig = BatchConfig(
                availableStart = LocalTime.of(20, 0),
                availableEnd = LocalTime.of(6, 0)
            ),
            vendor = Epic(
                clientId = "clientId",
                instanceName = "Epic Sandbox",
                authenticationConfig = AuthenticationConfig(
                    authEndpoint = "http://localhost/oauth2/token",
                    publicKey = "publicKey",
                    privateKey = "privateKey"
                ),
                serviceEndpoint = "http://localhost/",
                release = "release",
                ehrUserId = "ehr user",
                messageType = "message type",
                practitionerProviderSystem = "practitionerSystemExample",
                practitionerUserSystem = "userSystemExample",
                patientMRNSystem = "mrnSystemExample",
                patientInternalSystem = "internalSystemExample"
            )
        )
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { ehr } returns standardEHRDO1
            every { availableBatchStart } returns LocalTime.of(20, 0)
            every { availableBatchEnd } returns LocalTime.of(6, 0)
        }
        every { ehrDAO.getByInstance("Epic Sandbox") } returns standardEHRDO1
        every { tenantDAO.insertTenant(any()) } returns tenantDO
        every { epicTenantDAO.insert(any()) } returns standardEpicTenantDO

        val tenant = service.insertTenant(newTenant)
        assertEquals(newTenant, tenant)
    }

    @Test
    fun `insert bad when there's no EHRs in the db`() {
        every { ehrDAO.getByInstance("Epic Sandbox") } returns null
        assertThrows<NoEHRFoundException> { service.insertTenant(standardTenant) }
    }

    @Test
    fun `update ok`() {
        every { ehrDAO.getByInstance("Epic Sandbox") } returns standardEHRDO1
        every { tenantDAO.getTenantForMnemonic(standardTenant.mnemonic) } returns standardTenantDO
        every { tenantDAO.updateTenant(any()) } returns 1
        every { epicTenantDAO.update(any()) } returns 1

        val tenant = service.updateTenant(standardTenant)
        assertEquals(standardTenant, tenant)
    }

    @Test
    fun `update bad when there's no EHRs in the db`() {
        every { tenantDAO.getTenantForMnemonic(standardTenant.mnemonic) } returns standardTenantDO
        every { ehrDAO.getByInstance("Epic Sandbox") } returns null
        assertThrows<NoEHRFoundException> { service.updateTenant(standardTenant) }
    }

    @Test
    fun `update bad when there's no existing tenant in the db`() {
        every { tenantDAO.getTenantForMnemonic(standardTenant.mnemonic) } returns null
        assertThrows<NoTenantFoundException> { service.updateTenant(standardTenant) }
    }
}
