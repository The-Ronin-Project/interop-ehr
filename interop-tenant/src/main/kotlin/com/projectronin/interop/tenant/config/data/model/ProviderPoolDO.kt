package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

/**
 * Entity definition for the Provider-Pool data object.
 */
interface ProviderPoolDO : Entity<ProviderPoolDO> {
    val id: Long
    val tenantId: Int
    val providerId: String
    val poolId: String
}
