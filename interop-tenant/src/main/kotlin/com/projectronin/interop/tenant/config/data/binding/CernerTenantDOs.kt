package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.CernerTenantDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object CernerTenantDOs : Table<CernerTenantDO>("io_tenant_cerner") {
    var tenantId = int("io_tenant_id").bindTo { it.tenantId }
    val serviceEndpoint = varchar("service_endpoint").bindTo { it.serviceEndpoint }
    val patientMRNSystem = varchar("mrn_system").bindTo { it.patientMRNSystem }
}
