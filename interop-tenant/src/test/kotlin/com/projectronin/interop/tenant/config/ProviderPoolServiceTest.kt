package com.projectronin.interop.tenant.config

import com.projectronin.interop.tenant.config.data.ProviderPoolDAO
import com.projectronin.interop.tenant.config.data.model.ProviderPoolDO
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProviderPoolServiceTest {
    private lateinit var providerPoolDAO: ProviderPoolDAO
    private lateinit var service: ProviderPoolService

    @BeforeEach
    fun setup() {
        providerPoolDAO = mockk()
        service = ProviderPoolService(providerPoolDAO)
    }

    @Test
    fun `provider-pool request supports unknown providers`() {
        val tenant = mockk<Tenant> {
            every { internalId } returns 100
        }
        every { providerPoolDAO.getPoolsForProviders(100, listOf("unknown1", "unknown2")) } returns listOf()

        val poolsByProvider = service.getPoolsForProviders(tenant, listOf("unknown1", "unknown2"))
        assertTrue(poolsByProvider.isEmpty())
    }

    @Test
    fun `provider-pool request supports known providers`() {
        val tenant = mockk<Tenant> {
            every { internalId } returns 100
        }
        every {
            providerPoolDAO.getPoolsForProviders(
                100,
                listOf("known1", "known2")
            )
        } returns listOf(
            ProviderPoolDO {
                id = 1
                providerId = "known1"
                poolId = "pool1"
            },
            ProviderPoolDO {
                id = 2
                providerId = "known2"
                poolId = "pool2"
            }
        )

        val poolsByProvider = service.getPoolsForProviders(tenant, listOf("known1", "known2"))
        assertEquals(2, poolsByProvider.size)
        assertEquals("pool1", poolsByProvider["known1"])
        assertEquals("pool2", poolsByProvider["known2"])
    }

    @Test
    fun `provider-pool request supports known and unknown providers`() {
        val tenant = mockk<Tenant> {
            every { internalId } returns 100
        }
        every {
            providerPoolDAO.getPoolsForProviders(
                100,
                listOf("known1", "unknown2")
            )
        } returns listOf(
            ProviderPoolDO {
                id = 1
                providerId = "known1"
                poolId = "pool1"
            }
        )

        val poolsByProvider = service.getPoolsForProviders(tenant, listOf("known1", "unknown2"))
        assertEquals(1, poolsByProvider.size)
        assertEquals("pool1", poolsByProvider["known1"])
    }
}
