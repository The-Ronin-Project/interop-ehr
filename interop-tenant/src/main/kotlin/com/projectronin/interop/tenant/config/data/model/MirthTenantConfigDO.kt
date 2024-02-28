package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity
import java.time.OffsetDateTime

/**
 * Entity definition for Mirth-specific configuration for a tenant
 */
interface MirthTenantConfigDO : Entity<MirthTenantConfigDO> {
    companion object : Entity.Factory<MirthTenantConfigDO>()

    var tenant: TenantDO
    var locationIds: String
    var lastUpdated: OffsetDateTime?
    var blockedResources: String?
    var allowedResources: String?
}
