package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.common.ktorm.binding.utcDateTime
import com.projectronin.interop.tenant.config.data.binding.TenantDOs.bindTo
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import com.projectronin.interop.tenant.config.data.model.MirthTenantConfigDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Table binding definition for [EpicTenantDO] data objects.
 */
object MirthTenantConfigDOs : Table<MirthTenantConfigDO>("io_mirth_tenant_config") {
    var tenantId = int("io_tenant_id").references(TenantDOs) { it.tenant }
    val locationIds = varchar("location_ids").bindTo { it.locationIds }
    val loadTimestamp = utcDateTime("load_last_run").bindTo { it.lastUpdated }
    val blockedResources = varchar("blocked_resources").bindTo { it.blockedResources }
    val allowedResources = varchar("allowed_resources").bindTo { it.allowedResources }
}
