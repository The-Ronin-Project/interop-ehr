package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.CernerTenantDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object CernerTenantDOs : Table<CernerTenantDO>("io_tenant_cerner") {
    var tenantId = int("io_tenant_id").bindTo { it.tenantId }
    val serviceEndpoint = varchar("service_endpoint").bindTo { it.serviceEndpoint }
    val patientMRNSystem = varchar("mrn_system").bindTo { it.patientMRNSystem }
    val authEndpoint = varchar("auth_endpoint").bindTo { it.authEndpoint }
    val messagePractitioner = varchar("message_practitioner").bindTo { it.messagePractitioner }
    val messageTopic = varchar("message_topic").bindTo { it.messageTopic }
    val messageCategory = varchar("message_category").bindTo { it.messageCategory }
    val messagePriority = varchar("message_priority").bindTo { it.messagePriority }
}
