package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity
import java.time.LocalTime

/**
 * Entity definition for the Tenant data object.
 */
interface TenantDO : Entity<TenantDO> {
    companion object : Entity.Factory<TenantDO>()

    var id: Int
    var mnemonic: String
    var ehr: EhrDO
    var availableBatchStart: LocalTime?
    var availableBatchEnd: LocalTime?
}
