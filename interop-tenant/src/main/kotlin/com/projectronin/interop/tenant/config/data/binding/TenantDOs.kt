package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.TenantDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.time
import org.ktorm.schema.varchar

/**
 * Table binding definition for [TenantDO] data objects.
 */
object TenantDOs : Table<TenantDO>("io_tenant") {
    val id = int("io_tenant_id").primaryKey().bindTo { it.id }
    val mnemonic = varchar("mnemonic").bindTo { it.mnemonic }
    val name = varchar("full_name").bindTo { it.name }
    val ehr = int("io_ehr_id").references(EhrDOs) { it.ehr }
    val availableBatchStart = time("available_batch_start").bindTo { it.availableBatchStart }
    val availableBatchEnd = time("available_batch_end").bindTo { it.availableBatchEnd }
}
