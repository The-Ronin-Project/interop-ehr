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
    private val ehrDO = mockk<EhrDO> {
        every { id } returns 101
        every { vendorType } returns VendorType.EPIC
        every { clientId } returns "client"
        every { publicKey } returns "public"
        every { privateKey } returns "private"
    }
    private val tenantDO = mockk<TenantDO> {
        every { id } returns 1001
        every { mnemonic } returns "apposend"
        every { name } returns "Epic AppOrchard Sandbox"
        every { ehr } returns ehrDO
        every { availableBatchStart } returns null
        every { availableBatchEnd } returns null
    }
    private val tenantDO2 = mockk<TenantDO> {
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
        assertEquals(1, mirthConfigs.size)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val mirthConfigs = dao.getByTenantMnemonic("apposnd")
        assertNotNull(mirthConfigs)
        assertEquals("blah,blegh,blurgh", mirthConfigs?.locationIds)
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
    @ExpectedDataSet(value = ["/dbunit/mirth-tenant-config/ExpectedAfterUpdate.yaml"])
    fun `update ehr`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val testobj = MirthTenantConfigDO {
            tenant = tenantDO
            locationIds = "blarn,blurn,blorn"
        }
        val result = dao.updateConfig(testobj)
        assertEquals(1, result)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"])
    fun `update ehr fails correctly`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val testobj = MirthTenantConfigDO {
            tenant = tenantDO2
            locationIds = "blarn,blurn,blorn"
        }
        val result = dao.updateConfig(testobj)
        assertEquals(0, result)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/mirth-tenant-config/ExpectedAfterInsert.yaml"])
    fun `insert works correctly`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val testobj = MirthTenantConfigDO {
            tenant = tenantDO2
            locationIds = "blart,bleet,blurt"
        }
        val result = dao.insertConfig(testobj)
        assertEquals(testobj.locationIds, result.locationIds)
        assertEquals(testobj.tenant.id, result.tenant.id)
    }

    @Test
    @DataSet(value = ["/dbunit/mirth-tenant-config/MirthTenantConfig.yaml"], cleanAfter = true)
    fun `insert fails correctly`() {
        val dao = MirthTenantConfigDAO(KtormHelper.database())
        val faketTenantDO = mockk<TenantDO> {
            every { id } returns -1
            every { mnemonic } returns "no"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }
        val testobj = MirthTenantConfigDO {
            tenant = faketTenantDO
            locationIds = "no"
        }
        assertThrows<SQLIntegrityConstraintViolationException> { dao.insertConfig(testobj) }
    }
}
