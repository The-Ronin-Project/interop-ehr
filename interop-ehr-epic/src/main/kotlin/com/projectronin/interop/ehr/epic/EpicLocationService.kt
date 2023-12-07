package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.LocationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EpicLocationService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:10}") batchSize: Int,
) : LocationService, EpicFHIRService<Location>(epicClient, batchSize) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Location"
    override val fhirResourceType = Location::class.java

    @Deprecated("Use getByIDs")
    @Trace
    override fun getLocationsByFHIRId(
        tenant: Tenant,
        locationIds: List<String>,
    ): Map<String, Location> {
        return getByIDs(tenant, locationIds)
    }
}
