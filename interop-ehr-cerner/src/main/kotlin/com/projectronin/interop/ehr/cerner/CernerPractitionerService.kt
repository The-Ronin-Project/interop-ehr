package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CernerPractitionerService(
    cernerClient: CernerClient
) : PractitionerService, CernerFHIRService<Practitioner>(cernerClient) {
    override val fhirURLSearchPart = "/Practitioner"
    override val fhirResourceType = Practitioner::class.java

    @Trace
    override fun getPractitioner(tenant: Tenant, practitionerFHIRId: String): Practitioner = getByID(tenant, practitionerFHIRId)

    @Trace
    override fun getPractitionerByProvider(tenant: Tenant, providerId: String): Practitioner {
        val parameters = mapOf(
            "_id" to providerId
        )
        return getResourceListFromSearch(tenant, parameters).single()
    }

    override fun findPractitionersByLocation(tenant: Tenant, locationIds: List<String>): FindPractitionersResponse {
        throw NotImplementedError("Cerner does not support the PractitionerRole resource that connects Practitioners with Locations")
    }
}