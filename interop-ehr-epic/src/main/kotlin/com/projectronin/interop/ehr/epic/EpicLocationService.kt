package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.LocationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

@Component
class EpicLocationService(private val epicClient: EpicClient) : LocationService {
    private val locationSearchUrlPart = "/api/FHIR/R4/Location"

    /**
     * Returns a Map of FHIR ID to [Location] resource. Requires a list of location FHIR IDs as input
     */
    override fun getLocationsByFHIRId(tenant: Tenant, locationIds: List<String>): Map<String, Location> {
        // Epic allows multiple IDs to be searched at once
        val parameters = mapOf("_id" to locationIds.toSet().joinToString(separator = ","))
        return runBlocking {
            epicClient.get(tenant, locationSearchUrlPart, parameters).body<Bundle>()
        }.toListOfType<Location>().associateBy { it.id!!.value }
    }
}
