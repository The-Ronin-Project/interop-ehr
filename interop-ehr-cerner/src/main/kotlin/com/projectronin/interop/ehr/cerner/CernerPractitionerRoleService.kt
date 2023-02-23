package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.PractitionerRoleService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

@Component
class CernerPractitionerRoleService(
    cernerClient: CernerClient
) : PractitionerRoleService,
    CernerFHIRService<PractitionerRole>(cernerClient) {
    override val fhirURLSearchPart = ""
    override val fhirResourceType = PractitionerRole::class.java

    override fun getByID(tenant: Tenant, resourceFHIRId: String): PractitionerRole {
        return PractitionerRole()
    }

    override fun findPractitionersByLocation(tenant: Tenant, locationIds: List<String>): FindPractitionersResponse {
        return FindPractitionersResponse(
            Bundle(
                type = null
            )
        )
    }
}
