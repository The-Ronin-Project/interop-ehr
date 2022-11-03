package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.LocationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class EpicLocationService(epicClient: EpicClient) : LocationService, EpicFHIRService<Location>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Location"
    override val fhirResourceType = Location::class.java

    /**
     * Returns a Map of FHIR ID to [Location] resource. Requires a list of location FHIR IDs as input
     */
    @Trace
    override fun getLocationsByFHIRId(tenant: Tenant, locationIds: List<String>): Map<String, Location> {
        // Epic allows multiple IDs to be searched at once
        val parameters = mapOf("_id" to locationIds.toSet().joinToString(separator = ","))
        return getResourceListFromSearch(tenant, parameters).associateBy { it.id!!.value }
    }
}
