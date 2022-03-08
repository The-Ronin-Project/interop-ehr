package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EhrDO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
        val testobj = EhrDO {
            vendorType = VendorType.EPIC
            clientId = "12345"
            publicKey = "public"
            privateKey = "private"
        }
        val result = dao.insert(testobj)
        assertTrue(result != null)
        assertTrue(result?.id != 0)
        assertEquals(result?.clientId, testobj.clientId)
        assertEquals(result?.publicKey, testobj.publicKey)
        assertEquals(result?.privateKey, testobj.privateKey)
    }

    /**
     * Testing insert that fails, returns null
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanAfter = true)
    fun `insert ehr fails`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj = EhrDO {
            vendorType = VendorType.EPIC
            clientId = "12345"
            publicKey = "public"
            privateKey = "private"
        }
        val result = dao.insert(testobj)
        assertNull(result)
    }

    /**
     * Testing updating existing value in db, returns result to match
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanAfter = true)
    fun `update ehr`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj = EhrDO {
            vendorType = VendorType.EPIC
            clientId = "56789"
            publicKey = "tomato"
            privateKey = "potato"
        }
        val result = dao.update(testobj)
        assertEquals(result?.clientId, testobj.clientId)
        assertEquals(result?.publicKey, testobj.publicKey)
        assertEquals(result?.privateKey, testobj.privateKey)
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
        assertEquals("12345", result?.get(0).clientId)
        assertEquals("public", result?.get(0).publicKey)
        assertEquals("private", result?.get(0).privateKey)
    }

    /**
     * Testing read empty db, clear out db before, returns empty
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanBefore = true)
    fun `read ehr no results`() {
        val dao = EhrDAO(KtormHelper.database())
        val result = dao.read()
        assertTrue(result.isEmpty())
    }

    /**
     * Testing updating empty db, returns null
     */
    @Test
    @DataSet(value = ["/dbunit/ehr/EHRVendor.yaml"], cleanBefore = true)
    fun `update ehr fails`() {
        val dao = EhrDAO(KtormHelper.database())
        val testobj = EhrDO {
            vendorType = VendorType.EPIC
            clientId = "56789"
            publicKey = "roses"
            privateKey = "peonies"
        }
        val result = dao.update(testobj)
        assertNull(result)
    }
}
