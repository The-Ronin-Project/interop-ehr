package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.model.EHRTenantDO
import org.springframework.stereotype.Repository
/**
 * Provides data access operations for Cerner tenant data models.
 */
@Repository
class CernerTenantDAO() : EHRTenantDAO {
    override fun update(ehrTenantDO: EHRTenantDO): Int {
        TODO("Not yet implemented")
    }

    override fun insert(ehrTenantDO: EHRTenantDO): EHRTenantDO {
        TODO("Not yet implemented")
    }

    override fun getByTenantMnemonic(tenantMnemonic: String): EHRTenantDO? {
        TODO("Not yet implemented")
    }

    override fun getAll(): List<EHRTenantDO> {
        TODO("Not yet implemented")
    }
}
