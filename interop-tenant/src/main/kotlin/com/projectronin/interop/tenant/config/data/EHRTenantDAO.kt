package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.model.EHRTenantDO

interface EHRTenantDAO {
    fun update(ehrTenantDO: EHRTenantDO): Int

    fun insert(ehrTenantDO: EHRTenantDO): EHRTenantDO

    fun getByTenantMnemonic(tenantMnemonic: String): EHRTenantDO?

    fun getAll(): List<EHRTenantDO>
}
