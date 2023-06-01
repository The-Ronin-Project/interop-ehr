package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

interface TenantMDMConfigDO : Entity<TenantMDMConfigDO> {
    companion object : Entity.Factory<TenantMDMConfigDO>()

    var tenant: TenantDO
    var mdmDocumentTypeID: String
    var providerIdentifierSystem: String
    var receivingSystem: String
}
