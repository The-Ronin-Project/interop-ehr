package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

interface TenantBlockedResourceDO : Entity<TenantBlockedResourceDO> {
    companion object : Entity.Factory<TenantBlockedResourceDO>()

    var tenantId: Int
    var resource: String
}
