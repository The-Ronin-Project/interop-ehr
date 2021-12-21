package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.model.FindPractitionersResponse
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's practitioner service.
 */
interface PractitionerService {
    /**
     * Finds the practitioners associated to the requested [tenant] and [FHIR location IDs][locationIds].
     */
    fun findPractitionersByLocation(tenant: Tenant, locationIds: List<String>): FindPractitionersResponse
}
