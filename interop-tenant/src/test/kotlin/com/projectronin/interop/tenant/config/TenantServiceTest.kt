package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.CernerTenantDAO
import com.projectronin.interop.tenant.config.data.EHRTenantDAO
import com.projectronin.interop.tenant.config.data.EhrDAO
import com.projectronin.interop.tenant.config.data.EpicTenantDAO
import com.projectronin.interop.tenant.config.data.TenantDAO
import com.projectronin.interop.tenant.config.data.model.CernerTenantDO
import com.projectronin.interop.tenant.config.data.model.EHRTenantDO
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import com.projectronin.interop.tenant.config.exception.NoEHRFoundException
import com.projectronin.interop.tenant.config.exception.NoTenantFoundException
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.CernerAuthenticationConfig
import com.projectronin.interop.tenant.config.model.EpicAuthenticationConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
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
import java.time.ZoneId

class TenantServiceTest {
    private lateinit var database: Database
    private lateinit var tenantDAO: TenantDAO
    private lateinit var ehrDAO: EhrDAO
    private lateinit var epicTenantDAO: EpicTenantDAO
    private lateinit var cernerTenantDAO: CernerTenantDAO
    private lateinit var service: TenantService
    private lateinit var ehrTenantDAOFactory: EHRTenantDAOFactory

    private val epicHsiValue = "urn:epic:apporchard.curprod"
    private val epicDepartmentSystem = "urn:oid:1.2.840.114350.1.13.297.3.7.2.686980"
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

    private val standardEHRDO3 = mockk<EhrDO> {
        every { id } returns 3
        every { instanceName } returns "Cerner Instance 1"
        every { vendorType } returns VendorType.CERNER
        every { clientId } returns "clientId2"
        every { accountId } returns "accountId"
        every { secret } returns "secret"
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
        every { encounterCSNSystem } returns "csnSystem"
        every { patientMRNTypeText } returns "patientMRNTypeText"
        every { hsi } returns epicHsiValue
        every { departmentInternalSystem } returns epicDepartmentSystem
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
        every { patientMRNSystem } returns "mrnSystemExample2"
        every { patientInternalSystem } returns "internalSystemExample2"
        every { encounterCSNSystem } returns "csnSystem2"
        every { patientMRNTypeText } returns "patientMRNTypeText"
        every { hsi } returns epicHsiValue
        every { departmentInternalSystem } returns epicDepartmentSystem
    }
    private val standardCernerTenantDO = mockk<CernerTenantDO> {
        every { tenantId } returns 3
        every { serviceEndpoint } returns "http://localhost/"
        every { authEndpoint } returns "http://localhost/"
        every { patientMRNSystem } returns "mrnSystemExample"
    }

    private val standardTenantDO = mockk<TenantDO> {
        every { id } returns 1
        every { mnemonic } returns "Tenant1"
        every { name } returns "Test Tenant"
        every { timezone } returns ZoneId.of("America/Chicago")
        every { ehr } returns standardEHRDO1
        every { availableBatchStart } returns null
        every { availableBatchEnd } returns null
    }

    private val standardTenantDO2 = mockk<TenantDO> {
        every { id } returns 2
        every { mnemonic } returns "Tenant2"
        every { name } returns "Second Alternate Tenant"
        every { timezone } returns ZoneId.of("America/Los_Angeles")
        every { ehr } returns standardEHRDO2
        every { availableBatchStart } returns null
        every { availableBatchEnd } returns null
    }

    private val standardTenantDO3 = mockk<TenantDO> {
        every { id } returns 3
        every { mnemonic } returns "Tenant3"
        every { name } returns "Third Alternate Tenant"
        every { timezone } returns ZoneId.of("America/Los_Angeles")
        every { ehr } returns standardEHRDO3
        every { availableBatchStart } returns null
        every { availableBatchEnd } returns null
    }

    private val standardTenant = Tenant(
        internalId = 1,
        mnemonic = "Tenant1",
        name = "Test Tenant",
        timezone = ZoneId.of("America/Chicago"),
        batchConfig = null,
        vendor = Epic(
            clientId = "clientId",
            instanceName = "Epic Sandbox",
            authenticationConfig = EpicAuthenticationConfig(
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
            encounterCSNSystem = "csnSystem",
            patientMRNTypeText = "patientMRNTypeText",
            hsi = epicHsiValue,
            departmentInternalSystem = epicDepartmentSystem,
        )
    )

    private val standardTenant2 = Tenant(
        internalId = 2,
        mnemonic = "Tenant2",
        name = "Second Alternate Tenant",
        timezone = ZoneId.of("America/Los_Angeles"),
        batchConfig = null,
        vendor = Epic(
            clientId = "clientId2",
            instanceName = "Epic Sandbox Instance 2",
            authenticationConfig = EpicAuthenticationConfig(
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
            patientMRNSystem = "mrnSystemExample2",
            patientInternalSystem = "internalSystemExample2",
            encounterCSNSystem = "csnSystem2",
            patientMRNTypeText = "patientMRNTypeText",
            hsi = epicHsiValue,
            departmentInternalSystem = epicDepartmentSystem,
        )
    )

    private val standardTenant3 = Tenant(
        internalId = 3,
        mnemonic = "Tenant3",
        name = "Third Alternate Tenant",
        timezone = ZoneId.of("America/Los_Angeles"),
        batchConfig = null,
        vendor = Cerner(
            clientId = "clientId2",
            instanceName = "Cerner Instance 1",
            authenticationConfig = CernerAuthenticationConfig(
                authEndpoint = "http://localhost/oauth2/token",
                accountId = "accountId",
                secret = "secretKey"
            ),
            serviceEndpoint = "http://localhost/",
            patientMRNSystem = "mrnSystemExample",
        )
    )

    @BeforeEach
    fun setup() {
        tenantDAO = mockk()
        ehrDAO = mockk()
        epicTenantDAO = mockk()
        cernerTenantDAO = mockk()
        database = mockk()
        ehrTenantDAOFactory = mockk {
            every { getEHRTenantDAO(any()) } returns epicTenantDAO
            every { getEHRTenantDAOByVendorType(VendorType.EPIC) } returns epicTenantDAO
            every { getEHRTenantDAOByVendorType(VendorType.CERNER) } returns cernerTenantDAO
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
        assertEquals(standardTenant.toString(), tenant.toString())

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
            every { name } returns "Test Tenant"
            every { timezone } returns ZoneId.of("America/Chicago")
            every { ehr } returns standardEHRDO1
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns LocalTime.of(6, 0)
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns standardEpicTenantDO

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(standardTenant.toString(), tenant.toString())

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
            every { name } returns "Test Tenant"
            every { timezone } returns ZoneId.of("America/Chicago")
            every { ehr } returns standardEHRDO1
            every { availableBatchStart } returns LocalTime.of(20, 0)
            every { availableBatchEnd } returns null
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns standardEpicTenantDO

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(standardTenant.toString(), tenant.toString())

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
            every { name } returns "Test Tenant"
            every { ehr } returns standardEHRDO1
            every { timezone } returns ZoneId.of("America/Chicago")
            every { availableBatchStart } returns LocalTime.of(20, 0)
            every { availableBatchEnd } returns LocalTime.of(6, 0)
        }
        every { tenantDAO.getTenantForMnemonic("Tenant1") } returns tenantDO

        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns standardEpicTenantDO

        val expectedTenant = Tenant(
            internalId = 1,
            mnemonic = "Tenant1",
            name = "Test Tenant",
            timezone = ZoneId.of("America/Chicago"),
            batchConfig = BatchConfig(
                availableStart = LocalTime.of(20, 0),
                availableEnd = LocalTime.of(6, 0)
            ),
            vendor = Epic(
                clientId = "clientId",
                instanceName = "Epic Sandbox",
                authenticationConfig = EpicAuthenticationConfig(
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
                encounterCSNSystem = "csnSystem",
                patientMRNTypeText = "patientMRNTypeText",
                hsi = epicHsiValue,
                departmentInternalSystem = epicDepartmentSystem,
            )
        )

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(expectedTenant.toString(), tenant.toString())

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
            every { encounterCSNSystem } returns "csnSystem"
            every { patientMRNTypeText } returns "patientMRNTypeText"
            every { hsi } returns epicHsiValue
            every { departmentInternalSystem } returns epicDepartmentSystem
        }
        every { epicTenantDAO.getByTenantMnemonic("Tenant1") } returns epicTenantDO

        val expectedTenant = Tenant(
            internalId = 1,
            mnemonic = "Tenant1",
            name = "Test Tenant",
            timezone = ZoneId.of("America/Chicago"),
            batchConfig = null,
            vendor = Epic(
                clientId = "clientId",
                instanceName = "Epic Sandbox",
                authenticationConfig = EpicAuthenticationConfig(
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
                encounterCSNSystem = "csnSystem",
                patientMRNTypeText = "patientMRNTypeText",
                hsi = epicHsiValue,
                departmentInternalSystem = epicDepartmentSystem,
            )
        )

        val tenant = service.getTenantForMnemonic("Tenant1")
        assertEquals(expectedTenant.toString(), tenant.toString())

        verify(exactly = 1) {
            tenantDAO.getTenantForMnemonic("Tenant1")
            epicTenantDAO.getByTenantMnemonic("Tenant1")
        }
    }

    @Test
    fun `get with bad database throws error, actually impossible to hit`() {
        val mockEhrDO = mockk<EhrDO> {}
        val mockTenantDO = mockk<TenantDO> {
            every { ehr } returns mockEhrDO
            every { mnemonic } returns "test"
            every { availableBatchEnd } returns null
            every { availableBatchStart } returns null
            every { id } returns 123
            every { name } returns "cursedVendor"
            every { timezone } returns ZoneId.of("America/Los_Angeles")
        }
        every { tenantDAO.getTenantForMnemonic("test") } returns mockTenantDO
        val mockEHRTenantDAO = mockk<FakeVendor> {}
        // this would normally blow up, but this allows us to bypass the blow up to hit a different code path
        every { ehrTenantDAOFactory.getEHRTenantDAO(mockTenantDO) } returns mockEHRTenantDAO

        every { mockEHRTenantDAO.getByTenantMnemonic("test") } returns mockk {
        }
        assertThrows<NotImplementedError> { service.getTenantForMnemonic("test") }
    }

    class FakeVendor : EHRTenantDAO {
        override fun update(ehrTenantDO: EHRTenantDO): Int = mockk()
        override fun insert(ehrTenantDO: EHRTenantDO): EHRTenantDO = mockk()
        override fun getByTenantMnemonic(tenantMnemonic: String): EHRTenantDO = mockk()
        override fun getAll(): List<EHRTenantDO> = mockk()
    }

    @Test
    fun `getAll none found`() {
        every { tenantDAO.getAllTenants() } returns emptyList()

        val tenants = service.getAllTenants()
        assertEquals(0, tenants.size)
    }

    @Test
    fun `getAll finds 'em`() {
        every { tenantDAO.getAllTenants() } returns listOf(standardTenantDO, standardTenantDO3)
        every { epicTenantDAO.getAll() } returns listOf(standardEpicTenantDO)
        every { cernerTenantDAO.getAll() } returns listOf(standardCernerTenantDO)

        val tenants = service.getAllTenants()
        assertEquals(2, tenants.size)
        assertEquals(listOf(standardTenant, standardTenant3).toString(), tenants.toString())
    }

    @Test
    fun `getAll returns multiple tenants with multiple instances`() {
        every { tenantDAO.getAllTenants() } returns listOf(standardTenantDO, standardTenantDO2)
        every { epicTenantDAO.getAll() } returns listOf(standardEpicTenantDO, standardEpicTenantDO2)
        every { cernerTenantDAO.getAll() } returns emptyList()

        val tenants = service.getAllTenants()
        assertEquals(2, tenants.size)
        assertEquals(listOf(standardTenant, standardTenant2).toString(), tenants.toString())
    }

    @Test
    fun `insert ok - epic`() {
        every { ehrDAO.getByInstance("Epic Sandbox") } returns standardEHRDO1
        every { tenantDAO.insertTenant(any()) } returns standardTenantDO
        every { epicTenantDAO.insert(any()) } returns standardEpicTenantDO

        val tenant = service.insertTenant(standardTenant)
        assertEquals(standardTenant.toString(), tenant.toString())
    }

    @Test
    fun `insert ok - cerner`() {
        every { ehrDAO.getByInstance("Cerner Instance 1") } returns standardEHRDO3
        every { ehrTenantDAOFactory.getEHRTenantDAO(standardTenantDO3) } returns cernerTenantDAO
        every { tenantDAO.insertTenant(any()) } returns standardTenantDO3
        every { cernerTenantDAO.insert(any()) } returns standardCernerTenantDO

        val tenant = service.insertTenant(standardTenant3)
        assertEquals(standardTenant3.toString(), tenant.toString())
    }

    @Test
    fun `insert ok with batch`() {
        val newTenant = Tenant(
            internalId = 1,
            mnemonic = "Tenant1",
            name = "Test Tenant",
            timezone = ZoneId.of("America/Chicago"),
            batchConfig = BatchConfig(
                availableStart = LocalTime.of(20, 0),
                availableEnd = LocalTime.of(6, 0)
            ),
            vendor = Epic(
                clientId = "clientId",
                instanceName = "Epic Sandbox",
                authenticationConfig = EpicAuthenticationConfig(
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
                encounterCSNSystem = "csnSystem",
                patientMRNTypeText = "patientMRNTypeText",
                hsi = epicHsiValue,
                departmentInternalSystem = epicDepartmentSystem,
            )
        )
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "Tenant1"
            every { name } returns "Test Tenant"
            every { timezone } returns ZoneId.of("America/Chicago")
            every { ehr } returns standardEHRDO1
            every { availableBatchStart } returns LocalTime.of(20, 0)
            every { availableBatchEnd } returns LocalTime.of(6, 0)
        }
        every { ehrDAO.getByInstance("Epic Sandbox") } returns standardEHRDO1
        every { tenantDAO.insertTenant(any()) } returns tenantDO
        every { epicTenantDAO.insert(any()) } returns standardEpicTenantDO

        val tenant = service.insertTenant(newTenant)
        assertEquals(newTenant.toString(), tenant.toString())
    }

    @Test
    fun `insert bad when there's no EHRs in the db`() {
        every { ehrDAO.getByInstance("Epic Sandbox") } returns null
        assertThrows<NoEHRFoundException> { service.insertTenant(standardTenant) }
    }

    @Test
    fun `update ok`() {
        every { ehrDAO.getByInstance("Epic Sandbox") } returns standardEHRDO1
        every { tenantDAO.getTenantForId(standardTenant.internalId) } returns standardTenantDO
        every { tenantDAO.updateTenant(any()) } returns 1
        every { epicTenantDAO.update(any()) } returns 1

        val tenant = service.updateTenant(standardTenant)
        assertEquals(standardTenant.toString(), tenant.toString())
    }

    @Test
    fun `update bad when there's no EHRs in the db`() {
        every { tenantDAO.getTenantForId(standardTenant.internalId) } returns standardTenantDO
        every { ehrDAO.getByInstance("Epic Sandbox") } returns null
        assertThrows<NoEHRFoundException> { service.updateTenant(standardTenant) }
    }

    @Test
    fun `update bad when there's no existing tenant in the db`() {
        every { tenantDAO.getTenantForId(standardTenant.internalId) } returns null
        assertThrows<NoTenantFoundException> { service.updateTenant(standardTenant) }
    }
}
