package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

/**
 * Entity definition for Mirth-specific configuraiton for a tenant
 */
interface MirthTenantConfigDO : Entity<MirthTenantConfigDO> {
    companion object : Entity.Factory<MirthTenantConfigDO>()

    var tenant: TenantDO
    var locationIds: String
}
