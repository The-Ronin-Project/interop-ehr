package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's practitioner service.
 */
interface LocationService {
    /**
     * Finds the practitioners associated to the requested [tenant] and [FHIR location IDs][locationIds].
     */
    fun getLocationsByFHIRId(tenant: Tenant, locationIds: List<String>): Map<String, Location>
}
