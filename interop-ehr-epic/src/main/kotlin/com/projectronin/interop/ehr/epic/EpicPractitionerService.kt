package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicFindPractitionersResponse
import com.projectronin.interop.ehr.model.FindPractitionersResponse
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

@Component
class EpicPractitionerService(epicClient: EpicClient) : PractitionerService, EpicPagingService(epicClient) {
    private val practitionerSearchUrlPart = "/api/FHIR/R4/PractitionerRole"

    override fun findPractitionersByLocation(tenant: Tenant, locationIds: List<String>): FindPractitionersResponse {
        /*
        Epic has a problem handling multiple locations in 1 call, so in the meantime we're sending each location
        individually.  Leaving the old code here in case they fix it and we can switch back.
        See https://sherlock.epic.com/default.aspx?view=slg/home#id=6412518&rv=0

        val locationsParameter = locationIds.joinToString(separator = ",")
        val parameters = mapOf("_include" to "PractitionerRole:practitioner", "location" to locationsParameter)

        return getBundleWithPaging(tenant, practitionerSearchUrlPart, parameters, ::EpicFindPractitionersResponse)
        */
        val practitionerResponses = locationIds.map {
            val parameters = mapOf("_include" to "PractitionerRole:practitioner", "location" to it)
            getBundleWithPaging(tenant, practitionerSearchUrlPart, parameters, ::EpicFindPractitionersResponse)
        }

        return mergeResponses(practitionerResponses, ::EpicFindPractitionersResponse)
    }
}
