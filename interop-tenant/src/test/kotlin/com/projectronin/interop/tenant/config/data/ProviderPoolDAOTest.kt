package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.ProviderPoolDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class ProviderPoolDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    fun `no pools for tenant`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1002, listOf("provider"))
        assertTrue(poolsByProvider.isEmpty())
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    fun `no pools for providers`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1001, listOf("provider4", "provider5"))
        assertTrue(poolsByProvider.isEmpty())
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    fun `some providers with pools`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1001, listOf("provider1", "provider2", "provider5"))
        assertEquals(2, poolsByProvider.size)
        assertEquals("pool1", poolsByProvider["provider1"])
        assertEquals("pool2", poolsByProvider["provider2"])
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    fun `all providers with pools`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1001, listOf("provider1", "provider2", "provider3"))
        assertEquals(3, poolsByProvider.size)
        assertEquals("pool1", poolsByProvider["provider1"])
        assertEquals("pool2", poolsByProvider["provider2"])
        assertEquals("pool2", poolsByProvider["provider3"])
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    fun `provider has pool in different tenant`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val poolsByProvider = dao.getPoolsForProviders(1002, listOf("provider1", "provider2"))
        assertTrue(poolsByProvider.isEmpty())
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/provider-pool/ExpectedProviderPoolsAfterInsert.yaml"], ignoreCols = ["io_tenant_provider_pool_id"])
    fun `insert provider`() {
        val providerdao = ProviderPoolDAO(KtormHelper.database())
        val ehrDO = mockk<EhrDO> {
            every { id } returns 101
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "12345"
            every { publicKey } returns "public"
            every { privateKey } returns "private"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1001
            every { mnemonic } returns "mdaoc"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }
        val insertedProviderPoolDO = ProviderPoolDO {
            id = 0
            tenantId = tenantDO
            providerId = "provider5"
            poolId = "pool4"
        }
        val actualProviderPoolDO = providerdao.insert(insertedProviderPoolDO)
        assertSame(insertedProviderPoolDO, actualProviderPoolDO)
        assertNotEquals(0, insertedProviderPoolDO.id)
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"])
    fun `insert provider fails`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val ehrDO = mockk<EhrDO> {
            every { id } returns 1
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "client"
            every { publicKey } returns "public"
            every { privateKey } returns "private"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1
            every { mnemonic } returns "blah"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }
        val insertedProviderPoolDO = ProviderPoolDO {
            id = 0
            tenantId = tenantDO
            providerId = "provider1"
            poolId = "poolId"
        }
        val inserted = dao.insert(insertedProviderPoolDO)
        assertSame(null, inserted)
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    @ExpectedDataSet("/dbunit/provider-pool/ExpectedProviderPoolsAfterUpdate.yaml")
    fun `update provider`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val ehrDO = mockk<EhrDO> {
            every { id } returns 101
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "client"
            every { publicKey } returns "public"
            every { privateKey } returns "private"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1001
            every { mnemonic } returns "blah"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }
        val updatedProviderPoolDO = ProviderPoolDO {
            id = 10001
            tenantId = tenantDO
            providerId = "provider20"
            poolId = "pool1"
        }
        val updated = dao.update(updatedProviderPoolDO)
        assertSame(1, updated)
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"])
    fun `update provider fails`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val ehrDO = mockk<EhrDO> {
            every { id } returns 101
            every { vendorType } returns VendorType.EPIC
            every { clientId } returns "client"
            every { publicKey } returns "public"
            every { privateKey } returns "private"
        }
        val tenantDO = mockk<TenantDO> {
            every { id } returns 1001
            every { mnemonic } returns "blah"
            every { ehr } returns ehrDO
            every { availableBatchStart } returns null
            every { availableBatchEnd } returns null
        }
        val updatedProviderPoolDO = ProviderPoolDO {
            id = 10001
            tenantId = tenantDO
            providerId = "provider2"
            poolId = "pool4"
        }

        val updated = dao.update(updatedProviderPoolDO)
        assertEquals(null, updated)
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    @ExpectedDataSet("/dbunit/provider-pool/ExpectedProviderPoolsAfterDelete.yaml")
    fun `delete provider`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val deleted = dao.delete((10001))
        assertEquals(1, deleted)
    }

    @Test
    @DataSet(value = ["/dbunit/provider-pool/ProviderPools.yaml"], cleanAfter = true)
    fun `no provider to delete`() {
        val dao = ProviderPoolDAO(KtormHelper.database())
        val deleted = dao.delete((1))
        assertEquals(0, deleted)
    }
}
