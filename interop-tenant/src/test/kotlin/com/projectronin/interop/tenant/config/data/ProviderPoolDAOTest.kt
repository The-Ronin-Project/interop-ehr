package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
@DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
class ProviderPoolDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    fun `no pools for tenant`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1002, listOf("provider"))
        assertTrue(poolsByProvider.isEmpty())
    }

    @Test
    fun `no pools for providers`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1001, listOf("provider4", "provider5"))
        assertTrue(poolsByProvider.isEmpty())
    }

    @Test
    fun `some providers with pools`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1001, listOf("provider1", "provider2", "provider5"))
        assertEquals(2, poolsByProvider.size)
        assertEquals("pool1", poolsByProvider["provider1"])
        assertEquals("pool2", poolsByProvider["provider2"])
    }

    @Test
    fun `all providers with pools`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1001, listOf("provider1", "provider2", "provider3"))
        assertEquals(3, poolsByProvider.size)
        assertEquals("pool1", poolsByProvider["provider1"])
        assertEquals("pool2", poolsByProvider["provider2"])
        assertEquals("pool2", poolsByProvider["provider3"])
    }

    @Test
    fun `provider has pool in different tenant`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1002, listOf("provider1", "provider2"))
        assertTrue(poolsByProvider.isEmpty())
    }
}
