package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.TenantMDMConfigDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object TenantMDMConfigDOs : Table<TenantMDMConfigDO>("io_tenant_mdm_config") {
    var tenantId = int("io_tenant_id").references(TenantDOs) { it.tenant }
    val mdmDocumentTypeID = varchar("mdm_document_type_id").bindTo { it.mdmDocumentTypeID }
    val providerIdentifierSystem = varchar("provider_identifier_system").bindTo { it.providerIdentifierSystem }
    val receivingSystem = varchar("receiving_system").bindTo { it.receivingSystem }
}
