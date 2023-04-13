package com.projectronin.interop.ehr

import com.projectronin.interop.tenant.config.model.Tenant

interface HealthCheckService {
    /**
     * Checks the health of the given [tenant]
     */
    fun healthCheck(tenant: Tenant): Boolean

    /**
     * Checks the health of all tenants flagged as monitored
     */
    fun healthCheck(): Map<Tenant, Boolean>
}
