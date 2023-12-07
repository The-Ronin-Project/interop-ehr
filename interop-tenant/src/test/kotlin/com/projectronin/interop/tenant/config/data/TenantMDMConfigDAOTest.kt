package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import com.projectronin.interop.tenant.config.data.model.TenantMDMConfigDO
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class TenantMDMConfigDAOTest {
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
    @DataSet(value = ["/dbunit/tenant-mdm-config/TenantMDMConfig.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works`() {
        val dao = TenantMDMConfigDAO(KtormHelper.database())
        val mdmConfigs = dao.getByTenantMnemonic("apposnd")
        assertNotNull(mdmConfigs)
        assertEquals("12345", mdmConfigs?.mdmDocumentTypeID)
        assertEquals("system", mdmConfigs?.providerIdentifierSystem)
        assertEquals("application", mdmConfigs?.receivingSystem)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-mdm-config/TenantMDMConfig.yaml"], cleanAfter = true)
    fun `getByTenantMnemonic works when not found`() {
        val dao = TenantMDMConfigDAO(KtormHelper.database())
        val mdmConfigs = dao.getByTenantMnemonic("blooop")
        assertNull(mdmConfigs)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-mdm-config/TenantMDMConfig.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant-mdm-config/ExpectedAfterUpdate.yaml"])
    fun `updateConfig works`() {
        val dao = TenantMDMConfigDAO(KtormHelper.database())
        val mdmDocumentTypeID = "54321"
        val providerIdentifierSystem = "system2"
        val receivingSystem = "application2"

        val testobj =
            TenantMDMConfigDO {
                this.tenant = tenantDO
                this.mdmDocumentTypeID = mdmDocumentTypeID
                this.providerIdentifierSystem = providerIdentifierSystem
                this.receivingSystem = receivingSystem
            }
        val result = dao.updateConfig(testobj)!!
        assertEquals(tenantDO.id, result.tenant.id)
        assertEquals(mdmDocumentTypeID, result.mdmDocumentTypeID)
        assertEquals(providerIdentifierSystem, result.providerIdentifierSystem)
        assertEquals(receivingSystem, result.receivingSystem)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-mdm-config/TenantMDMConfig.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant-mdm-config/TenantMDMConfig.yaml"])
    fun `updateConfig fails correctly`() {
        val dao = TenantMDMConfigDAO(KtormHelper.database())
        val testobj =
            TenantMDMConfigDO {
                tenant = tenantDO2
                mdmDocumentTypeID = "54321"
                providerIdentifierSystem = "system"
                receivingSystem = "application"
            }
        val result = dao.updateConfig(testobj)
        assertNull(result)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-mdm-config/TenantMDMConfig.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/tenant-mdm-config/ExpectedAfterInsert.yaml"])
    fun `insert works correctly`() {
        val dao = TenantMDMConfigDAO(KtormHelper.database())
        val testobj =
            TenantMDMConfigDO {
                tenant = tenantDO2
                mdmDocumentTypeID = "23456"
                providerIdentifierSystem = "system2"
                receivingSystem = "application2"
            }
        val result = dao.insertConfig(testobj)
        assertEquals(testobj.mdmDocumentTypeID, result.mdmDocumentTypeID)
        assertEquals(testobj.tenant.id, result.tenant.id)
        assertEquals(testobj.providerIdentifierSystem, result.providerIdentifierSystem)
        assertEquals(testobj.receivingSystem, result.receivingSystem)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-mdm-config/TenantMDMConfig.yaml"], cleanAfter = true)
    fun `insert fails correctly`() {
        val dao = TenantMDMConfigDAO(KtormHelper.database())
        val fakeTenantDO =
            mockk<TenantDO> {
                every { id } returns -1
                every { mnemonic } returns "no"
                every { ehr } returns ehrDO
                every { availableBatchStart } returns null
                every { availableBatchEnd } returns null
            }
        val testobj =
            TenantMDMConfigDO {
                tenant = fakeTenantDO
                mdmDocumentTypeID = "no"
            }
        assertThrows<SQLIntegrityConstraintViolationException> { dao.insertConfig(testobj) }
    }
}
