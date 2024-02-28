package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.MirthTenantConfigDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class MirthTenantConfigDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder
    private val ehrDO =
        mockk<EhrDO> {
            every { id } returns 101
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "client"
            every { publicKey } returns "public"
            every { privateKey } returns "private"
        }
    private val tenantDO =
        mockk<TenantDO> {
            every { id } returns 1001
            every { mnemonic } returns "apposend"
            every { name } returns "Epic AppOrchard Sandbox"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }
    private val tenantDO2 =
        mockk<TenantDO> {
            every { id } returns 1002
            every { mnemonic } returns "apposend2"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    fun `getAll works`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val mirthConfigs = dao.getAll()
        assertTrue(mirthConfigs.isNotEmpty())
        assertEquals(2, mirthConfigs.size)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val mirthConfigs = dao.getByTenantMnemonic("apposnd")
        assertNotNull(mirthConfigs)
        assertEquals("blah,blegh,blurgh", mirthConfigs?.locationIds)
        assertEquals("appointment,organization", mirthConfigs?.blockedResources)
        assertEquals("patient", mirthConfigs?.allowedResources)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works when not found`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val mirthConfigs = dao.getByTenantMnemonic("blooop")
        assertNull(mirthConfigs)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works with null in nullable columns`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val mirthConfigs = dao.getByTenantMnemonic("apposnd3")
        assertNull(mirthConfigs?.lastUpdated)
        assertNull(mirthConfigs?.blockedResources)
        assertNull(mirthConfigs?.allowedResources)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/mirth-tenant-config/ExpectedAfterUpdate.yaml"])
    fun `updateConfig works`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val locationIds = "blarn,blurn,blorn"
        val blockedResources = "patient,encounter"
        val allowedResources = "appointment"

        val testobj =
            MirthTenantConfigDO {
                this.tenant = tenantDO
                this.locationIds = locationIds
                this.blockedResources = blockedResources
                this.allowedResources = allowedResources
            }
        val result = dao.updateConfig(testobj)!!
        assertEquals(tenantDO.id, result.tenant.id)
        assertEquals(locationIds, result.locationIds)
        assertEquals(blockedResources, result.blockedResources)
        assertEquals(allowedResources, result.allowedResources)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"])
    fun `updateConfig fails correctly`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val testobj =
            MirthTenantConfigDO {
                tenant = tenantDO2
                locationIds = "blarn,blurn,blorn"
                blockedResources = "patient,encounter"
                allowedResources = "observation"
            }
        val result = dao.updateConfig(testobj)
        assertNull(result)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/mirth-tenant-config/ExpectedAfterInsert.yaml"])
    fun `insert works correctly`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val testobj =
            MirthTenantConfigDO {
                tenant = tenantDO2
                locationIds = "blart,bleet,blurt"
                blockedResources = "patient,encounter"
                allowedResources = "appointment"
            }
        val result = dao.insertConfig(testobj)
        assertEquals(testobj.locationIds, result.locationIds)
        assertEquals(testobj.tenant.id, result.tenant.id)
        assertEquals(testobj.blockedResources, result.blockedResources)
        assertEquals(testobj.allowedResources, result.allowedResources)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    fun `insert fails correctly`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val fakeTenantDO =
            mockk<TenantDO> {
                every { id } returns -1
                every { mnemonic } returns "no"
                every { ehr } returns ehrDO
                every { availableBatchStart } returns null
                every { availableBatchEnd } returns null
            }
        val testobj =
            MirthTenantConfigDO {
                tenant = fakeTenantDO
                locationIds = "no"
                blockedResources = "nada"
                allowedResources = "nope"
            }
        assertThrows<SQLIntegrityConstraintViolationException> { dao.insertConfig(testobj) }
    }
}
