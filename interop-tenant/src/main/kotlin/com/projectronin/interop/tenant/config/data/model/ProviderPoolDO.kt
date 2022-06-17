package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

/**
 * Entity definition for the Provider-Pool data object.
 */
interface ProviderPoolDO : Entity<ProviderPoolDO> {
    companion object : Entity.Factory<ProviderPoolDO>()

    var id: Int
    var tenant: TenantDO
    var providerId: String
    var poolId: String
}
