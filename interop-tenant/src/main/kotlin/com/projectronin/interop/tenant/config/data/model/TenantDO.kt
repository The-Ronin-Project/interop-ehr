package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity
import java.time.LocalTime
import java.time.ZoneId

/**
 * Entity definition for the Tenant data object.
 * @property id The ID of the backing data store for this Tenant.
 * @property mnemonic The tenant's mnemonic.
 * @property name The tenant's full name.
 * @property ehr EHR data object provides EHR ID, vendorType, access keys, etc.
 * @property availableBatchStart Sets an availability start time for the tenant EHR.
 * @property availableBatchStart Sets an availability end time for the tenant EHR.
 */
interface TenantDO : Entity<TenantDO> {
    companion object : Entity.Factory<TenantDO>()

    var id: Int
    var mnemonic: String
    var name: String
    var ehr: EhrDO
    var availableBatchStart: LocalTime?
    var availableBatchEnd: LocalTime?
    var timezone: ZoneId
}
