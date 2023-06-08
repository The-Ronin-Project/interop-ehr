package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.TenantBlockedResourceDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object TenantBlockedResourceDOs : Table<TenantBlockedResourceDO>("io_tenant_blocked_resource") {
    var tenantId = int("io_tenant_id").bindTo { it.tenantId }
    var resource = varchar("resource").bindTo { it.resource }
}
