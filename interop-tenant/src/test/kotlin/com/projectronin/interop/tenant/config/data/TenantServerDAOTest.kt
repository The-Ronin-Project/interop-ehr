package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.projectronin.interop.common.hl7.MessageType
import com.projectronin.interop.common.hl7.ProcessingID
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.tenant.config.data.model.TenantDO
import com.projectronin.interop.tenant.config.data.model.TenantServerDO
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class TenantServerDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder
    private val mockTenantDO1 =
        mockk<TenantDO> {
            every { id } returns 1002
            every { mnemonic } returns "tenant"
        }
    private val mockTenantDO2 =
        mockk<TenantDO> {
            every { id } returns 2001
            every { mnemonic } returns "tnanet"
        }

    @Test
    @DataSet(value = ["/dbunit/tenant-server/StartingTenantServer.yaml"], cleanAfter = true)
    fun `get works`() {
        val dao = TenantServerDAO(KtormHelper.database())
        val tenantServerList = dao.getTenantServers(mockTenantDO1.mnemonic)
        assertNotNull(tenantServerList)
        assertEquals(1, tenantServerList.size)
        val tenantServer = tenantServerList.first()
        assertEquals(1, tenantServer.id)
        assertEquals(1002, tenantServer.tenant.id)
        assertEquals(MessageType.MDM, tenantServer.messageType)
        assertEquals("127.0.0.1", tenantServer.address)
        assertEquals(8080, tenantServer.port)
        assertEquals(ProcessingID.NONPRODUCTIONTESTING, tenantServer.serverType)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-server/StartingTenantServer.yaml"], cleanAfter = true)
    fun `get by type works`() {
        val dao = TenantServerDAO(KtormHelper.database())
        val tenantServerList = dao.getTenantServers(mockTenantDO1.mnemonic, MessageType.MDM)
        assertNotNull(tenantServerList)
        assertEquals(1, tenantServerList.size)
        val tenantServer = tenantServerList.first()
        assertEquals(1, tenantServer.id)
        assertEquals(1002, tenantServer.tenant.id)
        assertEquals("127.0.0.1", tenantServer.address)
        assertEquals(MessageType.MDM, tenantServer.messageType)
        assertEquals(8080, tenantServer.port)
        assertEquals(ProcessingID.NONPRODUCTIONTESTING, tenantServer.serverType)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-server/StartingTenantServer.yaml"], cleanAfter = true)
    fun `get by id works`() {
        val dao = TenantServerDAO(KtormHelper.database())
        val tenantServer = dao.getTenantServer(1)
        assertNotNull(tenantServer)
        assertEquals(1, tenantServer?.id)
        assertEquals(1002, tenantServer?.tenant?.id)
        assertEquals(MessageType.MDM, tenantServer?.messageType)
        assertEquals("127.0.0.1", tenantServer?.address)
        assertEquals(8080, tenantServer?.port)
        assertEquals(ProcessingID.NONPRODUCTIONTESTING, tenantServer?.serverType)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-server/StartingTenantServer.yaml"], cleanAfter = true)
    fun `get by id works can return null`() {
        val dao = TenantServerDAO(KtormHelper.database())
        val tenantServer = dao.getTenantServer(2)
        assertNull(tenantServer)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-server/StartingTenantServer.yaml"], cleanAfter = true)
    fun `update works`() {
        val dao = TenantServerDAO(KtormHelper.database())
        val tenantServerDO =
            TenantServerDO {
                id = 1
                tenant = mockTenantDO1
                messageType = MessageType.MDM
                address = "changed!"
                port = 8080
                serverType = ProcessingID.NONPRODUCTIONTESTING
            }
        val tenantServer = dao.updateTenantServer(tenantServerDO)
        assertNotNull(tenantServer)
        assertEquals(1, tenantServer?.id)
        assertEquals(1002, tenantServer?.tenant?.id)
        assertEquals(MessageType.MDM, tenantServer?.messageType)
        assertEquals("changed!", tenantServer?.address)
        assertEquals(8080, tenantServer?.port)
        assertEquals(ProcessingID.NONPRODUCTIONTESTING, tenantServer?.serverType)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-server/StartingTenantServer.yaml"], cleanAfter = true)
    fun `update won't update a non-existent server`() {
        val dao = TenantServerDAO(KtormHelper.database())
        val tenantServerDO =
            TenantServerDO {
                id = 2
                tenant = mockTenantDO2
                messageType = MessageType.MDM
                address = "changed!"
                port = 8080
                serverType = ProcessingID.NONPRODUCTIONTESTING
            }
        assertNull(dao.updateTenantServer(tenantServerDO))
        val tenantServer = dao.getTenantServer(1)
        assertNotNull(tenantServer)
        assertEquals("127.0.0.1", tenantServer?.address)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-server/StartingTenantServer.yaml"], cleanAfter = true)
    fun `insert works`() {
        val dao = TenantServerDAO(KtormHelper.database())
        val tenantServerDO =
            TenantServerDO {
                tenant = mockTenantDO2
                messageType = MessageType.MDM
                address = "www.zombo.com"
                port = 8080
                serverType = ProcessingID.NONPRODUCTIONTESTING
            }
        val tenantServer = dao.insertTenantServer(tenantServerDO)
        assertNotNull(tenantServer)
        assertNotNull(tenantServer.id)
        assertEquals(mockTenantDO2.id, tenantServer.tenant.id)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant-server/StartingTenantServer.yaml"], cleanAfter = true)
    fun `insert works fails on duplicate`() {
        val dao = TenantServerDAO(KtormHelper.database())
        val tenantServerDO =
            TenantServerDO {
                tenant = mockTenantDO1
                messageType = MessageType.MDM
                address = "www.zombo.com"
                port = 8080
                serverType = ProcessingID.NONPRODUCTIONTESTING
            }
        assertThrows<SQLIntegrityConstraintViolationException> { dao.insertTenantServer(tenantServerDO) }
    }
}
