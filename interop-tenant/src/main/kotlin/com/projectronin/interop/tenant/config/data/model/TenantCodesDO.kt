package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

interface TenantCodesDO : Entity<TenantCodesDO> {
    companion object : Entity.Factory<TenantCodesDO>()

    var tenantId: Int
    var bsaCode: String?
    var bmiCode: String?
    var stageCodes: String?
}
