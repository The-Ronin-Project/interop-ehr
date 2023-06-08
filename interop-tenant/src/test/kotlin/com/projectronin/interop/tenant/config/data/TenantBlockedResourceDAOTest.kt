package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.TenantBlockedResourceDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class TenantBlockedResourceDAOTest {

    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder
    private val ehrDO = mockk<EhrDO> {
        every { id } returns 101
        every { vendorType } returns VendorType.EPIC
        every { clientId } returns "client"
        every { publicKey } returns "public"
        every { privateKey } returns "private"
    }
    private val tenantDO = mockk<TenantDO> {
        every { id } returns 1001
        every { mnemonic } returns "apposnd"
        every { name } returns "Epic AppOrchard Sandbox"
        every { ehr } returns ehrDO
        every { availableBatchStart } returns null
        every { availableBatchEnd } returns null
    }
    private val tenantDO2 = mockk<TenantDO> {
        every { id } returns 1002
        every { mnemonic } returns "apposnd2"
        every { ehr } returns ehrDO
        every { availableBatchStart } returns null
        every { availableBatchEnd } returns null
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-blocked-resource/TenantBlockedResource.yaml"], cleanAfter = true)
    fun `getAll works`() {
        val dao = TenantBlockedResourceDAO(KtormHelper.database())
        val tenantBlockedResources = dao.getAll()
        Assertions.assertTrue(tenantBlockedResources.isNotEmpty())
        Assertions.assertEquals(2, tenantBlockedResources.size)
        Assertions.assertEquals(1001, tenantBlockedResources.first().tenantId)
        Assertions.assertEquals("Observation", tenantBlockedResources.first().resource)
        Assertions.assertEquals(1002, tenantBlockedResources.last().tenantId)
        Assertions.assertEquals("Appointment", tenantBlockedResources.last().resource)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-blocked-resource/TenantBlockedResource.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works`() {
        val dao = TenantBlockedResourceDAO(KtormHelper.database())
        val tenantBlockedResources = dao.getByTenantMnemonic(tenantDO.mnemonic)
        Assertions.assertEquals(1, tenantBlockedResources.size)
        Assertions.assertEquals(1001, tenantBlockedResources.first().tenantId)
        Assertions.assertEquals("Observation", tenantBlockedResources.first().resource)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-blocked-resource/TenantBlockedResource.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works when not found`() {
        val dao = TenantBlockedResourceDAO(KtormHelper.database())
        val tenantBlockedResources = dao.getByTenantMnemonic("blooop")
        Assertions.assertEquals(emptyList<TenantBlockedResourceDO>(), tenantBlockedResources)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-blocked-resource/TenantBlockedResource.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant-blocked-resource/ExpectedAfterInsert.yaml"])
    fun `insert works correctly`() {
        val dao = TenantBlockedResourceDAO(KtormHelper.database())
        val testObj = TenantBlockedResourceDO {
            this.tenantId = tenantDO2.id
            this.resource = "Practitioner"
        }
        val result = dao.insertBlockedResource(testObj)
        Assertions.assertEquals(tenantDO2.id, result.tenantId)
        Assertions.assertEquals("Practitioner", result.resource)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-blocked-resource/TenantBlockedResource.yaml"], cleanAfter = true)
    fun `insert fails correctly`() {
        val dao = TenantBlockedResourceDAO(KtormHelper.database())
        val testObj = TenantBlockedResourceDO {
            this.tenantId = 1003
            this.resource = "Bananas"
        }
        assertThrows<SQLIntegrityConstraintViolationException> { dao.insertBlockedResource(testObj) }
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-blocked-resource/TenantBlockedResource.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant-blocked-resource/ExpectedAfterDelete.yaml"])
    fun `delete works correctly`() {
        val dao = TenantBlockedResourceDAO(KtormHelper.database())
        val testobj = TenantBlockedResourceDO {
            this.tenantId = 1002
            this.resource = "Appointment"
        }
        val result = dao.deleteBlockedResource(testobj)
        Assertions.assertEquals(1, result)
    }
}
