package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EpicPractitionerService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:1}") private val batchSize: Int, // This is currently ignored.  See comment below.
) : PractitionerService,
    EpicPagingService(epicClient) {
    private val practitionerSearchUrlPart = "/api/FHIR/R4/Practitioner"
    private val practitionerRoleSearchUrlPart = "/api/FHIR/R4/PractitionerRole"

    override fun getPractitioner(tenant: Tenant, practitionerFHIRId: String): Practitioner = runBlocking {
        epicClient.get(tenant, "$practitionerSearchUrlPart/$practitionerFHIRId").body()
    }

    override fun getPractitionerByProvider(tenant: Tenant, providerId: String): Practitioner {
        val parameters = mapOf("identifier" to "External|$providerId")
        val bundle = runBlocking { epicClient.get(tenant, practitionerSearchUrlPart, parameters).body<Bundle>() }

        return bundle.toListOfType<Practitioner>().single()
    }

    override fun findPractitionersByLocation(tenant: Tenant, locationIds: List<String>): FindPractitionersResponse {
        // Epic has a problem handling multiple locations in 1 call, so in the meantime force batch size to 1.
        // See https://sherlock.epic.com/default.aspx?view=slg/home#id=6412518&rv=0
        val practitionerResponses = locationIds.chunked(1) {
            val locationsParameter = it.joinToString(separator = ",")
            val parameters = mapOf(
                "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                "location" to locationsParameter
            )
            getBundleWithPaging(tenant, practitionerRoleSearchUrlPart, parameters)
        }
        return FindPractitionersResponse(mergeResponses(practitionerResponses))
    }
}
