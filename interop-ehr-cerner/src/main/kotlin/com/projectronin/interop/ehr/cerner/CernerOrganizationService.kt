package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.OrganizationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Service providing access to [Organization]s within Cerner
 */
@Component
class CernerOrganizationService(
    cernerClient: CernerClient
) : OrganizationService, CernerFHIRService<Organization>(cernerClient) {
    override val fhirURLSearchPart = "/Organization"
    override val fhirResourceType = Organization::class.java

    override fun findOrganizationsByFHIRId(
        tenant: Tenant,
        organizationFHIRIds: List<String>
    ): List<Organization> {
        // Cerner allows searching by multiple _ids at once
        val parameters = mapOf("_id" to organizationFHIRIds.toSet().joinToString(separator = ","))
        return getResourceListFromSearch(tenant, parameters)
    }
}
