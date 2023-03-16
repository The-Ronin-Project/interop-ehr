package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.OrganizationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Service providing access to [Organization]s within Epic
 */
@Component
class EpicOrganizationService(
    epicClient: EpicClient
) : OrganizationService, EpicFHIRService<Organization>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Organization"
    override val fhirResourceType = Organization::class.java

    override fun findOrganizationsByFHIRId(
        tenant: Tenant,
        organizationFHIRIds: List<String>
    ): List<Organization> {
        // Epic allows searching by multiple _ids at once
        val parameters = mapOf("_id" to organizationFHIRIds.toSet().joinToString(separator = ","))
        return getResourceListFromSearch(tenant, parameters)
    }
}
