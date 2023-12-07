package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.tenant.config.data.model.CernerTenantDO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class CernerTenantDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    @DataSet(value = ["/dbunit/cerner-tenants/CernerTenants.yaml"], cleanAfter = true)
    fun `tenant found`() {
        val dao = CernerTenantDAO(KtormHelper.database())
        val cernerTenant = dao.getByTenantMnemonic("tenant")
        cernerTenant!!
        assertEquals(1002, cernerTenant.tenantId)
        assertEquals("https://localhost:8080/", cernerTenant.serviceEndpoint)
        assertEquals("urn:oid:mrn.system", cernerTenant.patientMRNSystem)
        assertEquals("https://localhost:8080/", cernerTenant.authEndpoint)
        assertEquals("Practitioner1", cernerTenant.messagePractitioner)
        assertEquals("Ronin Alerts", cernerTenant.messageTopic)
        assertEquals("notification", cernerTenant.messageCategory)
        assertEquals("urgent", cernerTenant.messagePriority)
    }

    @Test
    @DataSet(value = ["/dbunit/cerner-tenants/CernerTenants.yaml"], cleanAfter = true)
    fun `no tenants found`() {
        val dao = CernerTenantDAO(KtormHelper.database())
        val tenant = dao.getByTenantMnemonic("fake")
        assertNull(tenant)
    }

    @Test
    @DataSet(value = ["/dbunit/cerner-tenants/CernerTenants.yaml"], cleanAfter = true)
    fun `all tenants found`() {
        val dao = CernerTenantDAO(KtormHelper.database())
        val tenants = dao.getAll()
        assertEquals(2, tenants.size)
        assertEquals(1002, tenants[0].tenantId)
        assertEquals(1003, tenants[1].tenantId)
    }

    @Test
    @DataSet(value = ["/dbunit/cerner-tenants/Tenants.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/cerner-tenants/ExpectedCernerTenantsAfterInsert.yaml"])
    fun `insert cernerTenant`() {
        val tenantDao = TenantDAO(KtormHelper.database())
        val tenant = tenantDao.getTenantForMnemonic("tenant")!!
        val dao = CernerTenantDAO(KtormHelper.database())
        val testobj =
            CernerTenantDO {
                tenantId = tenant.id
                serviceEndpoint = "newServiceEndpoints"
                patientMRNSystem = "mrnSystem"
                authEndpoint = "newAuthEndpoint"
                messagePractitioner = "practitioner"
                messageTopic = "topic"
                messageCategory = "category"
                messagePriority = "priority"
            }

        val result = dao.insert(testobj)
        assertEquals(testobj.serviceEndpoint, result.serviceEndpoint)
        assertEquals(testobj.patientMRNSystem, result.patientMRNSystem)
        assertEquals(testobj.authEndpoint, result.authEndpoint)
    }

    @Test
    @DataSet(value = ["/dbunit/cerner-tenants/Tenants.yaml"], cleanAfter = true)
    fun `insert cernerTenant fails`() {
        val dao = CernerTenantDAO(KtormHelper.database())
        val testobj =
            CernerTenantDO {
                tenantId = -1
                patientMRNSystem = "mrnSystem"
            }

        assertThrows<SQLIntegrityConstraintViolationException> { dao.insert(testobj) }
    }

    @Test
    @DataSet(value = ["/dbunit/cerner-tenants/CernerTenants.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/cerner-tenants/ExpectedCernerTenantsAfterUpdate.yaml"])
    fun `update cernerTenant`() {
        val tenantDao = TenantDAO(KtormHelper.database())
        val tenant = tenantDao.getTenantForMnemonic("tenant")!!
        val dao = CernerTenantDAO(KtormHelper.database())
        val updated =
            CernerTenantDO {
                tenantId = tenant.id
                serviceEndpoint = "newServiceEndpoint"
                patientMRNSystem = "mrnSystem"
                authEndpoint = "newAuthEndpoint"
                messagePractitioner = "Practitioner1"
                messageTopic = null
                messageCategory = null
                messagePriority = null
            }
        val result = dao.update(updated)
        assertNotNull(result)
        assertEquals(1, result)

        val found = dao.getByTenantMnemonic("tenant")
        assertEquals(updated.serviceEndpoint, found?.serviceEndpoint)
        assertEquals(updated.patientMRNSystem, found?.patientMRNSystem)
    }
}
