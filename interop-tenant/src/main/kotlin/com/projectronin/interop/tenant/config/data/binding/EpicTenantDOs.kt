package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Table binding definition for [EpicTenantDO] data objects.
 */
object EpicTenantDOs : Table<EpicTenantDO>("io_tenant_epic") {
    val tenantId = int("io_tenant_id").bindTo { it.tenantId }
    private val release = varchar("release_version").bindTo { it.release }
    private val serviceEndpoint = varchar("service_endpoint").bindTo { it.serviceEndpoint }
    private val ehrUserId = varchar("ehr_user_id").bindTo { it.ehrUserId }
    private val messageType = varchar("message_type").bindTo { it.messageType }
}
