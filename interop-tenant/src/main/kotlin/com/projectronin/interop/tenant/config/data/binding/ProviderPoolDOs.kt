package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.ProviderPoolDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object ProviderPoolDOs : Table<ProviderPoolDO>("io_tenant_provider_pool") {
    private val id = long("io_tenant_provider_pool_id").primaryKey().bindTo { it.id }
    val tenantId = int("io_tenant_id").bindTo { it.tenantId }
    val providerId = varchar("provider_id").bindTo { it.providerId }
    private val poolId = varchar("pool_id").bindTo { it.poolId }
}
