package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's practitioner service.
 */
interface PractitionerRoleService : FHIRService<PractitionerRole> {
    fun findPractitionersByLocation(
        tenant: Tenant,
        locationIds: List<String>,
    ): FindPractitionersResponse
}
