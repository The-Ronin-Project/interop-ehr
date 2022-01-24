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
        val locationsParameter = locationIds.joinToString(separator = ",")
        val parameters = mapOf("_include" to "PractitionerRole:practitioner", "location" to locationsParameter)

        return getBundleWithPaging(tenant, practitionerSearchUrlPart, parameters, ::EpicFindPractitionersResponse)
    }
}
