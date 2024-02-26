package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class EpicTenantDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    @DataSet(value = ["/dbunit/epic-tenants/EpicTenants.yaml"], cleanAfter = true)
    fun `tenant found`() {
        val dao = EpicTenantDAO(KtormHelper.database())
        val epicTenant = dao.getByTenantMnemonic("tenant")
        assertNotNull(epicTenant)
        assertEquals(1002, epicTenant?.tenantId)
        assertEquals("2021.10", epicTenant?.release)
        assertEquals("https://localhost:8080/", epicTenant?.serviceEndpoint)
        assertEquals("https://localhost:8080/auth/", epicTenant?.authEndpoint)
        assertEquals("RONIN", epicTenant?.ehrUserId)
        assertEquals("Ronin Alerts", epicTenant?.messageType)
        assertEquals("urn:oid:provider.system", epicTenant?.practitionerProviderSystem)
        assertEquals("urn:oid:user.system", epicTenant?.practitionerUserSystem)
        assertEquals("urn:oid:mrn.system", epicTenant?.patientMRNSystem)
        assertEquals("urn:oid:internal.system", epicTenant?.patientInternalSystem)
        assertEquals("urn:oid:encounter.system", epicTenant?.encounterCSNSystem)
        assertEquals("urn:oid:1.2.840.114350.1.13.297.3.7.2.686980", epicTenant?.departmentInternalSystem)
        assertEquals("urn:epic:apporchard.curprod", epicTenant?.hsi)
        assertEquals("E8675309", epicTenant?.patientOnboardedFlagId)
        assertEquals("urn:order.system1", epicTenant?.orderSystem)
        assertEquals("2024.02", epicTenant?.appVersion)
    }

    @Test
    @DataSet(value = ["/dbunit/epic-tenants/EpicTenants.yaml"], cleanAfter = true)
    fun `no tenants found`() {
        val dao = EpicTenantDAO(KtormHelper.database())
        val tenant = dao.getByTenantMnemonic("fake")
        assertNull(tenant)
    }

    @Test
    @DataSet(value = ["/dbunit/epic-tenants/EpicTenants.yaml"], cleanAfter = true)
    fun `all tenants found`() {
        val dao = EpicTenantDAO(KtormHelper.database())
        val tenants = dao.getAll()
        assertEquals(2, tenants.size)
        assertEquals(1002, tenants[0].tenantId)
        assertEquals(1003, tenants[1].tenantId)
    }

    @Test
    @DataSet(value = ["/dbunit/epic-tenants/Tenants.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/epic-tenants/ExpectedEpicTenantsAfterInsert.yaml"])
    fun `insert epicTenant`() {
        val tenantDao = TenantDAO(KtormHelper.database())
        val tenant = tenantDao.getTenantForMnemonic("tenant")!!
        val dao = EpicTenantDAO(KtormHelper.database())
        val testobj =
            EpicTenantDO {
                tenantId = tenant.id
                release = "release"
                serviceEndpoint = "serviceEndpoint"
                authEndpoint = "authEndpoint"
                ehrUserId = "userID"
                messageType = "messageType"
                practitionerProviderSystem = "providerSystem"
                practitionerUserSystem = "userSystem"
                patientMRNSystem = "mrnSystem"
                patientMRNTypeText = "MRN"
                patientInternalSystem = "internalSystem"
                encounterCSNSystem = "csnSystem"
                hsi = "hsi"
                departmentInternalSystem = "departmentSystem"
                patientOnboardedFlagId = "flagID"
                orderSystem = "urn:order.system3"
            }

        val result = dao.insert(testobj)
        assertEquals(testobj.release, result.release)
        assertEquals(testobj.serviceEndpoint, result.serviceEndpoint)
        assertEquals(testobj.authEndpoint, result.authEndpoint)
        assertEquals(testobj.ehrUserId, result.ehrUserId)
        assertEquals(testobj.messageType, result.messageType)
        assertEquals(testobj.practitionerProviderSystem, result.practitionerProviderSystem)
        assertEquals(testobj.practitionerUserSystem, result.practitionerUserSystem)
        assertEquals(testobj.patientMRNSystem, result.patientMRNSystem)
        assertEquals(testobj.patientMRNTypeText, result.patientMRNTypeText)
        assertEquals(testobj.patientInternalSystem, result.patientInternalSystem)
        assertEquals(testobj.encounterCSNSystem, result.encounterCSNSystem)
        assertEquals(testobj.hsi, result.hsi)
        assertEquals(testobj.departmentInternalSystem, result.departmentInternalSystem)
        assertEquals(testobj.patientOnboardedFlagId, result.patientOnboardedFlagId)
        assertEquals(testobj.orderSystem, result.orderSystem)
    }

    @Test
    @DataSet(value = ["/dbunit/epic-tenants/Tenants.yaml"], cleanAfter = true)
    fun `insert epicTenant fails`() {
        val dao = EpicTenantDAO(KtormHelper.database())
        val testobj =
            EpicTenantDO {
                tenantId = -1
                release = "release"
                ehrUserId = "userID"
                messageType = "messageType"
                practitionerProviderSystem = "providerSystem"
                practitionerUserSystem = "userSystem"
                patientMRNSystem = "mrnSystem"
                patientInternalSystem = "internalSystem"
                encounterCSNSystem = "csnSystem"
                patientMRNTypeText = "MRN"
                hsi = "hsi"
                orderSystem = "urn:order.system1"
            }

        assertThrows<SQLIntegrityConstraintViolationException> { dao.insert(testobj) }
    }

    @Test
    @DataSet(value = ["/dbunit/epic-tenants/EpicTenants.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/epic-tenants/ExpectedEpicTenantsAfterUpdate.yaml"])
    fun `update epicTenant`() {
        val tenantDao = TenantDAO(KtormHelper.database())
        val tenant = tenantDao.getTenantForMnemonic("tenant")!!
        val dao = EpicTenantDAO(KtormHelper.database())
        val updated =
            EpicTenantDO {
                tenantId = tenant.id
                release = "release"
                serviceEndpoint = "serviceEndpoint"
                authEndpoint = "authEndpoint"
                ehrUserId = "userID"
                messageType = "messageType"
                practitionerProviderSystem = "providerSystem"
                practitionerUserSystem = "userSystem"
                patientMRNSystem = "mrnSystem"
                patientMRNTypeText = "MRN"
                patientInternalSystem = "internalSystem"
                encounterCSNSystem = "csnSystem"
                hsi = "hsi"
                departmentInternalSystem = "departmentSystem"
                patientOnboardedFlagId = "flagID"
                orderSystem = "urn:order.system4"
            }
        val result = dao.update(updated)
        assertNotNull(result)
        assertEquals(1, result)

        val found = dao.getByTenantMnemonic("tenant")
        assertEquals(updated.release, found?.release)
        assertEquals(updated.serviceEndpoint, found?.serviceEndpoint)
        assertEquals(updated.authEndpoint, found?.authEndpoint)
        assertEquals(updated.ehrUserId, found?.ehrUserId)
        assertEquals(updated.messageType, found?.messageType)
        assertEquals(updated.practitionerProviderSystem, found?.practitionerProviderSystem)
        assertEquals(updated.practitionerUserSystem, found?.practitionerUserSystem)
        assertEquals(updated.patientMRNSystem, found?.patientMRNSystem)
        assertEquals(updated.patientMRNTypeText, found?.patientMRNTypeText)
        assertEquals(updated.patientInternalSystem, found?.patientInternalSystem)
        assertEquals(updated.encounterCSNSystem, found?.encounterCSNSystem)
        assertEquals(updated.hsi, found?.hsi)
        assertEquals(updated.departmentInternalSystem, found?.departmentInternalSystem)
        assertEquals(updated.patientOnboardedFlagId, found?.patientOnboardedFlagId)
        assertEquals(updated.orderSystem, found?.orderSystem)
    }
}
