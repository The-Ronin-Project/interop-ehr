package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.OnboardFlagService
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

@Component
class CernerOnboardFlagService : OnboardFlagService {
    override fun setOnboardedFlag(tenant: Tenant, patientFhirID: String): Boolean {
        return true
    }
}
