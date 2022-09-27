package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's location service.
 */
interface LocationService {
    /**
     * Finds the [Location]s associated with the requested [tenant] and FHIR [locationIds].
     */
    fun getLocationsByFHIRId(tenant: Tenant, locationIds: List<String>): Map<String, Location>
}