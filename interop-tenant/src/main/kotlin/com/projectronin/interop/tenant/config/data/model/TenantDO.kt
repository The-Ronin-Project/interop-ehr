package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity
import java.time.LocalTime

/**
 * Entity definition for the Tenant data object.
 */
interface TenantDO : Entity<TenantDO> {
    val id: Int
    val mnemonic: String
    val ehr: EhrDO
    val availableBatchStart: LocalTime?
    val availableBatchEnd: LocalTime?
}
