package com.projectronin.interop.ehr

import com.projectronin.interop.tenant.config.model.Tenant

/***
 * Defines an EMR's service for setting a patient onboarded flag
 */
interface OnboardFlagService {
    fun setOnboardedFlag(
        tenant: Tenant,
        patientFhirID: String,
    ): Boolean
}
