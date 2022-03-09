package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.update
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalTime

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class TenantDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    fun `no tenants found`() {
        val dao = TenantDAO(KtormHelper.database())
        val tenant = dao.getTenantForMnemonic("fake")
        assertNull(tenant)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    fun `tenant found`() {
        val dao = TenantDAO(KtormHelper.database())
        val tenant = dao.getTenantForMnemonic("mdaoc")
        assertNotNull(tenant)
        tenant?.let {
            assertEquals(1001, tenant.id)
            assertEquals("mdaoc", tenant.mnemonic)
            assertEquals(LocalTime.of(22, 0, 0), tenant.availableBatchStart)
            assertEquals(LocalTime.of(6, 0, 0), tenant.availableBatchEnd)

            val ehr = tenant.ehr
            assertEquals(101, ehr.id)
            assertEquals(VendorType.EPIC, ehr.vendorType)
            assertEquals("12345", ehr.clientId)
            assertEquals("public", ehr.publicKey)
            assertEquals("private", ehr.privateKey)
        }
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/EHRTenants.yaml"], cleanAfter = true)
    fun `no ehr tenant found`() {
        val dao = TenantDAO(KtormHelper.database())
        val ehrTenant = dao.getEHRTenant<EpicTenantDO>(1002, VendorType.EPIC)
        assertNull(ehrTenant)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/EHRTenants.yaml"], cleanAfter = true)
    fun `epic tenant found`() {
        val dao = TenantDAO(KtormHelper.database())
        val ehrTenant = dao.getEHRTenant<EpicTenantDO>(1001, VendorType.EPIC)
        assertNotNull(ehrTenant)
        ehrTenant?.let {
            assertEquals(1001, ehrTenant.tenantId)
            assertEquals("2021.10", ehrTenant.release)
            assertEquals("https://localhost:8080/", ehrTenant.serviceEndpoint)
            assertEquals("RONIN", ehrTenant.ehrUserId)
            assertEquals("Ronin Alerts", ehrTenant.messageType)
            assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.2.836982", ehrTenant.practitionerProviderSystem)
            assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.2.697780", ehrTenant.practitionerUserSystem)
            assertNull(ehrTenant.hsi)
        }
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/EpicHSITenants.yaml"], cleanAfter = true)
    fun `epic tenant with hsi found`() {
        val dao = TenantDAO(KtormHelper.database())
        val ehrTenant = dao.getEHRTenant<EpicTenantDO>(1001, VendorType.EPIC)
        assertNotNull(ehrTenant)
        ehrTenant?.let {
            assertEquals(1001, ehrTenant.tenantId)
            assertEquals("2021.10", ehrTenant.release)
            assertEquals("https://localhost:8080/", ehrTenant.serviceEndpoint)
            assertEquals("RONIN", ehrTenant.ehrUserId)
            assertEquals("Ronin Alerts", ehrTenant.messageType)
            assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.2.836982", ehrTenant.practitionerProviderSystem)
            assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.2.697780", ehrTenant.practitionerUserSystem)
            assertEquals("urn:epic:apporchard.curprod", ehrTenant.hsi)
        }
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant/ExpectedTenantsAfterInsert.yaml"], ignoreCols = ["io_tenant_id"])
    fun `can insert tenant`() {
        val database = KtormHelper.database()
        val tenantDAO = TenantDAO(database)
        val ehrDAO = EhrDAO(database)

        val insertedTenantDO = TenantDO {
            id = 0
            mnemonic = "mnemonic"
            ehr = ehrDAO.read().first()
            availableBatchStart = LocalTime.of(22, 0, 0)
            availableBatchEnd = LocalTime.of(6, 0, 0)
        }
        val actualTenantDO = tenantDAO.insertTenant(insertedTenantDO)

        assertSame(insertedTenantDO, actualTenantDO)
        assertNotEquals(0, insertedTenantDO.id)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    fun `handles failed insert tenant`() {
        val tenantDAO = TenantDAO(KtormHelper.database())

        val fakeEhrDO = EhrDO {
            vendorType = VendorType.EPIC
            clientId = "12345"
            publicKey = "public"
            privateKey = "private"
        }

        val insertedTenantDO = TenantDO {
            id = 0
            mnemonic = "mnemonic"
            ehr = fakeEhrDO
            availableBatchStart = LocalTime.of(22, 0)
            availableBatchEnd = LocalTime.of(6, 0, 0)
        }

        assertThrows<SQLIntegrityConstraintViolationException> {
            tenantDAO.insertTenant(insertedTenantDO)
        }
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    @ExpectedDataSet("/dbunit/tenant/ExpectedTenantsAfterUpdate.yaml")
    fun `can update tenant`() {
        val database = KtormHelper.database()
        val tenantDAO = TenantDAO(database)
        val ehrDAO = EhrDAO(database)

        val updatedTenantDO = TenantDO {
            id = 1001
            mnemonic = "mnemonic"
            ehr = ehrDAO.read().first()
            availableBatchStart = LocalTime.of(23, 0)
            availableBatchEnd = LocalTime.of(5, 0, 0)
        }
        val rowCount = tenantDAO.updateTenant(updatedTenantDO)

        assertEquals(1, rowCount)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    @ExpectedDataSet("/dbunit/tenant/Tenants.yaml")
    fun `handles update tenant when no rows match`() {
        val database = KtormHelper.database()
        val tenantDAO = TenantDAO(database)
        val ehrDAO = EhrDAO(database)

        val updatedTenantDO = TenantDO {
            id = 54321
            mnemonic = "mnemonic"
            ehr = ehrDAO.read().first()
            availableBatchStart = LocalTime.of(23, 0)
            availableBatchEnd = LocalTime.of(5, 0, 0)
        }
        val rowCount = tenantDAO.updateTenant(updatedTenantDO)

        assertEquals(0, rowCount)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    fun `handles failed update tenant`() {
        val database = mockk<Database>()
        val tenantDAO = TenantDAO(database)
        val ehrDAO = EhrDAO(KtormHelper.database())

        val updatedTenantDO = TenantDO {
            id = 54321
            mnemonic = "mnemonic"
            ehr = ehrDAO.read().first()
            availableBatchStart = LocalTime.of(23, 0)
            availableBatchEnd = LocalTime.of(5, 0, 0)
        }

        every {
            database.update(TenantDOs) {
                set(it.mnemonic, updatedTenantDO.mnemonic)
                set(it.ehr, updatedTenantDO.ehr.id)
                set(it.availableBatchStart, updatedTenantDO.availableBatchStart)
                set(it.availableBatchEnd, updatedTenantDO.availableBatchEnd)
                where {
                    it.id eq updatedTenantDO.id
                }
            }
        }.throws(Exception("Error"))

        val exception = assertThrows<Exception> {
            tenantDAO.updateTenant(updatedTenantDO)
        }
        assertEquals("Error", exception.message)
    }
}
