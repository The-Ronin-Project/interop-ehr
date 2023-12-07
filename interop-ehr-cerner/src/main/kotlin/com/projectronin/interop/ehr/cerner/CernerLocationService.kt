package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.LocationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CernerLocationService(
    cernerClient: CernerClient,
    @Value("\${cerner.fhir.batchSize:10}") private val batchSize: Int,
) : LocationService, CernerFHIRService<Location>(cernerClient, batchSize) {
    override val fhirURLSearchPart = "/Location"
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
