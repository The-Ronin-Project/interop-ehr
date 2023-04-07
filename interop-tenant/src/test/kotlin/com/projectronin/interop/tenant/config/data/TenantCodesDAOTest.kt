package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.TenantCodesDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class TenantCodesDAOTest {

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
    @DataSet(value = ["/dbunit/tenant-codes/TenantCodes.yaml"], cleanAfter = true)
    fun `getAll works`() {
        val dao = TenantCodesDAO(KtormHelper.database())
        val tenantCodes = dao.getAll()
        Assertions.assertTrue(tenantCodes.isNotEmpty())
        assertEquals(1, tenantCodes.size)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-codes/TenantCodes.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works`() {
        val dao = TenantCodesDAO(KtormHelper.database())
        val tenantCodes = dao.getByTenantMnemonic("apposnd")
        Assertions.assertNotNull(tenantCodes)
        assertEquals("12345", tenantCodes?.bsaCode)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-codes/TenantCodes.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works when not found`() {
        val dao = TenantCodesDAO(KtormHelper.database())
        val tenantCodes = dao.getByTenantMnemonic("blooop")
        Assertions.assertNull(tenantCodes)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-codes/TenantCodes.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant-codes/ExpectedAfterUpdate.yaml"])
    fun `updateConfig works`() {
        val dao = TenantCodesDAO(KtormHelper.database())
        val bsaCode = "123456"

        val testobj = TenantCodesDO {
            this.tenantId = 1001
            this.bsaCode = "123456"
            this.bmiCode = "67890"
        }
        val result = dao.updateCodes(testobj)!!
        assertEquals(tenantDO.id, result.tenantId)
        assertEquals(bsaCode, result.bsaCode)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-codes/TenantCodes.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant-codes/TenantCodes.yaml"])
    fun `updateConfig fails correctly`() {
        val dao = TenantCodesDAO(KtormHelper.database())
        val testobj = TenantCodesDO {
            tenantId = 10000
            bsaCode = "12345"
        }
        val result = dao.updateCodes(testobj)
        Assertions.assertNull(result)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-codes/TenantCodes.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant-codes/ExpectedAfterInsert.yaml"])
    fun `insert works correctly`() {
        val dao = TenantCodesDAO(KtormHelper.database())
        val testobj = TenantCodesDO {
            this.tenantId = 1002
            this.bsaCode = "54321"
            this.bmiCode = "09876"
        }
        val result = dao.insertCodes(testobj)
        assertEquals(tenantDO2.id, result.tenantId)
        assertEquals("54321", result.bsaCode)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-codes/TenantCodes.yaml"], cleanAfter = true)
    fun `insert fails correctly`() {
        val dao = TenantCodesDAO(KtormHelper.database())
        val testobj = TenantCodesDO {
            this.tenantId = 1003
            this.bsaCode = "12345"
        }
        assertThrows<SQLIntegrityConstraintViolationException> { dao.insertCodes(testobj) }
    }
}
