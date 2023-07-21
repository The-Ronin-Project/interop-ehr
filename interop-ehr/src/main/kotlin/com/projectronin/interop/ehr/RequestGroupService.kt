package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's request group service.
 */
interface RequestGroupService : FHIRService<RequestGroup> {
    /**
     * Finds the [RequestGroup]s associated with the requested [tenant] and FHIR [requestGroupIds].
     */
    @Deprecated("Use getByIDs")
    fun getRequestGroupByFHIRId(tenant: Tenant, requestGroupIds: List<String>): Map<String, RequestGroup>
}
