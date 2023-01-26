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
    @Value("\${cerner.fhir.batchSize:5}") private val batchSize: Int
) : LocationService, CernerFHIRService<Location>(cernerClient) {
    override val fhirURLSearchPart = "/Location"
    override val fhirResourceType = Location::class.java

    @Trace
    override fun getLocationsByFHIRId(tenant: Tenant, locationIds: List<String>): Map<String, Location> {
        // cerner also allows multiple ids to be searched, but chunk them out so we don't hit too many at once
        val chunkedIds = locationIds.toSet().chunked(batchSize)
        val locations = chunkedIds.map { idSubset ->
            val parameters = mapOf("_id" to idSubset.joinToString(separator = ","))
            getResourceListFromSearch(tenant, parameters)
        }.flatten()
        return locations.associateBy { it.id!!.value!! }
    }
}
