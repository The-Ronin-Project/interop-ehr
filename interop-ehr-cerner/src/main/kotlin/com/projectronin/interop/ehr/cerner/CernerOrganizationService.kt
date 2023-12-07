package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.OrganizationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Service providing access to [Organization]s within Cerner
 */
@Component
class CernerOrganizationService(
    cernerClient: CernerClient,
    @Value("\${cerner.fhir.batchSize:10}") private val batchSize: Int,
) : OrganizationService, CernerFHIRService<Organization>(cernerClient, batchSize) {
    override val fhirURLSearchPart = "/Organization"
    override val fhirResourceType = Organization::class.java

    @Deprecated("Use getByIDs")
    @Trace
    override fun findOrganizationsByFHIRId(
        tenant: Tenant,
        organizationFHIRIds: List<String>,
    ): List<Organization> {
        return getByIDs(tenant, organizationFHIRIds).values.toList()
    }
}
