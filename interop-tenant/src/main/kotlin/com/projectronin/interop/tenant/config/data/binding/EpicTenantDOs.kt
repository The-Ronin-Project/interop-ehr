package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Table binding definition for [EpicTenantDO] data objects.
 */
object EpicTenantDOs : Table<EpicTenantDO>("io_tenant_epic") {
    var tenantId = int("io_tenant_id").bindTo { it.tenantId }
    val release = varchar("release_version").bindTo { it.release }
    val serviceEndpoint = varchar("service_endpoint").bindTo { it.serviceEndpoint }
    val ehrUserId = varchar("ehr_user_id").bindTo { it.ehrUserId }
    val messageType = varchar("message_type").bindTo { it.messageType }
    val practitionerProviderSystem =
        varchar("practitioner_provider_system").bindTo { it.practitionerProviderSystem }
    val practitionerUserSystem = varchar("practitioner_user_system").bindTo { it.practitionerUserSystem }
    val mrnSystem = varchar("mrn_system").bindTo { it.mrnSystem }
    val hsi = varchar("hsi").bindTo { it.hsi }
}
