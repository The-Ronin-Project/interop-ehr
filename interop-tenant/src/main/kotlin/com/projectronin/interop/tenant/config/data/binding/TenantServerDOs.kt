package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.common.hl7.MessageType
import com.projectronin.interop.common.hl7.ProcessingID
import com.projectronin.interop.tenant.config.data.model.TenantServerDO
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Table binding definition for [TenantServerDO] data objects.
 */
object TenantServerDOs : Table<TenantServerDO>("io_tenant_server") {
    val id = int("io_server_id").primaryKey().bindTo { it.id }
    var tenantId = int("io_tenant_id").references(TenantDOs) { it.tenant }
    val messageType = enum<MessageType>("message_type").bindTo { it.messageType }
    val address = varchar("address").bindTo { it.address }
    val port = int("port").bindTo { it.port }
    val serverType = enum<ProcessingID>("server_type").bindTo { it.serverType }
}
