package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EpicPractitionerService(
    epicClient: EpicClient,
    private val practitionerRoleService: EpicPractitionerRoleService,
    @Value("\${epic.fhir.batchSize:1}") private val batchSize: Int, // This is currently ignored.  See comment below.
) : PractitionerService,
    EpicFHIRService<Practitioner>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Practitioner"
    override val fhirResourceType = Practitioner::class.java

    @Trace
    override fun getPractitioner(tenant: Tenant, practitionerFHIRId: String): Practitioner =
        getByID(tenant, practitionerFHIRId)

    @Trace
    override fun getPractitionerByProvider(tenant: Tenant, providerId: String): Practitioner {
        val parameters = mapOf("identifier" to "External|$providerId")

        return getResourceListFromSearch(tenant, parameters).single()
    }

    override fun findPractitionersByLocation(tenant: Tenant, locationIds: List<String>): FindPractitionersResponse {
        return practitionerRoleService.findPractitionersByLocation(tenant, locationIds)
    }
}
