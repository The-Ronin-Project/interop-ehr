package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.exception.NoEHRFoundException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class EhrDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    /**
     * Testing insert, returns result to match
     */
    @Test
    fun `insert ehr`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj =
            EhrDO {
                vendorType = VendorType.EPIC
                clientId = "12345"
                publicKey = "public"
                privateKey = "private"
            }
        val result = dao.insert(testobj)
        assertTrue(result.id != 0)
        assertEquals(result.clientId, testobj.clientId)
        assertEquals(result.publicKey, testobj.publicKey)
        assertEquals(result.privateKey, testobj.privateKey)
    }

    /**
     * Testing insert that fails, returns null
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanAfter = true)
    fun `insert ehr fails`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj =
            EhrDO {
                instanceName = "Epic Sandbox"
                vendorType = VendorType.EPIC
                clientId = "12345"
                publicKey = "public"
                privateKey = "private"
            }
        assertThrows<SQLIntegrityConstraintViolationException> { dao.insert(testobj) }
    }

    /**
     * Testing insert of a second EHR instance.
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanAfter = true)
    fun `insert new ehr instance`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj =
            EhrDO {
                instanceName = "Epic Prod"
                vendorType = VendorType.EPIC
                clientId = "12345"
                publicKey = "public"
                privateKey = "private"
            }
        val result = dao.insert(testobj)
        assertEquals(result.clientId, testobj.clientId)
        assertEquals(result.instanceName, testobj.instanceName)
        assertEquals(result.publicKey, testobj.publicKey)
        assertEquals(result.privateKey, testobj.privateKey)
    }

    /**
     * Testing updating existing value in db, returns result to match
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanAfter = true)
    fun `update ehr`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj =
            EhrDO {
                id = 101
                vendorType = VendorType.EPIC
                instanceName = "Epic Sandbox"
                clientId = "56789"
                publicKey = "tomato"
                privateKey = "potato"
            }
        val result = dao.update(testobj)
        assertEquals(result.clientId, testobj.clientId)
        assertEquals(result.publicKey, testobj.publicKey)
        assertEquals(result.privateKey, testobj.privateKey)
    }

    /**
     * Testing read, returns row from prepopulated db
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanAfter = true)
    fun `read ehr`() {
        val dao = EhrDAO(KtormHelper.database())
        val result = dao.read()
        assertTrue(result.size == 1)
        assertEquals("12345", result[0].clientId)
        assertEquals("public", result[0].publicKey)
        assertEquals("private", result[0].privateKey)
    }

    /**
     * Testing read empty db, clear out db before, returns empty
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/NoEHRVendor.yaml"], cleanBefore = true)
    fun `read ehr no results`() {
        val dao = EhrDAO(KtormHelper.database())
        val result = dao.read()
        assertTrue(result.isEmpty())
    }

    /**
     * Testing updating empty db, returns null
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/NoEHRVendor.yaml"], cleanBefore = true)
    fun `update ehr fails`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj =
            EhrDO {
                instanceName = "Epic fake"
                vendorType = VendorType.EPIC
                clientId = "56789"
                publicKey = "roses"
                privateKey = "peonies"
            }
        assertThrows<NoEHRFoundException> { dao.update(testobj) }
    }

    @Test
    fun `update handles exception`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj =
            mockk<EhrDO>(relaxed = true) {
                every { vendorType } throws RuntimeException()
            }
        assertThrows<RuntimeException> { dao.update(testobj) }
    }

    /**
     * Testing read, returns row from prepopulated db
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanAfter = true)
    fun `read by ehr`() {
        val dao = EhrDAO(KtormHelper.database())
        val result = dao.getByInstance("Epic Sandbox")
        assertNotNull(result)
        assertEquals("12345", result?.clientId)
        assertEquals("public", result?.publicKey)
        assertEquals("private", result?.privateKey)
    }
}
