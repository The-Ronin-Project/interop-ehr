package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EpicPractitionerService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:1}") private val batchSize: Int, // This is currently ignored.  See comment below.
) : PractitionerService,
    EpicPagingService(epicClient) {
    private val practitionerSearchUrlPart = "/api/FHIR/R4/PractitionerRole"

    override fun findPractitionersByLocation(tenant: Tenant, locationIds: List<String>): FindPractitionersResponse {
        // Epic has a problem handling multiple locations in 1 call, so in the meantime force batch size to 1.
        // See https://sherlock.epic.com/default.aspx?view=slg/home#id=6412518&rv=0
        val practitionerResponses = locationIds.chunked(1) {
            val locationsParameter = it.joinToString(separator = ",")
            val parameters = mapOf(
                "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                "location" to locationsParameter
            )
            getBundleWithPaging(tenant, practitionerSearchUrlPart, parameters)
        }
        return FindPractitionersResponse(mergeResponses(practitionerResponses))
    }
}
