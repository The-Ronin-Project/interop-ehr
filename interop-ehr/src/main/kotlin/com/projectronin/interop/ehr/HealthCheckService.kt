package com.projectronin.interop.ehr

import com.projectronin.interop.tenant.config.model.Tenant

interface HealthCheckService {
    fun healthCheck(tenant: Tenant): Boolean
}
