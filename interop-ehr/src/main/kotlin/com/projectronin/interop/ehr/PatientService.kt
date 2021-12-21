package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality for an EHR's patient service.
 */
interface PatientService {
    /**
     * Finds the Bundle of patients associated to the requested
     * [tenant], [birthDate], [givenName], and [familyName] from an EHR tenant.
     */
    fun findPatient(tenant: Tenant, birthDate: String, givenName: String, familyName: String): Bundle<Patient>
}
