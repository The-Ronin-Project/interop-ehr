package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.EpicTenantDAO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EHRTenantDAOFactoryTest {
    private val epicTenantDAO = mockk<EpicTenantDAO>()

    @Test
    fun `returns correctly`() {
        val tenantDO = mockk<TenantDO> {
            every { ehr.vendorType } returns VendorType.EPIC
        }
        val dao = EHRTenantDAOFactory(epicTenantDAO).getEHRTenantDAO(tenantDO)
        assertEquals(epicTenantDAO, dao)
    }
}
