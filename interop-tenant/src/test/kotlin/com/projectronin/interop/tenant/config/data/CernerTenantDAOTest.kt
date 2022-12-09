package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CernerTenantDAOTest {
    // TODO: Tests for coverage pending implementation
    @Test
    fun `test stubs`() {
        val dao = CernerTenantDAO()
        val tenant = EpicTenantDO()
        assertThrows<NotImplementedError> { dao.insert(tenant) }
        assertThrows<NotImplementedError> { dao.update(tenant) }
        assertThrows<NotImplementedError> { dao.getByTenantMnemonic("cerner") }
        assertThrows<NotImplementedError> { dao.getAll() }
    }
}
